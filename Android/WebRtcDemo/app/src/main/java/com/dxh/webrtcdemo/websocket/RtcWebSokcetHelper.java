package com.dxh.webrtcdemo.websocket;

import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.dxh.webrtcdemo.MainApplication;

import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * Create By XHD On 2022/03/11
 * Description：辅助通话的WebSocket帮助类
 **/
public class RtcWebSokcetHelper {
    public BaseWebSocket baseWebSocket;
    public Handler handler = new Handler();
    public HashMap<String, WebsocketDealEvent> eventMap = new HashMap<>();//请求响应事件
    private OnReciveServerMgsListener onReciveServerMgsListener;
    private static RtcWebSokcetHelper rtcWebSokcetHelper;

    public static RtcWebSokcetHelper getInstance() {
        if (rtcWebSokcetHelper == null)
            rtcWebSokcetHelper = new RtcWebSokcetHelper();
        return rtcWebSokcetHelper;
    }

    public boolean sendRequest(String requestJson) {
        boolean b = sendMessage(requestJson);
        return b;
    }

    public boolean connectServer(String url, final OnReciveServerMgsListener onReciveServerMgsListener) {
        setOnReciveServerMgsListener(onReciveServerMgsListener);
        if (baseWebSocket == null) {
            baseWebSocket = BaseWebSocket.connectServer(url, new BaseWebSocket.OnBaseWebSocketApi() {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    handler.post(new java.lang.Runnable() {
                        @Override
                        public void run() {
                            if (onReciveServerMgsListener != null) {
                                onReciveServerMgsListener.onConnect();
                            }
                        }
                    });
                }

                @Override
                public void onMessage(String message) {
                    handler.post(new java.lang.Runnable() {
                        @Override
                        public void run() {
                            if (onReciveServerMgsListener != null)
                                onReciveServerMgsListener.onRecive(message);
                        }
                    });
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    handler.post(new java.lang.Runnable() {
                        @Override
                        public void run() {
                            if (onReciveServerMgsListener != null) {
                                onReciveServerMgsListener.onClose(code, reason, remote);
                            }
                            destroy();
                        }
                    });
                }

                @Override
                public void onError(Exception ex) {
                    handler.post(new java.lang.Runnable() {
                        @Override
                        public void run() {
                            Log.e("------------connectServer", "onError: " + ex.getMessage());
                            Toast.makeText(MainApplication.instance, "connectServer:" + ex.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
            return true;
        } else {
            return false;
        }
    }

    public boolean connectServer(String ip, String port, String method, final OnReciveServerMgsListener onReciveServerMgsListener) {
        setOnReciveServerMgsListener(onReciveServerMgsListener);
        if (baseWebSocket == null) {
            baseWebSocket = BaseWebSocket.connectServer(ip, port, method, new BaseWebSocket.OnBaseWebSocketApi() {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    if (onReciveServerMgsListener != null) {
                        onReciveServerMgsListener.onConnect();
                    }
                }

                @Override
                public void onMessage(String message) {
                    if (onReciveServerMgsListener != null)
                        onReciveServerMgsListener.onRecive(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    if (onReciveServerMgsListener != null) {
                        onReciveServerMgsListener.onClose(code, reason, remote);
                    }
                    destroy();
                }

                @Override
                public void onError(Exception ex) {

                }
            });
            return true;
        } else {
            return false;
        }
    }

    public void destroy() {
        disConnectServer();
        onReciveServerMgsListener = null;
        eventMap.clear();
    }

    private boolean disConnectServer() {
        if (baseWebSocket != null) {
            baseWebSocket.disConnectServer();
            baseWebSocket = null;
            return true;
        } else {
            return false;
        }
    }

    private boolean sendMessage(String message) {
        if (baseWebSocket != null) {
            baseWebSocket.sendMessage(message);
            return true;
        } else {
            return false;
        }
    }

    public void putWebsocketDealEvent(String requestJson, Runnable dealRunnable) {
        try {
            JSONObject jsonObject = new JSONObject(requestJson);
            String sequence = jsonObject.getString("sequenceId");//应用报文id
            eventMap.put(sequence, new WebsocketDealEvent(requestJson, dealRunnable));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public WebsocketDealEvent getWebsocketDealEvent(String responseJson) {
        try {
            JSONObject jsonObject = new JSONObject(responseJson);
            String sequence = jsonObject.getString("sequenceId");//应用报文id
            return eventMap.get(sequence);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void removeWebsocketDealEvent(String responseJson) {
        try {
            JSONObject jsonObject = new JSONObject(responseJson);
            String sequence = jsonObject.getString("sequence");//应用报文id
            eventMap.remove(sequence);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static class WebsocketDealEvent {
        public String requsetJson;
        public String responseJson;
        public Runnable dealRunnable;

        public WebsocketDealEvent(String requsetJson, Runnable dealRunnable) {
            this.requsetJson = requsetJson;
            this.dealRunnable = dealRunnable;
            dealRunnable.websocketDealEvent = this;
        }
    }

    public static abstract class Runnable {
        public WebsocketDealEvent websocketDealEvent;

        public Runnable() {
        }

        public abstract void run();
    }

    private void setOnReciveServerMgsListener(OnReciveServerMgsListener onReciveServerMgsListener) {
        this.onReciveServerMgsListener = onReciveServerMgsListener;
    }

    public interface OnReciveServerMgsListener {
        void onConnect();//接受json

        void onRecive(String json);//接受json

        void onClose(int code, String reason, boolean remote);//断开连接
    }
}
