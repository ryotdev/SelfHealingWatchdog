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

    /** Namen der zu überwachenden Container. */
    private List<String> targetContainers = List.of();

    private Backoff backoff = new Backoff();

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

    public List<String> getTargetContainers() {
        return targetContainers;
    }

    public void setTargetContainers(List<String> targetContainers) {
        this.targetContainers = targetContainers;
    }

    public Backoff getBackoff() {
        return backoff;
    }

    public void setBackoff(Backoff backoff) {
        this.backoff = backoff;
    }

    public Docker getDocker() {
        return docker;
    }

    public void setDocker(Docker docker) {
        this.docker = docker;
    }

    /** Exponentielles Backoff für die Wartezeit zwischen Neustart-Versuchen. */
    public static class Backoff {

        /** Basis-Wartezeit; Versuch n wartet {@code base * 2^(n-1)} (klein halten für Demo/Eval). */
        private Duration base = Duration.ofSeconds(2);

        /** Obergrenze für die Wartezeit. */
        private Duration maxWait = Duration.ofSeconds(30);

        public Duration getBase() {
            return base;
        }

        public void setBase(Duration base) {
            this.base = base;
        }

        public Duration getMaxWait() {
            return maxWait;
        }

        public void setMaxWait(Duration maxWait) {
            this.maxWait = maxWait;
        }
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
