package com.selfhealing.watchdog.docker;

import java.util.List;

/**
 * Schlanke Kapselung der Docker-API für den Watchdog. Hinter dem Interface, damit der
 * Watchdog ohne echten Docker-Daemon getestet werden kann.
 */
public interface DockerService {

    /** Namen der konfigurierten Ziel-Container, die in Docker tatsächlich existieren. */
    List<String> listTargetContainers();

    /**
     * Inspiziert einen Container und liefert Lauf-Zustand und Health-Status.
     *
     * @param containerName Name oder ID des Containers
     */
    ContainerStatus inspect(String containerName);
}
