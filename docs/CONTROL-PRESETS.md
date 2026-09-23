# Preset dei controlli DOS

Le configurazioni contengono solo assegnazioni e posizioni dei pulsanti. Sono
disponibili durante l'aggiunta di un gioco e nell'editor dei controlli.
Ogni applicazione genera nuovi ID; nell'editor viene mantenuta l'identità del
profilo in modifica. Entrambi i layout sono sostituiti insieme e possono essere
ripristinati con Annulla. Nessun preset modifica le impostazioni interne del gioco.

## Titoli e fonti delle assegnazioni

| Preset | Comandi principali | Fonte |
| --- | --- | --- |
| The Lords of Midnight, conversione DOS di Chris Wild | Q avanza; E guarda; R pensa; T sceglie; U notte; 1–8 bussola; C/V/B/N eroi; M selezione; A nuova; D carica; S salva; G/J sì/no | [Scheda tastiera dell'autore](https://www.icemark.com/tower/manual/keyboard.htm), [guida](https://www.icemark.com/tower/manual/guide.htm) |
| Lords of Midnight: The Citadel | T parla; M orologio; Spazio ferma il movimento; F1–F8 sezioni; F9 un'ora; P destinazione sulla mappa; 1 vista soggettiva; mouse | [Riferimento dei comandi](https://classicreload.com/lords-of-midnight-the-citadel.html) |
| DOOM / DOOM II | Frecce; Ctrl fuoco; Spazio usa; Shift corsa; Alt laterale; virgola/punto laterale sinistro/destro; Tab mappa; 1–7 armi | [Doom/Controls](https://en.wikibooks.org/wiki/Doom/Controls) |
| Wolfenstein 3D | Frecce; Ctrl fuoco; Alt laterale; Shift destro corsa; Spazio porte; 1–4 armi | [Sorgente ufficiale id Software, `dirscan` e `buttonscan`](https://github.com/id-Software/wolf3d/blob/master/WOLFSRC/WL_PLAY.C) |
| Prince of Persia DOS | Frecce; Shift azione; tastierino 7/9 salti diagonali; Spazio tempo; Ctrl+K tastiera; Ctrl+G salva; Ctrl+L carica | [Manuale IBM](https://www.freegameempire.com/games/Prince-of-Persia/manual), [comandi DOS](https://www.dosdays.co.uk/topics/Games/game_prince.php) |
| Commander Keen 4 | Frecce; Ctrl salto; Alt pogo; Spazio fuoco; Invio stato; Esc menu | [Guida, sezione Keyboard Controls](https://gamefaqs.gamespot.com/pc/564721-commander-keen-episode-iv-secret-of-the-oracle/faqs/38435) |

Lords of Midnight classico e The Citadel sono distinti nel selettore: il primo
utilizza azioni discrete e direzioni numeriche, il secondo mouse e controllo
dell'orologio. La tastiera virtuale inclusa in ogni layout serve per il testo
e altri comandi; dall'editor si possono aggiungere ulteriori tasti dedicati.

## Comportamento touch

- Movimento, fuoco continuo e modificatori richiedono pressione mantenuta.
- I comandi discreti (notte, scelta personaggio, menu, cambio arma) inviano un
  solo tocco. Tasti, combinazioni e clic rimangono premuti per 50 ms affinché
  il gioco possa rilevarli, e vengono rilasciati anche se il gesto è cancellato.
- Per The Citadel si trascina lo sfondo per muovere il puntatore e si usa Clic
  per il pulsante sinistro o Destro per quello destro.
- I preset usano i codici della tastiera già supportati dal frontend: le diagonali
  del tastierino in Prince of Persia restano distinte dalle frecce.

I test JVM verificano geometria, assenza di sovrapposizioni, codici supportati,
serializzazione, indipendenza delle copie, comandi specifici di Midnight e
rilascio degli input. Non equivalgono a una partita completa di ciascun titolo.
