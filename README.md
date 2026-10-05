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

### Etappe 2b: Testmap „Familienhaus“ ✅
- Die Testmap ist größer und abwechslungsreicher. Die Befehle bleiben gleich (`/breachline map build|clear`).
- **Haus** mit 25×19 Blöcken, 2 Stockwerken und je 6 Räumen.
- **Keller** unter dem ganzen Haus mit 2 Räumen, Beton und Lampen im Boden. Die Treppe nach unten liegt im Raum hinten links (Nordwesten).
- **Garage** an der Ostseite mit offenem Tor, einem Auto und einer Tür ins Haus.
- **Balkon** im 1. Stock über dem Haupteingang, mit Geländer.
- **Dach** mit 2 Dachluken (Falltüren) und einer Leiter an der Ostwand.
- **Deckung draußen:**
  - Vorgarten (Angreifer-Seite): Mauer, Autowrack, Fässer.
  - Garten (Verteidiger-Seite): Hecken und ein Holzstapel.
- **Materialien:**
  - Außenwände aus Ziegel, Innenwände aus Fichtenholz, Keller aus grauem Beton.
  - Die Konstanten `OUTER_WALL`, `INNER_WALL` und `BASEMENT_WALL` in `HouseBuilder` ersetzen das alte `WALL_BLOCK`.
- Reicht der Platz nach unten nicht für den Keller, wird die Map automatisch angehoben. In einer Superflach-Welt passiert das (2 Blöcke), weil das Bedrock dort nur 4 Blöcke unter dem Gras liegt. Danach wirst du in den Angreifer-Spawn teleportiert.
- Mixins werden keine gebraucht.

**Grenzen:**
- In einer Superflach-Welt bleibt nach `clear` ein 2 Blöcke hohes, flaches Erd-Plateau stehen, weil die Map dort angehoben wurde.
- Funktional getestet, sieht aber noch schlicht aus. Wege zu schöneren Builds stehen in `docs/research.md` unter „Schöne Builds“.

### Etappe 3a: Konfiguration + Befehle ✅
- Zentrales Einstellungssystem in `config/`. Jeder Wert (`Setting`) hat Name, Kategorie, Typ, Standard, Minimum und Maximum.
- Erste Werte (Standard, erlaubter Bereich):

  | Einstellung | Standard | Bereich |
  |---|---|---|
  | `pvp.enabled` | an | an/aus |
  | `mode.cross_mode_damage` | aus | an/aus |
  | `mode.switch_cooldown_seconds` | 10 | 0–600 |
  | `mode.damage_lock_seconds` | 5 | 0–60 |
  | `operator.switch_cooldown_seconds` | 30 | 0–600 |

  Die Werte wirken noch nicht im Spiel. Das kommt in Etappe 3b, die sie liest.
- Gespeichert pro Welt in `<Weltordner>/breachline/settings.json`, nur Werte, die vom Standard abweichen. Beim Laden werden unbekannte Namen ignoriert, falsche Typen auf Standard gesetzt und Werte außerhalb von Min/Max auf die Grenze gesetzt, jeweils mit Hinweis im Log.
- Befehle:
  - `/breachline get [einstellung]` zeigt Werte an (jeder).
  - `/breachline set <einstellung> <wert>` ändert einen Wert, Schalter mit `an`/`aus`. Werte außerhalb von Min/Max werden abgelehnt.
  - `/breachline reset [einstellung]` setzt einen oder alle Werte auf Standard.
  - `/breachline preset casual|realistisch|chaos` setzt die Cooldowns auf Standard × 0,5 / × 2 / × 0 und PvP an.
  - Tab vervollständigt die Namen.
