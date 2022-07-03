package com.dxh.webrtcdemo.config;

public class Constant {
    public static final int Login_Code = 0;//客户端登录code
    public static final int SendMsg_Code = 1;//服务端帮忙转发消息code
    public static final int OFFLine_Code = 2;//服务端主动断线code
    public static final int OnLineUserInfo_Code = 4;//服务端主动推送在线用户信息code

    public static final int Message_SDP_Offer_Code = 10;
    public static final int Message_SDP_Answer_Code = 11;
    public static final int Message_IceCandidate_Request_Code = 12;
    public static final int Message_IceCandidate_Response_Code = 13;
    public static final int Message_Call_Code = 14;
    public static final int Message_Call_Result_Code = 15;

}
