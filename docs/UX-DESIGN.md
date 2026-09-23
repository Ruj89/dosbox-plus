# Un ingresso più chiaro in DOSBox Plus

## Ricerca prima dell'implementazione

Fonti consultate il 22 settembre 2026:

- [Android — Authentication & Onboarding](https://developer.android.com/design/ui/mobile/guides/patterns/onboarding):
  spiegazioni contestuali, azione dopo la spiegazione del valore, pochi passaggi
  logici, progresso visibile, possibilità di tornare indietro.
- [Android — Accessibility](https://developer.android.com/design/ui/mobile/guides/foundations/accessibility):
  testo scalabile, contrasto del testo almeno 4,5:1, bersagli tattili da 48 dp,
  etichette accessibili e alternative alle azioni basate esclusivamente sui gesti.
- [Nielsen Norman Group — Progressive Disclosure](https://www.nngroup.com/articles/progressive-disclosure/):
  mostrare subito le azioni frequenti e rendere disponibili le opzioni avanzate
  attraverso un comando esplicito. Usare passaggi sequenziali per compiti distinti.
- [Nielsen Norman Group — 10 Usability Heuristics](https://www.nngroup.com/articles/ten-usability-heuristics/):
  linguaggio dell'utente, stato visibile, riconoscimento invece di memorizzazione,
  coerenza, annullamento e messaggi che suggeriscono come recuperare dagli errori.

## Problemi rilevati nell'interfaccia precedente

- La libreria vuota mostrava solo comandi tecnici, senza spiegare cosa preparare.
- «HUD globale» non comunicava né lo scopo né la differenza rispetto ai controlli del gioco.
- L'importazione esponeva contemporaneamente nome, comando e argomenti, senza
  spiegare come individuare l'eseguibile o cosa fare con un archivio ZIP.
- L'editor mostrava subito molti comandi e slider; su telefono il pannello delle
  proprietà occupava spazio anche prima di aver selezionato un controllo.
- Tema, titoli, pulsanti e navigazione erano poco coerenti tra schermate.

## Decisioni applicate

1. **Imparare facendo.** La prima home presenta tre brevi indicazioni e il
   pulsante «Aggiungi il primo gioco». La guida resta sempre disponibile;
   la home con una libreria popolata dà invece priorità ai giochi.
2. **Navigazione esplicita.** Giochi, Controlli e Guida sono destinazioni con
   icona e testo. I controlli comuni e quelli del singolo gioco hanno spiegazioni
   distinte. Il tasto Indietro segue il flusso interno.
3. **Importazione in tre passaggi.** Cartella → Avvio → Pronto, con titolo,
   progresso e azione principale persistenti. I file disponibili sono scelte
   visibili. Parametri e percorso manuale compaiono nelle opzioni avanzate.
4. **Editor concentrato sull'anteprima.** Aggiungi pulsante è l'azione principale;
   i controlli speciali sono nel menu Altri. Su telefono, Modifica apre le
   proprietà in un pannello; sugli schermi ampi restano accanto all'anteprima.
   Posizione precisa, dimensioni e livelli sono espandibili. Gli slider offrono
   un'alternativa al trascinamento. La guida all'editor è nel menu File.
5. **Base utilizzabile.** I nuovi profili partono dagli stessi controlli del
   gioco: frecce, Spazio, Ctrl, Esc e tastiera. Personalizzare un gioco che
   eredita il profilo comune parte da una copia con identità indipendente.
6. **Identità visiva coerente.** Tema scuro blu ardesia, accento verde acqua,
   superfici arrotondate, titoli gerarchici, spaziature regolari e colonne con
   larghezza massima su tablet. Testo e icone delle azioni non dipendono solo
   dal colore.

## Verifiche d'uso consigliate su dispositivo

- Installazione vuota: capire cosa serve e arrivare all'aggiunta del gioco.
- Selezione cartella annullata, accesso negato, cartella senza eseguibili,
  un solo eseguibile e più eseguibili.
- Tornare al passaggio precedente senza perdere nome e file scelti.
- Salvare un gioco, avviarlo oppure personalizzarne i controlli.
- Modificare un pulsante su telefono verticale/orizzontale e su tablet,
  anche con tastiera software aperta e testo ingrandito.
- Navigare con TalkBack: destinazioni, file di avvio, controlli, slider e azioni.

Queste verifiche di interazione non sono sostituite dalla compilazione o dai
test JVM. La documentazione delle fonti è una motivazione progettuale, non
una dichiarazione di validazione con utenti.
