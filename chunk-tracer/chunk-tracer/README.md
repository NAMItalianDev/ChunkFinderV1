# Chunk Tracer

Mod Fabric per **Minecraft 26.1.2** che traccia in quali chunk i giocatori
stazionano più spesso e mostra una **heatmap dall'alto** direttamente
sull'HUD, aggiornata in tempo reale mentre cammini o voli.

## Come funziona

- **Lato server** (`ChunkTracer.java`): ogni secondo registra il chunk in
  cui si trova ciascun giocatore online e incrementa un contatore di
  visite per quel chunk, per dimensione. Ogni 5 minuti i dati vengono
  salvati su disco in `config/chunktracer/heatmap.txt`, e ricaricati
  all'avvio del server. Funziona sia in singleplayer che su server con
  più giocatori: se in un chunk passano spesso giocatori diversi, il
  conteggio cresce comunque.
- **Rete**: il server invia al client uno snapshot (`ChunkHeatSyncPayload`)
  con i conteggi dei chunk in un raggio di 6 chunk (griglia 13x13)
  attorno al giocatore, una volta al secondo.
- **Lato client** (`ChunkHeatOverlay.java`): disegna in alto a destra
  dello schermo una griglia colorata (blu = poco visitato, giallo/rosso
  = molto visitato), con il chunk corrente del giocatore evidenziato da
  un bordo bianco. Premi **K** per mostrare/nascondere la heatmap.

## Requisiti per compilare

Il progetto è configurato secondo il nuovo toolchain "unobfuscated"
introdotto con Minecraft 26.1 (niente più Yarn, si usano direttamente i
nomi ufficiali Mojang):

- **Java 25** (obbligatorio per Gradle e per la compilazione)
- **Gradle 9.4+** (se non hai la wrapper, apri il progetto in IntelliJ
  IDEA 2025.3+ e lascialo generare/scaricare automaticamente)
- Loom `1.17-SNAPSHOT`, Fabric Loader `0.19.3`, Fabric API
  `0.155.2+26.1.2` (già impostati in `gradle.properties`)

### Compilare

```bash
./gradlew build
```

(se non hai ancora il wrapper, generalo con `gradle wrapper
--gradle-version 9.4` una volta che hai Gradle 9.4+ installato, oppure
apri semplicemente la cartella in IntelliJ IDEA che lo farà per te).

Il file `.jar` compilato si troverà in `build/libs/`. Va installato nella
cartella `mods` di un'istanza Fabric per Minecraft 26.1.2 insieme a
Fabric API.

## Struttura del progetto

```
src/main/java/com/chunktracer/
  ChunkTracer.java          -> entrypoint comune, tick server, rete, salvataggio
  ChunkHeatData.java        -> struttura dati dei conteggi per chunk
  network/ChunkHeatSyncPayload.java -> pacchetto server->client

src/client/java/com/chunktracer/client/
  ChunkTracerClient.java    -> entrypoint client, keybind, ricezione pacchetti
  ChunkHeatOverlay.java     -> disegno della heatmap sull'HUD
```

## Note importanti

Minecraft 26.1 ha introdotto cambiamenti molto grandi (mappature Mojang
ufficiali al posto di Yarn, nuovo sistema di rendering HUD a due fasi,
`HudRenderCallback` rimosso in favore di `HudElementRegistry`, ecc.).
Questo codice è stato scritto seguendo la documentazione ufficiale più
recente disponibile (Fabric blog "Fabric for Minecraft 26.1", Fabric
Docs aggiornati a metà 2026). Se nel frattempo Fabric API ha rinominato
ulteriormente qualche classe (succede spesso nelle prime release dopo un
grande cambio di mappature), il posto giusto dove controllare è
https://docs.fabricmc.net/develop/porting/fabric-api — lì trovi la
tabella completa dei rename da applicare.

Idee per estendere il mod:
- comando `/chunktracer top` per elencare i chunk più "battuti";
- salvataggio per-mondo invece che globale nella cartella config;
- vera minimappa con texture invece di semplici quadrati colorati;
- soglia configurabile per il colore "caldo".