- **Rechte:** `set`, `reset` und `preset` nur mit `Permissions.COMMANDS_MODERATOR` (Operator-Level 2). Ohne das Recht sind die Befehle unsichtbar. Im Einzelspieler mit *Cheats an* hast du es.
- JSON über Gson, das Minecraft schon mitbringt. Gespeichert wird beim Ändern, geladen beim Start der Welt (Fabric `ServerLifecycleEvents`). Mixins werden keine gebraucht.

**Bewusst noch nicht drin** (kommt mit der Etappe, die es braucht):
- Kommazahlen: Es gibt noch keinen Wert, der eine braucht.
- Persönliche Optionen (HUD usw.): kommen in 3b und werden am Spieler gespeichert, nicht in der Welt-Datei.

**Test:** Siehe Testanleitung unten.

### Etappe 3b: Modus-Umschalter + Spielerzustand + Operator-Auswahl (im Test)
- **Modus pro Spieler:** Taste **G** (umbelegbar unter *Optionen → Steuerung → Breachline*) oder `/breachline mode [normal|siege]` schaltet zwischen „Normal-Minecraft“ und „Siege-Mix“ um.
  - Die Taste schickt nur eine Anfrage (`ToggleModePayload`), das ist unser erstes eigenes Netzwerk-Paket. Der Server prüft und antwortet in der Aktionsleiste über der Hotbar.
  - Geprüft wird: der Cooldown `mode.switch_cooldown_seconds` und die Schadenssperre `mode.damage_lock_seconds` (kein Wechsel kurz nach erlittenem Schaden, in beide Richtungen).
- **Wechsel nach Siege:** Du bekommst das Loadout deines Operators neu. **Wechsel nach Normal:** Nur Breachline-Items werden entfernt (Inventar, Rüstung, Nebenhand, 2×2-Werkbank, Item am Mauszeiger), Vanilla-Items bleiben.
- **Breachline-Items** haben eine Markierung in den Item-Daten (`CUSTOM_DATA` mit `breachline_item`). Dazu kommt der Vanilla-„Fluch des Verschwindens“: Beim Tod verschwinden sie, statt herumzuliegen. Nach dem Respawn gibt es ein frisches Loadout.
- **Operatoren (Platzhalter):** `/breachline operator breacher|warden`, ohne Angabe zeigt der Befehl Modus und Operator an. Es gilt der Cooldown `operator.switch_cooldown_seconds`. In Etappe 8 werden die Operatoren durch JSON-Dateien ersetzt.
  - **Breacher:** Eisenaxt, Eisenschwert, Lederhelm
  - **Warden:** Eisenschwert, Schild (Nebenhand), Eisenbrustpanzer
- **Spielerzustand** (Modus, Operator, HUD-Option) hängt am Spieler (Fabric Data Attachment). Er bleibt beim Tod und nach dem Neustart erhalten und wird automatisch an den eigenen Client geschickt. Cooldowns und Schadenssperre werden nicht gespeichert, nach einem Neustart ist alles frei.
- **Schadensregeln** (Fabric `ServerLivingEntityEvents.ALLOW_DAMAGE`), nur zwischen Spielern:
  - Siege gegen Normal (beide Richtungen): nur wenn `mode.cross_mode_damage` an ist (Standard aus).
  - Siege gegen Siege: nur wenn `pvp.enabled` an ist (Standard an).
  - Normal gegen Normal: wie in Vanilla.
  - Der Verursacher kommt aus der Schadensquelle, bei Pfeilen also der Schütze.
- **Mini-HUD** oben links: „SIEGE“ (orange) oder „NORMAL“ (grau). Persönliche Option: `/breachline hud an|aus`, jeder darf sie ändern.
- Mixins werden keine gebraucht.

**Grenzen:**
- Wer ein Breachline-Item mit Q wegwirft, kann es an andere Spieler weitergeben. Entfernt wird es erst, wenn deren Besitzer in den Normalmodus wechselt oder ein neues Loadout bekommt.
- Schaden durch gezähmte Tiere (z. B. Wölfe) zählt nicht als Spielerschaden und wird nicht geregelt.

