package ch.uzh.ifi.hase.soprafs26.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
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
            .withSockJS();
    }

    // find sources here: https://spring.io/guides/gs/messaging-stomp-websocket

}