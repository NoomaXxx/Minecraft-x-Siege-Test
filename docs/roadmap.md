# Roadmap: Breachline als Sandbox-Mod

Stand: Oktober 2026 · reine Planung, noch kein Code.

## Grundkonzept

Breachline ist **kein Rundenspiel**. Es gibt keine Runden, Phasen, Timer oder festen Teams.
Spieler nutzen die Taktik-Mechaniken **jederzeit frei in der normalen Minecraft-Welt**: Operator wählen, Wände verstärken oder sprengen, schießen, Fallen legen.

Daraus folgen drei Design-Regeln für alle Etappen:

1. **Begrenzung über Vorrat und Cooldown statt Rundenlimit.** Jedes Gadget hat Ladungen, die sich mit der Zeit wieder auffüllen, und eine Obergrenze für gleichzeitig aktive Objekte.
2. **Ladungen gehören dem Spieler, nicht dem Item.** Sie werden am Spieler gespeichert (Fabric Data Attachment API). So lassen sich Gadgets nicht durch Wegwerfen, Kisten oder Duplizieren vermehren.
3. **Mehrspieler von Anfang an.** Der Server entscheidet alles (Treffer, Ladungen, Schaden), der Client zeigt nur an. Zustände pro Spieler, nie global.

> Hinweis: `docs/research.md` stammt aus der Zeit vor der Konzeptänderung. Die Technik-Recherche gilt weiter, die Etappennummern und das Rundensystem dort sind veraltet.

---

## Etappen im Überblick

| # | Etappe | Hängt ab von | Risiko | Mixins? |
|---|---|---|---|---|
| 1 | Projekt-Setup ✅ | – | niedrig | nein |
| 2 | Testmap „Haus“ ✅ | 1 | niedrig | nein |
| 3 | Sandbox-Grundlage | 1 | niedrig–mittel | nein |
| 4 | Zerstörbare Wände + Verstärkung | 3 | mittel | nein |
| 5 | Schießen (Hitscan) | 3 | mittel | nein |
| 6 | Bewegung (Hocke, Kriechen, Lehnen) | 3 (5 für Zielen) | **hoch** | **ja** (2–4) |
| 7 | Fallen | 3, 4 | mittel | nein |
| 8 | Operatoren-System | 3, 4, 5, 7 | mittel | nein |
| – | Später: Drohnen, Kameras, Blend, Rauch, Rundenmodus | 8 | hoch | teilweise |

### Abhängigkeiten

```
1 Setup ─► 2 Testmap (Testgelände für alles Weitere)
   │
   └──► 3 Sandbox-Grundlage (Spielerzustand, Ladungen, Loadout, PvP)
          ├──► 4 Wände ───────┐
          ├──► 5 Schießen ────┤
          │      └──► 6 Bewegung (Zielen nutzt Waffe)
          ├──► 7 Fallen ◄─────┘ (nutzt Platzier- und Ladungslogik aus 4)
          └──► 8 Operatoren (bündelt Waffen aus 5 + Gadgets aus 4/7)
                 └──► Später: Drohnen, Kameras, Granaten, Rundenmodus
```

### Empfehlung zur Reihenfolge

**Etappe 6 (Bewegung) nach Etappe 8 verschieben.** Die empfohlene Reihenfolge ist damit 3 → 4 → 5 → 7 → 8 → 6.

**Warum:**
- Bewegung ist die einzige Etappe mit hohem Risiko und Mixins.
- Nichts anderes hängt von ihr ab.
- Wenn sie hakt, blockiert sie sonst Fallen und Operatoren.

So entsteht zuerst ein vollständig spielbarer Sandbox-Mod. Die Nummern bleiben zur Orientierung trotzdem gleich.

---

## Etappe 3: Sandbox-Grundlage

**Ziel:** Jeder Spieler kann jederzeit einen Operator wählen und bekommt dessen Loadout.

| Baustein | Umsetzung | API |
|---|---|---|
| Operator wählen | Befehl `/breachline operator <name>` (Tab-Vervollständigung). Ein Auswahl-Menü (Screen) folgt später. | Fabric Command API |
| Loadout geben | Inventar-Slots füllen, alte Breachline-Items vorher entfernen | Vanilla |
| Seite | Angreifer/Verteidiger ergibt sich aus dem gewählten Operator, nicht aus einem Team | eigene Daten |
| Spielerzustand | Gewählter Operator + Ladungen pro Gadget, gespeichert am Spieler, übersteht Neustart und Tod | **Fabric Data Attachment API** |
| Tod | Normaler Minecraft-Tod und Respawn. Der Operator bleibt gewählt, das Loadout wird beim Respawn neu gegeben. | `ServerPlayerEvents` (Respawn) |
| PvP an/aus | `/breachline pvp on\|off`, gilt serverweit, nur für Admins. Blockt Spieler-gegen-Spieler-Schaden. | `ServerLivingEntityEvents.ALLOW_DAMAGE` |
| Platzhalter-Operatoren | 2 Test-Operatoren mit Vanilla-Items, bis echte Waffen und Gadgets existieren | – |

