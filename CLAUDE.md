# CLAUDE.md – Breachline

## Projektziel
Breachline ist ein **Sandbox**-Mod für Minecraft Java (Fabric), der Taktik-Shooter-Mechaniken im Stil von Rainbow Six Siege nachbaut: Operatoren mit Loadout, zerstörbare und verstärkbare Wände, Hitscan-Waffen, Fallen, Kriechen und Lehnen.
- **Kein Rundenspiel:** kein Rundensystem, keine Phasen, keine Timer, keine festen Teams. Alle Mechaniken sind jederzeit frei in der normalen Minecraft-Welt nutzbar. Ein Rundenmodus kommt höchstens später als optionaler Zusatz.
- Begrenzung über **Vorrat/Cooldown pro Spieler** statt Rundenlimit. Ladungen werden am Spieler gespeichert, nicht im Item.
- **Modus pro Spieler:** Taste G (frei belegbar) bzw. `/breachline mode` schaltet zwischen „Normal-Minecraft“ (Vanilla, alle Siege-Funktionen aus) und „Siege-Mix“. Der Zustand liegt serverseitig am Spieler.
- Mehrspieler immer mitdenken: Zustände pro Spieler.
- Etappenplan: `docs/roadmap.md`.
- **Nur die Mechaniken.** Keine Ubisoft-Assets, -Namen, -Logos, -Operator- oder -Gadget-Namen.
- Alle Namen, Texturen und Sounds sind eigene oder Platzhalter.

## Versionen
| | Version |
|---|---|
| Minecraft | 26.3 |
| Java | 25 |
| Fabric Loader | 0.19.5 |
| Fabric API | 0.161.0+26.3 |
| Loom | 1.18-SNAPSHOT (Plugin `net.fabricmc.fabric-loom`) |

Die Versionen stehen in `gradle.properties`. Basis ist die offizielle `fabric-example-mod`.

## Code-Konventionen
- **Der Spielcode ist seit 26.1 unverschleiert.** Verwende Mojangs offizielle Klassennamen (`Player`, `Level`, `Component`, `Commands`, `KeyMapping`). Yarn-Namen aus älteren Tutorials (`PlayerEntity`, `World`, `Text`, `KeyBinding`) gibt es hier nicht.
- Fabric-API-Namen wurden ebenfalls angepasst (z. B. `KeyMappingHelper` statt `KeyBindingHelper`). Im Zweifel im Quellcode von `FabricMC/fabric-api` nachsehen, nicht aus dem Gedächtnis.
- Gefärbte Blöcke sind in 26.x Familien: `Blocks.WOOL.red()`, `Blocks.STAINED_GLASS.blue()` statt `Blocks.RED_WOOL` usw.
- Mod-ID `breachline`, Paket `dev.noah.breachline`. Server/gemeinsamer Code liegt in `src/main`, reiner Client-Code in `src/client`.
- Wo es geht, offizielle Fabric-API nutzen. Mixins nur, wenn es nicht anders geht, und den Grund im Code und im README nennen.
- Recherche-Ergebnisse stehen in `docs/research.md` (Punkt 5: Tasten, Pakete, Screens, Rechte in 26.3).

## Server ist maßgeblich, Werte aus der Konfiguration
- **Der Server entscheidet immer.** Der Client zeigt nur an und schickt Anfragen (Taste, GUI-Regler). Jedes Paket wird auf dem Server geprüft: Rechte, Cooldowns, Min/Max. Nie dem Client vertrauen.
- **Alle Limits und Spielwerte kommen aus der Konfiguration**: Munition, Magazin, Nachladezeit, Schaden, Vorrat, Cooldown, Regeneration, Max-aktiv, PvP, Wechsel-Cooldowns.
- **Keine festen Spielwerte im Code.** Jeder Wert ist ein Konfigurationseintrag mit Standard, Minimum und Maximum und wird beim Setzen und Laden geprüft. Neue Werte kommen in der Etappe dazu, die sie braucht.
- Konfiguration: JSON pro Welt (`<Welt>/breachline/settings.json`), Presets Casual/Realistisch/Chaos, Reset auf Standard, Befehle `/breachline set|get|preset|reset`.
- Rechte: Globale Werte nur mit `Permissions.COMMANDS_MODERATOR` (Level 2), persönliche Optionen (HUD, Taste) für jeden Spieler.
- Ein späterer Rundenmodus ist ein **separates, optionales Modul** und darf den Sandbox-Modus nicht verändern.

