# Project History & Milestones 📜

**Project Persona**: Smart Edge: Sidebar & Gestures — Floating overlay service launcher
**Tech Stack**: Kotlin, Android SDK 26+, Material3, ViewBinding, DynamicAnimation, Glide
**Design Language**: Material You (M3) + Custom Glassmorphism

---

### v1.2.1 (Latest)
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
