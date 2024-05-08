package cn.opentp.gossip.gms;

import cn.opentp.gossip.Gossiper;
import cn.opentp.gossip.enums.ApplicationStateEnum;
import cn.opentp.gossip.io.util.FileUtils;
import cn.opentp.gossip.util.BoundedStatsDeque;
import cn.opentp.gossip.util.FBUtilities;
import cn.opentp.gossip.util.SocketAddressUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class FailureDetector implements IFailureDetector, FailureDetectorMBean {
    public static final String MBEAN_NAME = "org.apache.cassandra.net:type=FailureDetector";
    public static final IFailureDetector instance = new FailureDetector();
    private static Logger logger_ = LoggerFactory.getLogger(FailureDetector.class);
    private static final int sampleSize_ = 1000;
    private static int phiConvictThreshold_;

    private Map<InetSocketAddress, ArrivalWindow> arrivalSamples_ = new Hashtable<InetSocketAddress, ArrivalWindow>();
    private List<IFailureDetectionEventListener> fdEvntListeners_ = new CopyOnWriteArrayList<IFailureDetectionEventListener>();

    public FailureDetector() {
        phiConvictThreshold_ = Gossiper.getPhiConvictThreshold();
        // Register this instance with JMX
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            mbs.registerMBean(this, new ObjectName(MBEAN_NAME));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public boolean isAlive(InetSocketAddress ep) {
        if (ep.equals(FBUtilities.getBroadcastAddress()))
            return true;

        EndpointState epState = GossiperApp.instance.getEndpointStateForEndpoint(ep);
        // we could assert not-null, but having isAlive fail screws a node over so badly that
        // it's worth being defensive here so minor bugs don't cause disproportionate
        // badness.  (See CASSANDRA-1463 for an example).
        if (epState == null)
            logger_.error("unknown endpoint " + ep);
        return epState != null && epState.isAlive();
    }

    public void report(InetSocketAddress ep) {
        if (logger_.isTraceEnabled())
            logger_.trace("reporting {}", ep);
        long now = System.currentTimeMillis();
        ArrivalWindow heartbeatWindow = arrivalSamples_.get(ep);
        if (heartbeatWindow == null) {
            heartbeatWindow = new ArrivalWindow(sampleSize_);
            arrivalSamples_.put(ep, heartbeatWindow);
        }
        heartbeatWindow.add(now);
    }

    public void interpret(InetSocketAddress ep) {
        ArrivalWindow hbWnd = arrivalSamples_.get(ep);
        if (hbWnd == null) {
            return;
        }
        long now = System.currentTimeMillis();
        double phi = hbWnd.phi(now);
        if (logger_.isTraceEnabled())
            logger_.trace("PHI for " + ep + " : " + phi);

        if (phi > phiConvictThreshold_) {
            logger_.trace("notifying listeners that {} is down", ep);
            logger_.trace("intervals: {} mean: {}", hbWnd, hbWnd.mean());
            for (IFailureDetectionEventListener listener : fdEvntListeners_) {
                listener.convict(ep, phi);
            }
        }
    }

    public void forceConviction(InetSocketAddress ep) {
        logger_.debug("Forcing conviction of {}", ep);
        for (IFailureDetectionEventListener listener : fdEvntListeners_) {
            listener.convict(ep, phiConvictThreshold_);
        }
    }

    public void clear(InetSocketAddress ep) {
        ArrivalWindow heartbeatWindow = arrivalSamples_.get(ep);
        if (heartbeatWindow != null)
            heartbeatWindow.clear();
    }

    public void remove(InetSocketAddress ep) {
        arrivalSamples_.remove(ep);
    }

    public void registerFailureDetectionEventListener(IFailureDetectionEventListener listener) {
        fdEvntListeners_.add(listener);
    }

    public void unregisterFailureDetectionEventListener(IFailureDetectionEventListener listener) {
        fdEvntListeners_.remove(listener);
    }


/***
 * 以下为FailureDetectorMBean的实现    
 */
    /**
     * Dump the inter arrival times for examination if necessary.
     */
    public void dumpInterArrivalTimes() {
        OutputStream os = null;
        try {
            File file = File.createTempFile("failuredetector-", ".dat");
            os = new BufferedOutputStream(new FileOutputStream(file, true));
            os.write(toString().getBytes());
        } catch (IOException e) {
            throw new IOError(e);
        } finally {
            FileUtils.closeQuietly(os);
        }
    }

    public void setPhiConvictThreshold(int phi) {
        phiConvictThreshold_ = phi;
    }

    public int getPhiConvictThreshold() {
        return phiConvictThreshold_;
    }

    public String getAllEndpointStates() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<InetSocketAddress, EndpointState> entry : GossiperApp.instance.endpointStateMap.entrySet()) {
            sb.append(entry.getKey()).append("\n");
            appendEndpointState(sb, entry.getValue());
        }
        return sb.toString();
    }

    public Map<String, String> getSimpleStates() {
        Map<String, String> nodesStatus = new HashMap<String, String>(GossiperApp.instance.endpointStateMap.size());
        for (Map.Entry<InetSocketAddress, EndpointState> entry : GossiperApp.instance.endpointStateMap.entrySet()) {
            if (entry.getValue().isAlive())
                nodesStatus.put(entry.getKey().toString(), "UP");
            else
                nodesStatus.put(entry.getKey().toString(), "DOWN");
        }
        return nodesStatus;
    }

    public String getEndpointState(String address) throws UnknownHostException {
        StringBuilder sb = new StringBuilder();
        EndpointState endpointState = GossiperApp.instance.getEndpointStateForEndpoint(SocketAddressUtil.parseSocketAddress(address));
        appendEndpointState(sb, endpointState);
        return sb.toString();
    }

    private void appendEndpointState(StringBuilder sb, EndpointState endpointState) {
        for (Map.Entry<ApplicationStateEnum, VersionedValue> state : endpointState.applicationState.entrySet())
            sb.append("  ").append(state.getKey()).append(":").append(state.getValue().value).append("\n");
    }


    public String toString() {
        StringBuilder sb = new StringBuilder();
        Set<InetSocketAddress> eps = arrivalSamples_.keySet();

        sb.append("-----------------------------------------------------------------------");
        for (InetSocketAddress ep : eps) {
            ArrivalWindow hWnd = arrivalSamples_.get(ep);
            sb.append(ep + " : ");
            sb.append(hWnd.toString());
            sb.append(System.getProperty("line.separator"));
        }
        sb.append("-----------------------------------------------------------------------");
        return sb.toString();
    }

    public static void main(String[] args) throws Throwable {
    }
}

