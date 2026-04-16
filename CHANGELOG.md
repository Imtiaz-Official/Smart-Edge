# Project History & Milestones 📜

**Project Persona**: Smart Edge: Sidebar & Gestures — Floating overlay service launcher
**Tech Stack**: Kotlin, Android SDK 26+, Material3, ViewBinding, DynamicAnimation, Glide
**Design Language**: Material You (M3) + Custom Glassmorphism

---

### v1.3.2 (Latest)
- **feat(tile)**: Completely rewrote Quick Settings Tile — now matches the Start button behavior exactly with a reliable, seamless toggle.
- **feat(tile)**: Optimistic UI update — tile switches state instantly on tap, with no visible delay.
- **feat(tile)**: Haptic feedback on Quick Tile click for tactile confirmation.
- **feat(tile)**: Fixed cross-process SharedPreferences desync on OEM devices (Vivo, MIUI, ColorOS) causing tile to auto-revert to OFF.
- **feat(tile)**: Seamless shade behavior — toggling the tile no longer forces your current app to close.
- **feat(tile)**: Centralized Start/Stop logic shared between App button and Quick Tile via `PanelPreferences.toggleService()`.
- **feat(ui)**: App status now updates live when toggled from the Quick Tile while the app is open.
- **feat(appearance)**: Replaced double-column toggle with a multi-column selector dropdown (1–3 columns).
- **feat(settings)**: Added Backup & Restore — export/import all settings as JSON to/from Downloads folder.
- **feat(ui)**: App-wide Light / Dark / System theme mode support.
- **feat(shizuku)**: Integrated Shizuku for rootless system automation; added Revoke permission option.
- **feat(handle)**: Drag-to-reposition the edge handle directly on screen.
- **feat(interaction)**: Toggle to disable auto-showing keyboard when opening the app picker dashboard.
- **feat(notifications)**: Permission check and direct OEM intent fallback for Notification Access.
- **fix**: Apps no longer vanish on long-press when Drag-to-Split is disabled.
- **fix**: Active notification apps now reliably appear in the panel.
- **fix**: Settings reset fully clears panel apps and hides disabled features from search.
- **fix**: Reversed back arrow in the Support screen corrected.
- **fix**: Disabled panel bounce animation for a smoother feel.

---

### v1.3.1
- **feat(shortcuts)**: Added **Quick Action Shortcut** (Toggle Sidebar) accessible via app icon long-press and launcher widgets.
- **feat(gestures)**: Added **Advanced Tap Gestures** support (Double Tap and Triple Tap) with a smart gesture detection engine and redesigned settings UI.
- **feat(productivity)**: Added **Notification Hub** to the dashboard. View active notifications and launch apps in **Freeform Windows** instantly.
- **feat(ui)**: Completely revamped **Color Picker** with modern skydoves implementation. Added **Hex input**, live preview, and Alpha/Brightness sliders.
- **feat(ui)**: Added **Icon Scale** customization (0.8x to 2.0x). Enlarged icons everywhere while keeping the panel slim.
- **feat(tools)**: Integrated **Volume and Brightness** system controls with continuous hold-to-adjust support and real-time status indicators.
- **feat(handle)**: Significantly expanded **Vertical Offset** range (+/- 500dp) for extreme top/bottom placement.
- **feat(onboarding)**: Modernized **Setup Screen** with Material 3 theme and integrated Notification Access guide.
- **fix**: Fixed bug where disabling swipe hid the pill handle when tap gestures were active.
- **fix**: Optimized panel width calculation to prevent layout bloating when scaling icons.
- **fix**: Fixed various stability issues and crashes related to background service context.

---

### v1.3.0
- **feat(ui)**: Redesigned App Picker UI with a modern dashboard header and staggered entry animations.
- **feat(ui)**: Enhanced 2-column mode with tighter panel width and 10% larger icons.
- **feat(ui)**: Redesigned search result highlights with a satisfying 3D bounce and prominent pill flash.
- **fix(ui)**: Fixed Settings search bar layout bug that caused typed text to be invisible.
- **chore**: Adjusted default picker gap spacing (20dp) for a cleaner layout.

