package com.dxh.webrtcdemo.websocket;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * Create By XHD On 2022/03/11
 * Description：辅助通话的WebSocket
 **/
public class BaseWebSocket extends WebSocketClient {
    private OnBaseWebSocketApi webSocketApi;

    public static BaseWebSocket connectServer(String ip, String port, String method, OnBaseWebSocketApi webSocketApi) {
        BaseWebSocket webSocketClient = null;
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("origin", "http:" + ip + ":" + port);
        try {
            webSocketClient = new BaseWebSocket(new URI("ws://" + ip + ":" + port + method), new Draft_6455(), headerMap, 1000 * 10);
            webSocketClient.webSocketApi = webSocketApi;
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        webSocketClient.setConnectionLostTimeout(60);//连接丢失检测时间设默认60s(心跳时间)
        if (!webSocketClient.isOpen()) {
            webSocketClient.connect();
        }
        return webSocketClient;
    }

    public static BaseWebSocket connectServer(String url, OnBaseWebSocketApi webSocketApi) {
        BaseWebSocket webSocketClient = null;
        Map<String, String> headerMap = new HashMap<>();
        try {
            webSocketClient = new BaseWebSocket(new URI(url), new Draft_6455(), headerMap, 1000 * 10);
            webSocketClient.webSocketApi = webSocketApi;
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        webSocketClient.setConnectionLostTimeout(60);//连接丢失检测时间设默认60s(心跳时间)
        if (!webSocketClient.isOpen()) {
            webSocketClient.connect();
        }
        return webSocketClient;
    }

    public void disConnectServer() {
        if (isOpen()) {
            close();
        }
    }

    public boolean sendMessage(String message) {
        if (isOpen()) {
            send(message);
            return true;
        } else {
            return false;
        }
    }

    public BaseWebSocket(URI serverUri, Draft protocolDraft, Map<String, String> httpHeaders, int connectTimeout) {
        super(serverUri, protocolDraft, httpHeaders, connectTimeout);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        if (webSocketApi != null) {
            webSocketApi.onOpen(handshakedata);
        }
    }

    @Override
    public void onMessage(String message) {
        if (webSocketApi != null) {
            webSocketApi.onMessage(message);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        if (webSocketApi != null) {
            webSocketApi.onClose(code, reason, remote);
        }
    }

    @Override
    public void onError(Exception ex) {
        if (webSocketApi != null) {
            webSocketApi.onError(ex);
        }
    }

    public interface OnBaseWebSocketApi {
        void onOpen(ServerHandshake handshakedata);//连接成功

        void onMessage(String message);//收到消息

        void onClose(int code, String reason, boolean remote);//断开连接

        void onError(Exception ex);//发生异常
    }
}
