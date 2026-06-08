/**
 * Camunda-Anbindung: der BPMN-Wiederherstellungsprozess (mehrere Restart-Versuche,
 * Health-Check, Eskalation) und die zugehörigen Service-Task-Delegates, die die
 * Schritte des Prozesses an die Docker- und Watchdog-Logik anbinden.
 */
package com.selfhealing.watchdog.process;
