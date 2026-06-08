package com.selfhealing.watchdog.process;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Service-Task "Menschliche Hilfe": Eskalation, wenn der Container nach allen Versuchen nicht
 * wiederhergestellt werden konnte. Loggt klar auf ERROR-Level.
 */
@Component
public class EscalationDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(EscalationDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {
        String containerName = (String) execution.getVariable("containerName");
        Integer maxAttempts = (Integer) execution.getVariable("maxAttempts");
        log.error("Eskalation: {} nach {} Versuchen nicht wiederhergestellt — menschliche Hilfe nötig.",
                containerName, maxAttempts);
    }
}
