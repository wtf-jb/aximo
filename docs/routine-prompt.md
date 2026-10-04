# Routine-Prompt

Für die Routine „Aximo: nächstes Inkrement“ (claude.ai/code → Routines). Repo `wtf-jb/aximo`, Zeitplan täglich 9:50 und 18:50.

```
Lies CLAUDE.md und docs/progress.md und arbeite autonom nach CLAUDE.md.

1. Gibt es offene Pull Requests in wtf-jb/aximo? Dann zuerst diese fertig machen: CI-Logs lesen, Fehler beheben, pushen, bis CI grün ist. Bei grünem CI selbst mergen (Squash), docs/progress.md auf main aktualisieren.
2. Danach den nächsten Schritt aus docs/progress.md als ein Inkrement umsetzen: Plan kurz in docs/progress.md, Entscheidungen in docs/decisions.md, Build und Tests grün (lokal, falls das Android SDK verfügbar ist, sonst über GitHub Actions), PR mit Zusammenfassung und „Auf dem Pixel prüfen“-Liste. Den PR selbst mergen, sobald CI grün ist.
3. Nur anhalten bei Dingen, die nur Jonas tun kann (Environment, Zugänge), Stack- oder Regeländerungen und Irreversiblem. Dann den Punkt in docs/progress.md unter „Offen für Jonas“ eintragen und kurz melden.
Zum Schluss eine knappe Zusammenfassung auf Deutsch: was erledigt ist, APK-Link, was Jonas prüfen soll.
```
