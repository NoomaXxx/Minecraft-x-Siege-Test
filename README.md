# Breachline

Ein **Sandbox**-Mod für Minecraft Java (Fabric) mit Mechaniken aus Taktik-Shootern: Operatoren, zerstörbare und verstärkbare Wände, Hitscan-Waffen, Fallen, Lehnen.
Es gibt **keine Runden, Phasen oder festen Teams**: Alle Mechaniken sind jederzeit frei in der normalen Welt nutzbar.
Jeder Spieler schaltet per Taste **G** zwischen „Normal-Minecraft“ und „Siege-Mix“ um. Alle Spielwerte sind einstellbar (Befehle und GUI), der Server entscheidet immer.
Alle Namen, Texturen und Sounds sind eigene oder Platzhalter, es gibt keine fremden Assets.

| | Version |
|---|---|
| Minecraft | 26.3 |
| Fabric Loader | 0.19.5 |
| Fabric API | 0.161.0+26.3 |
| Java | 25 |
| Gradle | 9.7.1 (kommt über den Wrapper `gradlew`) |

Ab Minecraft 26.x liefert Mojang den Code unverschleiert aus. Der Mod nutzt deshalb die offiziellen Mojang-Namen (`Component`, `Commands`, …) und keine Yarn-Mappings.

## Setup (Windows)

