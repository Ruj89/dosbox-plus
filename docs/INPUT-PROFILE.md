# Formato profilo input v1

Un profilo JSON contiene `version: 1`, un nome e due liste, `portrait` e
`landscape`. Ogni controllo è posizionato in coordinate normalizzate (`x`, `y`,
`width`, `height`, intervallo 0..1) e ha uno `zIndex`.
`opacity` controlla la trasparenza e `trigger` distingue il tap dalla pressione
prolungata. I campi hanno valori predefiniti per mantenere compatibili i profili
v1 creati dalle prime build.

Le azioni serializzate sono: `Key` (scancode DOS), `Combo` (lista di scancode),
`Macro` (passi key-up/key-down con `delayMs`), `MouseButton` e
`JoystickButton`. Le versioni future devono rifiutare esplicitamente profili
non supportati, invece di interpretarli in modo silenzioso.
