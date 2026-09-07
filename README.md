# JavaZone Android App

## Om prosjektet
Dette er en uoffisiell applikasjon for å holde oversikt over programmet på JavaZone. Appen er utviklet for å utforske teknologier, og samtidig få en mer skreddersydd opplevelse av applikasjonen til mine behov. Kanskje dette gjelder dine behov også?

## Funksjonalitet
*   **Tidslinje:** Full oversikt over alle sesjoner med filtrering på rom, format (lyntale, workshop, etc.) og språk.
*   **Favoritter:** Marker sesjoner du vil få med deg for å lage din egen personlige tidsplan.
*   **Påminnelser:** Lokale varslinger som gir deg beskjed i god tid før favorittsesjonene dine starter.
*   **Sesjonsdetaljer:** Se detaljert informasjon om foredragsholdere og les sammendrag av sesjonene.
*   **Arkiv:** Tilgang til programmet fra tidligere år.

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
