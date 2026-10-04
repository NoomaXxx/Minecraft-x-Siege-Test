# Roadmap: Breachline als Sandbox-Mod

Stand: Oktober 2026 · reine Planung, noch kein Code für Etappe 3+.

## Grundkonzept

Breachline ist **kein Rundenspiel**. Spieler nutzen die Taktik-Mechaniken **jederzeit frei in der normalen Minecraft-Welt**.
Jeder Spieler kann per Taste zwischen **„Normal-Minecraft“** und **„Siege-Mix“** umschalten.

Design-Regeln für alle Etappen:

1. **Der Server entscheidet immer.** Der Client zeigt an und schickt Anfragen (Taste gedrückt, Regler bewegt). Der Server prüft Rechte und Grenzen und antwortet.
2. **Keine festen Zahlen im Code.** Jeder Spielwert (Munition, Cooldown, Schaden …) kommt aus der Konfiguration. Jeder Wert hat Standard, Minimum und Maximum.
3. **Begrenzung über Vorrat, Regeneration und Max-aktiv** statt Rundenlimit. Ladungen gehören dem **Spieler**, nicht dem Item (Fabric Data Attachment API). Wegwerfen oder Duplizieren bringt also nichts.
4. **Mehrspieler von Anfang an:** alle Zustände pro Spieler-UUID, Rechte-Prüfung bei globalen Einstellungen.
5. **Normalmodus = Vanilla.** Für Spieler im Normalmodus sind Siege-Items, Wandschutz, Ladungen und HUD aus.

> `docs/research.md` stammt teilweise aus der Zeit vor der Konzeptänderung. Punkt 5 dort (Tasten, Pakete, Screens) ist aktuell. Die Etappennummern in den Punkten 1–4 sind veraltet.

---

## Etappen im Überblick

Empfohlene Reihenfolge von oben nach unten.

| # | Etappe | Hängt ab von | Risiko | Mixins | Server-Code | Client-Code |
|---|---|---|---|---|---|---|
| 1 | Projekt-Setup ✅ | – | niedrig | nein | Log, Befehl | Log |
| 2 | Testmap „Haus“ ✅ | 1 | niedrig | nein | Bau-Befehl | – |
| **3a** | Konfiguration + Befehle | 1 | mittel | nein | **fast alles** | – |
| **3b** | Modus-Umschalter + Spielerzustand + Operator-Auswahl | 3a | mittel | nein | Zustand, Prüfung, Loadout | Taste G, Paket, Mini-HUD |
| **3c** | Einstellungs-GUI | 3a, 3b | mittel | nein | Werte senden, Änderungen prüfen | **Screen**, Taste K |
| 4 | Wände + Verstärkung (überall) | 3b | mittel | nein | Blöcke, Schutz, Gadget-Logik | Partikel/Sound |
| 5 | Schießen (Hitscan) | 3b | mittel | nein | Treffer, Schaden, Munition | Taste R, Rückstoß, Hitmarker |
| 7 | Fallen | 3b, 4 | mittel | nein | Fallen-Logik, Besitzer | Warn-Effekte |
| 8 | Operatoren (JSON) | 3b, 4, 5, 7 | mittel | nein | Laden, Loadout | Auswahl-Liste im GUI |
| 6 | Bewegung (Hocke, Kriechen, Lehnen) | 3b, 5 | **hoch** | **ja (2–4)** | Pose, Lean-Status | Kamera, Tasten C/Q/E |
| R | Optionaler Rundenmodus | 3a, 8 | mittel | nein | Teams, Runden, Respawn-Regeln | Rundenanzeige |
| – | Später: Drohnen, Kameras, Blend, Rauch | 8 | hoch | teilweise | Entities | Kamera-Wechsel, Overlays |

### Abhängigkeiten

```
1 ─► 2 Testmap (Testgelände)
│
└─► 3a Konfiguration + Befehle ─────────────────────────────┐
       └─► 3b Modus G + Spielerzustand + Operator-Wahl       │
              ├─► 3c Einstellungs-GUI (K)                    │
              ├─► 4 Wände ──┐                                │
              ├─► 5 Schießen┼─► 7 Fallen                     │
              │             └───────► 8 Operatoren ──────────┼─► R Rundenmodus (optional)
              └─────────────────────────► 6 Bewegung (Ende)  │
                                                             └─ alle Etappen lesen Werte aus 3a
```

