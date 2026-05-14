# Design System Specification

## 1. Overview & Creative North Star: "Kinetic Precision"

This design system is anchored by the Creative North Star of **Kinetic Precision**. In the context of a high-performance bike riding app, the UI must feel as fast, lean, and intentional as a carbon-fiber racing frame. We are moving away from the "boxy" nature of standard apps toward a "High-End Editorial" aesthetic that favors aggressive contrast, generous negative space, and tonal depth.

The system breaks the "template" look by utilizing **intentional asymmetry**. Map interfaces should bleed into the background, while data overlays use overlapping layers to create a sense of movement. This isn't just a utility; it’s a premium cockpit for the rider.

---

## 2. Colors: Tonal Depth & The "No-Line" Rule

The palette is built on a foundation of absolute blacks and "Vantablack-adjacent" grays, punctuated by a high-frequency neon green.

### Palette Highlights
- **Primary (`#9cff93`)**: The "Electric Neon." Used for the most critical actions and active path tracking.
- **Surface Hierarchy**: We utilize `surface-container-lowest` (#000000) for the deepest background layers and `surface-bright` (#2c2c2c) for elevated interactive elements.

### The "No-Line" Rule
**Explicit Instruction:** Designers are prohibited from using 1px solid borders to section content. Boundaries must be defined solely through background color shifts. For example, a rider's profile card (`surface-container-high`) should sit directly on the dashboard background (`surface`) without a stroke. Separation is achieved through the 4-6% shift in luminance.

### The "Glass & Gradient" Rule
To prevent the UI from feeling "flat" or "static," floating telemetry panels must use **Glassmorphism**. Apply `surface-container-low` at 60% opacity with a `20px` backdrop blur. 
- **Signature Texture:** Use a linear gradient on Primary CTAs transitioning from `primary` (#9cff93) to `primary-container` (#00fc40) at a 135-degree angle to simulate the sheen of a polished bike frame.

---

## 3. Typography: High-Contrast Utility

Typography must remain legible at 25mph on a vibrating handlebar mount. We pair the technical, futuristic geometry of **Space Grotesk** with the clean, Swiss-influenced readability of **Plus Jakarta Sans**.

- **Display (Space Grotesk):** Large, aggressive, and tight-kerning. Used for speedometers and "Race Start" headers. It conveys a sense of mechanical urgency.
- **Body (Plus Jakarta Sans):** Highly legible with a generous x-height. Used for group chat messages and navigation instructions.

| Role | Font | Size | Weight | Use Case |
| :--- | :--- | :--- | :--- | :--- |
| **Display-LG** | Space Grotesk | 3.5rem | Bold | Speed / Prime Metrics |
| **Headline-MD** | Space Grotesk | 1.75rem | Medium | Route Names / Leaderboards |
| **Title-LG** | Plus Jakarta Sans | 1.375rem | SemiBold | Card Headers |
| **Body-LG** | Plus Jakarta Sans | 1rem | Regular | Descriptive Text |
| **Label-MD** | Plus Jakarta Sans | 0.75rem | Bold | Map Markers / Small Metadata |

---

## 4. Elevation & Depth: Tonal Layering

We reject the standard Material Design drop-shadow. Elevation in this system is a result of light emission and stacking.

- **The Layering Principle:** Depth is achieved by "stacking" surface tiers. An active "Ride Feed" card uses `surface-container-highest` (#262626) nested within a `surface-container-low` (#131313) container.
- **Ambient Glows:** Instead of black shadows, use a "Primary Glow" for active elements. A floating "Start Ride" button should have a shadow of `0px 12px 24px rgba(156, 255, 147, 0.15)`.
- **The "Ghost Border" Fallback:** If a divider is mandatory for accessibility (e.g., in dense settings), use a **Ghost Border**: `outline-variant` (#484847) at 15% opacity.

---

## 5. Components

### Buttons
- **Primary:** `primary` background, `on-primary` text. Use `xl` (0.75rem) roundedness. No border.
- **Secondary:** `surface-container-highest` background. High-contrast white text.
- **Ghost (Tertiary):** No background. `primary` text. Used for "Cancel" or "Back."

### Custom Map Markers
- **User Marker:** A `primary` neon circle with a `white` 2px "Ghost Border" and a pulse animation using 20% opacity `primary-container`.
- **Group Members:** `secondary` (#89faaa) markers to differentiate from the self-icon.
- **POI Markers:** `surface-bright` diamond shapes with `tertiary` (#8af2ff) icons for water stops or repair stations.

### Input Fields
- **Container-less Design:** Use a simple `surface-container-highest` fill with a `0.25rem` bottom-radius only. 
- **Active State:** The bottom edge glows with a 2px `primary` line. No full-box stroke.

### Cards & Lists
- **Forbid Dividers:** Use `8px` of vertical white space or a shift from `surface-container-low` to `surface-container-highest` to separate list items. 
- **Asymmetry:** Give cards a `0.75rem` (xl) radius on the top-left and bottom-right corners, but a `0.125rem` (sm) radius on the others to create a "directional" aerodynamic feel.

---

## 6. Do's and Don'ts

### Do
- **Do** use `display-lg` typography for single, impactful data points.
- **Do** allow the map to be the "bottom-most" layer, with UI elements floating using Glassmorphism.
- **Do** use `primary` sparingly. It should represent "Action" and "Success" only.

### Don't
- **Don't** use 100% opaque borders. They clutter the dark mode and feel dated.
- **Don't** use pure white (#FFFFFF) for long-form body text; use `on-surface-variant` (#adaaaa) to reduce eye strain during outdoor use.
- **Don't** use standard "Drop Shadows." Use tonal shifts or subtle ambient color glows.