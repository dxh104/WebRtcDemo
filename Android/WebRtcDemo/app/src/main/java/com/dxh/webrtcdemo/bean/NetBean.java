package com.dxh.webrtcdemo.bean;

import java.util.List;

public class NetBean {
    public static class LoginRequsetBean {
        public int code;
        public String sequenceId;
        public String userId;

        public LoginRequsetBean(int code, String sequenceId, String userId) {
            this.code = code;
            this.sequenceId = sequenceId;
            this.userId = userId;
        }
    }

    public static class LoginResponseBean {
        public int code;
        public String sequenceId;
        public boolean isSucceed;
        public String message;

        public LoginResponseBean(int code, String sequenceId, boolean isSucceed, String message) {
            this.code = code;
            this.sequenceId = sequenceId;
            this.isSucceed = isSucceed;
            this.message = message;
        }
    }

    public static class SendMsgRequestBean {
        public int code;
        public String sequenceId;
        public String fromUserId;
        public String toUserId;
        public String message;

        public SendMsgRequestBean(int code, String sequenceId, String fromUserId, String toUserId) {
            this.code = code;
            this.sequenceId = sequenceId;
            this.fromUserId = fromUserId;
            this.toUserId = toUserId;
        }

        public SendMsgRequestBean(int code, String sequenceId, String fromUserId, String toUserId, String message) {
            this.code = code;
            this.sequenceId = sequenceId;
            this.fromUserId = fromUserId;
            this.toUserId = toUserId;
            this.message = message;
        }
    }

    public static class SendMsgResponseBean {
        public int code;
        public String sequenceId;
        public String fromUserId;
        public String toUserId;
        public boolean isSucceed;
        public String message;

        public SendMsgResponseBean(int code, String sequenceId, String fromUserId, String toUserId, boolean isSucceed, String message) {
            this.code = code;
            this.sequenceId = sequenceId;
            this.fromUserId = fromUserId;
            this.toUserId = toUserId;
            this.isSucceed = isSucceed;
            this.message = message;
        }
    }

    public static class OffLineResponseBean {
        public int code;
        public String sequenceId;
        public String message;

        public OffLineResponseBean(int code, String sequenceId, String message) {
            this.code = code;
            this.sequenceId = sequenceId;
            this.message = message;
        }
    }

    public static class OnLineUserInfoResponseBean {
        public int code;
        public String sequenceId;
        public List<String> userIdList;

        public OnLineUserInfoResponseBean(int code, String sequenceId, List<String> userIdList) {
            this.code = code;
            this.sequenceId = sequenceId;
            this.userIdList = userIdList;
        }

        public OnLineUserInfoResponseBean(int code, List<String> userIdList) {
            this.code = code;
            this.userIdList = userIdList;
        }
    }

    public static class SdpMessage {
        public int code;
        public String description;//sdp描述

        public SdpMessage(int code, String description) {
            this.code = code;
            this.description = description;
        }
    }

    public static class NormalMessage {
        public int code;

        public NormalMessage(int code) {
            this.code = code;
        }
    }

    public static class IceCandidateMessage {
        public int code;
        public String sdpMid;
        public int sdpMLineIndex;
        public String sdp;

        public IceCandidateMessage(int code, String sdpMid, int sdpMLineIndex, String sdp) {
            this.code = code;
            this.sdpMid = sdpMid;
            this.sdpMLineIndex = sdpMLineIndex;
            this.sdp = sdp;
        }
    }
}
