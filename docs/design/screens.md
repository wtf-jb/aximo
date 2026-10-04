# Screens

Die Mockups unter `mockups/` sind die visuelle Referenz: HTML mit Inline-Styles, 390 dp breit, Beispieldaten. Sie sind **keine** Vorlage zum Kopieren von Code, sondern zum Ablesen von Layout, Hierarchie und Abständen. Farben, Schriften und Radien kommen immer aus `tokens.json` bzw. dem Compose-Theme, nie aus den Hex-Werten im HTML.

Höhen über 844 dp (Statistik, Review, Einstellungen, Übungsdetail, Abschluss) zeigen die volle Scrollhöhe.

| Screen | Datei | Prio | Kern |
| --- | --- | --- | --- |
| Heute | `Heute.html` | A | Hero „Nächstes Workout“ mit Progressionsvorschau, Wochenleiste, Review-Hinweis (B), zuletzt |
| Workout aktiv | `Workout.html` | A | Logging-Karte mit SetRows, Progressions-Hinweis, Supersatz-Gruppe, schwebender Rest-Timer; Mikrofon = B-04 |
| Workout-Abschluss | `Abschluss.html` | A | Bilanz, neue Bestwerte, Progression fürs nächste Mal, Gefühl 1–5, Notiz |
| Statistik | `Statistik.html` | A | Zeitraum-Segment, e1RM-Verlauf je Übung, Sätze pro Muskelgruppe mit Zielband, Konsistenz-Heatmap |
| Übungsdetail | `UebungDetail.html` | A | Kennzahlen, nächste Vorgabe, Verlauf mit Satz-Chips; Tabs Diagramm/Bestwerte/Info |
| Pläne | `Plaene.html` | A | Block-Fortschritt, freies Training, Cardio, Routinenliste; „Mit KI erstellen“ = B-03 |
| Routine bearbeiten | `Routine.html` | A | Sortierbare Übungen mit Zielen, Supersatz-Gruppe, Übung hinzufügen |
| Übung hinzufügen | `Uebungen.html` | A | Suche, Muskelgruppen-Filter, Mehrfachauswahl, eigene Übungen |
| Cardio erfassen | `Cardio.html` | A | Typ, Dauer, Distanz, berechnete Pace, optionale Felder; Health-Connect-Import = C-01 |
| Einstellungen | `Einstellungen.html` | A | Sprache, Einheit, Training, KI-Coach (B), Backup/Export, Health Connect (C) |
| KI-Wochen-Review | `Review.html` | B | Zusammenfassung, Vorschlags-Karten mit Diff, Composer für Coach-Chat |
| Dark-Varianten | `*Dark.html` | B | Gleiche Struktur; `inverse-surface` kippt hell |

Navigation: untere Leiste mit Heute · Pläne · Statistik · Coach. Im Workout und in Detail-Screens ausgeblendet. Solange Prio B fehlt, zeigt „Coach“ einen Platzhalter.

Elemente späterer Prios werden in Prio A **weggelassen**, nicht ausgegraut.
