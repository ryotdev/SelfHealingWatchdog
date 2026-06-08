package com.selfhealing.watchdog.process;

import com.selfhealing.watchdog.config.WatchdogProperties;
import com.selfhealing.watchdog.docker.DockerService;
import java.time.Duration;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Service-Task "Neustart": erhöht den Versuchszähler, berechnet die nächste Wartezeit per
 * exponentiellem Backoff und startet den Container neu.
 */
@Component
public class RestartDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(RestartDelegate.class);

    /** BPMN-Error-Code; wird vom Error-Boundary-Event am Neustart-Task gefangen. */
    public static final String RESTART_FAILED = "RESTART_FAILED";

    private final DockerService dockerService;
    private final WatchdogProperties properties;

    public RestartDelegate(DockerService dockerService, WatchdogProperties properties) {
        this.dockerService = dockerService;
        this.properties = properties;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String containerName = (String) execution.getVariable("containerName");
        Integer previousAttempts = (Integer) execution.getVariable("attempts");
        Object maxAttempts = execution.getVariable("maxAttempts");
        int attempt = (previousAttempts == null ? 0 : previousAttempts) + 1;
        execution.setVariable("attempts", attempt);

        // Backoff für die Wartezeit bis zum Health-Check: base * 2^(attempt-1), gedeckelt.
        Duration wait = backoffFor(attempt);
        execution.setVariable("timerWait", wait.toString());

        log.info("Neustart-Versuch {}/{} für Container {} (Wartezeit bis Health-Check: {})",
                attempt, maxAttempts, containerName, wait);
        try {
            dockerService.restart(containerName);
        } catch (RuntimeException e) {
            // Container nicht vorhanden / Docker-API-Fehler: als BpmnError eskalieren statt Incident.
            log.warn("Neustart von {} fehlgeschlagen: {}", containerName, e.getMessage());
            throw new BpmnError(RESTART_FAILED, "Neustart fehlgeschlagen: " + e.getMessage());
        }
    }

    private Duration backoffFor(int attempt) {
        WatchdogProperties.Backoff backoff = properties.getBackoff();
        // Exponent deckeln, um Long-Überlauf bei sehr vielen Versuchen zu vermeiden.
        int exponent = Math.min(attempt - 1, 16);
        long waitMillis = backoff.getBase().toMillis() * (1L << exponent);
        long cappedMillis = Math.min(waitMillis, backoff.getMaxWait().toMillis());
        return Duration.ofMillis(cappedMillis);
    }
}
