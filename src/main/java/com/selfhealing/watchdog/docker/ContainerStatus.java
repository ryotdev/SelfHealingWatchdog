package com.selfhealing.watchdog.docker;

/**
 * Momentaufnahme eines Containers: Lauf-Zustand und (falls vorhanden) Health-Status.
 *
 * @param name    Container-Name (ohne führenden Slash)
 * @param id      Container-ID
 * @param running ob der Container gerade läuft
 * @param state   roher Docker-State ("running", "exited", ...)
 * @param health  Health-Status oder {@link ContainerHealth#NONE} ohne HEALTHCHECK
 */
public record ContainerStatus(
        String name,
        String id,
        boolean running,
        String state,
        ContainerHealth health) {
}
