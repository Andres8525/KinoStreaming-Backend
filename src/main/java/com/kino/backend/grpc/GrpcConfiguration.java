package com.kino.backend.grpc;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GrpcProperties.class)
public class GrpcConfiguration {
}