### Abweichungen von deinem Vorschlag und warum

| Dein Vorschlag | Meine Änderung | Begründung |
|---|---|---|
| 3 = Sandbox + Modus + Einstellungen + Befehle | **Aufgeteilt in 3a und 3b** | Das wären 4 neue Systeme in einer Etappe: JSON-Speicherung, Rechte, Netzwerk-Pakete, Spielerzustand. Für „kleine, testbare Etappen“ zu groß. 3a ist rein serverseitig und per Befehl testbar. 3b bringt die erste Client-Server-Kommunikation (Taste G). Geht etwas schief, weiß man so sofort, in welchem Teil. |
| Einstellungen und Modus gleichzeitig | **Konfiguration zuerst (3a)** | Der Modus-Umschalter braucht schon einen Konfigurationswert (Cooldown für den Wechsel). Ohne fertige Konfiguration müsste man Zahlen fest einbauen, gegen unsere Regel. |
| 3b = GUI | heißt jetzt **3c**, Inhalt gleich | Nur Umbenennung wegen der Aufteilung oben |
| 6 Bewegung ans Ende | **übernommen** | Höchstes Risiko, Mixins, nichts hängt davon ab |
| Rundenmodus danach | **übernommen** als „R“ | Baut auf 3a (Einstellungen) und 8 (Operatoren) auf und lässt die Sandbox unverändert |

---

## Etappe 3a: Konfiguration + Befehle

**Ziel:** Ein zentrales Einstellungssystem, aus dem später alle Etappen ihre Werte lesen.

**Aufbau eines Werts** (Datenklasse, z. B. `Setting`):

| Feld | Beispiel |
|---|---|
| Schlüssel | `gadget.mine.charges` |
| Typ | Ganzzahl, Kommazahl oder an/aus |
| Standard / Min / Max | 2 / 0 / 10 |
| Bereich | `GLOBAL` (nur Admins) oder `PERSONAL` (jeder Spieler für sich) |
| Kategorie | „Gadgets“, „Waffen“, „Allgemein“ (für die GUI) |

**Funktionen:**
- **Speicherung als JSON pro Welt:** `<Weltordner>/breachline/settings.json`. Gespeichert werden nur Abweichungen vom Standard, die Datei bleibt also kurz.
- **Prüfung beim Laden und Setzen:** Ein Wert außerhalb von Min/Max wird abgelehnt (Befehl) bzw. auf die Grenze gesetzt und geloggt (beim Laden einer kaputten Datei).
- **Reset:** `/breachline reset` setzt alles auf Standard, `/breachline reset <Wert>` nur einen Wert.
- **Presets:** `/breachline preset casual|realistisch|chaos` überschreibt mehrere Werte auf einmal (siehe unten).
- **Befehle:** `/breachline set <Wert> <Zahl>`, `/breachline get [Wert]` (ohne Angabe: alle). Tab-Vervollständigung für Wertnamen.
- **Rechte:** `GLOBAL`-Werte nur mit Operator-Level (geprüft über `source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR)`, siehe research.md). `get` darf jeder.
- **Erste Werte:** `pvp.enabled` (Standard an), `mode.cross_mode_damage` (Standard aus), `mode.switch_cooldown_seconds`, `mode.damage_lock_seconds` (Standard 5), `operator.switch_cooldown_seconds`. Alle weiteren Werte kommen **in der Etappe dazu, die sie braucht** (Waffenwerte in 5, Fallenwerte in 7 …), damit es keine toten Einstellungen gibt.

| Teil | Server | Client |
|---|---|---|
| Datenklassen, JSON, Prüfung, Presets, Befehle | ✅ | – |

**Risiko: mittel.** JSON-Speichern mit Mojangs `Codec` und der Pfad zum Weltordner sind neu für uns. **Mixins: nein.**

## Etappe 3b: Modus-Umschalter + Spielerzustand + Operator-Auswahl

**Modus „Normal“ / „Siege-Mix“:**
- **Taste G** (frei belegbar in *Optionen → Steuerung*, eigene Kategorie „Breachline“) schickt nur eine **Anfrage** an den Server: `ToggleModePayload`.
- Der Server prüft den Cooldown aus der Konfiguration und die **Schadenssperre**: Wer in den letzten `mode.damage_lock_seconds` (Standard 5 s) Schaden bekommen hat, kann den Modus nicht wechseln. Die Sperre gilt in beide Richtungen. Erst nach bestandener Prüfung schaltet der Server um und schickt den neuen Zustand zurück.
- Auch per Befehl: `/breachline mode [normal|siege]`.
- Der Zustand liegt am Spieler (Data Attachment, `persistent` + `copyOnDeath`). Er übersteht Tod und Neustart.

