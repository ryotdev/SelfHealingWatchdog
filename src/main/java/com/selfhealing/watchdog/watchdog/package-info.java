/**
 * Kern der Überwachung: prüft periodisch den Zustand der beobachteten Container und
 * stößt bei einem Ausfall den BPMN-Wiederherstellungsprozess an. Entscheidet das
 * <em>Wann</em> der Überwachung und Eskalation, nicht das <em>Wie</em> des Docker-Zugriffs.
 */
package com.selfhealing.watchdog.watchdog;
