package com.selfhealing.watchdog.watchdog;

import com.selfhealing.watchdog.config.WatchdogProperties;
import com.selfhealing.watchdog.docker.ContainerHealth;
import com.selfhealing.watchdog.docker.ContainerStatus;
import com.selfhealing.watchdog.docker.DockerService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Pollt periodisch die konfigurierten Ziel-Container und erkennt Ausfälle.
 * {@code fixedDelay} (nicht {@code fixedRate}), damit sich Läufe nie überlappen.
 */
@Component
public class ContainerWatchdog {

    private static final Logger log = LoggerFactory.getLogger(ContainerWatchdog.class);

    private final DockerService dockerService;
    private final WatchdogProperties properties;

    public ContainerWatchdog(DockerService dockerService, WatchdogProperties properties) {
        this.dockerService = dockerService;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${watchdog.poll-interval}")
    public void poll() {
        for (ContainerFailure failure : detectFailures()) {
            log.warn("Ausfall erkannt: {}, Grund: {}", failure.containerName(), failure.reason().label());
        }
        // Nächster Schritt: bei Ausfall den Camunda-Wiederherstellungsprozess starten.
    }

    /** Wertet alle konfigurierten Ziel-Container aus und liefert die erkannten Ausfälle. */
    List<ContainerFailure> detectFailures() {
        List<String> existingContainers = dockerService.listTargetContainers();
        List<ContainerFailure> failures = new ArrayList<>();
        for (String name : properties.getTargetContainers()) {
            evaluate(name, existingContainers)
                    .ifPresent(reason -> failures.add(new ContainerFailure(name, reason)));
        }
        return failures;
    }

    private Optional<FailureReason> evaluate(String name, List<String> existingContainers) {
        if (!existingContainers.contains(name)) {
            return Optional.of(FailureReason.GONE);
        }
        ContainerStatus status = dockerService.inspect(name);
        if (!status.running()) {
            return Optional.of(FailureReason.NOT_RUNNING);
        }
        if (status.health() == ContainerHealth.UNHEALTHY) {
            return Optional.of(FailureReason.UNHEALTHY);
        }
        // running + HEALTHY/NONE/STARTING gelten nicht als Ausfall.
        return Optional.empty();
    }
}
