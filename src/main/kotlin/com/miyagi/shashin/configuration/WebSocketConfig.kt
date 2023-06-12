package com.miyagi.shashin.configuration

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer


@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig : WebSocketMessageBrokerConfigurer {
    override fun configureMessageBroker(config: MessageBrokerRegistry) {
        val heartbeatServer: Long = 10000 // 10 seconds
        val heartbeatClient: Long = 10000 // 10 seconds

        val ts = ThreadPoolTaskScheduler()
        ts.poolSize = 2
        ts.setThreadNamePrefix("wss-heartbeat-thread-")
        ts.initialize()

        config.enableSimpleBroker("/topic")
            .setHeartbeatValue(longArrayOf(heartbeatServer, heartbeatClient))
            .setTaskScheduler(ts)
        config.setApplicationDestinationPrefixes("/app")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/websocket-endpoint").setAllowedOriginPatterns("*")
        registry.addEndpoint("/websocket-endpoint").setAllowedOriginPatterns("*").withSockJS()
    }
}