**Mehrspieler-Punkte:**
- Alle Daten pro Spieler-UUID.
- Befehle mit Rechte-Prüfung: `pvp` nur für Admins, `operator` für alle.
- Operator-Wechsel bekommt einen Cooldown (z. B. 30 s), sonst lassen sich Ladungen durch Wechseln auffüllen.

**Risiko: niedrig–mittel.** Neu für uns sind die Data Attachment API und Rechte-Prüfungen in 26.x.

## Etappe 4: Zerstörbare Wände + Verstärkung (überall)

- Eigener Block **„Weiche Wand“** + Block-Tag `breachline:breakable_wall`. Der Tag ist erweiterbar, z. B. für Holzbretter in der Welt. Die Testmap tauscht `HouseBuilder.WALL_BLOCK` auf diesen Block.
- **Verstärkungs-Item:** Rechtsklick auf eine Wand ersetzt einen Bereich von 2×3 Blöcken durch den Block **„Verstärkte Wand“**.
- **Durchbruch-Ladung:** platzierbar an weicher Wand, zündet nach 3 s und entfernt einen begrenzten Bereich (z. B. 3×3). Eigene Logik, keine Vanilla-Explosion.
- **Schutz:** Spieler können Breachline-Wände nicht abbauen (`AttackBlockCallback` + `PlayerBlockBreakEvents.BEFORE`). Normale Welt-Blöcke bleiben in der Sandbox abbaubar.
- Ab hier kommt das **Ladungssystem** aus Etappe 3 zum ersten Mal zum Einsatz.

**Risiko: mittel.** Block-Flackern beim Client (gelöst durch zwei Schichten). Außerdem muss der Schutz in jeder Welt greifen, nicht nur in der Testmap.

## Etappe 5: Schießen

Hitscan-Waffe als Item: Magazin (Data Component), Nachladen (Taste R → Paket an Server), Streuung, Schaden, Cooldown, Rückstoß, Hitmarker und Sound-Platzhalter. Details stehen in `docs/research.md`, Punkt 2.

- **Ohne Runden:** Reservemunition am Spieler. Auffüllen über Munitionskiste (Block, Cooldown pro Spieler) oder beim Respawn.
- Waffenwerte (Schaden, Feuerrate, Streuung, Magazin) kommen von Anfang an aus einer **Datenklasse**. Die 4 Kategorien in Etappe 8 sind dann nur noch Daten.

**Risiko: mittel.** Schießen per Rechtsklick (Linksklick ist Abbauen) und Lag im Mehrspieler.

## Etappe 6: Bewegung (Risiko hoch)

Hocke (Vanilla-Schleichen), Kriechen/Liegen (Pose `SWIMMING` + Mixin auf `Player.updatePlayerPose`), Lehnen Q/E (Kamera-Mixin auf `Camera.alignWithEntity`). Details stehen in `docs/research.md`, Punkt 1.

**Ehrliche Einschätzung:** Lehnen ist nur eine Kamera-Illusion, die Hitbox lehnt nicht mit. Mixins können bei jedem Minecraft-Update brechen.

**Risiko: hoch · Mixins: ja (2–4).**

## Etappe 7: Fallen

| Falle | Wirkung | Technik |
|---|---|---|
| Stachelmatte | Verlangsamt stark + leichter Schaden beim Betreten | Eigener flacher Block, `entityInside` → Slowness + Schaden mit Cooldown pro Opfer |
| Elektrodraht | Schaden über Zeit. Vorher Warnung durch Funken-Partikel und Summen-Sound. | Block an Wand/Boden, tickt alle 0,5 s im Radius |
| Mine | Explosion **ohne Blockzerstörung**: Schaden + Rückstoß | Block/Entity mit Annäherungs-Sensor, Explosion ohne Block-Interaktion oder eigene Schadenslogik |

- **Besitzer-Regel:** Fallen merken sich, wer sie gelegt hat. Sie lösen nicht beim Besitzer aus, später optional auch nicht bei seiner Seite.
- **Aufräumen:** Fallen verschwinden nach 10 min oder wenn der Besitzer offline geht (Option). Sonst ist die Welt irgendwann voller Fallen.
- **Abbaubar:** Gegner können Fallen mit Schlägen zerstören (Konter).

**Risiko: mittel.** Neu sind Block-Entities mit Tick-Logik und die Besitzer-Speicherung.

## Etappe 8: Operatoren-System

**Datenmodell (eine Datei pro Operator, JSON im Mod oder im Datapack):**

