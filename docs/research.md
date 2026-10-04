# Recherche: Technische Umsetzung in Minecraft 26.x + Fabric

Stand: Oktober 2026, Zielversion **Minecraft 26.3, Fabric API 0.161.0, Java 25**.

> **Hinweis (Konzeptänderung):** Breachline ist inzwischen ein Sandbox-Mod ohne Rundensystem, siehe `docs/roadmap.md`. Die Technik in den Punkten 1–4 gilt weiter, die Etappennummern und das Rundensystem dort sind veraltet. **Punkt 5** (Tasten, Pakete, Screens, Rechte, Spielerdaten) ist neu und auf 26.3 geprüft.

> **Vorab, wichtig für alles Weitere:** Seit Minecraft 26.1 ist der Spielcode unverschleiert. Fabric nutzt nur noch Mojangs offizielle Namen (z. B. `Player`, `Level`, `KeyMapping`). Die alten Yarn-Namen (`PlayerEntity`, `World`, `KeyBinding`) aus älteren Tutorials funktionieren nicht mehr. Auch einige Fabric-API-Klassen wurden umbenannt, z. B. `KeyBindingHelper` → `KeyMappingHelper`.
> Quellen: [Fabric: Porting to 26.1](https://docs.fabricmc.net/develop/porting/fabric-api) · [Fabric: Migrating Mappings](https://docs.fabricmc.net/develop/porting/mappings/) · [PaperChunk: Fabric 26.1 Overhaul](https://paperchunk.com/blog/fabric-26-1-biggest-overhaul)

**Wie geprüft:** Die Fabric-Klassennamen unten habe ich direkt im Quellcode von `FabricMC/fabric-api` (Stand 0.161.0+26.3) nachgeschaut. Code anderer Mods habe ich aus deren GitHub-Repos gelesen. Teile, die ich nicht prüfen konnte, sind mit **(ungeprüft)** markiert.

---

## 1. Kriechen/Liegen und seitliches Lehnen

### 1a. Kriechen / Liegen (Pose + Hitbox)

**Wie Minecraft das intern macht:**
- Jede Entity hat eine `Pose` (`STANDING`, `CROUCHING`, `SWIMMING`, …). Die Hitbox hängt direkt an der Pose.
- Die Pose `SWIMMING` ist genau das Kriechen, das man unter Falltüren oder beim Schwimmen sieht. Hitbox: ca. **0,6 × 0,6 Blöcke**, statt 0,6 × 1,8 im Stehen. Der Spieler passt durch 1 Block hohe Lücken und ist langsamer.
- Die Pose wird **automatisch an alle Spieler synchronisiert** (Entity-Daten). Andere Spieler sehen also korrekt, dass du liegst, und deine kleinere Hitbox gilt auch für Treffer.
- **Problem:** `Player.updatePlayerPose()` setzt die Pose **jeden Tick neu**, abhängig von Schwimmen, Schleichen und Platz über dem Kopf. Setzt man die Pose einfach von außen, wird sie im nächsten Tick überschrieben.

**Vorbilder (Open Source):**

| Mod | Was es macht | Code |
|---|---|---|
| GoProne (Alpvax) | Taste „C“ = Kriechen bzw. Liegen umschalten | [github.com/Alpvax/GoProne](https://github.com/Alpvax/GoProne) |
| Crawl on Demand | Kriechen per Taste, es gibt eine Fabric-Version für 26.1 | [Modrinth](https://modrinth.com/mod/crawl-on-demand/version/1.3.0+fabric) |
| Pozitification | Sitzen/Kriechen für 26.1, für andere Spieler synchronisiert | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/pozitification) |

So löst GoProne das Problem (Fabric-Teil, gelesen in `PlayerMixin.java`):
- **Mixin** auf `Player.updatePlayerPose()` mit `@Inject(at = HEAD, cancellable = true)`: Ist eine erzwungene Pose gesetzt, wird sie gesetzt und die Vanilla-Logik übersprungen.
- **Mixin** auf `Player.jumpFromGround()`, um Springen im Liegen zu verbieten.
- Der Client sendet den Tastendruck per Netzwerkpaket an den Server, der Server setzt den Zustand.

**Stabilste Lösung für uns:**
1. Taste (z. B. `C`) über die Fabric-API `KeyMappingHelper` registrieren (Client).
2. Beim Drücken ein eigenes C2S-Paket schicken: `PayloadTypeRegistry.serverboundPlay()`, `ClientPlayNetworking.send`, `ServerPlayNetworking.registerGlobalReceiver`.
3. Der Server speichert pro Spieler „liegt ja/nein“.
4. **Ein kleiner Mixin** auf `Player.updatePlayerPose()` setzt dann `Pose.SWIMMING`, so wie GoProne.
5. Langsamere Bewegung über einen Attribut-Modifier auf `movement_speed`. Das ist Vanilla-API, dafür braucht es keinen Mixin.

**Risiken:**
- Aufstehen unter einer niedrigen Decke: Vor dem Wechsel zurück auf `STANDING` muss geprüft werden, ob Platz ist, sonst steckt der Spieler im Block fest. Vanilla hat dafür eine Prüfung, die wir wiederverwenden.
- Der Mixin hängt am Methodennamen `updatePlayerPose`. Bei Minecraft-Updates kann er brechen. Weil der Code jetzt unverschleiert ist, sieht man das aber sofort beim Build.
- Die Vanilla-Kriech-Animation sieht aus wie Schwimmen an Land. Für Prototypen reicht das, eine eigene Liege-Animation wäre ein großes Extra-Projekt.

### 1b. Seitliches Lehnen (Q/E)

**Ehrliche Einordnung, was geht und was nicht:**

| Aspekt | Machbar? | Erklärung |
|---|---|---|
| Kamera kippen (Roll) und seitlich verschieben | ✅ ja, mit Mixin | Reine Client-Sache, sieht gut aus |
| Um Ecken schauen | ✅ ja | Die Kamera wird z. B. 0,4 Blöcke zur Seite verschoben |
| Andere Spieler sehen das Lehnen | ⚠️ nur mit Aufwand | Braucht ein Sync-Paket und eine Änderung am Spielermodell (Render-Mixin) |
| Hitbox lehnt mit | ❌ praktisch nein | Minecraft-Hitboxen sind achsparallele Quader (AABB) und können nicht kippen |
| Kopf wirklich neben der Wand (kann getroffen werden) | ⚠️ nur über Tricks | Siehe Risiken |

**Vorbilder:**
- **SneakyQoL**: Q/E bewegt die Kamera nach links/rechts, „wie in Rainbow 6 Siege“. Nur clientseitig, andere sehen es nicht. Bis 1.21.4. [Modrinth](https://modrinth.com/mod/sneakyqol)
- **CameraOverhaul** (Open Source, schon auf 26.1 portiert): kippt die Kamera beim Strafen. [github.com/Mirsario/Minecraft-CameraOverhaul](https://github.com/Mirsario/Minecraft-CameraOverhaul)
  - Gelesen in `CameraMixin.java`: Ab 26.1 heißt die Kamera-Methode **`Camera.alignWithEntity(float)`**, vorher hieß sie `setup(...)`. Dort wird mit `@Inject(at = RETURN)` die Rotation (Quaternion) inklusive Roll verändert.
  - Das ist das wichtigste Code-Vorbild für uns.
- **Studio Camera**: Q/E = Kamera-Roll, Fabric 1.21.11. [GitHub](https://github.com/scala01030-sketch/studiocamera)

**Stabilste Lösung für uns:**
1. Q/E als `KeyMapping` registrieren. Achtung: Q ist in Vanilla „Item fallen lassen“, das muss umbelegt oder in den Einstellungen geändert werden.
2. **Ein Client-Mixin** auf `Camera.alignWithEntity` nach dem Muster von CameraOverhaul: Roll ±10–15° und eine kleine seitliche Verschiebung. Den Übergang weich interpolieren, damit es nicht ruckelt.
3. Die seitliche Verschiebung begrenzen: einen kurzen Raycast zur Seite machen, damit die Kamera nicht in die Wand rutscht. Sonst wäre das ein Wallhack.
4. Später optional: den Lean-Status per S2C-Paket an andere Spieler schicken und den Oberkörper im Spielermodell neigen (zusätzlicher Render-Mixin, rein optisch).

**Risiken:**
- **Fairness:** Siehst du um die Ecke, ohne dass deine Hitbox mitkommt, kannst du gesehen werden, ohne getroffen werden zu können. Gegenmittel: Beim Schießen startet der Raycast von der gelehnten Kameraposition aus (der Server kennt den Lean-Status). Andere Spieler bekommen dafür eine zusätzliche kleine „Kopf-Hitbox“, die wir im eigenen Treffer-Code prüfen. Das ist machbar, aber Eigenbau.
- Die Kamera-Position wird nur fürs Rendern verschoben. Was der Spieler anvisieren kann (Block-Auswahl), bleibt am echten Kopf, außer wir passen das an.
- Keine Fabric-API-Funktion für Kamera-Roll gefunden. Das **geht nur mit Mixin**.

### 1c. Hocken / Zielen

- Schleichen (Shift) ist Vanilla, inklusive Pose `CROUCHING` und kleinerer Hitbox (1,5 Blöcke hoch). Das ist sofort nutzbar und wird synchronisiert.
- „Zielen“ (Rechtsklick halten → Sichtfeld verkleinern, weniger Streuung) lässt sich über die Item-Benutzung lösen, ähnlich wie beim Fernrohr. FOV-Zoom braucht wahrscheinlich einen kleinen Client-Mixin **(ungeprüft)**.

### Übersicht: Was geht nur mit Mixins?

| Feature | Mixin nötig? | Wo |
|---|---|---|
| Liegen/Kriechen erzwingen | ✅ ja | `Player.updatePlayerPose` (Server + Client) |
| Springen im Liegen verhindern | ✅ ja | `Player.jumpFromGround` |
| Langsamer im Liegen | ❌ nein | Attribut-Modifier |
| Tasten Q/E/C | ❌ nein | Fabric `KeyMappingHelper` |
| Kamera kippen/verschieben | ✅ ja | `Camera.alignWithEntity` (nur Client) |
| Lehnen für andere sichtbar | ✅ ja | Spielermodell-Rendering (nur Client) |

---

## 2. Hitscan-Waffen (Magazin, Nachladen, Streuung)

**Grundprinzip „Hitscan“:** Keine echte Kugel fliegt. Beim Schuss wird sofort ein **Strahl (Raycast)** vom Auge in Blickrichtung berechnet, und das erste getroffene Ziel bekommt Schaden.

**Vorbilder:**
- **Vic's Point Blank**: großer Waffen-Mod für Forge, NeoForge und Fabric, bis 1.21.11. [CurseForge](https://www.curseforge.com/minecraft/mc-mods/vics-point-blank). Lehrreich ist [Issue #63](https://github.com/vicmods/pointblank-issues/issues/63) über die Frage, ob die Treffer serverseitig oder clientseitig erkannt werden sollen.
- **Guns&Stuff** (Fabric 1.21): Gewehr mit Magazin, Hitscan läuft auf dem Server, mit Streuung. [Link](https://www.player.games/en-US/creator-hub/minecraft/community/gunsstuff)
- Große Waffen-Mods sind für uns zu komplex als Vorlage. Wir bauen bewusst klein und eigen.

**Stabilste Architektur für uns (Server ist die Autorität):**

| Baustein | Umsetzung | Mixin? |
|---|---|---|
| Waffe | Eigene `Item`-Klasse, registriert in `BuiltInRegistries.ITEM` | nein |
| Schießen | Rechtsklick → `Item.use(...)` läuft **automatisch** auf Client und Server, also ohne eigenes Paket. Treffer werden nur auf dem Server berechnet. | nein |
| Magazin | Eigene **Data Component** (z. B. `breachline:ammo`, eine Zahl) auf dem `ItemStack`, siehe [Fabric-Doku: Custom Data Components](https://docs.fabricmc.net/develop/items/custom-data-components). Wird automatisch gespeichert und zum Client synchronisiert. | nein |
| Nachladen | Taste `R` per `KeyMappingHelper` → C2S-Paket → Server startet einen Nachlade-Timer (Ticks) und füllt danach das Magazin auf | nein |
| Feuerrate / Cooldown | `player.getCooldowns()` (Vanilla-Item-Cooldown, zeigt sogar die graue Animation in der Hotbar) | nein |
| Streuung | Blickrichtung um einen zufälligen Winkel drehen (z. B. ±1,5°; mehr beim Laufen, weniger beim Hocken) | nein |
| Raycast Blöcke | `level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))` | nein |
| Raycast Spieler | `ProjectileUtil.getEntityHitResult(...)` mit Suchbox entlang des Strahls. Danach das nähere Ergebnis aus Block- und Entity-Treffer nehmen. | nein |
| Schaden | `target.hurtServer(level, level.damageSources().playerAttack(player), damage)` **(Signatur seit 1.21.2, für 26.x ungeprüft)**. Für schnelle Schüsse die Unverwundbarkeits-Ticks (`invulnerableTime`) zurücksetzen. | nein |
| Rückstoß | Client: Pitch (`xRot`) des Spielers nach dem Schuss etwas nach oben setzen | nein |
| Treffer-Feedback | Server → S2C-Paket „Treffer“ → Client zeichnet einen Hitmarker per `HudElementRegistry` und spielt einen Ton | nein |
| Sound-Platzhalter | Eigenes `SoundEvent` + `sounds.json`. Zum Start reicht ein Vanilla-Ton (z. B. Feuerwerk-Knall). | nein |

**Gute Nachricht:** Waffen gehen **komplett ohne Mixins**, nur mit Fabric-API und Vanilla-Klassen.

**Risiken:**
- **Linksklick vs. Rechtsklick:** Linksklick ist in Minecraft „Angreifen/Abbauen“. Schießen mit Linksklick bräuchte Client-Tricks (Ereignis abfangen und eigenes Paket schicken). Empfehlung: **Rechtsklick = Schießen**, wie bei Bogen und Armbrust.
- **Automatisches Feuer:** Rechtsklick gedrückt halten lässt sich über `Item.onUseTick` mit langer Benutzungsdauer lösen. Das ist machbar, aber etwas fummelig.
- **Lag:** Da der Server rechnet, kann man bei hohem Ping „daneben“ schießen, obwohl es auf dem eigenen Bildschirm ein Treffer war. Im Singleplayer und LAN ist das egal. Lag-Kompensation wäre ein Profi-Thema für später.
- **Kopftreffer:** Die Hitbox ist ein Quader. Kopfschüsse kann man trotzdem erkennen: Liegt der Trefferpunkt im oberen Bereich der Hitbox, gibt es mehr Schaden. Das ist einfach und robust.

---

## 3. Testmap per Befehl oder Strukturdatei

**Drei Wege im Vergleich:**

| Weg | Wie | Vorteile | Nachteile |
|---|---|---|---|
| **A: Java-Code** | Befehl `/breachline map haus` setzt Block für Block per `level.setBlock(...)` | Läuft in jeder Version, ich kann die Map ändern, ohne das Spiel zu öffnen, Spawn-Punkte sind direkt im Code bekannt, Git zeigt jede Änderung | Detaillierte Deko ist mühsam |
| **B: Strukturdatei (.nbt)** | Im Spiel mit dem **Strukturblock** bauen und speichern, Datei in den Mod legen unter `src/main/resources/data/breachline/structure/haus.nbt`, platzieren mit Vanilla `/place template breachline:haus ~ ~ ~` oder per Code über `StructureTemplateManager` | Bauen im Spiel, schön und schnell, Vanilla-Befehl bereits vorhanden | Binärdatei (in Git nicht lesbar), max. **48×48×48** pro Strukturblock, ich kann sie in der Cloud nicht erstellen, Spawn-Punkte müssen extra markiert werden |
| **C: Kombi** | Haus als .nbt + Code platziert es und kennt die Spawn-Positionen relativ zum Ursprung | Das Beste aus beiden | Etwas mehr Aufwand |

**Empfehlung (stabilste Lösung):**
- **Für Etappe 2 mit Weg A (Java-Code) anfangen.** Das ist reproduzierbar, ich kann es ohne Minecraft-Client schreiben, und die Spawn-Bereiche für Angreifer und Verteidiger sind direkt als Koordinaten verfügbar, die das Rundensystem in Etappe 3 braucht.
- **Später auf Weg C wechseln**, wenn du die Map im Spiel schöner bauen willst: mit dem Strukturblock speichern, die .nbt-Datei in den Mod legen, und der Code platziert sie.

**Risiken:**
- Ab 1.21 heißt der Ordner `structure` (Einzahl), früher hieß er `structures`. Alte Tutorials sind hier falsch.
- `/place template` braucht geladene Chunks am Zielort.
- Bei Weg A muss alles am Ende ein einziger Befehl erzeugen. Sonst entstehen halbfertige Häuser, wenn ein Fehler auftritt.

**Quellen:** [Minecraft Wiki: Structure file](https://minecraft.wiki/w/Structure_file) · [Minecraft Wiki: /place](https://minecraft.wiki/w/Commands/place)

---

## 4. Wandblöcke für bestimmte Spieler unzerstörbar machen

**Optionen:**

| Option | Wie | Bewertung |
|---|---|---|
| **Adventure-Modus** (Vanilla) | Alle Spieler während der Runde im Abenteuermodus: Sie können **nichts** abbauen, außer mit Items, die die Komponente `can_break` haben | ✅ sehr stabil, kein Code, kein Flackern |
| **Fabric-Events** | `PlayerBlockBreakEvents.BEFORE` → `false` zurückgeben = Abbau verhindert (läuft nur auf dem Server). Zusätzlich `AttackBlockCallback` → `InteractionResult.FAIL`, damit der Client gar nicht erst anfängt zu „hacken“. | ✅ stabil, flexibel nach Team, Block-Tag oder Phase |
| Unzerstörbarer eigener Block | Block mit Härte −1 (wie Grundgestein) | ⚠️ gilt für **alle**, auch für Gadgets per Vanilla-Abbau. Nur sinnvoll für die „verstärkte Wand“ |
| Vanilla Spawn-Schutz / Plugins | – | ❌ passt nicht |

Bestätigt im Quellcode (`PlayerBlockBreakEvents`, Fabric API 26.3): `BEFORE` bekommt `(Level, Player, BlockPos, BlockState, BlockEntity)`. Gibt ein Listener `false` zurück, wird `CANCELED` statt `AFTER` ausgelöst. Bekanntes Problem: Wird nur auf dem Server abgebrochen, **flackert der Block** kurz auf dem Client. Siehe [fabric-api Issue #3332](https://github.com/FabricMC/fabric-api/issues/3332). Deshalb zusätzlich `AttackBlockCallback` nutzen.

**Stabilste Lösung für uns (zwei Schichten):**
1. **Block-Tag** `breachline:breakable_wall`: Er legt fest, welche Blöcke als „weiche Wand“ zählen (z. B. eigene Wandblöcke oder Holzbretter). Daten statt Code, leicht änderbar.
2. **Schutz-Schicht:** `AttackBlockCallback` + `PlayerBlockBreakEvents.BEFORE` verbieten **allen** Spielern das manuelle Abbauen während einer Runde. Optional kommt der Adventure-Modus als Sicherheitsnetz dazu.
3. **Zerstören nur über Gadgets:** Das Durchbruchs-Gadget ruft im Server-Code selbst `level.destroyBlock(pos, false)` für Blöcke im Bereich auf. Die Spieler-Events greifen dabei **nicht**, also wird das Gadget nicht vom eigenen Schutz blockiert.
4. **Verstärkung:** Das Verstärkungs-Item ersetzt die Wandblöcke durch einen eigenen Block `breachline:reinforced_wall`. Das Breach-Gadget ignoriert diesen Block. Nur ein spezielles Gadget (z. B. Thermit-Ersatz) darf ihn entfernen. Die hohe Explosionsresistenz schützt zusätzlich vor Vanilla-TNT.

**Risiken:**
- Der Adventure-Modus verbietet auch das **Platzieren** von Blöcken. Bauen Verteidiger Barrikaden, muss das über unsere Items im Code passieren (oder mit `can_place_on`).
- Vanilla-Explosionen (TNT, Creeper) umgehen Spieler-Events. Deshalb soll das Breach-Gadget eine eigene Logik haben statt einer echten Explosion, und Wandblöcke brauchen hohe Explosionsresistenz.
- Im Kreativmodus greift `AttackBlockCallback` teils anders. Tests deshalb immer im Überlebens- oder Abenteuermodus.

**Quellen:** [Fabric Wiki: Event Index](https://wiki.fabricmc.net/tutorial:event_index) · [PlayerBlockBreakEvents Javadoc](https://maven.fabricmc.net/docs/fabric-api-0.100.1+1.21/net/fabricmc/fabric/api/event/player/PlayerBlockBreakEvents.html) · [fabric-api Issue #3332](https://github.com/FabricMC/fabric-api/issues/3332) · [can_break-Komponente erklärt](https://gamever.io/knowledge-base/minecraft-commands-guide-how-to-destroy-blocks-with-can_break-component)

---

## 5. Client-Server-Grundlagen in 26.3: Tasten, Pakete, Screens, Rechte, Spielerdaten

**Quellen geprüft:**
- Fabric API (`FabricMC/fabric-api`, Stand 0.161.0+26.3)
- Referenz-Code der offiziellen Fabric-Doku (`FabricMC/fabric-docs`, Ordner `reference/latest`, Stand 30.09.2026)

Alles ohne **(ungeprüft)** steht so wörtlich im Quellcode.

### 5a. Tastenbelegung (KeyMapping), nur Client

```java
// Eigene Kategorie in Optionen → Steuerung
KeyMapping.Category CATEGORY = KeyMapping.Category.register(Breachline.id("breachline"));

// Taste registrieren (Standard: G)
KeyMapping toggleModeKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
        "key.breachline.toggle_mode",   // Übersetzungsschlüssel (Text in assets/breachline/lang/*.json)
        InputConstants.Type.KEYSYM,     // Tastatur (MOUSE für Maustasten)
        InputConstants.KEY_G,           // Standardtaste
        CATEGORY));

// Abfragen: jeden Client-Tick
ClientTickEvents.END_CLIENT_TICK.register(client -> {
    while (toggleModeKey.consumeClick()) { /* Paket an Server schicken */ }
});
```

- Pakete: `net.minecraft.client.KeyMapping`, `com.mojang.blaze3d.platform.InputConstants`, `net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper`, `net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents`.
- **Neu seit 26.x:** Kategorien sind Objekte (`KeyMapping.Category.register(Identifier)`), keine Strings mehr.
- Der Spieler kann die Taste in *Optionen → Steuerung* frei umbelegen, Minecraft speichert das automatisch. Dafür brauchen wir keinen eigenen Code.
- `consumeClick()` in einer `while`-Schleife verarbeitet jeden Tastendruck genau einmal.
- Achtung: `KEY_G`/`KEY_K` sind in Vanilla frei. Q ist „Item fallen lassen“, deshalb ist Lehnen mit Q/E später ein Konflikt.

### 5b. Client-Server-Pakete (Custom Payloads)

**1. Paket als `record` definieren** (gemeinsamer Code, `src/main`):

```java
public record ToggleModePayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ToggleModePayload> TYPE =
            new CustomPacketPayload.Type<>(Breachline.id("toggle_mode"));
    public static final StreamCodec<ByteBuf, ToggleModePayload> CODEC = // fabric-api nutzt hier ByteBuf, ob die Registrierung das so annimmt: (ungeprüft)
            StreamCodec.unit(new ToggleModePayload()); // für Pakete ohne Inhalt (so in fabric-api genutzt)
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
```

- Pakete mit Inhalt: `StreamCodec.composite(ByteBufCodecs.INT, MyPayload::value, MyPayload::new)`, so im Doku-Beispiel `GiveGlowingEffectServerboundPayload`.

**2. Registrieren** (gemeinsamer Code, beim Start):
- Client → Server: `PayloadTypeRegistry.serverboundPlay().register(TYPE, CODEC)`
- Server → Client: `PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC)`
- **Neu seit 26.x:** `serverboundPlay`/`clientboundPlay` statt früher `playC2S`/`playS2C`.

**3. Empfangen und senden:**

| Richtung | Senden | Empfangen |
|---|---|---|
| Client → Server | `ClientPlayNetworking.send(payload)` | `ServerPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) -> { ServerPlayer p = context.player(); … })` |
| Server → Client | `ServerPlayNetworking.send(player, payload)` | `ClientPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) -> …)` |

- **Prüfen auf dem Server ist Pflicht.** Die Doku sagt ausdrücklich: *„It is important that you validate the content of the packet on the server side.“* Für uns heißt das: Rechte, Cooldowns und Min/Max **immer** auf dem Server prüfen, nie dem Client vertrauen.
- Handler laufen auf dem Server-Hauptthread, man darf die Welt also direkt ändern **(ungeprüft, im Beispiel wird die Welt direkt geändert, was dafür spricht)**.

### 5c. Bildschirme (Screens), nur Client

Aus dem Doku-Beispiel `CustomScreen` (26.3):

```java
public class SettingsScreen extends Screen {
    public SettingsScreen(Component title) { super(title); }

    @Override
    protected void init() {            // hier Widgets anlegen, nicht im Konstruktor
        addRenderableWidget(Button.builder(Component.literal("Reset"), btn -> { /* Paket */ })
                .bounds(40, 40, 120, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);   // Hintergrund + Widgets
        graphics.text(this.font, "Breachline", 40, 20, 0xFFFFFFFF, true);
    }
}
```

**Wichtige Umbenennungen in 26.x (bestätigt):**

| Früher (1.21 und älter) | Jetzt (26.3) |
|---|---|
| `render(GuiGraphics …)` | `extractRenderState(GuiGraphicsExtractor …)` |
| `graphics.drawString(…)` | `graphics.text(font, text, x, y, color, shadow)` |
| `Minecraft.getInstance().setScreen(…)` | `Minecraft.getInstance().gui.setScreen(…)` |
| `minecraft.screen` | `minecraft.gui.screen()` |

- Öffnen: `Minecraft.getInstance().gui.setScreen(new SettingsScreen(…))`, z. B. aus dem Tasten-Handler (K) oder einem Server→Client-Paket (`/breachline settings`).
- Schließen: `gui.setScreen(null)`. Mit `onClose()` überschreiben, um zum vorherigen Screen zurückzukehren.
- Knöpfe: `Button.builder(text, onPress).bounds(x, y, w, h).build()`. Laut Doku Höhe 20 verwenden, sonst gibt es Textur-Fehler.
- **Regler (Slider):** In Vanilla gibt es `AbstractSliderButton` **(ungeprüft für 26.3, im Doku-Referenzcode nicht verwendet)**. Notfalls bauen wir ein eigenes Widget nach dem Doku-Beispiel `CustomWidget`.
- Fabric `ScreenEvents` / `ScreenKeyboardEvents` existieren für Tasten in fremden Screens. Für unseren eigenen Screen brauchen wir sie nicht.
- Menüs mit Inventar-Slots (`MenuScreens.register`) sind etwas anderes. Die brauchen wir für die Einstellungen **nicht**.

### 5d. Rechte bei Befehlen

```java
Commands.literal("set")
    .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
```

- **Neu seit 26.x:** `source.permissions().hasPermission(Permissions.…)` statt früher `source.hasPermission(2)`. So steht es im Doku-Referenzcode.
- Welche Stufe „Admin“ entspricht (`COMMANDS_MODERATOR` vs. `COMMANDS_GAMEMASTER`) **(ungeprüft)**. Wir prüfen das beim Bauen von 3a.
- Für Pakete (GUI-Änderungen) dieselbe Prüfung auf dem `ServerPlayer` machen **(ungeprüft, ob `player.permissions()` direkt geht)**.

### 5e. Spielerdaten (Data Attachment API)

Für Modus, Operator und Ladungen pro Spieler. Bestätigt in `AttachmentRegistry` (Fabric API 26.3):

- `AttachmentRegistry.create(id, builder -> builder.persistent(codec))`: wird mit dem Spieler gespeichert und übersteht Neustarts.
- `.copyOnDeath()`: bleibt beim Tod erhalten. Das brauchen wir für Ladungen und Modus.
- `.syncWith(streamCodec, AttachmentSyncPredicate…)`: automatische Synchronisation zum Client, z. B. für die HUD-Anzeige. Das spart eigene Pakete.
- Zusätzlich gibt es `GlobalAttachments` in derselben API. Ob sich das für weltweite Einstellungen statt einer JSON-Datei eignet, ist **(ungeprüft)**. Der Plan bleibt JSON, weil Admins die Datei lesen und bearbeiten können.

### 5f. Ungeklärt für Etappe 3a
- Pfad zum Weltordner für `settings.json` (vermutlich `server.getWorldPath(LevelResource.ROOT)`) **(ungeprüft)**.
- JSON lesen und schreiben mit Mojang-`Codec` + `JsonOps` **(ungeprüft für 26.3, war in 1.21 so)**.

**Quellen Punkt 5:**
- [fabric-docs: key-mappings.md](https://github.com/FabricMC/fabric-docs/blob/main/develop/key-mappings.md) + Referenz `ExampleModKeyMappingsClient.java`
- [fabric-docs: networking.md](https://github.com/FabricMC/fabric-docs/blob/main/develop/networking.md) + Referenz `GiveGlowingEffectServerboundPayload.java`, `NetworkPayloads.java`, `ExampleModNetworkingBasic.java`
- [fabric-docs: custom-screens.md](https://github.com/FabricMC/fabric-docs/blob/main/develop/rendering/gui/custom-screens.md) + Referenz `CustomScreen.java`
- [fabric-api: AttachmentRegistry.java](https://github.com/FabricMC/fabric-api/blob/HEAD/fabric-data-attachment-api-v1/src/main/java/net/fabricmc/fabric/api/attachment/v1/AttachmentRegistry.java)
- [fabric-api: ServerLivingEntityEvents.java](https://github.com/FabricMC/fabric-api/blob/HEAD/fabric-entity-events-v1/src/main/java/net/fabricmc/fabric/api/entity/event/v1/ServerLivingEntityEvents.java) (`ALLOW_DAMAGE` für PvP an/aus)

---

## Gesamt-Fazit

| Etappe | Schwierigkeit | Mixins | Hauptrisiko |
|---|---|---|---|
| 2 Testmap | leicht | 0 | keins nennenswert |
| 3 Rundensystem | mittel | 0 | Sauberes Zurücksetzen nach der Runde |
| 4 Zerstörbare Wände | mittel | 0 | Block-Flackern (gelöst durch zwei Schichten) |
| 5 Hitscan-Waffe | mittel | 0 | Rechtsklick statt Linksklick, Lag |
| 6 Bewegung | **schwer** | 2–4 | Lehnen ist nur eine Kamera-Illusion, die Hitbox lehnt nicht mit |

**Ehrlich gesagt:** Etappe 6 ist am riskantesten, weil sie tief in Minecraft eingreift und Mixins bei jedem Update brechen können. Die übrigen Etappen kommen ohne Mixins aus. Es ist richtig, dass wir Bewegung ans Ende gelegt haben.

## Alle Quellen

- [Fabric: Porting to Fabric API 26.1](https://docs.fabricmc.net/develop/porting/fabric-api)
- [Fabric: Migrating Mappings](https://docs.fabricmc.net/develop/porting/mappings/)
- [Fabric Docs Quellcode (GitHub)](https://github.com/FabricMC/fabric-docs): Key Mappings, Networking, Custom Data Components
- [Fabric API Quellcode (GitHub)](https://github.com/FabricMC/fabric-api)
- [PaperChunk: Fabric 26.1](https://paperchunk.com/blog/fabric-26-1-biggest-overhaul)
- [GoProne (GitHub)](https://github.com/Alpvax/GoProne) · [GoProne Fabric (CurseForge)](https://www.curseforge.com/minecraft/mc-mods/goprone-fabric)
- [Crawl on Demand 26.1 Fabric (Modrinth)](https://modrinth.com/mod/crawl-on-demand/version/1.3.0+fabric)
- [Pozitification (CurseForge)](https://www.curseforge.com/minecraft/mc-mods/pozitification)
- [CameraOverhaul (GitHub)](https://github.com/Mirsario/Minecraft-CameraOverhaul)
- [SneakyQoL (Modrinth)](https://modrinth.com/mod/sneakyqol)
- [Studio Camera (GitHub)](https://github.com/scala01030-sketch/studiocamera)
- [Vic's Point Blank (CurseForge)](https://www.curseforge.com/minecraft/mc-mods/vics-point-blank) · [Issue #63 Hitscan](https://github.com/vicmods/pointblank-issues/issues/63)
- [Guns&Stuff](https://www.player.games/en-US/creator-hub/minecraft/community/gunsstuff)
- [Minecraft Wiki: Structure file](https://minecraft.wiki/w/Structure_file) · [/place](https://minecraft.wiki/w/Commands/place)
- [Fabric Wiki: Event Index](https://wiki.fabricmc.net/tutorial:event_index)
- [fabric-api Issue #3332 (Block-Flackern)](https://github.com/FabricMC/fabric-api/issues/3332)
- [Forge-Forum: setForcedPose-Probleme](https://forums.minecraftforge.net/topic/123841-strange-behavior-with-setpose-and-setforcedpose-in-1194/)
