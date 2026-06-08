package com.selfhealing.watchdog.process;

import com.selfhealing.watchdog.docker.ContainerHealth;
import com.selfhealing.watchdog.docker.ContainerStatus;
import com.selfhealing.watchdog.docker.DockerService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Service-Task "Health-Check": prüft, ob der Container wiederhergestellt ist, und legt das
 * Ergebnis in der Prozessvariable {@code recovered} ab.
 *
 * <p>"Wiederhergestellt" = running UND (health == HEALTHY ODER NONE) — dieselbe Regel wie im
 * Watchdog (dort gilt UNHEALTHY als Ausfall, HEALTHY/NONE/STARTING nicht).
 */
@Component
public class HealthCheckDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(HealthCheckDelegate.class);

    private final DockerService dockerService;

    public HealthCheckDelegate(DockerService dockerService) {
        this.dockerService = dockerService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String containerName = (String) execution.getVariable("containerName");
        ContainerStatus status = dockerService.inspect(containerName);
        boolean recovered = status.running()
                && (status.health() == ContainerHealth.HEALTHY || status.health() == ContainerHealth.NONE);
        execution.setVariable("recovered", recovered);

        log.info("Health-Check für {}: running={}, health={} -> recovered={}",
                containerName, status.running(), status.health(), recovered);
    }
}