class ArrivalWindow {
    private static Logger logger_ = LoggerFactory.getLogger(ArrivalWindow.class);
    private double tLast_ = 0L;
    private BoundedStatsDeque arrivalIntervals_;

    // this is useless except to provide backwards compatibility in phi_convict_threshold,
    // because everyone seems pretty accustomed to the default of 8, and users who have
    // already tuned their phi_convict_threshold for their own environments won't need to
    // change.
    private final double PHI_FACTOR = 1.0 / Math.log(10.0);

    // in the event of a long partition, never record an interval longer than the rpc timeout,
    // since if a host is regularly experiencing connectivity problems lasting this long we'd
    // rather mark it down quickly instead of adapting
    private final double MAX_INTERVAL_IN_MS = Gossiper.getRpcTimeout();

    ArrivalWindow(int size) {
        arrivalIntervals_ = new BoundedStatsDeque(size);
    }

    synchronized void add(double value) {
        double interArrivalTime;
        if (tLast_ > 0L) {
            interArrivalTime = (value - tLast_);
        } else {
            interArrivalTime = GossiperApp.intervalInMillis / 2;
        }
        if (interArrivalTime <= MAX_INTERVAL_IN_MS) {
            arrivalIntervals_.add(interArrivalTime);
        } else {
            logger_.debug("Ignoring interval time of {}", interArrivalTime);
        }
        tLast_ = value;
    }

    synchronized double sum() {
        return arrivalIntervals_.sum();
    }

    synchronized double sumOfDeviations() {
        return arrivalIntervals_.sumOfDeviations();
    }

    synchronized double mean() {
        return arrivalIntervals_.mean();
    }

    synchronized double variance() {
        return arrivalIntervals_.variance();
    }

    double stdev() {
        return arrivalIntervals_.stdev();
    }

    void clear() {
        arrivalIntervals_.clear();
    }

    // see CASSANDRA-2597 for an explanation of the math at work here.
    synchronized double phi(long tnow) {
        int size = arrivalIntervals_.size();
        double t = tnow - tLast_;
        return (size > 0)
                ? PHI_FACTOR * t / mean()
                : 0.0;
    }

    public String toString() {
        Iterator<Double> iterator = arrivalIntervals_.iterator();

        StringBuilder builder = new StringBuilder(256);

        while (iterator.hasNext()) {
            Object obj = iterator.next();
            if (obj != null) {
                builder.append(obj);
            }
            builder.append(" ");
        }

        return builder.toString();
    }
}

