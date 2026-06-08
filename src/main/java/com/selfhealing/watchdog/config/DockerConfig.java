package com.selfhealing.watchdog.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(WatchdogProperties.class)
public class DockerConfig {

    /**
     * Docker-Client mit HttpClient5-Transport. Der Host stammt aus den Watchdog-Properties;
     * der Client baut die Verbindung erst beim ersten Kommando auf, nicht schon hier.
     */
    @Bean
    DockerClient dockerClient(WatchdogProperties properties) {
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(properties.getDocker().getHost())
                .build();

        ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .build();

        return DockerClientImpl.getInstance(config, httpClient);
    }
}
