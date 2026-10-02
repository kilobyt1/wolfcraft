# WolfCraft: Return to Castle Wolfenstein Arsenal

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-brightgreen.svg)](https://minecraft.net/)
[![Fabric API](https://img.shields.io/badge/Fabric-0.141.6%2B1.21.11-blue.svg)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**WolfCraft** is a high-fidelity Minecraft mod for **Fabric 1.21.11** that brings the iconic weaponry, authentic audio, 3D polygonal models, and combat mechanics from the legendary shooter **Return to Castle Wolfenstein** (RTCW) into Minecraft.

---

## ⚔️ Arsenal & Equipment

### 🔫 Sidearms & Submachine Guns
* **Colt 1911** — Classic .45 ACP semi-automatic sidearm (7 rounds, discrete single-fire cadence).
* **Luger P08** — Standard 9mm German semi-automatic pistol (8 rounds).
* **Thompson M1A1** — Iconic American submachine gun (600 RPM, .45 ACP, 30 rounds, rapid spray).
* **MP40** — Standard issue automatic SMG (550 RPM, 9mm Parabellum, 32 rounds, balanced recoil).
* **Sten (Silenced)** — Covert stealth SMG that does not alert nearby monsters; features active barrel heat buildup (9mm, 32 rounds).

### 🎯 Rifles & Heavy Ordnance
* **Mauser 98k** — Bolt-action battle rifle with heavy stopping power and lethal headshot critical multipliers (7.92mm stripper clips, 10 rounds).
* **Venom Gatling Gun** — 6-barrel rotary minigun with genuine spin-up acceleration, 1200+ RPM firing rate, barrel overheating, and recoil drag (12.7mm ammo drum, 500 rounds).
* **Flammenwerfer 16** — Continuous stream flamethrower shooting a wide 12-block fire cone, igniting blocks and entities (150 fuel tank capacity, refueled via Fuel Canisters).
* **Tesla Gun** — Experimental occult electrical projector firing horizontal chain lightning between up to 3 targets with a rotating 3-electrode corona rotor (50 Tesla energy cells).
* **Panzerfaust 60** — Single-shot recoilless anti-tank rocket launcher causing massive crater explosions and destructive shockwaves.

### 💣 Explosives & Gear
* **Stielhandgranate 24** — Authentic stick grenade with friction igniter cord, parabolic physics, and a 3.5-second fuse.
* **Combat Knife** — Rapid-slashing trench dagger with steel blade and wood grip.
* **First Aid Kits** — Small First Aid Kit (+8 HP instant) and Field First Aid Kit (+20 HP instant + Regeneration II for 10 seconds).

---

## 🛠️ Technical Highlights

* **Authentic 3D Polygon Meshes:** RTCW meshes rendered natively on the GPU via a custom `ObjItemModel` pipeline built specifically for Minecraft 1.21.11's modern model system.
* **Animated Weapon Dynamics:** Concentric rotating Tesla ring with spark emission and continuous 6-barrel minigun rotation during spin-up and fire.
* **Ballistics & Particles:** Directional muzzle flashes, smoke puffs, and straight-line tracer rounds calculated with yaw trigonometry to prevent vector degeneration when looking vertically.
* **Viewmodel Stability:** Fine-tuned first-person viewmodel offsets, recoil kickback, and instant hand updates that prevent re-equip dipping animations while firing.
* **Bilingual Localization:** 100% full localization support for English (`en_us`) and Russian (`ru_ru`) across all weapon stats, ammo types, controls, and action-bar feedback.

---

## 📦 Installation & Requirements

### Requirements:
- **Minecraft:** `1.21.11`
- **Fabric Loader:** `0.16.9` or higher
- **Fabric API:** `0.141.6+1.21.11`
- **Java:** `21`

### Installation:
1. Download or compile `wolfcraft-1.0.0.jar`.
2. Ensure you have installed **Fabric Loader** and **Fabric API** for Minecraft 1.21.11.
3. Place `wolfcraft-1.0.0.jar` into your Minecraft `.minecraft/mods` directory.
4. Launch the game and enjoy!

---

## 🔨 Building from Source

To compile the mod JAR yourself:

```bash
# Clone the repository
git clone https://github.com/kilobyt1/wolfcraft.git
cd wolfcraft

# Build with Gradle wrapper
./gradlew remapJar
```

The resulting mod JAR will be located in:
`build/libs/wolfcraft-1.0.0.jar`

---

## 📜 Credits & License

- **Return to Castle Wolfenstein** original game assets and inspiration: **id Software** & **Gray Matter Interactive**.
- Code and mod implementation licensed under the **[MIT License](LICENSE)**.
