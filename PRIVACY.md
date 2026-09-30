# Privacy Policy for JavaZone Android App

*Last updated: September 2026*

This is the privacy policy for the unofficial **JavaZone Android application**, developed as an open-source community project ([github.com/olavbg/JavaZone](https://github.com/olavbg/JavaZone)).

---

## In Short (TL;DR)

- **Zero Tracking:** We do not collect, track, or sell your personal data. There are no user accounts, no analytics SDKs, and no ads.
- **Everything Stays on Your Device:** Your favorited talks, settings, and reminders are stored exclusively on your phone.
- **Transparent Permissions:** Permissions (like notifications and exact alarms) are only used to notify you before your favorite talks start.
- **Open Source:** Every line of code is open and verifiable by the community on GitHub.

---

## Detailed Privacy Policy

### 1. Information Collection and Storage

#### Personal Data
The app **does not collect, transmit, or share any personally identifiable information (PII)**. You do not need to register, log in, or provide an email address, name, or phone number to use the app.

#### On-Device Storage
To make the app fast, reliable, and functional offline, the following data is saved locally on your device:
- **Favorite Sessions:** Talks you bookmark are stored in a local on-device database (Room).
- **App Preferences:** Your chosen theme (dark/light), app language, selected conference year, and notification lead times are stored locally (DataStore).
- **Cached Conference Program:** The schedule and speaker list are cached locally on your device so you can view the program even with spotty conference Wi-Fi.

This data never leaves your device and is deleted if you uninstall the app or clear its application storage.

---

### 2. Network Connections and External Services

The app connects to the internet only when necessary to perform user-requested actions:

- **JavaZone Schedule API:** The app fetches the public conference program and speaker information from the official, public JavaZone APIs (`sleepingpill.javazone.no` and `api.javazone.no`).
- **Speaker Images:** Speaker profile photos are loaded from JavaZone's public content delivery servers.
- **Video Recordings (Archive):** When you tap to watch a past talk's recording, an embedded web player (Vimeo or YouTube) loads the video stream. These services may collect standard web telemetry according to their own privacy policies when their player is loaded.
- **External Links:** Links to speakers' social profiles (LinkedIn, Bluesky, X) and optional developer donation links (Vipps, Buy Me a Coffee) open in your device's browser or native apps.

---

### 3. Device Permissions

The app requests only the minimal set of permissions needed to provide its features:

- **Notifications (`POST_NOTIFICATIONS`):** Used on Android 13+ to send you a reminder alert before a talk you have marked as a favorite begins. You can disable notifications at any time in the app settings or system settings.
- **Exact Alarms (`SCHEDULE_EXACT_ALARM`):** Used to schedule talk reminders with minute-level precision so you are notified at the exact scheduled time before a session starts.
- **Internet Access (`INTERNET` & `ACCESS_NETWORK_STATE`):** Used to check network connectivity and download the latest conference schedule from JavaZone's public API.

---

### 4. Analytics, Tracking, and Advertising

- **No Analytics:** The app does not include any analytics tools (such as Google Analytics or Firebase Analytics). We do not collect usage statistics or behavioral data.
- **No Advertising:** The app contains zero ads and does not use any advertising identifiers.
- **No Third-Party Trackers:** No third-party tracking libraries or data brokers are integrated into the application.

---

### 5. Diagnostics and Crash Reporting

If an unexpected error or crash occurs:
- The app catches the failure locally and presents a dedicated crash screen.
- **Nothing is transmitted automatically.**
- The crash screen provides an anonymized diagnostic report (exception message, stack trace, device model, Android version, and app version).
- You can voluntarily choose to copy this report or submit it to the developer via GitHub Issues or email to help resolve the issue.

---

### 6. Children's Privacy

The application is a conference program guide and does not knowingly collect or solicit any personal information from children under the age of 13.

---

### 7. Open Source and Verification

This application is completely open source. You are welcome to inspect the source code, verify how data and permissions are handled, or contribute improvements at:

🔗 **[https://github.com/olavbg/JavaZone](https://github.com/olavbg/JavaZone)**

---

### 8. Contact

If you have questions, feedback, or concerns regarding this privacy policy or the app, please feel free to open an issue on GitHub:
- **GitHub Issues:** [github.com/olavbg/JavaZone/issues](https://github.com/olavbg/JavaZone/issues)

---
---

# Personvernerklæring for JavaZone Android-appen (Norsk)

*Sist oppdatert: September 2026*

Dette er personvernerklæringen for den uoffisielle **JavaZone Android-applikasjonen**, utviklet som et åpent kildekodeprosjekt ([github.com/olavbg/JavaZone](https://github.com/olavbg/JavaZone)).

### Kort oppsummert
- **Ingen sporing:** Vi samler ikke inn, lagrer ikke og deler ikke dine personopplysninger. Appen har ingen brukerkonto, ingen analyseverktøy og ingen reklame.
- **Alt blir på enheten din:** Favorittforedragene dine, innstillinger og påminnelser lagres utelukkende lokalt på telefonen.
- **Tydelige tillatelser:** Tillatelser (som varsler og eksakte alarmer) brukes kun til å gi deg beskjed før favorittsesjonene dine starter.
- **Åpen kildekode:** All kildekode er offentlig tilgjengelig og kan verifiseres av hvem som helst på GitHub.

### Hva lagres lokalt på telefonen?
- **Favoritter:** Foredrag du stjernemerker lagres i en lokal database på enheten (Room).
- **Innstillinger:** Valg av tema (mørkt/lyst), språk, konferanseår og varslingstidspunkt lagres lokalt (DataStore).
- **Mellomlagret program:** Programmet lagres lokalt slik at appen fungerer raskt og stabilt også når du er offline i konferanselokalene.

### Nettverk og eksterne tjenester
Appen kobler seg kun til internett for å hente offentlig konferansedata fra JavaZones åpne API-er (`sleepingpill.javazone.no` og `api.javazone.no`). Dersom du velger å se videoopptak fra tidligere år, spilles disse via en integrert nettspiller (Vimeo/YouTube).

### Krasjhåndtering
Dersom appen krasjer, vises en lokal feilmeldingsskjerm på enheten din. **Ingen data sendes automatisk.** Du kan selv velge om du vil kopiere eller sende rapporten via GitHub Issues eller e-post for å hjelpe til med feilretting.

### Kontakt
Spørsmål eller tilbakemeldinger kan rettes via GitHub: [github.com/olavbg/JavaZone/issues](https://github.com/olavbg/JavaZone/issues).
