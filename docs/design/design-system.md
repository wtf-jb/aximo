# Fitnesstracker Design System

Light-first, funktional, nicht langweilig: ein ruhiger, heller Grund, dunkle Inverse-Flächen für das Wichtigste und ein einziger Koralle-Akzent für die nächste Aktion. Zahlen sind die Helden der App.

Quelle: die Mockups im Canvas „Fitnesstracker – Key Screens“ (Heute, Workout aktiv, Statistik, KI-Review, Pläne, Routine, Übungsauswahl, Cardio, Einstellungen, Übungsdetail, Abschluss, Dark-Varianten). Die Werte hier gelten; wo Mockups um 2px abweichen, wird beim Umsetzen auf die Tokens gerundet.

## Prinzipien

1. **Eine Aktion leuchtet.** `accent` markiert nur, was als Nächstes zu tun ist: „Workout starten“, den aktiven Satz, den Haken zum Abschließen, den laufenden Timer. Alles andere bleibt neutral. Steht Koralle zweimal gleichwertig auf einem Screen, ist einer davon falsch.
2. **Inverse für das Wichtigste.** Pro Screen höchstens eine große `inverse-surface`-Fläche: Hero-Karte, Timer-Bar, KI-Zusammenfassung, Abschluss-Bilanz. Im Dark Theme kippt sie hell, das Prinzip bleibt.
3. **Zahlen zuerst.** Gewichte, Wiederholungen, Zeiten und Kennzahlen stehen groß in `display`/`title`-Stilen und immer tabellarisch (`tnum`), damit Spalten nicht springen.
4. **Vorschlag statt Automatik.** Alles, was Regel oder KI ändern wollen, erscheint als Diff (alt durchgestrichen in `ink-muted` → neu in `ink`, fett) mit Begründung und expliziten Buttons.
5. **Daumen-tauglich.** Jede Tap-Fläche ≥ `size-touch` (44px); ein Satz ist mit höchstens zwei Taps geloggt.

## Farbe

Neutrale Basis in drei Ebenen: `ground` (Seite) → `surface` (Karte) → `surface-sunken` (Fläche in der Karte). Text immer `ink`, Sekundäres `ink-muted`, Vorbelegtes `ink-placeholder`.

| Rolle | Token | Typischer Einsatz |
| --- | --- | --- |
| Nächste Aktion | `accent` + `on-accent` | Primär-CTA, aktiver Satz, Skip, Fortschritt |
| Hinweis | `accent-container` + `on-accent-container` | Progression „+2,5“, Kategorie-Chips, aktiver Nav-Tab |
| Betonung | `inverse-surface` + `on-inverse` | Hero, Timer-Bar, „Übernehmen“, erledigte Haken |
| Daten | `accent-chart`, `chart-band`, `chart-grid`, `chart-area` | Diagramme, Volumen, Heatmap |

Regeln:
- Auf `accent` steht **dunkle** Schrift (`on-accent`). Weiß verfehlt den Kontrast.
- Erledigt = `inverse-surface`-Kreis mit Haken, nicht Grün. Unterschiede tragen Helligkeit, nicht nur Farbton.
- In Diagrammen: innerhalb des Ziels `ink`, unter Ziel `accent-chart`. Kraft = `accent-chart`, Cardio = `inverse-surface`.
- Kein Material You / Dynamic Color: die Marke ist fix.

## Typografie

Zwei Familien, beide Google Fonts unter OFL:
- **Bricolage Grotesque** (`display`, 600–800) für Titel und große Zahlen.
- **Manrope** (`body`, 400–800) für alles andere.

Gewichte sind bewusst schwer: Labels 700–800, Fließtext 500. `overline` und `micro` stehen in Versalien mit Laufweite. Zahlen im deutschen Format: `82,5 kg`, `11.840 kg`, `5:31 /km`, schmale Malzeichen `3 × 8`.

## Abstände und Form

- Raster in 2er-Schritten von `space-4` bis `space-28`. Seitenrand Haupt-Tabs `space-20`, Detail-Screens `space-16`, Workout `space-12`.
- Radien wachsen mit der Fläche: Pill `radius-md` → Kachel `radius-lg` → Karte `radius-xl`/`radius-2xl` → Hero `radius-3xl`. Kreise und Chips `radius-full`.
- Schatten nur für Schwebendes (`shadow-float`). Karten heben sich allein durch Fläche ab, nie durch Rahmen oder Schatten.

## Ikonografie