## Entschiedene Spielregeln
- **Cross-Mode-Schaden ist symmetrisch:** Ist `mode.cross_mode_damage` aus (Standard), kann weder ein Siege-Spieler einen Normal-Spieler verletzen noch umgekehrt. Das gilt für Siege-Waffen, Fallen, Minen und Vanilla-Waffen. Zwischen Siege-Spielern entscheidet `pvp.enabled` (Standard an).
- **Weiche** Breachline-Wände darf jeder abbauen: Normal-Spieler wie Steinziegel, **Siege-Spieler deutlich langsamer** (ähnlich Obsidian, Faktor in der Konfiguration). **Verstärkte** Wände sind geschützt, außer für Admins (`COMMANDS_MODERATOR`) und im Kreativmodus.
- Moduswechsel ist nach erlittenem Schaden gesperrt (`mode.damage_lock_seconds`, Standard 5 s, in beide Richtungen).
- Beim Wechsel in den Normalmodus werden **nur Breachline-Items** entfernt, Vanilla-Items bleiben unangetastet. Beim Zurückwechseln wird das Loadout **neu vergeben**, es wird nichts geparkt.
- Globale Einstellungen ändern darf, wer `Permissions.COMMANDS_MODERATOR` (Operator-Level 2) hat. Persönliche Optionen darf jeder ändern.
- Offene Fragen stehen in `docs/roadmap.md` unter „Offene Fragen“.

## Arbeitsweise mit Noah (Anfänger)
- Noah lernt Python und hat kein Java-Wissen. Antworte auf Deutsch.
- Jeden Schritt kurz erklären: was gebaut wird und warum. Keine langen Vorträge.
- **Kleine, testbare Etappen**, immer nur eine Etappe pro Schritt. Danach eine kurze Testanleitung geben und auf Noahs „weiter“ warten.
- Bei Unklarheiten höchstens 3 gezielte Fragen stellen, dann einen sinnvollen Vorschlag machen.
- Ehrlich sagen, was in Minecraft nur eingeschränkt geht.
- Am Ende jeder Etappe die `README.md` aktualisieren: was gebaut wurde und was als Nächstes kommt.

## Modelle mit Blockbench (MCP)
- Modelle für Waffen, Gadgets und Fallen erstellt Claude in Blockbench über den MCP-Server (`.mcp.json`, `http://localhost:3000/bb-mcp`), Format **Java Block/Item** (`java_block`).
- **Reihenfolge:** Erst Blockbench (Desktop) mit dem Plugin „MCP Server“ starten, dann Claude Code. Läuft Blockbench nicht, schlägt die Verbindung mit `ECONNREFUSED` fehl. Danach `/mcp` → neu verbinden.
- **Eigene Designs**, keine Ubisoft-Modelle oder -Namen. Texturen werden selbst erstellt.
- **Speicherorte:**
  - Blockbench-Quelldateien: `art/blockbench/<waffen|gadgets|fallen>/<name>.bbmodel` (nicht im Mod-Jar).
  - Exportierte Modelle: `src/main/resources/assets/breachline/models/item/<name>.json`
  - Texturen: `src/main/resources/assets/breachline/textures/item/<name>.png`
- **`risky_eval` nicht benutzen.** Nur die dafür vorgesehenen MCP-Werkzeuge (z. B. `place_cube`, `modify_cube`, `create_texture`, `export_model`).

## Build und Prüfung
- In der Cloud-Umgebung ist `maven.fabricmc.net` gesperrt, ein lokaler Build ist dort nicht möglich.
- **Nach jedem Push den GitHub-Actions-Build (`.github/workflows/build.yml`) prüfen.** Erst wenn er grün ist, gilt eine Etappe als gebaut. Ist er rot, Logs lesen, fixen und erneut pushen.
- Ein grüner Build heißt nur „kompiliert“. Ob es im Spiel funktioniert, testet Noah lokal mit `gradlew.bat runClient`.
- **Branches:** Jede Etappe bekommt einen eigenen Branch (z. B. `etappe-2b-map`). In `main` gemergt wird erst, wenn Noah die Etappe im Spiel getestet und freigegeben hat.
