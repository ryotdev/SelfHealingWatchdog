package com.selfhealing.watchdog.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typsichere Watchdog-Einstellungen aus {@code application.yaml} (Präfix {@code watchdog}).
 */
@ConfigurationProperties(prefix = "watchdog")
public class WatchdogProperties {

    /** Abstand zwischen zwei Überwachungsläufen. */
    private Duration pollInterval = Duration.ofSeconds(10);

    /** Maximale Anzahl an Restart-Versuchen, bevor eskaliert wird. */
    private int restartAttempts = 3;

    /** Zeitfenster, in dem ein neugestarteter Container "healthy" werden muss. */
    private Duration healthCheckTimeout = Duration.ofSeconds(30);

    /** Namen der zu überwachenden Container. */
    private List<String> targetContainers = List.of();

    private Docker docker = new Docker();

    public Duration getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(Duration pollInterval) {
        this.pollInterval = pollInterval;
    }

    public int getRestartAttempts() {
        return restartAttempts;
    }

    public void setRestartAttempts(int restartAttempts) {
        this.restartAttempts = restartAttempts;
    }

    public Duration getHealthCheckTimeout() {
        return healthCheckTimeout;
    }

    public void setHealthCheckTimeout(Duration healthCheckTimeout) {
        this.healthCheckTimeout = healthCheckTimeout;
    }

    public List<String> getTargetContainers() {
        return targetContainers;
    }

    public void setTargetContainers(List<String> targetContainers) {
        this.targetContainers = targetContainers;
    }

    public Docker getDocker() {
        return docker;
    }

    public void setDocker(Docker docker) {
        this.docker = docker;
    }

    /** Verbindungseinstellungen für die Docker-API. */
    public static class Docker {

        /**
         * Docker-Host-URI. Default ist die Windows/Docker-Desktop-Named-Pipe; unter Linux/Mac
         * stattdessen {@code unix:///var/run/docker.sock} setzen.
         */
        private String host = "npipe:////./pipe/docker_engine";

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }
    }
}
