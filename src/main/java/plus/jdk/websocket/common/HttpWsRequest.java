package plus.jdk.websocket.common;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import lombok.Getter;
import java.util.HashMap;

public class HttpWsRequest {

    @Getter
    private final FullHttpRequest request;

    @Getter
    private final String uri;

    @Getter
    private final HttpHeaders headers;

    private HashMap<String, String> cookiesMap;

    private HashMap<String, String> queryParams;

    public HttpWsRequest(FullHttpRequest request) {
        this.request = request;
        this.uri = request.uri();
        this.headers = request.headers();
        buildCookies();
        buildQueryParams();
    }

    public String getCookie(String name) {
        return cookiesMap.get(name);
    }

    public String getQueryValue(String name) {
        return queryParams.get(name);
    }

    private void buildQueryParams() {
        queryParams = new HashMap<>();
        String uri = request.uri();
        String[] tempArr = uri.split("[?]");
        if (tempArr.length < 2) {
            return;
        }
        String queryStr = tempArr[1];
        int hashIndex = queryStr.indexOf("#");
        if (hashIndex > 1) {
            queryStr = queryStr.substring(0, hashIndex);
        }
        String[] queryPairArr = queryStr.split("&");
        for (String queryPairStr : queryPairArr) {
            String[] queryPair = queryPairStr.split("=");
            String key = queryPair[0];
            String value = queryPair.length > 1 ? queryPair[1] : null;
            this.queryParams.put(key, value);
        }
    }

    private void buildCookies() {
        cookiesMap = new HashMap<>();
        String cookiesStr = request.headers().get("Cookie");
        if (cookiesStr == null) {
            return;
        }
        String[] cookiePairStrArr = cookiesStr.split(";");
        for (String cookiePairStr : cookiePairStrArr) {
            String[] cookiePair = cookiePairStr.split("=");
            String key = cookiePair[0];
            String value = cookiePair.length > 1 ? cookiePair[1] : null;
            cookiesMap.put(key, value);
        }
    }
}