```json
{
  "id": "breachline:breacher",
  "name": "Breacher",
  "side": "attacker",
  "primary": "breachline:assault_rifle",
  "secondary": "breachline:pistol",
  "gadget": { "item": "breachline:breach_charge", "charges": 3 },
  "ability": { "type": "breachline:heavy_breach", "cooldown_seconds": 120 }
}
```

- Ein neuer Operator braucht **keinen neuen Code**, nur eine JSON-Datei, solange er vorhandene Waffen, Gadgets und Fähigkeitstypen nutzt.
- Ladbar über den Datapack-Mechanismus. Damit können Server Operatoren hinzufügen oder anpassen.

**Platzhalter-Operatoren (eigene Namen):**

| Seite | Name | Gadget / Fähigkeit |
|---|---|---|
| Angreifer | Breacher | Schwere Durchbruch-Ladung (durchschlägt auch verstärkte Wände) |
| Angreifer | Scout | Extra-Drohne (bis Drohnen existieren: Platzhalter, z. B. kurzer Glüh-Effekt auf Gegner in der Nähe) |
| Angreifer | Medic | Heilspritze |
| Verteidiger | Warden | Zusätzliche Verstärkungen |
| Verteidiger | Trapper | Zusätzliche Fallen |
| Verteidiger | Watcher | Extra-Kameras (bis Kameras existieren: Platzhalter) |

**Waffenkategorien:** Sturmgewehr, SMG, Schrotflinte (mehrere Strahlen pro Schuss), Pistole. Alle laufen über dieselbe Hitscan-Logik aus Etappe 5 mit unterschiedlichen Werten.

**Risiko: mittel.** JSON-Laden mit Codecs (Mojang-Serialisierung) ist neu, die Logik selbst ist einfach.

## Später

| Feature | Notiz | Risiko |
|---|---|---|
| Drohnen | Steuerbare Entity mit Kamera-Wechsel, braucht Client-Kamera-Trick | hoch |
| Kameras | Fest platziert, Ansicht umschalten. Ähnliche Kamera-Technik wie Drohnen. | hoch |
| Blendgranate | Wurf-Entity, weißes Overlay + Verlangsamung je nach Blickrichtung | mittel |
| Rauchgranate | Partikelwolke, die die Sicht blockiert. Partikel blockieren keine Treffer, also ggf. Raycast-Sperre im eigenen Schuss-Code. | mittel |
| Rundenmodus (optional) | Aufsatz auf die Sandbox: Phasen, Timer, Teams, nutzt die Spawns aus `MapLayout` | mittel |

---

## Gadget-Begrenzungen ohne Runden

**Prinzip:**
- **Vorrat** (max. Ladungen) + **Regeneration** (1 Ladung alle X s) + **Max. aktiv** (gleichzeitig in der Welt).
- Wird ein Objekt über dem Limit gelegt, verschwindet das älteste.
- Ladungen bleiben beim Tod erhalten. Sonst wäre Selbstmord ein Nachfüll-Trick.

| Gadget | Vorrat | Regeneration | Max. aktiv | Sonstiges |
|---|---|---|---|---|
| Verstärkung | 5 (Warden: 8) | 1 / 45 s | – (Wand bleibt verstärkt) | Verstärkte Wand wird nach 30 min wieder weich (optional, gegen Bunker-Welten) |
| Durchbruch-Ladung | 2 | 1 / 60 s | 2 platziert | Zündverzögerung 3 s |
| Schwere Durchbruch-Ladung (Breacher) | 1 | Cooldown 120 s | 1 | Einziges Mittel gegen verstärkte Wände |
| Stachelmatte | 3 (Trapper: 5) | 1 / 40 s | 3 (Trapper: 5) | Despawn nach 10 min |
| Elektrodraht | 2 (Trapper: 3) | 1 / 60 s | 2 (Trapper: 3) | 1 s Warnung vor erstem Schaden |
| Mine | 2 (Trapper: 3) | 1 / 90 s | 2 (Trapper: 3) | Scharf erst 2 s nach dem Legen, Despawn nach 10 min |
| Heilspritze (Medic) | 3 | 1 / 30 s | – | Kein Selbst-Spam: 5 s Cooldown zwischen Einsätzen |
| Drohne | 1 (Scout: 2) | 1 / 45 s nach Zerstörung | 1 (Scout: 2) | – |
| Kamera | 3 (Watcher: 5) | 1 / 60 s | 3 (Watcher: 5) | – |
| Blendgranate | 2 | 1 / 60 s | – | – |
| Rauchgranate | 2 | 1 / 60 s | – | – |
| Munition | Magazin + Reserve | Munitionskiste, 30 s Cooldown pro Spieler | – | – |
| Operator-Wechsel | – | Cooldown 30 s | – | Verhindert Nachfüllen durch Wechseln |

**Alle Werte sind Startwerte.** Sie gehören in eine Konfiguration, damit man sie ohne neuen Code anpassen kann.
