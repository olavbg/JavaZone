# JavaZone Android App

## Om prosjektet
Dette er en uoffisiell applikasjon for å holde oversikt over programmet på JavaZone. Appen er utviklet for å utforske teknologier, og samtidig få en mer skreddersydd opplevelse av applikasjonen til mine behov. Kanskje dette gjelder dine behov også?

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
