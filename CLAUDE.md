# CLAUDE.md – Breachline

## Projektziel
Breachline ist ein **Sandbox**-Mod für Minecraft Java (Fabric), der Taktik-Shooter-Mechaniken im Stil von Rainbow Six Siege nachbaut: Operatoren mit Loadout, zerstörbare und verstärkbare Wände, Hitscan-Waffen, Fallen, Kriechen und Lehnen.
- **Kein Rundenspiel:** kein Rundensystem, keine Phasen, keine Timer, keine festen Teams. Alle Mechaniken sind jederzeit frei in der normalen Minecraft-Welt nutzbar. Ein Rundenmodus kommt höchstens später als optionaler Zusatz.
- Begrenzung über **Vorrat/Cooldown pro Spieler** statt Rundenlimit. Ladungen werden am Spieler gespeichert, nicht im Item.
- Mehrspieler immer mitdenken: Der Server entscheidet, Zustände pro Spieler.
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
- Recherche-Ergebnisse stehen in `docs/research.md`.

## Arbeitsweise mit Noah (Anfänger)
- Noah lernt Python und hat kein Java-Wissen. Antworte auf Deutsch.
- Jeden Schritt kurz erklären: was gebaut wird und warum. Keine langen Vorträge.
- **Kleine, testbare Etappen**, immer nur eine Etappe pro Schritt. Danach eine kurze Testanleitung geben und auf Noahs „weiter“ warten.
- Bei Unklarheiten höchstens 3 gezielte Fragen stellen, dann einen sinnvollen Vorschlag machen.
- Ehrlich sagen, was in Minecraft nur eingeschränkt geht.
- Am Ende jeder Etappe die `README.md` aktualisieren: was gebaut wurde und was als Nächstes kommt.

## Build und Prüfung
- In der Cloud-Umgebung ist `maven.fabricmc.net` gesperrt, ein lokaler Build ist dort nicht möglich.
- **Nach jedem Push den GitHub-Actions-Build (`.github/workflows/build.yml`) prüfen.** Erst wenn er grün ist, gilt eine Etappe als gebaut. Ist er rot, Logs lesen, fixen und erneut pushen.
- Ein grüner Build heißt nur „kompiliert“. Ob es im Spiel funktioniert, testet Noah lokal mit `gradlew.bat runClient`.
- Entwicklungs-Branch: `claude/minecraft-tactical-shooter-mod-j0ygxt`.
