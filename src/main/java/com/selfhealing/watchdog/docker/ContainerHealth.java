package com.selfhealing.watchdog.docker;

/**
 * Health-Zustand eines Containers. {@link #NONE} bedeutet, dass kein Docker-HEALTHCHECK
 * definiert ist (z. B. {@code target-service-b}).
 */
public enum ContainerHealth {
    HEALTHY,
    UNHEALTHY,
    STARTING,
    NONE
}