Linien-Icons auf 24er-Raster, 2px Strich, runde Enden und Ecken, Farbe = Textfarbe. Gefüllt nur: Play, Haken in Kreis, Drei-Punkte-Menü. Größen über `size-icon`. Umsetzung mit **Lucide** (ISC-Lizenz) als `ImageVector`, ohne Google-Abhängigkeit. Keine Emojis, keine Illustrationen.

## Sprache und Ton

Deutsch, Du, knapp. Buttons sind Verben („Übernehmen“, „Verwerfen“, „Workout starten“). KI-Begründungen nennen die Zahl, die sie stützt („9 Sätze, Ziel 10–20“). Keine Ausrufezeichen, kein Coach-Pathos. Englisch über i18n-Ressourcen, Texte nie im Code.

## Barrierefreiheit

- Text ≥ 4,5:1 auf seinem Grund in beiden Themes; Grafik und Fokus ≥ 3:1. Ausnahme: `line-control` (siehe Token-Notiz).
- Icon-only-Buttons haben eine `contentDescription`.
- Der Rest-Timer wird zusätzlich per Vibration und Notification gemeldet, nicht nur visuell.

## Komponenten

### Button

Sechs Varianten mit klarer Rangfolge; pro Screen höchstens ein Primär-Button.

| Variante | Einsatz | Tokens |
| --- | --- | --- |
| Primär | Die eine nächste Aktion: „Workout starten“, „Speichern“ | `accent`, `on-accent`, `size-cta`, `radius-lg` |
| Inverse | Bestätigen ohne CTA-Rang: „Übernehmen“, „Beenden“ | `inverse-surface`, `on-inverse`, `radius-full` |
| Sekundär | Hilfsaktionen in Karten: „+ Satz“, „Supersatz“, Export | `surface-sunken`, `radius-md` |
| Outline | Gegenstück zu Inverse: „Verwerfen“ | Rahmen `line-control` |
| Text | Leise Aktionen: „Alle übernehmen“, „Rückgängig“ | `on-accent-container` |
| Icon | Navigation, Optionen; Akzent-Variante nur für Timer-Skip | `size-touch`, `radius-full` |

- Höhe nie unter `size-touch` (44).
- Beschriftung ist ein Verb, kein „OK“.
- Icon-only immer mit `contentDescription`.
- Auf `accent` nie weiße Schrift.

### Chip

Kleine, nicht klickbare Kennzeichnungen; klickbare Filter sind Sekundär-Buttons in `radius-full`.

| Variante | Einsatz | Tokens |
| --- | --- | --- |
| Hinweis | Progression, Trend: „+2,5“, „+9,1 % in 3 M“ | `accent-container`, `on-accent-container` |
| Kategorie | Art eines KI-Vorschlags: VOLUMEN, ÜBUNG, PERIODISIERUNG | dto., `micro` |
| Gruppe | SUPERSATZ A, PR | `inverse-surface`, `on-inverse`, `micro` |
| Badge | NEU, EIGENE | `surface-muted`, `radius-xs` |
| Satz | Satz im Verlauf: „82,5 × 8 · R2“; Warm-up getönt | `surface-sunken` bzw. `accent-container`, `radius-sm` |

- Versalien nur bei Kategorie, Gruppe und Badge.
- Höchstens ein Hinweis-Chip pro Karte.

### SetRow

Eine Zeile pro Satz im Workout-Logging, Raster `Satz | kg | Wdh. | RIR | Haken`.

Zustände:
- **Erledigt:** Zeile auf `surface-sunken`, Werte in `ink-muted`, Haken als `inverse-surface`-Kreis. Tap auf die Zeile öffnet sie wieder zum Bearbeiten.
- **Aktiv:** genau eine Zeile. Gewicht und Wdh. als Pills mit `accent`-Rahmen, RIR mit `line-control`-Rahmen, Abschließen als `accent`-Kreis.
- **Offen:** Pills mit Vorschlagswerten in `ink-placeholder`. Der Vorschlag kommt aus der Progressionsregel und wird beim Abschließen unverändert übernommen, wenn niemand tippt.
- **Warm-up:** Satznummer wird zum `W`-Badge (`accent-container`); zählt nicht in Volumen und Bestwerte.

Weitere Satztypen (Drop, Failure) als Badge an derselben Stelle: `D`, `F`.

- Pills sind `size-input` (46) hoch, Tap öffnet einen Zahlen-Stepper (±Inkrement) mit Tastatur-Fallback.
- Abschließen startet den Rest-Timer.
- Spalten nutzen tabellarische Ziffern.

