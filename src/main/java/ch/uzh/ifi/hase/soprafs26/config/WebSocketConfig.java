package ch.uzh.ifi.hase.soprafs26.config;

import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Bean(name = "wsHeartbeatScheduler")
    public TaskScheduler wsHeartbeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable heartbeats: server->client every 25s, client->server every 25s
        // This detects stale connections and prevents silent disconnections
        config.enableSimpleBroker("/topic", "/user")
            .setHeartbeatValue(new long[]{25000, 25000})
            .setTaskScheduler(wsHeartbeatScheduler()); 
        config.setApplicationDestinationPrefixes("/app");

        // Enable user-specific messaging for /user/queue/* destinations
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/gs-guide-websocket")
            .setAllowedOriginPatterns(
                // Production
                "https://sopra-fs26-group-24-client.vercel.app",
                "https://www.sopra-fs26-group-24-client.vercel.app",
                // Local development
                "http://localhost:3000",
                "http://127.0.0.1:3000"
            )
            .withSockJS()
                .setStreamBytesLimit(512 * 1024) // Increase buffer size for large messages
                .setHttpMessageCacheSize(1000) // Cache more messages for better performance
                .setDisconnectDelay(30 * 1000); // Increase disconnect delay to handle slow connections
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        // Configure WebSocket transport for App Engine environment
        // These settings prevent timeouts and buffer issues on cloud platforms
        registration.setMessageSizeLimit(128 * 1024)      // 128KB max message size
                    .setSendBufferSizeLimit(512 * 1024)   // 512KB send buffer
                    .setSendTimeLimit(20000);               // 20s timeout for sends
    }

    // find sources here: https://spring.io/guides/gs/messaging-stomp-websocket

}