1. **JDK 25 installieren**, z. B. [Microsoft Build of OpenJDK 25](https://learn.microsoft.com/java/openjdk/download) oder [Adoptium Temurin 25](https://adoptium.net/). Im Installer *„Set JAVA_HOME“* anhaken.
   - Prüfen in einem neuen Terminal: `java -version` muss `25` anzeigen.
2. **IntelliJ IDEA Community** installieren (kostenlos), dazu das Plugin **„Minecraft Development“** (Settings → Plugins).
3. **Git** installieren und das Repo klonen: `git clone https://github.com/NoomaXxx/Minecraft-x-Siege-Test.git`
4. In IntelliJ: *Open* → Projektordner wählen → warten, bis Gradle fertig importiert hat (beim ersten Mal 5–15 Minuten, weil Minecraft heruntergeladen wird).
   - Falls IntelliJ meckert: *File → Project Structure → SDK* auf JDK 25 stellen, außerdem *Settings → Build Tools → Gradle → Gradle JVM* auf 25.

Gradle selbst musst du nicht installieren, das macht `gradlew.bat` automatisch.

## Starten und testen

Im Projektordner (Terminal in IntelliJ unten):

```
gradlew.bat runClient
```

Alternativ in IntelliJ oben rechts die Run-Konfiguration **Minecraft Client** wählen.

Die fertige Mod-Datei entsteht mit `gradlew.bat build` unter `build/libs/breachline-0.1.0.jar`.

## Stand

### Etappe 1: Projekt-Setup ✅
- Projekt auf Basis der offiziellen `fabric-example-mod`, umbenannt zu `breachline` (Paket `dev.noah.breachline`).
- Beim Start loggt der Mod `Breachline geladen - Taktik-Mod ist bereit.` (und auf dem Client zusätzlich `Breachline Client-Teil geladen.`).
- Befehl `/breachline ping` antwortet im Chat mit `Breachline laeuft! Hallo <Name>.`
- Registrierung über die offizielle Fabric-API (`CommandRegistrationCallback`). Mixins werden noch keine gebraucht.
- GitHub Actions baut den Mod bei jedem Push automatisch (`.github/workflows/build.yml`).

**Test:** `gradlew.bat runClient` → neue Einzelspielerwelt mit *Cheats an* → `/breachline ping` eingeben → Chatnachricht erscheint. Im Log (Konsole oder `run/logs/latest.log`) steht die Lade-Nachricht.

### Etappe 2: Testmap „Haus“ ✅
- `/breachline map build` baut ein Haus per Java-Code (keine .nbt-Datei), relativ zur Spielerposition. Der Spieler steht danach mitten im Angreifer-Spawn.
- Zwei Stockwerke mit je 6 Räumen (12 insgesamt), Fenster, Holztüren, offene Durchgänge, eine Treppe und ein Flachdach mit Brüstung.
- **Alle Wände** (innen und außen) bestehen aus `HouseBuilder.WALL_BLOCK` (zurzeit Steinziegel). In Etappe 4 wird er durch einen eigenen Block ersetzt.
- Spawn-Bereiche außerhalb des Hauses:
  - Angreifer: südlich, rote Wolle mit roten Glas-Pfosten in den Ecken.
  - Verteidiger: nördlich, blaue Wolle mit blauen Glas-Pfosten in den Ecken.
- `MapLayout` speichert alle Koordinaten (Haus, Spawns) und die Position der aktuellen Map. Die Map dient jetzt als **Testgelände** für die Sandbox-Mechaniken. Die Spawns nutzt später ein optionaler Rundenmodus.
- `/breachline map clear` entfernt die Map wieder. Ein erneutes `build` entfernt die alte Map automatisch.
- Befehle liegen jetzt in `command/BreachlineCommands.java`. Mixins werden keine gebraucht.

**Grenzen (bewusst einfach gehalten):**
- `clear` stellt nicht das alte Gelände her: Es räumt den Bereich frei und legt eine flache Grasschicht. Am besten testest du in einer **Superflach-Welt**.
- Die Position der Map wird nur im Arbeitsspeicher gemerkt. Nach einem Neustart des Spiels kennt `clear` sie nicht mehr. Dann einfach neu bauen und wieder entfernen.
- Die Befehle haben noch keine Rechte-Prüfung. Im Einzelspieler ist das egal, auf einem Server würden wir sie auf Admins beschränken.

**Test:** Siehe Testanleitung unten.

### Als Nächstes: Etappe 3a, Konfiguration + Befehle
Zentrales Einstellungssystem: Jeder Wert hat Standard/Min/Max, gespeichert als JSON pro Welt, Presets (Casual, Realistisch, Chaos), Befehle `/breachline set|get|preset|reset`, globale Werte nur für Admins.

Danach folgen:
- **3b:** Modus-Umschalter **G** (Normal-Minecraft ↔ Siege-Mix, pro Spieler auf dem Server), Spielerzustand, Operator-Auswahl, PvP an/aus.
- **3c:** Einstellungs-GUI (Taste **K**, `/breachline settings`).
- **Dann:** Wände, Schießen, Fallen, Operatoren, Bewegung, optionaler Rundenmodus.

**Gesamtplan:** Alle Etappen mit Abhängigkeiten, Risiken, Client-/Server-Aufteilung, Gadget-Begrenzungen und Presets stehen in der **[Roadmap (docs/roadmap.md)](docs/roadmap.md)**.

## Testanleitung Etappe 2

1. `gradlew.bat runClient` → neue Welt, Typ **Superflach**, **Cheats an**.
2. `/breachline map build` eingeben.
   - Chat: „Testmap gebaut. Du stehst im Angreifer-Spawn (rot)…“
   - Du stehst auf roter Wolle, nördlich vor dir das Steinziegel-Haus mit Holztür in der Mitte.
3. Ins Haus gehen und prüfen:
   - Erdgeschoss: 6 Räume, verbunden über Türen und offene Durchgänge.
   - Vom Eingangsraum durch den Durchgang nach links (Südwesten): Holztreppe nach oben in den 1. Stock, dort ebenfalls 6 Räume.
   - Fenster aus Glas in allen Außenwänden, Flachdach mit Rand oben.
4. Durch die Hintertür (Norden) raus: Dort liegt der blaue Verteidiger-Spawn.
5. `/breachline map clear` → Chat: „Testmap entfernt.“ Das Haus ist weg, nur flaches Gras bleibt.
6. Gegenprobe: Noch einmal `/breachline map clear` → rote Meldung „Keine Testmap bekannt…“.

## Projektstruktur

```
src/main/java/dev/noah/breachline/            Logik für Server und Client
  ├─ Breachline.java                           Einstiegspunkt, registriert alles
  ├─ command/BreachlineCommands.java           Alle /breachline-Befehle
  └─ map/MapLayout.java, HouseBuilder.java     Testmap: Koordinaten und Bau-Code
src/client/java/dev/noah/breachline/client/   Nur Client (HUD, Tasten, Kamera)
docs/roadmap.md                                Etappenplan (Sandbox-Konzept)
docs/research.md                               Technik-Recherche
src/main/resources/fabric.mod.json             Steckbrief des Mods für Fabric
gradle.properties                              Versionen von Minecraft, Loader, Fabric API
```