### RestTimer

Schwebende Leiste am unteren Rand des Workout-Screens, solange eine Pause läuft.

- Fläche `inverse-surface`, `radius-2xl`, `shadow-float`, Abstand `space-12` zu den Rändern.
- Countdown in `display`-Familie 34px, daneben die nächste Aktion.
- „+15 s“ auf `inverse-raised`, Überspringen als `accent`-Kreis.
- Fortschritt läuft von voll nach leer in `accent`.
- Parallel: laufende Notification mit Restzeit, bei Ablauf Vibration und Ton. Die Leiste ist nur die sichtbare Hälfte.
- Bei Supersätzen startet sie erst nach der letzten Übung der Runde.

### SuggestionCard

Ein einzelner Änderungsvorschlag aus Progressionsregel, Wochen-Review oder Coach-Chat, immer mit Bestätigung.

Aufbau von oben nach unten:
1. Kategorie-Chip (VOLUMEN, ÜBUNG, PROGRESSION, PERIODISIERUNG) und Kontext (Routine).
2. Titel in `title-s`.
3. Diff-Box auf `surface-sunken`: je Änderung eine Zeile `Feld · alt (durchgestrichen, ink-muted) → neu (fett, ink)`; neue Einträge mit NEU-Badge.
4. Begründung in einem Satz, mit der Zahl, die sie stützt.
5. „Verwerfen“ (Outline) und „Übernehmen“ (Inverse) als gleich breites Paar.

Zustände:
- Offen.
- Übernommen: Buttons weichen einem Haken-Label plus „Rückgängig“ als Text-Button.
- Verworfen: Karte klappt weg, Undo-Snackbar.

Der Inhalt kommt als validiertes JSON (`AiSuggestion.payloadJson`). Die Karte zeigt nie Freitext der KI ohne Schema.

### SegmentedControl

Wechsel zwischen 2–4 gleichrangigen Ansichten derselben Daten: Zeitraum, Tab im Übungsdetail, kg/lbs.

- Spur `track`, aktives Segment `segment-active` mit `shadow-segment`, Text aktiv `ink` 800, sonst `ink-muted` 700.
- Segmente gleich breit; Segmenthöhe 40 in einer 48 hohen Spur.
- Mehr als vier Optionen: stattdessen ein Auswahl-Button mit Menü.
- Semantik: `Role.Tab` bzw. `Role.RadioButton`, Auswahl ansagen.

### NavigationBar

Untere Leiste mit vier festen Tabs: Heute, Pläne, Statistik, Coach.

- Höhe `size-nav` (80), Fläche `surface`, obere Kante `line`.
- Aktiver Tab: Indikator-Pill 60 × 32 in `accent-container`, Icon `on-accent-container`, Label `ink` 800. Inaktiv: Icon und Label `ink-muted` 600.
- Der Coach-Tab bleibt sichtbar, auch ohne KI-Provider; er führt dann zur Einrichtung.
- Im Workout und in Detail-Screens ist die Leiste ausgeblendet.
- Compose: `NavigationBar` mit eigenen `NavigationBarItemColors`, Indicator = `primaryContainer`.

### StatTile

Eine Kennzahl mit Label, im 2er- oder 3er-Raster innerhalb einer Karte.

- Fläche `surface-sunken`, Radius 16, Padding `space-12`.
- Wert in `display`-Familie 20/800, Label `ink-muted` 12/600 darunter.
- Akzent-Variante (`accent-container`) nur für das eine Highlight, z. B. neue PRs.
- Auf `inverse-surface` (Abschluss-Bilanz) liegen die Kacheln auf `inverse-raised`.
- Werte ohne Einheit, wenn das Label sie trägt („Volumen (kg)“).

### Switch

Ein/Aus für Einstellungen, immer rechts in einer Zeile mit Label und Zustandszeile.

- Spur 52 × 32. Ein: `inverse-surface` mit `accent`-Knopf. Aus: `ink-placeholder` mit `surface`-Knopf.
- Die Aus-Spur ist bewusst dunkler als im Mockup (#D6D6D1), damit Knopf und Spur ≥ 3:1 erreichen.
- Die ganze Zeile ist die Tap-Fläche.
- Wirkt sofort, ohne Speichern-Button.
- Compose: `Switch` mit eigenen `SwitchColors` (checkedTrack = inverseSurface, checkedThumb = primary).

