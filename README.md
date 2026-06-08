# Self-Healing Container Watchdog

Ein Spring-Boot-Dienst, der Docker-Container überwacht und bei einem Ausfall die
Wiederherstellung über einen eingebetteten **Camunda-BPMN-Prozess** steuert. Der Watchdog
pollt periodisch den Zustand konfigurierter Ziel-Container; erkennt er einen Ausfall
(Container fehlt, läuft nicht oder ist *unhealthy*), startet er eine Prozessinstanz, die
mehrere Neustart-Versuche mit **exponentiellem Backoff** unternimmt, nach jedem Versuch einen
**Health-Check** durchführt und – falls die Wiederherstellung scheitert – an einen Menschen
**eskaliert** (User-Task in der Camunda-Tasklist).

## Architektur & Komponenten

Einstiegspunkt ist `WatchdogApplication` (`@SpringBootApplication`, `@EnableScheduling`).
Basispaket: `com.selfhealing.watchdog`.

| Paket | Aufgabe |
|---|---|
| `config` | Bean-Definitionen (Docker-Client mit HttpClient5-Transport) und typsichere `@ConfigurationProperties` (`WatchdogProperties`). |
| `watchdog` | Kern-Überwachung: periodischer Poller (`@Scheduled`, `fixedDelay`), Ausfallerkennung und Start des Recovery-Prozesses (mit De-Dup über Business Key). Das *Wann*. |
| `docker` | Kapselung der docker-java-API hinter `DockerService`: Container auflisten, inspizieren (Running-State + Health), neu starten, stoppen, entfernen. Das *Wie*. |
| `process` | Camunda-Anbindung: BPMN-Prozess (`process/recovery.bpmn`) und die JavaDelegates/TaskListener (`RestartDelegate`, `HealthCheckDelegate`, `EscalationDelegate`). |
| `chaos` | Test-/Demo-Werkzeug: REST-Endpoints, die Ziel-Container gezielt stoppen/entfernen, um Self-Healing und Eskalation auszulösen. **Nicht für Produktion.** |

### Recovery-Prozess (`recovery.bpmn`)

```
Start → Neustart → Timer (Backoff) → Health-Check → Gateway "recovered?"
                ↑                                      ├─ ja            → Ende "erfolgreich"
                └────────── Versuche < max ────────────┤
                                                        └─ sonst        → User-Task "Menschliche Hilfe" → Ende
        (Error-Boundary am Neustart: BpmnError RESTART_FAILED → direkt zum User-Task)
```

- „Wiederhergestellt" = Container *running* UND (`HEALTHY` oder kein Healthcheck) — dieselbe
  Regel wie im Watchdog (`UNHEALTHY` gilt als Ausfall, `STARTING`/`NONE` nicht).
- Zwei Eskalationswege: **erschöpfte Versuche** (Gateway nach `restart-attempts`) und
  **fehlgeschlagener Neustart** (Error-Boundary-Event, statt eines hängenden Incidents).

## Tech-Stack

| Komponente | Version |
|---|---|
| Java | 21 (Temurin) |
| Spring Boot | 3.5.3 |
| Camunda BPM 7 | 7.24.0 (embedded, inkl. Webapp) |
| docker-java | 3.5.1 (`docker-java-core` + `docker-java-transport-httpclient5`) |
| H2 | In-Memory (Prozess-Engine) |
| Maven | 3.9.x |

Kompatibilität: Laut offizieller Camunda-Matrix ist `7.24.x → Spring Boot 3.5.x` abgedeckt.

## Voraussetzungen

- **JDK 21** (`JAVA_HOME` darauf gesetzt)
- **Maven** 3.9+
- **laufender Docker-Daemon** (Docker Desktop). Der Dienst startet auch ohne Docker, kann dann
  aber nicht heilen — der Poller loggt eine Warnung und macht weiter.

## Build & Start

