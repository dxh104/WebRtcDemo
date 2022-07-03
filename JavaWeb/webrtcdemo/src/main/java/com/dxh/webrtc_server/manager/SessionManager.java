package com.dxh.webrtc_server.manager;

import javax.websocket.Session;
import java.io.IOException;
import java.util.*;

public class SessionManager {
    static SessionManager sessionManager = new SessionManager();

    static {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(1000 * 10);
                        System.out.println("当前在线人数---------->" + getInstance().hashMap.size());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public static SessionManager getInstance() {
        return sessionManager;
    }

    private volatile HashMap<String, Session> hashMap = new HashMap<>();

    public void putSession(String userId, Session session) {
        hashMap.put(userId, session);
    }

    public Session getSession(String userId) {
        return hashMap.get(userId);
    }

    public void removeSession(String userId) {
        Session remove = hashMap.remove(userId);
        try {
            if (remove != null && remove.isOpen()) {
                remove.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void removeSession(Session session) {
        if (hashMap.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<String, Session>> iterator = hashMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Session> next = iterator.next();
            if (next.getValue() == session) {
                iterator.remove();
                break;
            }
        }
        try {
            if (session != null && session.isOpen()) {
                session.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getSessionSize() {
        return hashMap.size();
    }

    public List<String> getUserIdList() {
        Iterator<Map.Entry<String, Session>> iterator = hashMap.entrySet().iterator();
        List<String> userIdList = new ArrayList<>();
        while (iterator.hasNext()) {
            Map.Entry<String, Session> next = iterator.next();
            Session session = next.getValue();
            String userId = next.getKey();
            userIdList.add(userId);
        }
        return userIdList;
    }

    public List<Session> getAllSessionList() {
        Iterator<Map.Entry<String, Session>> iterator = hashMap.entrySet().iterator();
        List<Session> sessionList = new ArrayList<>();
        while (iterator.hasNext()) {
            Map.Entry<String, Session> next = iterator.next();
            Session session = next.getValue();
            String userId = next.getKey();
            sessionList.add(session);
        }
        return sessionList;
    }
}
