package com.dxh.webrtc_server.server;

import com.dxh.webrtc_server.config.Constant;
import com.dxh.webrtc_server.util.GsonUtil;
import com.dxh.webrtc_server.base.BaseWebSocketServer;
import com.dxh.webrtc_server.bean.NetBean;
import com.dxh.webrtc_server.util.Logger;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;

@ServerEndpoint("/ws")
@Component
public class WebSocketServer extends BaseWebSocketServer {

    @OnOpen
    public void onOpen(Session session) {
        Logger.e("onOpen ");
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        Logger.e("onMessage：message=" + message);
        int code = (int) getJsonValue(message, "code");
        switch (code) {
            case Constant.Login_Code://登录
                NetBean.LoginRequsetBean wsRequestBean = GsonUtil.GsonToBean(message, NetBean.LoginRequsetBean.class);
                if (getSession(wsRequestBean.userId) != null) {
                    //发送断线通知
                    NetBean.LoginResponseBean wsResponseBean = new NetBean.LoginResponseBean(Constant.OFFLine_Code, wsRequestBean.sequenceId, false, "该账户被其他用户登录");
                    sendMessage(getSession(wsRequestBean.userId), GsonUtil.BeanToJson(wsResponseBean));
                    removeSession(wsRequestBean.userId);
                }
                if (session.isOpen())
                    putSession(wsRequestBean.userId, session);
                //登录响应
                sendMessage(session, GsonUtil.BeanToJson(new NetBean.LoginResponseBean(wsRequestBean.code, wsRequestBean.sequenceId, true, "登录成功,当前在线人数:" + getSessionSize())));
                //每次有人登录，广播所有人当前在线用户信息
                broadcastAllSession(GsonUtil.BeanToJson(new NetBean.OnLineUserInfoResponseBean(Constant.OnLineUserInfo_Code, getUserIdList())));
                Logger.e(wsRequestBean.userId + "登录成功");
                break;
            case Constant.SendMsg_Code://转发消息
                Logger.e("SendMsg_Code------"+message);
                NetBean.SendMsgRequestBean sendMsgRequestBean = GsonUtil.GsonToBean(message, NetBean.SendMsgRequestBean.class);
                if (getSession(sendMsgRequestBean.toUserId) == null) {
                    Logger.e("SendMsg_Code------该用户不在线");
                    sendMessage(session, GsonUtil.BeanToJson(new NetBean.SendMsgResponseBean(Constant.SendMsg_Code, sendMsgRequestBean.sequenceId, sendMsgRequestBean.fromUserId, sendMsgRequestBean.toUserId, false, "该用户不在线")));
                    return;
                }
                Logger.e("SendMsg_Code------getSession成功");
                Session session1 = getSession(sendMsgRequestBean.toUserId);
                Logger.e("SendMsg_Code------sendMessage成功");
                sendMessage(session1, message);
                break;
            default://直接断线
                if (session.isOpen()) {
                    try {
                        session.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }

    @OnClose
    public void onClose(Session session) {
        Logger.e("onClose");
        removeSession(session);
        //有人断线，广播所有人当前在线用户信息
        broadcastAllSession(GsonUtil.BeanToJson(new NetBean.OnLineUserInfoResponseBean(Constant.OnLineUserInfo_Code, getUserIdList())));
    }

    @OnError
    public void onError(Session session, Throwable error) {
        Logger.e("onError：" + error.getMessage());
    }
}
