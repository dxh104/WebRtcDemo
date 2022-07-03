package com.dxh.webrtcdemo;

import android.Manifest;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.dxh.webrtcdemo.adapter.PeerConnectionAdapter;
import com.dxh.webrtcdemo.adapter.SdpAdapter;
import com.dxh.webrtcdemo.bean.NetBean;
import com.dxh.webrtcdemo.config.Constant;
import com.dxh.webrtcdemo.util.GsonUtil;
import com.dxh.webrtcdemo.websocket.RtcWebSokcetHelper;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.audio.JavaAudioDeviceModule;
import org.webrtc.voiceengine.WebRtcAudioUtils;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private String TAG = "MainActivity------";
    private EditText etWsUrl;//ws地址
    private Button btnConnectSever;//连接ws
    private EditText etLoginUserId;//登录userId
    private Button btnLogin;//登录
    private EditText etUserInfo;//在线用户信息
    private EditText etCallUserId;//呼叫userId
    private Button btnCall;//呼叫
    private SurfaceViewRenderer localView;//本地摄像头预览(本地视频)
    private SurfaceViewRenderer remoteView;//服务器传来的摄像头预览(对端视频)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        checkPermission();
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {//适配6.0权限
            if (ContextCompat.checkSelfPermission(getApplication(),
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(getApplication(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(getApplication(),
                    Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.RECORD_AUDIO,
                                Manifest.permission.CAMERA,
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.ACCESS_WIFI_STATE,
                                Manifest.permission.RECORD_AUDIO
                        }, 1);
            } else {
                //已经有权限
                havePermission();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.e(TAG, "onRequestPermissionsResult: ");
        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, permissions[i] + "权限未打开", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }
        //已经有权限
        havePermission();
    }


    private EglBase.Context eglBaseContext;
    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection callPeerConnection;
    private PeerConnection receivePeerConnection;
    private MediaStream mMediaStream;

    private void havePermission() {
        Log.e(TAG, "havePermission: ");

        //创建EglBase对象 并获取上下文环境
        eglBaseContext = EglBase.create().getEglBaseContext();
        //1.初始化p2p连接工厂
        PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions
                        .builder(getApplicationContext())
                        .createInitializationOptions()
        );
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        DefaultVideoEncoderFactory defaultVideoEncoderFactory = new DefaultVideoEncoderFactory(eglBaseContext, true, true);//视频编码工厂
        DefaultVideoDecoderFactory defaultVideoDecoderFactory = new DefaultVideoDecoderFactory(eglBaseContext);//视频解码工厂

//        JavaAudioDeviceModule.Builder admbuilder = JavaAudioDeviceModule.builder(this);
//        admbuilder.setAudioSource(MediaRecorder.AudioSource.MIC);//控制录音来源
//        JavaAudioDeviceModule audioDeviceModule = admbuilder.createAudioDeviceModule();
        //2.创建p2p连接工厂
        peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(defaultVideoEncoderFactory)
                .setVideoDecoderFactory(defaultVideoDecoderFactory)
//                .setAudioDeviceModule(audioDeviceModule)
                .createPeerConnectionFactory();
        //3.创建SurfaceTextureHelper
        SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBaseContext);
        //4.创建视频捕获器
        VideoCapturer videoCapturer = createCameraCapturer(false);//是否正面摄像头
        //5.创建视频源
        VideoSource videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast());
        //6.初始化视频捕获器
        videoCapturer.initialize(surfaceTextureHelper, getApplicationContext(), videoSource.getCapturerObserver());
        //7.开始捕获
        videoCapturer.startCapture(480, 640, 30);
        localView.setMirror(false);//是否镜像
        localView.init(eglBaseContext, null);//初始化SurfaceView
        remoteView.setMirror(false);//是否镜像
        remoteView.init(eglBaseContext, null);//初始化SurfaceView
        //8.创建视频轨道
        VideoTrack videoTrack = peerConnectionFactory.createVideoTrack("100", videoSource);
        videoTrack.addSink(localView);//展示本地视频
        //9.创建本地媒体流
        mMediaStream = peerConnectionFactory.createLocalMediaStream("mMediaStream");
        mMediaStream.addTrack(videoTrack);//媒体流添加视频轨道
        mMediaStream.addTrack(createAudioTrack());//媒体流添加音频轨道
        //连接websocket服务
        btnConnectSever.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(etWsUrl.getText().toString())) {
                    Toast.makeText(MainActivity.this, "请输入wsUrl", Toast.LENGTH_SHORT).show();
                    return;
                }
                RtcWebSokcetHelper.getInstance().connectServer(etWsUrl.getText().toString().trim(), new RtcWebSokcetHelper.OnReciveServerMgsListener() {
                    @Override
                    public void onConnect() {
                        Log.e(TAG, "onConnect: ");
                        Toast.makeText(MainActivity.this, "服务连接成功", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onRecive(String json) {
                        RtcWebSokcetHelper.WebsocketDealEvent websocketDealEvent;
                        Log.e(TAG, "onRecive: json=" + json);
                        try {
                            int code = new JSONObject(json).getInt("code");
                            switch (code) {
                                case Constant.SendMsg_Code://收到转发消息
                                    NetBean.SendMsgRequestBean sendMsgRequestBean = GsonUtil.GsonToBean(json, NetBean.SendMsgRequestBean.class);
                                    if (sendMsgRequestBean.message.contains("不在线")) {
                                        Toast.makeText(MainActivity.this, "目标用户不在线", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    int turnCode = new JSONObject(sendMsgRequestBean.message).getInt("code");
                                    switch (turnCode) {
                                        case Constant.Message_Call_Code:
                                            Log.e(TAG, "onRecive: Message_Call_Code");
                                            showCustomeDialog("是否同意接听 " + sendMsgRequestBean.fromUserId + " 的来电?",
                                                    "同意", new View.OnClickListener() {
                                                        @Override
                                                        public void onClick(View v) {
                                                            NetBean.SendMsgResponseBean sendMsgResponseBean = new NetBean.SendMsgResponseBean(Constant.Message_Call_Result_Code, sendMsgRequestBean.sequenceId, sendMsgRequestBean.toUserId, sendMsgRequestBean.fromUserId, true, "同意接听");
                                                            NetBean.SendMsgRequestBean sendMsgRequestBean1 = new NetBean.SendMsgRequestBean(Constant.SendMsg_Code, sendMsgRequestBean.sequenceId, sendMsgRequestBean.toUserId, sendMsgRequestBean.fromUserId, GsonUtil.BeanToJson(sendMsgResponseBean));
                                                            RtcWebSokcetHelper.getInstance().sendRequest(GsonUtil.BeanToJson(sendMsgRequestBean1));
                                                            call(sendMsgRequestBean.toUserId, sendMsgRequestBean.fromUserId);//建立通话
                                                        }
                                                    }, "拒绝", new View.OnClickListener() {
                                                        @Override
                                                        public void onClick(View v) {
                                                            NetBean.SendMsgResponseBean sendMsgResponseBean = new NetBean.SendMsgResponseBean(Constant.Message_Call_Result_Code, sendMsgRequestBean.sequenceId, sendMsgRequestBean.toUserId, sendMsgRequestBean.fromUserId, false, "拒绝接听");
                                                            NetBean.SendMsgRequestBean sendMsgRequestBean1 = new NetBean.SendMsgRequestBean(Constant.SendMsg_Code, sendMsgRequestBean.sequenceId, sendMsgRequestBean.toUserId, sendMsgRequestBean.fromUserId, GsonUtil.BeanToJson(sendMsgResponseBean));
                                                            RtcWebSokcetHelper.getInstance().sendRequest(GsonUtil.BeanToJson(sendMsgRequestBean1));
                                                        }
                                                    });
                                            break;
                                        case Constant.Message_SDP_Offer_Code:
                                            Log.e(TAG, "--------onRecive: Message_SDP_Offer_Code 1");
                                            NetBean.SdpMessage sdpMessage = GsonUtil.GsonToBean(sendMsgRequestBean.message, NetBean.SdpMessage.class);
                                            receive(sdpMessage.description, sendMsgRequestBean.fromUserId, sendMsgRequestBean.toUserId);
                                            break;
                                        case Constant.Message_SDP_Answer_Code:
                                            Log.e(TAG, "--------onRecive: Message_SDP_Answer_Code 2");
                                            sdpMessage = GsonUtil.GsonToBean(sendMsgRequestBean.message, NetBean.SdpMessage.class);
                                            SessionDescription sessionDescription = new SessionDescription(SessionDescription.Type.ANSWER, sdpMessage.description);
                                            //11. receive sdp setRemoteDescription
                                            callPeerConnection.setRemoteDescription(new SdpAdapter("setRemoteDescription"), sessionDescription);
                                            break;
                                        case Constant.Message_IceCandidate_Request_Code:
                                            Log.e(TAG, "--------onRecive: Message_IceCandidate_Request_Code 3");
                                            NetBean.IceCandidateMessage iceCandidateMessage = GsonUtil.GsonToBean(sendMsgRequestBean.message, NetBean.IceCandidateMessage.class);
                                            IceCandidate iceCandidate = new IceCandidate(iceCandidateMessage.sdpMid, iceCandidateMessage.sdpMLineIndex, iceCandidateMessage.sdp);
                                            //14.receive  iceCandidate
                                            receivePeerConnection.addIceCandidate(iceCandidate);
                                            break;
                                        case Constant.Message_IceCandidate_Response_Code:
                                            Log.e(TAG, "--------onRecive: Message_IceCandidate_Response_Code 4");
                                            iceCandidateMessage = GsonUtil.GsonToBean(sendMsgRequestBean.message, NetBean.IceCandidateMessage.class);
                                            iceCandidate = new IceCandidate(iceCandidateMessage.sdpMid, iceCandidateMessage.sdpMLineIndex, iceCandidateMessage.sdp);
                                            //16.receive iceCandidate addIceCandidate
                                            callPeerConnection.addIceCandidate(iceCandidate);
                                            break;
                                    }
                                    callBackEvent(json);
                                    break;
                                case Constant.Login_Code://登录通知
                                    callBackEvent(json);
                                    break;
                                case Constant.OFFLine_Code://断线通知
                                    NetBean.OffLineResponseBean offLineResponseBean = GsonUtil.GsonToBean(json, NetBean.OffLineResponseBean.class);
                                    Toast.makeText(MainActivity.this, offLineResponseBean.message, Toast.LENGTH_SHORT).show();
                                    break;
                                case Constant.OnLineUserInfo_Code://登录在线人源信息通知
                                    NetBean.OnLineUserInfoResponseBean onLineUserInfoResponseBean = GsonUtil.GsonToBean(json, NetBean.OnLineUserInfoResponseBean.class);
                                    List<String> userIdList = onLineUserInfoResponseBean.userIdList;
                                    StringBuilder sb = new StringBuilder();
                                    for (int i = 0; i < userIdList.size(); i++) {
                                        String userId = userIdList.get(i);
                                        if (userId.equals(etLoginUserId.getText().toString()))
                                            sb.append(userId + "(自己)\n");
                                        else
                                            sb.append(userId + "\n");
                                    }
                                    etUserInfo.setText(sb.toString());
                                    break;
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onClose(int code, String reason, boolean remote) {
                        Log.e(TAG, "onClose: reason=" + reason);
                    }
                });
            }
        });
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(etLoginUserId.getText().toString())) {
                    Toast.makeText(MainActivity.this, "请先输入登录UserId", Toast.LENGTH_SHORT).show();
                    return;
                }
                NetBean.LoginRequsetBean loginRequsetBean = new NetBean.LoginRequsetBean(Constant.Login_Code, System.currentTimeMillis() + "", etLoginUserId.getText().toString());
                RtcWebSokcetHelper.getInstance().putWebsocketDealEvent(GsonUtil.BeanToJson(loginRequsetBean), new RtcWebSokcetHelper.Runnable() {
                    @Override
                    public void run() {
                        String responseJson = websocketDealEvent.responseJson;
                        NetBean.LoginResponseBean loginResponseBean = GsonUtil.GsonToBean(responseJson, NetBean.LoginResponseBean.class);
                        if (loginResponseBean.isSucceed) {
                            Toast.makeText(MainActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "登录失败，" + loginResponseBean.message, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                RtcWebSokcetHelper.getInstance().sendRequest(GsonUtil.BeanToJson(loginRequsetBean));
            }
        });
        btnCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String fromUserId = etLoginUserId.getText().toString();
                String toUserId = etCallUserId.getText().toString();
                if (TextUtils.isEmpty(fromUserId) || TextUtils.isEmpty(toUserId)) {
                    Toast.makeText(MainActivity.this, "请先填写fromUserId,toUserId", Toast.LENGTH_SHORT).show();
                    return;
                }
                String message = GsonUtil.GsonToString(new NetBean.NormalMessage(Constant.Message_Call_Code));
                NetBean.SendMsgRequestBean sendMsgRequestBean = new NetBean.SendMsgRequestBean(Constant.SendMsg_Code, System.currentTimeMillis() + "", fromUserId, toUserId, message);
                RtcWebSokcetHelper.getInstance().putWebsocketDealEvent(GsonUtil.BeanToJson(sendMsgRequestBean), new RtcWebSokcetHelper.Runnable() {
                    @Override
                    public void run() {
                        //{"code":1,"fromUserId":"30","message":"{\"code\":15,\"fromUserId\":\"30\",\"isSucceed\":true,\"message\":\"同意接听\",\"sequenceId\":\"1656590187385\",\"toUserId\":\"29\"}","sequenceId":"1656590187385","toUserId":"29"}
                        String responseJson = websocketDealEvent.responseJson;
                        try {
                            responseJson = new JSONObject(websocketDealEvent.responseJson).getString("message");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        NetBean.SendMsgResponseBean sendMsgResponseBean = GsonUtil.GsonToBean(responseJson, NetBean.SendMsgResponseBean.class);
                        if (sendMsgResponseBean.isSucceed) {
                            Log.e(TAG, "run: 呼叫成功");
                            Toast.makeText(MainActivity.this, "呼叫成功", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e(TAG, "run: 呼叫失败");
                            Toast.makeText(MainActivity.this, "呼叫失败，" + sendMsgResponseBean.message, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                RtcWebSokcetHelper.getInstance().sendRequest(GsonUtil.BeanToJson(sendMsgRequestBean));
            }
        });
    }

    private void callBackEvent(String json) {
        RtcWebSokcetHelper.WebsocketDealEvent websocketDealEvent;
        websocketDealEvent = RtcWebSokcetHelper.getInstance().getWebsocketDealEvent(json);
        if (websocketDealEvent != null && websocketDealEvent.dealRunnable != null) {
            websocketDealEvent.responseJson = json;
            websocketDealEvent.dealRunnable.run();
        }
        RtcWebSokcetHelper.getInstance().removeWebsocketDealEvent(json);
    }

    /**
     * Create local audio track
     *
     * @return AudioTrack
     */
    public AudioTrack createAudioTrack() {
        AudioSource audioSource = peerConnectionFactory.createAudioSource(new MediaConstraints());
        WebRtcAudioUtils.setWebRtcBasedAcousticEchoCanceler(true);
        WebRtcAudioUtils.setWebRtcBasedNoiseSuppressor(true);

        AudioTrack audioTrack = peerConnectionFactory.createAudioTrack("200", audioSource);
        audioTrack.setEnabled(true);
        mMediaStream.addTrack(audioTrack);
        return audioTrack;
    }

    private VideoCapturer createCameraCapturer(boolean isFront) {
        Camera1Enumerator enumerator = new Camera1Enumerator(false);
        final String[] deviceNames = enumerator.getDeviceNames();
        // First, try to find front facing camera
        for (String deviceName : deviceNames) {
            if (isFront ? enumerator.isFrontFacing(deviceName) : enumerator.isBackFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }
        return null;
    }

    //建立WebRtc通话
    private void call(String fromUserId, String toUserId) {
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();//turn/sTurn服务器集合
        PeerConnection.IceServer iceServer1 = PeerConnection.IceServer.builder("stun:stun.l.google.com:19302")
                .createIceServer();
        PeerConnection.IceServer iceServer2 = PeerConnection.IceServer.builder("stun:stun.ekiga.net")
                .createIceServer();
        PeerConnection.IceServer iceServer3 = PeerConnection.IceServer.builder("stun:stun.schlund.de")
                .createIceServer();
        PeerConnection.IceServer iceServer4 = PeerConnection.IceServer.builder("stun:stun.voxgratia.org")
                .createIceServer();
        PeerConnection.IceServer iceServer5 = PeerConnection.IceServer.builder("turn:1.117.194.160:3478?transport=tcp")
                .setUsername("test")
                .setPassword("123456")
                .createIceServer();
        PeerConnection.IceServer iceServer6 = PeerConnection.IceServer.builder("turn:1.117.194.160:3478?transport=udp")
                .setUsername("test")
                .setPassword("123456")
                .createIceServer();
        iceServers.add(iceServer1);
        iceServers.add(iceServer2);
        iceServers.add(iceServer3);
        iceServers.add(iceServer4);
        iceServers.add(iceServer5);
        iceServers.add(iceServer6);
        //1.createPeerConnection
        callPeerConnection = peerConnectionFactory.createPeerConnection(iceServers, new PeerConnectionAdapter("localconnection") {
            //12.onIceCandidate
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
//                String sdpMid;
//                int sdpMLineIndex;
//                String sdp; //todo---WS转发
                //13.send iceCandidate
                String message = GsonUtil.GsonToString(new NetBean.IceCandidateMessage(Constant.Message_IceCandidate_Request_Code, iceCandidate.sdpMid, iceCandidate.sdpMLineIndex, iceCandidate.sdp));
                NetBean.SendMsgRequestBean sendMsgRequestBean = new NetBean.SendMsgRequestBean(Constant.SendMsg_Code, System.currentTimeMillis() + "", fromUserId, toUserId, message);
                RtcWebSokcetHelper.getInstance().sendRequest(GsonUtil.BeanToJson(sendMsgRequestBean));
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                super.onAddStream(mediaStream);
                VideoTrack remoteVideoTrack = mediaStream.videoTracks.get(0);
                runOnUiThread(() -> {
                    remoteVideoTrack.addSink(remoteView);//展示对端的视频
                });
            }
        });
        //2.addStream
        callPeerConnection.addStream(mMediaStream);//添加流
        //3.createOffer
        callPeerConnection.createOffer(new SdpAdapter("local offer sdp") {//发送offer
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);
                //4.setLocalDescription
                callPeerConnection.setLocalDescription(new SdpAdapter("local set local"), sessionDescription);//服务器中转设置会话描述
                //5.send sdp
                //  String description;todo---WS发送
                String message = GsonUtil.GsonToString(new NetBean.SdpMessage(Constant.Message_SDP_Offer_Code, sessionDescription.description));
                NetBean.SendMsgRequestBean sendMsgRequestBean = new NetBean.SendMsgRequestBean(Constant.SendMsg_Code, System.currentTimeMillis() + "", fromUserId, toUserId, message);
                RtcWebSokcetHelper.getInstance().sendRequest(GsonUtil.BeanToJson(sendMsgRequestBean));
            }
        }, new MediaConstraints());

//        //11. receive sdp setRemoteDescription
//        callPeerConnection.setRemoteDescription(new SdpAdapter("setRemoteDescription"), null);
        //16.receive iceCandidate addIceCandidate
//        callPeerConnection.addIceCandidate(null);
    }

    private void receive(String description, String fromUserId, String toUserId) {
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();//turn/sTurn服务器集合
        PeerConnection.IceServer iceServer1 = PeerConnection.IceServer.builder("stun:stun.l.google.com:19302")
                .createIceServer();
        PeerConnection.IceServer iceServer2 = PeerConnection.IceServer.builder("stun:stun.ekiga.net")
                .createIceServer();
        PeerConnection.IceServer iceServer3 = PeerConnection.IceServer.builder("stun:stun.schlund.de")
                .createIceServer();
        PeerConnection.IceServer iceServer4 = PeerConnection.IceServer.builder("stun:stun.voxgratia.org")
                .createIceServer();
        PeerConnection.IceServer iceServer5 = PeerConnection.IceServer.builder("turn:1.117.194.160:3478?transport=tcp")
                .setUsername("test")
                .setPassword("123456")
                .createIceServer();
        PeerConnection.IceServer iceServer6 = PeerConnection.IceServer.builder("turn:1.117.194.160:3478?transport=udp")
                .setUsername("test")
                .setPassword("123456")
                .createIceServer();
        iceServers.add(iceServer1);
        iceServers.add(iceServer2);
        iceServers.add(iceServer3);
        iceServers.add(iceServer4);
        iceServers.add(iceServer5);
        iceServers.add(iceServer6);
        //6.createPeerConnection
        receivePeerConnection = peerConnectionFactory.createPeerConnection(iceServers, new PeerConnectionAdapter("localconnection") {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
//                String sdpMid;
//                int sdpMLineIndex;
//                String sdp; //todo---WS转发
                //15.send iceCandidate
                String message = GsonUtil.GsonToString(new NetBean.IceCandidateMessage(Constant.Message_IceCandidate_Response_Code, iceCandidate.sdpMid, iceCandidate.sdpMLineIndex, iceCandidate.sdp));
                NetBean.SendMsgRequestBean sendMsgRequestBean = new NetBean.SendMsgRequestBean(Constant.SendMsg_Code, System.currentTimeMillis() + "", toUserId, fromUserId, message);
                RtcWebSokcetHelper.getInstance().sendRequest(GsonUtil.BeanToJson(sendMsgRequestBean));

            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                super.onAddStream(mediaStream);
                VideoTrack remoteVideoTrack = mediaStream.videoTracks.get(0);
                runOnUiThread(() -> {
                    remoteVideoTrack.addSink(remoteView);//展示对端的视频
                });
            }
        });
        receivePeerConnection.addStream(mMediaStream);//添加流
        SessionDescription fromSdp = new SessionDescription(SessionDescription.Type.OFFER, description);
        //7.setRemoteDescription
        receivePeerConnection.setRemoteDescription(new SdpAdapter("Remote"), fromSdp);//被叫方设置会话描述
        //8.createAnswer
        receivePeerConnection.createAnswer(new SdpAdapter("remote answer sdp") {//发送sdp
            @Override
            public void onCreateSuccess(SessionDescription sdp) {
                super.onCreateSuccess(sdp);
                //9.setLocalDescription
                receivePeerConnection.setLocalDescription(new SdpAdapter("Local"), sdp);//服务器中转设置会话描述
                //10.发送sdp给对方 对方设置远端sdp
                String message = GsonUtil.GsonToString(new NetBean.SdpMessage(Constant.Message_SDP_Answer_Code, sdp.description));
                NetBean.SendMsgRequestBean sendMsgRequestBean = new NetBean.SendMsgRequestBean(Constant.SendMsg_Code, System.currentTimeMillis() + "", toUserId, fromUserId, message);
                RtcWebSokcetHelper.getInstance().sendRequest(GsonUtil.BeanToJson(sendMsgRequestBean));
            }
        }, new MediaConstraints());
//        //14.receive  iceCandidate
//        receivePeerConnection.addIceCandidate(null);

    }


    private void initView() {
        etWsUrl = (EditText) findViewById(R.id.et_wsUrl);
        btnConnectSever = (Button) findViewById(R.id.btn_connectSever);
        etLoginUserId = (EditText) findViewById(R.id.et_loginUserId);
        btnLogin = (Button) findViewById(R.id.btn_login);
        etUserInfo = (EditText) findViewById(R.id.et_userInfo);
        etCallUserId = (EditText) findViewById(R.id.et_callUserId);
        btnCall = (Button) findViewById(R.id.btn_call);
        localView = (SurfaceViewRenderer) findViewById(R.id.localView);
        remoteView = (SurfaceViewRenderer) findViewById(R.id.remoteView);
    }

    private void showCustomeDialog(String title, String leftButtonText, View.OnClickListener leftOnClickListener, String rightButtonText, View.OnClickListener rightOnClickListener) {
        Dialog dialog = new Dialog(this);
        ConstraintLayout constraintLayout = new ConstraintLayout(this);
        constraintLayout.setLayoutParams(new ConstraintLayout.LayoutParams(1000, 600));
        TextView textView = new TextView(this);
        textView.setText(title);
        textView.setTextSize(20);
        constraintLayout.addView(textView);
        ConstraintLayout.LayoutParams textViewLayoutParams = new ConstraintLayout.LayoutParams(-2, -2);
        textViewLayoutParams.startToStart = 0;
        textViewLayoutParams.endToEnd = 0;
        textViewLayoutParams.topToTop = 0;
        textViewLayoutParams.topMargin = 80;
        textView.setLayoutParams(textViewLayoutParams);
        Button leftButton = new Button(this);
        Button rightButton = new Button(this);
        leftButton.setText(leftButtonText);
        rightButton.setText(rightButtonText);
        leftButton.setTextSize(20);
        rightButton.setTextSize(20);
        constraintLayout.addView(leftButton);
        constraintLayout.addView(rightButton);
        ConstraintLayout.LayoutParams leftButtonLayoutParams = new ConstraintLayout.LayoutParams(350, 150);
        ConstraintLayout.LayoutParams rightButtonLayoutParams = new ConstraintLayout.LayoutParams(350, 150);
        leftButtonLayoutParams.startToStart = 0;
        leftButtonLayoutParams.bottomToBottom = 0;
        rightButtonLayoutParams.endToEnd = 0;
        rightButtonLayoutParams.bottomToBottom = 0;
        leftButton.setLayoutParams(leftButtonLayoutParams);
        rightButton.setLayoutParams(rightButtonLayoutParams);
        leftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (leftOnClickListener != null) {
                    leftOnClickListener.onClick(v);
                }
                dialog.dismiss();
            }
        });
        rightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (rightOnClickListener != null) {
                    rightOnClickListener.onClick(v);
                }
                dialog.dismiss();
            }
        });
        dialog.setContentView(constraintLayout);
        dialog.getWindow().setLayout(1000, 600);
        dialog.show();
    }


}
