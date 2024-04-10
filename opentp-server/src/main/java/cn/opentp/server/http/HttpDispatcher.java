package cn.opentp.server.http;

import cn.opentp.core.tp.ThreadPoolWrapper;
import cn.opentp.core.util.JSONUtils;
import cn.opentp.server.http.handler.HttpHandler;
import cn.opentp.server.tp.Configuration;
import io.netty.handler.codec.http.*;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HttpDispatcher {


    public static void doDispatcher(FullHttpRequest httpRequest, FullHttpResponse httpResponse) {
        String uri = httpRequest.uri();
        HttpMethod method = httpRequest.method();

        String[] split = uri.split("/");
        String endPoint = split[1];
        Map<String, HttpHandler> endPoints = Configuration.configuration().getEndPoints();
        HttpHandler httpHandler = endPoints.get(endPoint);

        if (HttpMethod.GET.equals(method)) {
            httpHandler.doGet(httpRequest, httpResponse);
        } else if (HttpMethod.POST.equals(method)) {
            httpHandler.doPost(httpRequest, httpResponse);
        } else if (HttpMethod.PUT.equals(method)) {
            httpHandler.doPut(httpRequest, httpResponse);
        } else if (HttpMethod.DELETE.equals(method)) {
            httpHandler.doDelete(httpRequest, httpResponse);
        }
    }
}
