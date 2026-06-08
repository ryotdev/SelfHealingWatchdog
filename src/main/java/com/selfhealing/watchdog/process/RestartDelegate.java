package com.selfhealing.watchdog.process;

import com.selfhealing.watchdog.docker.DockerService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Service-Task "Neustart": erhöht den Versuchszähler und startet den Container neu. */
@Component
public class RestartDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(RestartDelegate.class);

    private final DockerService dockerService;

    public RestartDelegate(DockerService dockerService) {
        this.dockerService = dockerService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String containerName = (String) execution.getVariable("containerName");
        Integer previousAttempts = (Integer) execution.getVariable("attempts");
        int attempt = (previousAttempts == null ? 0 : previousAttempts) + 1;
        execution.setVariable("attempts", attempt);

        log.info("Neustart-Versuch {} für Container {}", attempt, containerName);
        dockerService.restart(containerName);
    }
}
