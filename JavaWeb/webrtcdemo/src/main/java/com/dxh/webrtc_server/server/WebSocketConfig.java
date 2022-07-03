package com.dxh.webrtc_server.server;

import com.dxh.webrtc_server.util.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

//这个配置类很简单，通过这个配置 spring boot 才能去扫描后面的关于 websocket 的注解
@Configuration
@EnableWebSocket
public class WebSocketConfig {
    /**
     * 给spring容器注入这个ServerEndpointExporter对象
     * <p>
     * 检测所有带有@serverEndpoint注解的bean并注册他们。
     */
    @Bean
    public ServerEndpointExporter serverEndpoint() {
        ServerEndpointExporter serverEndpointExporter = new ServerEndpointExporter();
        serverEndpointExporter.setAnnotatedEndpointClasses();
        Logger.e("serverEndpoint ");
        return serverEndpointExporter;
    }

}
