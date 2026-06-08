package com.selfhealing.watchdog.chaos;

import com.selfhealing.watchdog.config.WatchdogProperties;
import com.selfhealing.watchdog.docker.DockerService;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * <strong>Test-/Demo-Werkzeug — NICHT für den Produktivbetrieb.</strong>
 *
 * <p>Löst Ausfälle gezielt aus, um Self-Healing und Eskalation reproduzierbar zu zeigen.
 * Zur Sicherheit nur auf den konfigurierten Ziel-Containern erlaubt.
 */
@RestController
@RequestMapping("/chaos")
public class ChaosController {

    private static final Logger log = LoggerFactory.getLogger(ChaosController.class);

    private final DockerService dockerService;
    private final Set<String> targetContainers;

    public ChaosController(DockerService dockerService, WatchdogProperties properties) {
        this.dockerService = dockerService;
        this.targetContainers = Set.copyOf(properties.getTargetContainers());
    }

    /** Stoppt einen Ziel-Container (simuliert einen Ausfall → Watchdog startet ihn neu). */
    @PostMapping("/stop")
    public String stop(@RequestParam String name) {
        requireTargetContainer(name);
        dockerService.stop(name);
        log.warn("[CHAOS] Container '{}' gestoppt.", name);
        return "stopped " + name;
    }

    /** Entfernt einen Ziel-Container (Neustart schlägt fehl → Watchdog eskaliert). */
    @PostMapping("/remove")
    public String remove(@RequestParam String name) {
        requireTargetContainer(name);
        dockerService.remove(name);
        log.warn("[CHAOS] Container '{}' entfernt.", name);
        return "removed " + name;
    }

    private void requireTargetContainer(String name) {
        if (!targetContainers.contains(name)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nur konfigurierte Ziel-Container erlaubt: " + targetContainers);
        }
    }
}
