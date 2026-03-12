# SidePanel - Industry-Level Overlay Experience

SidePanel is a high-performance, aesthetically polished Android overlay application inspired by premium Android skins like OriginOS, HyperOS, and Realme UI. It provides users with a seamless side-access app launcher featuring physics-based animations, frosted glass aesthetics, and deep customization.

## 🌟 Key Features

### 🎨 Premium UI Themes
Experience multiple industry-standard design languages with a single tap:
*   **OriginOS (Default):** The classic, smooth frosted pill aesthetic.
*   **HyperOS:** Modern semi-transparent "Square Glass" look with sharp, refined corners.
*   **Realme UI:** Minimalist dark theme with subtle accent borders.
*   **Rich UI (Premium):** Advanced layout featuring a premium outer glow, vertical grab handles, and high-density information displays.

### ⚙️ Deep Customization
*   **Live Preview:** Real-time visual feedback in the settings menu as you tweak your configuration.
*   **Dynamic Sizing:** Adjust edge handle height and width to fit your ergonomic needs.
*   **Opacity Control:** Full 0-100% transparency control for the panel and triggers.
*   **Vertical Positioning (Premium):** Slide the trigger handle to any vertical position on the screen edge.
*   **Multi-Column Layout (Premium):** Toggle between sleek single-column and high-density double-column app grids.

### 🚀 Performance & UX
*   **Zero-Latency Loading:** Optimized asynchronous app indexing using Kotlin Coroutines to ensure the panel opens instantly without frame drops.
*   **Physics-Based Animations:** Smooth, interruptible spring animations aligned with the device VSYNC.
*   **Haptic Feedback:** Professional-grade tactile response across all interactions (launches, removals, and toggles).
*   **Robust Persistence:** Smart caching of pinned apps ensures your setup is never lost, even after service restarts.

## 🛠 Technical Stack

*   **Language:** 100% Kotlin
*   **Asynchrony:** Kotlin Coroutines & Flow
*   **Animations:** Physics-based Spring Animations (`DynamicAnimation`)
*   **UI Engine:** Custom `WindowManager` overlay service with hardware-accelerated rendering
*   **Architecture:** Repository pattern for reliable `PackageManager` interaction
*   **View System:** ViewBinding for type-safe layout management

## 📸 Screenshots

| OriginOS Style | HyperOS Style | Rich UI Theme |
| :---: | :---: | :---: |
| ![Origin](screen.png) | *(Coming Soon)* | *(Coming Soon)* |

## 🚀 Getting Started

### Prerequisites
*   Android 8.0 (API 26) or higher.
*   `SYSTEM_ALERT_WINDOW` permission (Display over other apps).

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/Imtiaz-Official/SidePanel.git
   ```
2. Open in Android Studio.
3. Build and run on your device.
4. Grant the "Display over other apps" permission when prompted.

## 💎 Premium Features
The project includes a simulated Premium system that unlocks:
*   Vertical handle offset positioning.
*   High-density 2-column mode.
*   Advanced themes (HyperOS, Realme, Rich UI).
*   Custom UI accent colors.

---
Developed with ❤️ by [Imtiaz-Official](https://github.com/Imtiaz-Official)
