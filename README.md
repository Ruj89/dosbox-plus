# DOSBox Plus

Android 8+ frontend for the GPL `dosbox-libretro` core, with an importable DOS game
library and touch-input profiles. Games are never bundled.

The built-in HUD editor provides separate portrait/landscape canvases, visual
drag/resize controls, key assignment, opacity, hold behavior, layering and
undo/redo. Profiles can be global or assigned independently to a game.

## Primi passi

La home accompagna l'aggiunta del primo gioco. Tocca **Aggiungi il primo gioco**:

1. **Cartella**: scegli una cartella già estratta con tutti i file del gioco.
2. **Avvio**: assegna un nome e scegli il file `.exe`, `.bat` o `.com` dalla lista.
   Percorso manuale e parametri facoltativi sono nelle **Opzioni avanzate**.
   In **Controlli del gioco** puoi scegliere una configurazione pronta oppure
   mantenere i controlli comuni.
3. **Pronto**: avvia il gioco oppure personalizza i suoi controlli.

La barra inferiore separa **Giochi**, **Controlli** e **Guida**. In Controlli
puoi creare una disposizione comune; il pulsante Controlli sulla scheda di un
gioco apre invece la sua configurazione personale.

Le motivazioni e le fonti delle scelte di interfaccia sono in [docs/UX-DESIGN.md](docs/UX-DESIGN.md).

## Editor dei controlli

- Apri **Controlli → Personalizza i controlli** oppure **Controlli** sotto un gioco. Ogni configurazione
  contiene due layout indipendenti: **Orizzontale** e **Verticale**.
- Con **+ Pulsante** cerca un tasto (lettere, numeri, F1–F12,
  modificatori, simboli, navigazione o tastierino numerico). Tocca poi
  l'anteprima nella posizione desiderata, oppure trascina il pulsante.
- Tocca un controllo per selezionarlo, poi **Modifica** su telefono; sugli schermi
  ampi le proprietà sono accanto all'anteprima. Puoi modificare liberamente
  **Testo sul pulsante** (etichetta / placeholder), tasto associato, posizione, dimensioni, opacità e
  comportamento alla pressione. Trascina **↘** per ridimensionarlo; **Duplica**,
  **In fondo** e **In primo piano** permettono di organizzare il layout.
  Le regolazioni precise sono in **Dimensioni, posizione e livelli**.
- **Annulla / Ripristina** trattano un trascinamento o una regolazione con slider
  come una sola modifica. **Prova** evidenzia il controllo toccato e mostra
  l'azione assegnata nell'anteprima.
- Nel menu **File**, **Esporta configurazione…** salva in JSON entrambi i layout,
  comprese le modifiche ancora aperte nell'editor. **Importa configurazione…**
  carica un JSON tramite il selettore file Android; l'importazione è annullabile
  e non cambia il profilo di altri giochi. I file non validi mostrano un errore
  senza sostituire la configurazione corrente.
- Premi **Salva** per applicare il profilo al gioco o ai controlli globali.

### Configurazioni pronte

Nell'editor tocca **Preset** (oppure **File → Configurazioni pronte**), cerca
un gioco, selezionalo per vedere istruzioni e anteprima e premi **Usa configurazione**.
La scelta sostituisce entrambi gli orientamenti come una sola modifica annullabile;
premi **Salva** per applicarla. Ogni gioco riceve una copia indipendente, modificabile
ed esportabile nel normale formato JSON.

Sono inclusi:

- **The Lords of Midnight · DOS classico**: conversione di Chris Wild,
  `MIDNIGHT.COM`, con bussola numerica, Avanza, Guarda, Pensa, Scegli, Notte ed eroi.
- **Lords of Midnight: The Citadel**: edizione DOS 3D, con clic del mouse,
  mappa, sezioni F1–F8 e controllo del tempo.
- **DOOM / DOOM II**, **Wolfenstein 3D**, **Prince of Persia · DOS**,
  **Commander Keen 4** e **Base universale**.

In Lords of Midnight classico il tempo procede per turni: **Notte (U)** termina
la giornata. In The Citadel **Tempo (M)** ferma o riavvia l'orologio.
I preset assumono i comandi originali del gioco; puoi adattarli se li hai rimappati.
Le fonti delle assegnazioni sono raccolte in [docs/CONTROL-PRESETS.md](docs/CONTROL-PRESETS.md).

I nomi dei simboli fanno riferimento ai tasti fisici del layout DOS US;
il carattere prodotto dipende dal layout configurato nel DOS. Frecce e
tastierino numerico hanno assegnazioni distinte anche nei file esportati.

## Reproducible build

The Docker image pins Java 17, Android platform/NDK, CMake and Gradle. Per
compilare e firmare gli APK debug con una chiave persistente, esegui:

```sh
bash scripts/build-apk.sh
```

Lo script crea il volume Docker `dosbox-plus-signing` e monta `/root/.android`
durante la firma. Android genera `debug.keystore` al primo utilizzo e riusa
la stessa chiave nelle build successive: conserva il volume anche quando
ricrei l'immagine. Gli APK sono copiati in `dist/`.

Se possiedi già il `debug.keystore` usato per un APK installato, copialo nel
volume **prima della prima build**, altrimenti la nuova chiave non permetterà
di aggiornare quell'installazione. Per esempio, dalla directory del progetto:

```sh
docker volume create dosbox-plus-signing
docker run --rm --mount type=volume,source=dosbox-plus-signing,target=/root/.android \
  --mount "type=bind,source=$(realpath debug.keystore),target=/tmp/previous.keystore,readonly" \
  alpine sh -c 'cp /tmp/previous.keystore /root/.android/debug.keystore && chmod 600 /root/.android/debug.keystore'
```

Per conservare un log completo e rendere visibili le diagnostiche Kotlin:

```sh
bash scripts/build-apk.sh 2>&1 | tee build.log
```

The build downloads a pinned `dosbox-libretro` source archive and verifies
its checksum. I test JVM sono eseguiti durante la creazione dell'immagine;
per ripeterli dopo la build:

```sh
docker build --target build -t dosbox-plus-build .
docker run --rm dosbox-plus-build gradle --no-daemon test
```

Per verificare riavvii, video e tastiera sul core reale, su Linux con GCC,
Make e binutils e dopo aver scaricato il sorgente upstream:

```sh
bash scripts/test-core-session.sh
```

Lo script accetta anche il percorso di una copia del core come primo argomento.
Usa un programma DOS di test incluso nei sorgenti, senza richiedere giochi.

## Source and licensing

See [docs/UPSTREAM.md](docs/UPSTREAM.md). The JNI frontend hosts the libretro
core directly and supplies software video, stereo audio, keyboard and mouse
callbacks. UI profiles and SAF staging remain separate from the core.