```powershell
docker compose up -d        # Ziel-Container target-service-a/-b/-c starten
mvn spring-boot:run         # Watchdog starten (Port 8080)
```

- **Camunda Cockpit / Tasklist / Admin:** http://localhost:8080/camunda — Login **`admin` / `admin`**.
  Im Cockpit sind laufende Recovery-Instanzen sichtbar, in der Tasklist die Eskalations-Tasks.
- Build & Tests: `mvn clean package` · nur Tests: `mvn test`.

Die Ziel-Container (`docker-compose.yml`):

| Container | Port | Healthcheck |
|---|---|---|
| `target-service-a` | 8081 | ja (wird `healthy`) |
| `target-service-b` | 8082 | keiner (zum Vergleich) |
| `target-service-c` | 8083 | ja, schlägt **absichtlich immer fehl** (bleibt `unhealthy`) |

Bewusst **ohne** Docker-Restart-Policy — das Heilen übernimmt der Watchdog, nicht Docker.

## Demo: die vier Szenarien

Beobachten lässt sich alles im **Anwendungslog** (Zeilen „Ausfall erkannt", „Neustart-Versuch n/max",
„Health-Check … recovered=…", „Eskalation: …") und im **Cockpit/der Tasklist**.

1. **Stopp → Selbstheilung.** Container stoppen, Watchdog startet ihn neu:
   ```powershell
   curl -X POST "http://localhost:8080/chaos/stop?name=target-service-a"
   ```
   → `not-running` erkannt → Neustart → `recovered=true` → Prozess endet „erfolgreich".

2. **Entfernen → Eskalation über Error-Boundary.** Container löschen, Neustart schlägt fehl:
   ```powershell
   curl -X POST "http://localhost:8080/chaos/remove?name=target-service-b"
   ```
   → `gone` → Neustart wirft `BpmnError(RESTART_FAILED)` → Boundary-Event → User-Task „Menschliche Hilfe".

3. **Dauerhaft unhealthy → Eskalation über Gateway.** `target-service-c` ist von Haus aus
   `unhealthy` und in der Zielliste → der Watchdog schöpft `restart-attempts` aus
   (Backoff `PT2S → PT4S → PT8S`), `recovered` bleibt `false` → Gateway → User-Task. Passiert
   automatisch nach dem Start; **De-Dup + wartender User-Task verhindern Spam**.

4. **Docker nicht erreichbar.** Docker Desktop stoppen → der Poller fängt den Fehler ab, loggt
   eine **Warnung** und läuft beim nächsten Poll weiter (kein Absturz).

Die Eskalation lässt sich in der **Tasklist** (als `admin`) abschließen; der Prozess läuft dann
zum End-Event „Eskaliert".

## Konfiguration (`application.yaml`, Präfix `watchdog`)

| Property | Default | Bedeutung |
|---|---|---|
| `target-containers` | `a`, `b`, `c` | Liste der überwachten Container-Namen. |
| `poll-interval` | `10s` | Abstand zwischen Überwachungsläufen (`fixedDelay`). |
| `restart-attempts` | `3` | Max. Neustart-Versuche vor Gateway-Eskalation. |
| `backoff.base` | `2s` | Basis-Wartezeit; Versuch *n* wartet `base · 2^(n-1)`. |
| `backoff.max-wait` | `30s` | Obergrenze für die Backoff-Wartezeit. |
| `docker.host` | `npipe:////./pipe/docker_engine` | Docker-Host. Linux/Mac: `unix:///var/run/docker.sock`. |

`target-service-c` aus `target-containers` entfernen, wenn die automatische Eskalation beim
Start nicht gewünscht ist.

## Hinweis

Die **Chaos-Endpoints** (`/chaos/stop`, `/chaos/remove`) dienen ausschließlich Demo und Test.
Sie sind ungesichert und wirken nur auf konfigurierte Ziel-Container — **nicht für den
Produktivbetrieb** gedacht.
