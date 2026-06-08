package com.selfhealing.watchdog.watchdog;

/** Ein erkannter Ausfall eines Ziel-Containers. */
public record ContainerFailure(String containerName, FailureReason reason) {
}