**Was passiert beim Umschalten:**

| Richtung | Aktion |
|---|---|
| Siege → Normal | Siege-Items werden aus dem Inventar genommen und am Spieler „geparkt“, HUD aus, Wandschutz und Gadgets wirken für diesen Spieler nicht mehr |
| Normal → Siege | Geparkte Items kommen zurück, oder das Loadout des gewählten Operators wird neu gegeben |

**Weiteres in 3b:**
- Operator wählen: `/breachline operator <name>` mit 2 Platzhalter-Operatoren, Loadout aus Vanilla-Items.
- Normaler Tod und Respawn: Modus und Operator bleiben, das Loadout wird beim Respawn neu gegeben.
- Schadensregeln über Fabric `ServerLivingEntityEvents.ALLOW_DAMAGE` (siehe „Entschiedene Regeln“ unten): Siege gegen Normal blockiert, Siege gegen Siege nach `pvp.enabled`.
- Mini-HUD: zeigt nur „SIEGE“ / „NORMAL“ in einer Ecke (Fabric `HudElementRegistry`). Persönliche Option „HUD an/aus“.

| Teil | Server | Client |
|---|---|---|
| Spielerzustand, Cooldown-Prüfung, Item-Parken, Loadout, PvP | ✅ | – |
| Taste G, Paket senden, HUD-Anzeige | – | ✅ |

**Risiko: mittel.** Das ist das erste eigene Netzwerk-Paket, und das Item-Parken darf keine Items verlieren oder verdoppeln. **Mixins: nein.**

## Etappe 3c: Einstellungs-GUI

- Öffnen mit **Taste K** oder `/breachline settings`. Der Befehl schickt ein Paket an den Client: „öffne GUI“.
- Ablauf:
  1. Der Client fragt die aktuellen Werte an. Der Server antwortet mit **Wert + Min + Max + Bereich** für alle Einstellungen.
  2. Die GUI baut sich **automatisch aus dieser Liste**: Regler für Zahlen, Schalter für an/aus, Reiter pro Kategorie, Knöpfe für Presets und Reset. Neue Einstellungen aus späteren Etappen erscheinen also ohne GUI-Änderung.
  3. Eine Änderung schickt ein `SetSettingPayload`. Der Server prüft Rechte und Grenzen, speichert und schickt allen Admins mit offener GUI den neuen Stand.
- Nicht-Admins sehen globale Werte nur zum Lesen (Regler ausgegraut) und können ihre persönlichen Optionen ändern.

| Teil | Server | Client |
|---|---|---|
| Werte-Liste senden, Änderungen prüfen und speichern | ✅ | – |
| Screen, Widgets, Taste K | – | ✅ |

**Risiko: mittel.** Die Screen-API wurde in 26.x umbenannt (`extractRenderState`, `gui.setScreen`), dazu muss ein guter Regler-Widget gefunden werden **(ungeprüft)**. **Mixins: nein.**

## Etappe 4: Zerstörbare Wände + Verstärkung (überall)

- Eigene Blöcke **„Weiche Wand“** und **„Verstärkte Wand“** + Block-Tag `breachline:breakable_wall`. Die Testmap tauscht `HouseBuilder.WALL_BLOCK`.
- Verstärkungs-Item, Durchbruch-Ladung (eigene Logik, keine Vanilla-Explosion).
- **Abbau-Regel (entschieden):** Die **weiche Wand** baut jeder ab wie Steinziegel. Die **verstärkte Wand** ist gegen Abbauen geschützt (`AttackBlockCallback` + `PlayerBlockBreakEvents.BEFORE`), außer für Admins und im Kreativmodus. Zerstören lässt sie sich sonst nur mit dem passenden Gadget.
- Gadgets wirken nur für Spieler im Siege-Modus.

| Server | Client |
|---|---|
| Blöcke, Schutz, Gadget-Logik, Ladungen | Partikel, Sound, Ladungsanzeige |

**Risiko: mittel · Mixins: nein.**