**Test:** Siehe Testanleitung unten.

### Als Nächstes: Etappe 3c
- **3c:** Einstellungs-GUI (Taste **K**, `/breachline settings`).
- **Dann:** Wände, Schießen, Fallen, Operatoren, Bewegung, optionaler Rundenmodus.

**Gesamtplan:** Alle Etappen mit Abhängigkeiten, Risiken, Client-/Server-Aufteilung, Gadget-Begrenzungen und Presets stehen in der **[Roadmap (docs/roadmap.md)](docs/roadmap.md)**.

## Testanleitung Etappe 3b

1. `gradlew.bat runClient` → neue Welt im **Überlebensmodus** mit **Cheats an**. Oben links steht grau „NORMAL“.
2. Ein paar Vanilla-Items ins Inventar legen (z. B. Erde). **G** drücken → „Modus: SIEGE …“ über der Hotbar, oben links „SIEGE“. Du hast Eisenaxt, Eisenschwert und auf dem Kopf einen Lederhelm. Die Items leuchten (Fluch des Verschwindens).
3. Sofort noch einmal **G** → „Moduswechsel erst in … s“.
4. 10 s warten, **G** → „Modus: NORMAL“. Axt, Schwert und Helm sind weg, die Erde ist noch da.
5. **Schadenssperre:** `/breachline set mode.switch_cooldown_seconds 0`. Von 4 Blöcken Höhe springen und sofort **G** → „Du hast gerade Schaden bekommen …“. Nach 5 s klappt es.
6. **Operator:** `/breachline operator` zeigt Modus und Operator. Im Siege-Modus `/breachline operator warden` → Schwert, Schild in der linken Hand, Eisenbrustpanzer. Der Lederhelm ist weg. Sofort `/breachline operator breacher` → „Operator-Wechsel erst in … s“.
7. **Tod:** Im Siege-Modus `/kill` → am Boden liegen keine Loadout-Items (Vanilla-Items schon). Nach dem Respawn hast du das Loadout neu, oben links steht weiter „SIEGE“.
8. **Speichern:** Welt verlassen und neu laden → Modus und Operator sind gleich geblieben.
9. `/breachline hud aus` → die Anzeige ist weg. `/breachline hud an` → sie ist wieder da.
10. *Optionen → Steuerung → Breachline*: Die Taste „Modus wechseln“ lässt sich auf eine andere Taste legen.
11. **Schadensregeln** (optional, braucht einen 2. Spieler, z. B. per LAN): Siege-Spieler schlägt Normal-Spieler → kein Schaden. Nach `/breachline set mode.cross_mode_damage an` gibt es Schaden. Beide im Siege-Modus mit `/breachline set pvp.enabled aus` → kein Schaden.

## Projektstruktur

```
src/main/java/dev/noah/breachline/            Logik für Server und Client
  ├─ Breachline.java                           Einstiegspunkt, registriert alles
  ├─ command/BreachlineCommands.java           Alle /breachline-Befehle
  ├─ command/SettingsCommands.java             get/set/reset/preset
  ├─ command/PlayerCommands.java               mode/operator/hud
  ├─ config/                                   Einstellungen: Setting, BreachlineSettings, Preset
  ├─ player/                                   Spielerzustand, Modus- und Schadensregeln, Operatoren, Loadout
  ├─ network/ToggleModePayload.java            Paket Taste G (Client → Server)
  └─ map/MapLayout.java, HouseBuilder.java     Testmap: Koordinaten und Bau-Code
src/client/java/dev/noah/breachline/client/   Nur Client: Taste G, Modus-Anzeige
docs/roadmap.md                                Etappenplan (Sandbox-Konzept)
docs/research.md                               Technik-Recherche
src/main/resources/fabric.mod.json             Steckbrief des Mods für Fabric
gradle.properties                              Versionen von Minecraft, Loader, Fabric API
```