---

### v1.2.3
- **feat(handle)**: Added "Touch Area Width" customization (16dp to 100dp) in Handle & Pill settings.
- **fix(gesture)**: Improved swipe-to-open gesture reliability by reducing sensitivity to backward movement and jitter.
- **fix(handle)**: Fixed "ghosting" or duplicate pills when adjusting settings (enforced aggressive removeViewImmediate).
- **fix(handle)**: Fixed issue where the pill would disappear if gestures were disabled while tap-to-open was still active.
- **fix(layout)**: Synchronized visual pill alignment with dynamic touch area widths.
- **refactor**: Optimized gesture timing and hold duration for a more responsive feel.

---

### v1.2.2
- **feat(handle)**: Introduced Pill Color and Pill Thickness customization for the side handle.
- **feat(layout)**: Improved sidebar and app picker compactness with dynamic height wrapping and capped limits.
- **feat(ux)**: Added 'Restore Default' buttons for all customization sliders across all settings.
- **fix(accessibility)**: Robust accessibility service detection using a static status flag (fixes issues on Vivo and high-res devices).
- **fix(stability)**: Enforced strict screen boundaries for the edge handle to prevent it from disappearing on large screens.
- **refactor**: Simplified handle settings by consolidating trigger area and visual appearance controls.
- **design**: Brighter and more balanced default look for the pill indicator.

---

### v1.2.1
- **feat(freeform)**: Implemented Freeform Window multitasking with smart orientation-aware aspect ratios.
- **feat(icons)**: Added Superior Icon Pack support with full appfilter.xml parsing and heuristic mapping.
- **feat(automation)**: New professional System Automation dialog for ADB/Root setup.
- **feat(layout)**: Added Sidebar Max Height customization in Appearance settings.
- **fix(compatibility)**: Improved device compatibility for Android 14+ and optimized service stability.
- **fix(setup)**: Fixed SetupActivity redirect issue on fresh installations.
- **design(ui)**: Enhanced theme visuals for Realme UI and Rich UI (Glow) with custom strokes and gradients.
- **chore(phones)**: Locked icon scaling to 1.0f for phones to maintain perfect visual balance.

---

### v1.2
- **refactor**: Refactored settings into a multi-page dashboard (Appearance, Interaction, Handle, Tools).
- **feat(glide)**: Replaced slow PackageManager icon loading with Glide + custom AppIconModelLoader.
- **feat(cache)**: Added fast in-memory cache for system default icons.
- **fix(touch)**: Improved FloatingPanelService touch interception for reliable outside clicks.
- **design(accessibility)**: Standardized Accessibility Icon to native Android Material Vector.
- **fix(xiaomi)**: Added Xiaomi/MIUI/HyperOS specific intent fallback for accessibility settings.

---

### v1.1
- **fix(permission)**: Implemented programmatic auto-start detection for MIUI and OriginOS (Vivo) to fix false-positive 'granted' status.
- **fix(service)**: Fixed service restart bug where the sidebar would reappear after manual stopping.
- **feat(docs)**: Updated F-Droid metadata with new phone screenshots.
- **feat(licensing)**: Migrated to a 100% Open Source model for **F-Droid** compliance.
- **feat(ux)**: Unlocked all features by default for the FOSS version.
- **design(ui)**: Redesigned the Support page with community-focused visuals and branding.

---

### Previous Milestones
- **fix**: resolve duplicate apps bug and implement customizable picker gap spacing.
- **feat**: implement premium design system, onboarding screen, and UI optimizations.
- **feat**: overhaul settings UI with Material 3 components and optimized layouts.
- **feat**: migrate screenshot tool to native system action via AccessibilityService.
- **feat(project)**: Initial V1 implementation of OriginOS-inspired Side Panel.
- **feat(project)**: Initial project scaffold — Gradle KTS, version catalog, AndroidManifest.
