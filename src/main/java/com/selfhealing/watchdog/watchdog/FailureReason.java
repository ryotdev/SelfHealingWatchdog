package com.selfhealing.watchdog.watchdog;

/** Grund, aus dem ein Ziel-Container als ausgefallen gilt. */
public enum FailureReason {

    /** Container existiert nicht (mehr). */
    GONE("gone"),

    /** Container existiert, läuft aber nicht. */
    NOT_RUNNING("not-running"),

    /** Container läuft, ist laut HEALTHCHECK aber unhealthy. */
    UNHEALTHY("unhealthy");

    private final String label;

    FailureReason(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
