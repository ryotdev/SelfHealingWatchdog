package com.selfhealing.watchdog.watchdog;

import com.selfhealing.watchdog.config.WatchdogProperties;
import com.selfhealing.watchdog.docker.ContainerHealth;
import com.selfhealing.watchdog.docker.ContainerStatus;
import com.selfhealing.watchdog.docker.DockerService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.camunda.bpm.engine.RuntimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Pollt periodisch die konfigurierten Ziel-Container und erkennt Ausfälle.
 * {@code fixedDelay} (nicht {@code fixedRate}), damit sich Läufe nie überlappen.
 * Bei einem Ausfall wird der Camunda-Wiederherstellungsprozess gestartet.
 */
@Component
public class ContainerWatchdog {

    private static final Logger log = LoggerFactory.getLogger(ContainerWatchdog.class);

    /** Key des BPMN-Prozesses (siehe {@code process/recovery.bpmn}). */
    static final String RECOVERY_PROCESS_KEY = "container-recovery";

    private final DockerService dockerService;
    private final WatchdogProperties properties;
    private final RuntimeService runtimeService;

    public ContainerWatchdog(DockerService dockerService, WatchdogProperties properties,
            RuntimeService runtimeService) {
        this.dockerService = dockerService;
        this.properties = properties;
        this.runtimeService = runtimeService;
    }

    @Scheduled(fixedDelayString = "${watchdog.poll-interval}")
    public void poll() {
        try {
            for (ContainerFailure failure : detectFailures()) {
                triggerRecovery(failure);
            }
        } catch (RuntimeException e) {
            // z. B. Docker-Daemon nicht erreichbar: warnen, nicht abstürzen, beim nächsten Poll weiter.
            log.warn("Poll übersprungen — Überwachung fehlgeschlagen (Docker erreichbar?): {}", e.getMessage());
        }
    }

    /**
     * Startet den Wiederherstellungsprozess für einen ausgefallenen Container — aber nur, wenn
     * nicht bereits eine aktive Instanz für diesen Container (Business Key) läuft (De-Dup).
     */
    private void triggerRecovery(ContainerFailure failure) {
        String containerName = failure.containerName();
        if (hasActiveRecovery(containerName)) {
            log.info("Wiederherstellung für {} läuft bereits — kein erneuter Start (Grund: {}).",
                    containerName, failure.reason().label());
            return;
        }
        log.warn("Ausfall erkannt: {}, Grund: {} — starte Wiederherstellungsprozess.",
                containerName, failure.reason().label());

        Map<String, Object> variables = new HashMap<>();
        variables.put("containerName", containerName);
        variables.put("attempts", 0);
        variables.put("maxAttempts", properties.getRestartAttempts());
        // Die Backoff-Wartezeit (Prozessvariable "timerWait") setzt der RestartDelegate je Versuch,
        // bevor das Timer-Event sie liest.

        runtimeService.startProcessInstanceByKey(RECOVERY_PROCESS_KEY, containerName, variables);
    }

    private boolean hasActiveRecovery(String containerName) {
        return runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(RECOVERY_PROCESS_KEY)
                .processInstanceBusinessKey(containerName)
                .active()
                .count() > 0;
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
