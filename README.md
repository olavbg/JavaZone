# JavaZone Android App

En moderne Android-app laget for JavaZone-fellesskapet.

## Om prosjektet
Dette er et hobbyprosjekt laget av en JavaZone-fan for å skape en rask, moderne og skreddersydd konferanseopplevelse på mobilen.

Målet har vært å utforske ny teknologi på fritiden – særlig moderne Android-utvikling med Jetpack Compose og Navigation 3 – kombinert med utforsking av AI-assistert koding og parprogrammering.

Appen er i stor grad bygget som et eksperiment i AI-parprogrammering: AI har stått for mye av grovarbeidet og tastetrykkene, mens undertegnede har hatt rollen som arkitekt, tester og streng tech lead. Mesteparten av tiden fungerer dette samspillet overraskende bra – resten av tiden er jeg glad for at dette er et hobbyprosjekt.

Uten JavaZone og javaBins åpne API-er (SleepingPill) ville dette bare vært en god idé uten innhold. En stor takk til javaBin for at dere deler dataene og legger til rette for fellesskapet!

## Funksjonalitet
*   **Tidslinje:** Full oversikt over alle sesjoner med filtrering på rom, format (lyntale, workshop, etc.) og språk, med live-markering av foredrag som skjer nå.
*   **Favoritter:** Marker sesjoner du vil få med deg for å lage din egen personlige tidsplan.
*   **Påminnelser:** Lokale varslinger som gir deg beskjed i god tid før favorittsesjonene dine starter.
*   **Sesjonsdetaljer:** Se sammendrag, målgruppe og emneknagger for sesjonene, samt detaljert informasjon om foredragsholderne med sosiale medier-lenker (X, Bluesky, LinkedIn).
*   **Arkiv:** Tilgang til programmet fra tidligere år, inkludert link til video.
*   **Donasjoner:** Mulighet til å støtte via Vipps-boks og Buy Me a Coffee.

## Teknologier
Appen er bygget med:
*   **Kotlin**
*   **Jetpack Compose** med Material 3
*   **Navigation 3** (Den nyeste eksperimentelle navigasjonsløsningen fra Google)
*   **Room** for lokal lagring av sesjoner og favoritter
*   **Retrofit & Moshi** for henting av data fra API
*   **DataStore** for lagring av brukerinnstillinger
*   **Kotlin Coroutines & Flow** for asynkron programmering

## API
Appen henter data fra JavaZones offisielle "SleepingPill" API:
`https://sleepingpill.javazone.no/`

## Ansvarsfraskrivelse / Disclaimer
Dette er en uoffisiell app laget for fellesskapet. Den er verken tilknyttet, godkjent av eller sponset av javaBin eller JavaZone. JavaZone og javaBin er registrerte varemerker for javaBin.

*This is an unofficial app made for the community. It is not affiliated with, endorsed by, or sponsored by javaBin or JavaZone. JavaZone and javaBin are registered trademarks of javaBin.*
