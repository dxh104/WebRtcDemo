package com.dxh.webrtc_server.base;

import com.dxh.webrtc_server.manager.SessionManager;
import org.json.JSONException;
import org.json.JSONObject;

import javax.websocket.Session;
import java.io.IOException;
import java.util.List;

public class BaseWebSocketServer {
    public void sendMessage(Session session, String message) {
        if (session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void broadcastAllSession(String message) {
        List<Session> allSessionList = getAllSessionList();
        for (int i = 0; i < allSessionList.size(); i++) {
            Session session = allSessionList.get(i);
            sendMessage(session, message);
        }
    }

    SessionManager sessionManager = SessionManager.getInstance();

    public void putSession(String userId, Session session) {
        sessionManager.putSession(userId, session);
    }

    public Session getSession(String userId) {
        return sessionManager.getSession(userId);
    }

    public int getSessionSize() {
        return sessionManager.getSessionSize();
    }

    public List<String> getUserIdList() {
        return sessionManager.getUserIdList();
    }

    public List<Session> getAllSessionList() {
        return sessionManager.getAllSessionList();
    }

    public void removeSession(String userId) {
        sessionManager.removeSession(userId);
    }

    public void removeSession(Session session) {
        sessionManager.removeSession(session);
    }

    public Integer getJsonValue(String json, String key) {
        try {
            return new JSONObject(json).getInt(key);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