## Etappe 5: Schießen

Hitscan-Waffe: Magazin (Data Component), Nachladen (Taste R → Paket), Streuung, Schaden, Rückstoß, Hitmarker, Sound-Platzhalter. Details stehen in `docs/research.md`, Punkt 2.

- **Neue Konfigurationswerte je Kategorie** (Sturmgewehr, SMG, Schrotflinte, Pistole): Schaden, Magazingröße, Max-Reservemunition, Nachladezeit, Feuerrate, Streuung.

| Server | Client |
|---|---|
| Raycast, Schaden, Munition, Nachladen | Taste R, Rückstoß, Hitmarker, Munitionsanzeige |

**Risiko: mittel · Mixins: nein.**

## Etappe 7: Fallen

Stachelmatte (verlangsamt + Schaden), Elektrodraht (Schaden über Zeit mit Warnung), Mine (Explosion ohne Blockzerstörung).

- Fallen merken sich ihren Besitzer, lösen bei ihm nicht aus und verschwinden nach einer konfigurierbaren Zeit.
- Fallen und Minen lösen **nur bei Spielern im Siege-Modus** aus und verletzen nie Normal-Spieler (entschieden). Zwischen Siege-Spielern gilt `pvp.enabled`.

| Server | Client |
|---|---|
| Fallen-Logik, Besitzer, Ladungen | Warn-Partikel, Sounds |

**Risiko: mittel · Mixins: nein.**

## Etappe 8: Operatoren-System

- Operator = JSON-Datei (Primärwaffe, Sekundärwaffe, Gadget, Fähigkeit, Seite). Neue Operatoren brauchen keinen neuen Code. Ladbar per Datapack.
- Platzhalter: **Angreifer** Breacher, Scout, Medic · **Verteidiger** Warden, Trapper, Watcher (eigene Namen).
- Operator-Auswahl auch in der GUI (Liste aus 3c erweitern).

| Server | Client |
|---|---|
| JSON laden, prüfen, Loadout | Auswahl-Liste |

**Risiko: mittel · Mixins: nein.**

## Etappe 6: Bewegung (ans Ende verschoben)

Hocke (Vanilla), Kriechen (Pose `SWIMMING` + Mixin `Player.updatePlayerPose`), Lehnen Q/E (Mixin `Camera.alignWithEntity`). Details stehen in `docs/research.md`, Punkt 1.

- Nur im Siege-Modus aktiv. Die Tasten C/Q/E schicken Anfragen an den Server.
- **Ehrlich:** Lehnen ist eine Kamera-Illusion, die Hitbox lehnt nicht mit.

| Server | Client |
|---|---|
| Pose-Zustand, Lean-Status für Treffer | Kamera-Mixin, Tasten |

**Risiko: hoch · Mixins: ja (2–4).**

## Etappe R: Optionaler Rundenmodus

- **Eigenes Modul** auf Basis derselben Einstellungen: Teams, Rundenlänge, Vorbereitungszeit, Respawn-Regeln (an/aus, Zuschauer), Rundenanzeige.
- **Darf die Sandbox nicht verändern.** Er läuft nur, wenn ein Admin eine Runde startet, und nur für die Spieler in dieser Runde. Alle anderen spielen normal weiter.
- Nutzt die Spawns aus `MapLayout`.

| Server | Client |
|---|---|
| Teams, Rundenablauf, Respawn | Rundenanzeige, Timer |

**Risiko: mittel · Mixins: nein.**

## Später

Drohnen, Kameras (Kamera-Wechsel = hohes Risiko), Blendgranate, Rauchgranate.

---

## Gadget-Begrenzungen (Standardwerte der Konfiguration)

**Prinzip:**
- **Vorrat** + **Regeneration** (1 Ladung alle X s) + **Max. aktiv** (gleichzeitig in der Welt).
- Wird über dem Limit gelegt, verschwindet das älteste Objekt.
- Ladungen bleiben beim Tod erhalten, sonst wäre Selbstmord ein Nachfüll-Trick.
- **Alle Zahlen sind Standardwerte mit eigenem Min/Max in der Konfiguration.**

