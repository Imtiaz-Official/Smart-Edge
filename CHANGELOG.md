# Project History & Milestones 📜

**Project Persona**: Smart Edge: Sidebar & Gestures — Floating overlay service launcher
**Tech Stack**: Kotlin, Android SDK 26+, Material3, ViewBinding, DynamicAnimation, Glide
**Design Language**: Material You (M3) + Custom Glassmorphism

---

### v1.3.7 (Latest)
- **feat(interaction)**: Added Dual Edge Trigger Handles (#83) — configure Left, Right, or Both screen edges with independent side-docking and slide animations.
- **feat(sidebar)**: Added Auto-Close Sidebar Timer (#66) with configurable timeouts (3s, 5s, 10s, 15s) and active touch-reset.
- **feat(sidebar)**: Added setting to hide the bottom chevron/arrow close button (#70).
- **feat(sidebar)**: Added direct app unpin/remove confirmation dialog (#37) in edit mode and on long-press (when Drag-to-Split is disabled).
- **feat(diagnostics)**: Added built-in Crash & Diagnostic Logger (#74) in Miscellaneous settings with one-click copy and clear.
- **feat(ui)**: Added Android 13+ Material You themed monochrome launcher icon support (#27).
- **perf(memory)**: Added aggressive system memory trimming (`onTrimMemory`) to release Glide resources and icon caches under memory pressure.
- **fix(ui)**: Resolved custom accent and background colors not applying on HyperOS, Realme UI, and OriginOS devices (#84).
- **fix(ui)**: Resolved glassmorphic blur appearing only upon closing — blur now renders immediately upon panel open (#82).
- **fix(picker)**: Fixed search tap collapsing panel on keyboard/IME appearance (#78).
- **fix(picker)**: Fixed App Picker requiring two outside clicks to dismiss — now dismisses both keyboard and panel in a single tap (#79).
- **feat(picker)**: Pressing Enter/Search on keyboard now automatically launches the top matched app (#75).
- **fix(tools)**: Fixed Tools folder button persisting in sidebar when disabled in settings (#86, #69).
- **fix(gestures)**: Fixed single-tap on pill handle causing size shrink/unresponsiveness (#65).
- **fix(sidebar)**: Fixed 'Show only on home screen' not working on third-party launchers (#63).
- **fix(shortcuts)**: Fixed Power Menu shortcut triggering lock screen or crash on modern Android (#62).
- **fix(gestures)**: Fixed crash on landscape rotation by clamping edge handle offset bounds (thanks @spectra00, #72).
- **feat(localization)**: Added complete Ukrainian localization (thanks @SABERBIRD, #80) and in-app language picker in Miscellaneous settings.

---

### v1.3.6
- **feat(interaction)**: Reorganized Interaction settings dashboard into logical, consolidated groups (Main Trigger, Specialized Interaction, Panel Experience, Multitasking, Shortcuts, Advanced, System).
- **feat(interaction)**: Restored missing Panel Experience features (Landscape, Notification Apps, Scroll Memory, Keyboard) and System & Behavior features (Auto-start, Debug Logs, Animation Feel, Picker Gap).
- **feat(shortcuts)**: Restored Desktop Shortcuts feature to pin a Toggle Sidebar shortcut.
- **refactor(ui)**: Refactored InteractionSettingsActivity to fully utilize ViewBinding.
- **refactor(core)**: Centralized action execution logic into a new ActionDispatcher class to unify Edge and Notch handle behaviors.
- **fix(core)**: Added a mandatory engine check (Accessibility or Automation) in FloatingPanelService before displaying the handle.
- **fix(ui)**: Resolved crash in Handle settings by expanding vertical offset range and safely clamping out-of-bounds positions.
- **fix(multitasking)**: Fixed freeform window feature to correctly spawn floating windows with system caption bars and better orientation detection.
- **fix(multitasking)**: Restored the missing Multitasking & Windowing settings section (Freeform and Split-screen toggles) to the dashboard.
- **fix(ui)**: Fixed invisible pill graphic issue by using correct system defaults for reset actions and forcing synchronous redraws.
- **feat(automation)**: Improved Native Gesture Engine dialog feedback to accurately and instantly reflect Root/Shizuku active connection states.
- **fix(multitasking)**: Smoothed out freeform window launches by switching to native system transitions instead of custom fade animations.

---

### v1.3.5.1
- **feat(handle)**: Restored 'Move Handle' drag feature and set it as the default long-press action.
- **feat(ui)**: Restored independent pill color customization with a modern color picker and reset option.
- **feat(localization)**: Added Spanish localization and a new in-app language selector in Miscellaneous settings.
- **feat(gestures)**: Added Notch Gestures for camera cutout interaction (Tap, 2x, 3x, Hold).
- **feat(sidebar)**: Added 'Only on Home Screen' mode to hide sidebar/handle inside other apps.
- **feat(tools)**: Added new dashboard tools: Camera, Flashlight (with system sync), and Auto-Rotation.
- **fix(handle)**: Significant stability improvements for handle dragging with absolute-to-relative coordinate mapping.
- **fix(ui)**: Resolved handle drift and "slow sliding" issues by silencing system animations during drag.
- **fix(tools)**: Resolved flashlight toggle desync and edge side switching glitches.
- **fix(ui)**: Harmonized UI across settings, miscellaneous sections, and improved overall visual consistency.

---

### v1.3.5
- **feat(gestures)**: Added 'Long Press' gesture support to the edge handle. You can now configure a specific action (like Go to Home) for a long press.
- **feat(animation)**: Restored 'Slide' as a selectable animation style and made it the default for a smoother entry.
- **fix(animation)**: Resolved issue where the main sidebar would always slide even if 'Popup' was selected.
- **fix(ui)**: Fixed false-negative 'Accessibility Required' warning when using Shizuku-based Native Gestures.
- **feat(ui)**: Re-introduced Animation Style selection in Interaction Settings.
- **fix(animation)**: Unified animation logic between the main panel and app picker to respect user preferences consistently.
- **feat(ui)**: Added search functionality to all App Selectors (Hide Apps, Game Mode, Pinned Apps).
- **feat(icons)**: Modernized Icon Pack selection UI with a sleek BottomSheet design.
- **feat(gestures)**: Added split-screen support to the Native Gesture Engine.
- **feat(gestures)**: Implemented Native Gesture Engine (via Root or Shizuku) for seamless system navigation, bypassing traditional accessibility delays.
- **feat(gestures)**: Improved swipe responsiveness and added a customizable swipe sensitivity setting.
- **fix(ui)**: Resolved pill flickering and ghosting issues during rapid interactions.

---

### v1.3.4
- **feat(gestures)**: Added advanced tap gesture actions including Back, Home, Recents, Notifications, Quick Settings, Lock Screen, and Power Menu.
- **feat(gestures)**: Added fully configurable multi-tap gestures (single, double, and triple tap).
- **feat(sidebar)**: Added ability to manually select specific apps to hide the sidebar in (replaced automatic immersive mode detection).
- **feat(sidebar)**: Added a dedicated tools folder and navigation system.
- **fix(icons)**: Fixed "blank clickable icons" issue by disabling problematic RecyclerView optimizations and implementing explicit state clearing.
- **feat(icons)**: Implemented high-definition, super-sampled icon rendering for perfectly smooth "Circle" shapes.
- **fix(icons)**: Improved real-time settings refresh using a global appearance tracking key.
- **fix(ui)**: Fixed stuck drag shadow in freeform/drag-to-split operations.
- **fix(sidebar)**: Resolved glitchy app sorting and reordering in the sidebar.
- **fix(service)**: Prevented unintended service restarts when disabled and cleaned up debug artifacts.
- **chore**: Updated default settings to enable Dashboard/Tools by default, disabled multi-tap/remember-scroll by default.
- **feat(support)**: Added WebMoney (WMZ) donation option to the Support screen.

---

### v1.3.3
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
