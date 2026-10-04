# Breachline

Ein Minecraft-Java-Mod (Fabric), der Mechaniken aus Taktik-Shootern nachbaut: Runden, Teams, zerstörbare Wände, Hitscan-Waffen, Lehnen.
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

### Als Nächstes: Etappe 2, Testmap „Haus“
Zwei Stockwerke, mehrere Räume, Fenster, Türen, Spawn-Bereiche für Angreifer und Verteidiger, generierbar per Befehl.

## Projektstruktur

```
src/main/java/dev/noah/breachline/            Logik für Server und Client (Befehle, Runden, Items)
src/client/java/dev/noah/breachline/client/   Nur Client (HUD, Tasten, Kamera)
src/main/resources/fabric.mod.json             Steckbrief des Mods für Fabric
gradle.properties                              Versionen von Minecraft, Loader, Fabric API
```