| Gadget | Vorrat | Regeneration | Max. aktiv | Sonstiges |
|---|---|---|---|---|
| Verstärkung | 5 (Warden: 8) | 1 / 45 s | – | Optional: Verstärkung verfällt nach 30 min |
| Durchbruch-Ladung | 2 | 1 / 60 s | 2 | Zündverzögerung 3 s |
| Schwere Durchbruch-Ladung (Breacher) | 1 | Cooldown 120 s | 1 | Einziges Mittel gegen verstärkte Wände |
| Stachelmatte | 3 (Trapper: 5) | 1 / 40 s | 3 (5) | Despawn nach 10 min |
| Elektrodraht | 2 (Trapper: 3) | 1 / 60 s | 2 (3) | 1 s Warnung vor erstem Schaden |
| Mine | 2 (Trapper: 3) | 1 / 90 s | 2 (3) | Scharf erst nach 2 s |
| Heilspritze (Medic) | 3 | 1 / 30 s | – | 5 s zwischen Einsätzen |
| Drohne | 1 (Scout: 2) | 1 / 45 s | 1 (2) | – |
| Kamera | 3 (Watcher: 5) | 1 / 60 s | 3 (5) | – |
| Blend-/Rauchgranate | je 2 | 1 / 60 s | – | – |
| Munition | Magazin + Reserve | Munitionskiste, 30 s Cooldown | – | Werte je Waffenkategorie |
| Operator-Wechsel | – | Cooldown 30 s | – | gegen Nachfüllen durch Wechseln |
| Modus-Wechsel (G) | – | Cooldown 10 s | – | plus Schadenssperre 5 s (`mode.damage_lock_seconds`) |

## Presets

| Wert (Auswahl) | Casual | Realistisch | Chaos |
|---|---|---|---|
| Waffenschaden | ×0,75 | ×1,25 | ×1,0 |
| Reservemunition | ×2 | ×0,75 | ×5 |
| Nachladezeit | ×0,75 | ×1,25 | ×0,5 |
| Gadget-Vorrat | ×1,5 | ×1,0 | ×3 |
| Regeneration (Zeit) | ×0,5 | ×1,5 | ×0,25 |
| Max. aktive Fallen | ×1,0 | ×1,0 | ×3 |
| Modus-/Operator-Cooldown | ×0,5 | ×2 | ×0 |
| PvP | an | an | an |

Ein Preset setzt die betroffenen Werte auf **Standard × Faktor** und begrenzt sie auf Min/Max. Danach kann man einzelne Werte weiter anpassen.

## Entschiedene Regeln (Oktober 2026)

| Thema | Regel | Konfiguration |
|---|---|---|
| Siege gegen Normal | Siege-Waffen, Fallen und Minen verletzen **keine** Spieler im Normalmodus | `mode.cross_mode_damage` (Standard: aus) |
| Siege gegen Siege | Schaden je nach PvP-Einstellung | `pvp.enabled` (Standard: an) |
| Weiche Breachline-Wand | Darf von **allen** abgebaut werden, wie Steinziegel | – |
| Verstärkte Wand | Geschützt gegen Abbauen, **außer für Admins und im Kreativmodus** | – |
| Moduswechsel nach Schaden | 5 s gesperrt, in beide Richtungen | `mode.damage_lock_seconds` (Standard 5) |

## Offene Fragen

Diese Punkte sind noch nicht geklärt. Spätestens vor der genannten Etappe entscheiden.

1. **Normal-Spieler greift Siege-Spieler an** (vor 3b): Darf ein Normal-Spieler mit Vanilla-Schwert oder -Bogen einen Siege-Spieler verletzen? Sonst ist es einseitig: Siege-Spieler können Normal-Spieler nicht treffen, umgekehrt aber schon.
2. **Weiche Wand + Spitzhacke** (vor 4): Wenn Siege-Spieler weiche Wände auch einfach abbauen dürfen, verliert die Durchbruch-Ladung ihren Zweck. Soll das Abbauen für Siege-Spieler wenigstens deutlich langsamer sein (z. B. wie Obsidian)?
3. **Moduswechsel und Items** (vor 3b): Beim Wechsel zu Normal die Siege-Items „parken“ und später zurückgeben, oder einfach entfernen und beim Zurückwechseln das Loadout neu geben? Parken ist komfortabler, Neugeben ist einfacher und sicherer gegen Duplizieren.
4. **Admin-Stufe** (vor 3a): Reicht Operator-Level 2 (`COMMANDS_MODERATOR`) für globale Einstellungen, oder nur volle Admins (Level 4)?
