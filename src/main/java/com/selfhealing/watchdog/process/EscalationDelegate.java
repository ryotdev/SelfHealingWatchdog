package com.selfhealing.watchdog.process;

import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Task-Create-Listener am Eskalations-User-Task: hält die Eskalation klar im Log sichtbar
 * (ERROR), während der Prozess am User-Task auf manuellen Abschluss in der Tasklist wartet.
 */
@Component
public class EscalationDelegate implements TaskListener {

    private static final Logger log = LoggerFactory.getLogger(EscalationDelegate.class);

    @Override
    public void notify(DelegateTask delegateTask) {
        String containerName = (String) delegateTask.getVariable("containerName");
        Object attempts = delegateTask.getVariable("attempts");
        Object maxAttempts = delegateTask.getVariable("maxAttempts");
        log.error("Eskalation: {} nicht wiederhergestellt (Versuche {}/{}) — menschliche Hilfe nötig, "
                        + "Task '{}' wartet in der Tasklist.",
                containerName, attempts, maxAttempts, delegateTask.getName());
    }
}
