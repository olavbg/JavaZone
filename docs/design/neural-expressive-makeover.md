# JavaZone Android UI — Neural Expressive Makeover

## Goal

Redesign the JavaZone Android app around a compact, developer-focused conference experience inspired by Google's Neural Expressive / Material 3 Expressive principles. The timeline remains the primary surface; the redesign should improve scanability, hierarchy, navigation, and visual identity without sacrificing the useful conference information already present.

## Product principles

- **Timeline first:** a developer should be able to answer “what is happening now?”, “what is next?”, and “what else is happening in parallel?” at a glance.
- **Year is global context:** remove the separate Archive destination. `JavaZone 2026` in the top app bar becomes an interactive year selector.
- **No bulky bottom navigation:** use the available vertical space for schedule content. Secondary destinations are reached from the app bar and contextual actions.
- **Expressive, not decorative:** use color, shape, scale and motion to reinforce hierarchy and state rather than adding visual noise.
- **Information density with breathing room:** preserve useful metadata while reducing oversized controls and redundant chrome.
- **Accessible by default:** color must not be the only state indicator; maintain touch targets, contrast and large-font usability.

## Proposed information architecture

### Main timeline

Top app bar:

`JavaZone 2026 ▾                         ♡  ⚙`

Below it:

1. Compact day selector.
2. Optional search/filter surface.
3. Timeline grouped by time slot.
4. Strong visual treatment for the current time and next session.

### Year picker

Tapping the year opens a modal/bottom-sheet style year selector containing current and historical years. Selecting a historical year switches the app's global year context and presents that year's available programme/archive content. There should no longer be a dedicated “Archive” tab.

### Session detail

Use a clear hierarchy:

- format / time / room metadata
- prominent session title
- speakers
- favorite / calendar / reminder actions
- description
- video/resources when available

## Visual direction

### Palette

Use a dark “ink” foundation with a restrained Java identity:

- Ink background: deep navy/near-black
- Elevated surface: slightly lighter blue-black
- Java green: primary expressive accent
- Cyan/blue: standard talks and informational emphasis
- Coral/orange: high-energy or special states
- Violet: workshops
- Amber: lightning talks

The existing semantic format/room colors should be consolidated into a small, centrally defined token system. Avoid using accent colors as arbitrary decoration.

### Typography

Introduce a deliberate type scale rather than relying almost entirely on Material defaults:

- Display/title: expressive, compact and confident
- Session title: strong weight with controlled line height
- Metadata: compact and highly scannable
- Body: comfortable reading size
- Labels: small, medium/high weight, never overly condensed

### Shapes

Use a small shape vocabulary:

- rounded cards for sessions
- pill-shaped compact filters/statuses
- larger rounded containers for important state such as “NOW”
- avoid excessive nesting of rounded rectangles

### Motion

Use subtle spring-like transitions for:

- changing day/year
- expanding filters
- favorite state
- current-session emphasis
- sheet/dialog appearance

Motion should communicate state and orientation, not delay the user.

## Timeline redesign

The timeline should visually distinguish:

- **NOW** — current time marker and active session
- **NEXT** — nearest upcoming session
- **PAST** — reduced visual prominence
- **PARALLEL** — simultaneous sessions grouped naturally by time slot

Keep the existing automatic scroll-to-current/upcoming behavior, but make the resulting state visually obvious.

Session cards should prioritize:

1. time / duration
2. title
3. room
4. format
5. speaker(s)
6. favorite state

Avoid making every metadata item equally prominent.

## Header/filter redesign

The current combination of app bar, day selector and multiple horizontal chip rows consumes too much vertical space. Replace it with a compact hierarchy:

- app bar with year selector and two or three high-value actions
- day selector
- a single filter/search control row
- active filters displayed only when needed

Search and filters should be easy to reach but should not dominate the default schedule view.

## Implementation notes

The current application already contains useful architecture for year-specific archive loading, timeline grouping, favorites, filters, search, settings and session details. Reuse those behaviors where possible instead of rewriting business logic unnecessarily.

The implementation should centralize visual decisions in `ui/theme` and introduce reusable components/tokens for:

- app bar
- day selector
- filter controls
- session card
- timeline time marker
- format badge
- year picker
- session detail metadata

Navigation should be simplified by removing the `Archive` destination and treating year selection as the mechanism for accessing historical programmes.

## Acceptance criteria

- No large bottom navigation bar on the main experience.
- Historical years remain accessible without losing the existing archive functionality.
- `JavaZone <year>` is an interactive global year selector.
- Timeline remains the primary and fastest way to consume the programme.
- Header occupies substantially less vertical space.
- Parallel sessions remain easy to compare.
- Current/upcoming sessions are immediately recognizable.
- Existing search, filters, favorites, reminders, settings and session detail functionality remain available.
- Light and dark themes remain coherent.
- Accessibility and large-font layouts are considered.
- No business/data-layer behavior is changed unless required by the navigation redesign.
