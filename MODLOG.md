# WolfCraft Mod Log

## Environment
- Minecraft: 1.21.11 (Release)
- Fabric Loader: 0.19.5 / 0.16.9
- Fabric API: 0.141.6+1.21.11
- Gradle: 8.14
- Fabric Loom: 1.13.3
- Java: 21 (Microsoft OpenJDK 21.0.7 / 21.0.12.1)

## Fixes & Changes
1. **Targeting Minecraft 1.21.11 natively:**
   - Upgraded Gradle wrapper to 8.14 and Loom to 1.13.3.
   - Project dependencies now target `minecraft_version=1.21.11` and `fabric_api_version=0.141.6+1.21.11`.
2. **Fixed Raycast Crash (`NoSuchMethodError: method_18077`):**
   - Removed legacy `ProjectileUtil.getEntityHitResult` calls (Mojang changed its signature in 1.21.2+ to require `ProjectileEntity` instead of `Entity`).
   - Implemented direct, rock-solid entity raycasting via `level.getEntities()` with `AABB.clip(eyePos, reach)`.
3. **Fixed Damage Method for 1.21.11 (`NoSuchMethodError: method_5643`):**
   - In 1.21.11, entity damage uses `hurtServer(ServerLevel, DamageSource, float)` (`method_64397`), not the legacy 1.21.0 `hurt(DamageSource, float)`.
4. **Item Properties Registration:**
   - Used native `Item.Properties.setId(key)` with `Identifier.fromNamespaceAndPath(...)`.
5. **Fixed Knife Texture & UV Mapping:**
   - Cleaned RTCW authentic knife texture (`knife1a.jpg`), removed bright white background bleeding.
   - Recalibrated UV coordinates in `knife.json` to properly map the blade, guard, grip, and pommel.
6. **LMB & RMB Firing:**
   - Full-auto support maintained via `ClientTickEvents` and `ClientPreAttackCallback`.
7. **Fixed Double-Firing on LMB & Audio Phasing (v1.0.1):**
   - Debounced `ClientPreAttackCallback` and `ClientTickEvents.END_CLIENT_TICK` so clicking LMB fires strictly once per cadence cycle instead of triggering both callbacks simultaneously in the same tick.
   - Fixed Tesla Gun consuming 2 cells per shot.
   - Balanced gunshot & zap audio volume to `0.85f`.
   - On server side, passed `player` as first argument to `level.playSound` so the client player who already played the sound locally doesn't receive an echoing/overlapping delayed sound packet.
8. **Authentic RTCW 3D Polygon Meshes & Direct GPU Baking (v1.0.2):**
   - Eliminated all blocky/cuboid Minecraft box models for RTCW items.
   - Built custom `ObjItemModel` implementing native 1.21.11 `ItemModel`, wired through Fabric's `ModelLoadingPlugin.modifyItemModelAfterBake`.
   - Extracted authentic 3D meshes directly from RTCW `pak0.pk3`:
     * Combat Knife (`knife.mdc` -> 171 verts, 278 triangles)
     * MP40 Submachine Gun (`mp40.mdc` -> 198 verts, 138 triangles)
     * Tesla Gun (`tesla.mdc` -> 217 verts, 264 triangles)
     * 9mm MP40 Magazine (`am9mm_s.mdc` -> 24 verts, 12 triangles)
     * Tesla Battery Cell (`amcell.mdc` -> 94 verts, 76 triangles)
   - Transformed and normalized all meshes to item coordinates centered at `(0.5, 0.5, 0.5)` with hand grips placed at origin.
   - Converted mesh triangles into double-sided `BakedQuad`s with exact UVs packed via `UVPair.pack`, rendered directly on the GPU via `Sheets.translucentItemSheet()`.
   - Replaced artificial knife texture with genuine original RTCW `knife1a.jpg` (512x128 steel dagger blade, wood/steel hilt, no cyan).
   - Replaced ammo textures with authentic RTCW magazine and battery powerup textures.

9. **Fixed Invisible Items / Texture Atlas Separation in Minecraft 1.21.11 (v1.0.3):**
   - **Root Cause Identified:** In Minecraft 1.21.11, Mojang separated blocks and items into two separate atlases (TextureAtlas.LOCATION_BLOCKS = minecraft:textures/atlas/blocks.png and TextureAtlas.LOCATION_ITEMS = minecraft:textures/atlas/items.png).
   - In WolfCraftClient.java, item texture queries were requesting TextureAtlas.LOCATION_BLOCKS, which failed to find mod item textures in the block atlas, logged missing texture warnings, and returned lockMissing.
   - When ObjItemModel rendered with Sheets.translucentItemSheet() (which binds items.png), the quads sampled the top-left region of items.png where pixels are completely transparent (alpha = 0), making all weapons and ammo items 100% invisible in hand, on the ground, and in GUI.
   - **Fixes Applied:**
     * Updated WolfCraftClient.java to query TextureAtlas.LOCATION_ITEMS (Sheets.ITEMS_MAPPER.apply(...)), correctly resolving textures to minecraft:textures/atlas/items.png.
     * Added ssets/minecraft/atlases/items.json and ssets/wolfcraft/atlases/items.json atlas definitions.
     * Implemented robust UV coordinate wrapping in ObjItemModel.java to prevent out-of-bounds UV coordinates (>1.0 or <0.0) from bleeding across the atlas.
     * Called layer.prepareTintLayers(0) in ObjItemModel.update() to initialize tint layer state.
10. **Weapon Viewmodel Alignment, Sound Levels, Controls & Horizontal Lightning (v1.0.4):**
    - **Viewmodel Alignment:**
      * Rotated `mp40.json` and `tesla_gun.json` in `firstperson_righthand` from pointing sideways (`rotation: [0, -90, 25]`) to pointing straight forward into the screen towards the crosshair (`rotation: [-5, 185, -2]`, `translation: [-1.5, 0.5, -2.0]`, `scale: [0.65, 0.65, 0.65]`).
      * Configured matching symmetrical `firstperson_lefthand` and realistic third-person aiming poses.
    - **Audio Balancing:**
      * Reduced MP40 firing sound volume by 15% (from `0.85f` to `0.72f`).
      * Reduced Tesla Gun zap sound volume by 35% (from `0.85f` to `0.55f`).
    - **Controls Overhaul (LMB Fire / RMB Reload):**
      * LMB (Left Click): Fires weapons. Full-auto support maintained via network packet `ShootPayload` registered through Fabric Networking API v1 (`PayloadTypeRegistry.playC2S()` & `ServerPlayNetworking`).
      * RMB (Right Click): Reloads weapons. MP40 holds 32 rounds (consumes 9mm ammo from inventory, plays reload sound `ModSounds.MP40_RELOAD`, sets 1.25s cooldown). Tesla Gun holds 50 charges (recharged with `ModItems.TESLA_CELL`).
      * Added visual hotbar charge/ammo durability bars and informative tooltips.
    - **Muzzle Origin & Particles:**
      * Shifted particle / raycast spawn origin from player's eyes/face to the actual gun muzzle in the lower right viewport (`Vec3 muzzle = eyePos.add(look.scale(0.85)).add(right.scale(0.35)).add(up.scale(-0.25))`), eliminating screen occlusion.
      * Greatly reduced MP40 smoke and flame particle sizes so they no longer obstruct the player's view.
    - **Horizontal Tesla Lightning Beam:**
      * Removed the pink/white star `ELECTRIC_SPARK` particle tunnel.
      * Created `TeslaArcEntity` and `TeslaArcRenderer` utilizing Minecraft's native `RenderTypes.lightning()` shader.
      * Renders horizontal electric lightning arcs traveling directly from the gun muzzle to the struck creature and branching to chained enemies with authentic jagged geometry, glowing cyan-blue corona, and blazing white-hot core.
11. **Two-Handed Weapon Grip & Shooting Recoil Animation (v1.0.5):**
    - **Two-Handed Stance (Third Person):**
      * Created `AvatarRendererMixin` injecting into `AvatarRenderer.getArmPose` to return `HumanoidModel.ArmPose.CROSSBOW_HOLD` when holding WolfCraft guns (MP40, Tesla Gun).
      * Player now stands and runs raising both arms on chest level, holding the grip with right hand and supporting the forend with left hand, dynamically aiming up/down with player pitch.
      * Adjusted `thirdperson_righthand` transforms in `mp40.json` and `tesla_gun.json` (`rotation: [0, 180, 0]`, `translation: [0, 2.5, -1.5]`, `scale: [0.65, 0.65, 0.65]`) to perfectly fit the two-handed forward aiming pose.
    - **Dynamic Hand & Weapon Recoil Kickback:**
      * Implemented `RecoilHandler` with smooth decayed kickback impulse.
      * Created `ItemInHandRendererMixin`: In first-person view, each gunshot snappy-translates the weapon and hands backward by ~1.5 cm with a slight muzzle climb of ~2.5 degrees, then smoothly returns to rest.
      * Created `AnimationUtilsMixin`: In third-person view, kicks both raised arms backward into the shoulders on each shot.
    - **Fixes Applied in v1.0.5:**
      * Corrected Mixin descriptor in `AvatarRendererMixin`: in 1.21.11, `AvatarRenderer.getArmPose` is overloaded; explicitly targeted `getArmPose(Avatar, HumanoidArm)` with signature `(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/world/entity/HumanoidArm;)Lnet/minecraft/client/model/HumanoidModel$ArmPose;`, fixing knot class transformation on game launch.

12. **Straightened Aim Alignment, Deepened Grip & Accurate Muzzle Flash (v1.0.6):**
    - **Straightened Gun Yaw & Pitch (`CROSSBOW_HOLD` Compensation):**
      * Vanilla Minecraft `AnimationUtils.animateCrossbowHold` turns the right arm inward by `-0.3` rad (`-17.19°`) and pitches up by `+0.1` rad (`+5.73°`).
      * Updated `thirdperson_righthand` and `thirdperson_lefthand` rotation in `mp40.json` and `tesla_gun.json` to `rotation: [ 6, 163, 0 ]`. This counteracts the arm inward rotation and elevation, keeping the gun barrel pointing straight ahead parallel to the crosshair / view vector.
    - **Deepened Pistol Grip into Fist:**
      * Shifted translation Y from `0.8` down to `0.0` in `mp40.json` and from `0.5` to `0.0` in `tesla_gun.json`.
      * Moves the pistol grip center directly into the character's hand palm instead of grasping the bottom tip of the handle.
    - **Centered Muzzle Flash & Particles:**
      * In `MP40Item.java` and `TeslaGunItem.java`, replaced `right.scale(0.35)` with `right.scale(0.04)` and `up.scale(-0.35)` at `look.scale(0.85)` / `look.scale(0.90)`.
      * Muzzle smoke, flame, and electric arcs now spawn directly from the gun barrel muzzle rather than floating out past the right elbow.

13. **Calibrated Muzzle Height & Bullet Tracer Origin (v1.0.7):**
    - **Exact Chest Barrel Elevation:**
      * Replaced `up.scale(-0.35)` with `up.scale(-0.55)` and forward offset `look.scale(0.75)` in `MP40Item.java` and `look.scale(0.80)` in `TeslaGunItem.java`.
      * Lowers the muzzle spawn position from mouth/visor level (height 1.27m) down to genuine gun barrel height (1.07m from feet), perfectly matching where the two-handed gun rests across the player's chest.
    - **Tracer Emission from Muzzle:**
      * Shifted the CRIT bullet tracer line from `eyePos.add(shootDir.scale(d))` to `muzzle.add(shootDir.scale(d))`.
      * Eliminates particles floating in front of the player's face/forehead; tracers now originate directly from the metal muzzle tip.

14. **Full Authentic RTCW Arsenal Port (v1.1.0):**
    - **Sten Submachine Gun:**
      * Authentic 3D mesh (`sten.mdc` -> 276 verts, 312 triangles) and original texture (`sten.png`).
      * Silenced stealth firing (suppresses distant mob aggro, quiet suppressed gunshot sound `ModSounds.STEN_FIRE`).
      * Heat management system: heat builds up per shot (`HEAT_PER_SHOT = 9`), steam particles hiss from barrel jacket when hot; reaches 100% causes overheat lock (`ModSounds.STEN_OVERHEAT`, 2.5s cooldown).
      * 32-round magazine capacity using `ammo_9mm`, reload via RMB.
    - **Venom 6-Barrel Gatling Gun:**
      * Authentic 3D mesh (`venom.mdc` -> 291 verts, 231 triangles) and original texture (`venom.png`).
      * Authentic Gatling mechanics: 3-tick barrel spin-up delay (`ModSounds.VENOM_SPINUP`), blistering 1-tick fire cadence (1200+ RPM) with heavy 12.7mm damage (9.0 per bullet).
      * When LMB is released, barrels spin down with authentic `ModSounds.VENOM_SPINDOWN`.
      * Player movement speed slowdown while firing to simulate heavy rotary cannon weight.
      * 500-round drum capacity using `ammo_127mm` ammo boxes (each box loads 100 rounds).
    - **Panzerfaust Rocket Launcher:**
      * Authentic 3D mesh (`panzerfaust.mdc` -> 254 verts, 271 triangles) and warhead rocket mesh (`panzerfaust_rocket.mdc` -> 270 verts, 292 triangles).
      * Launches high-velocity `PanzerfaustRocketEntity` with trailing smoke plume (`ModSounds.PANZERFAUST_FIRE`).
      * Real terrain destruction (`Level.ExplosionInteraction.BLOCK`) and massive splash damage up to 65 HP on impact (`ModSounds.PANZERFAUST_EXPLODE`).
      * Reloadable with `panzerfaust_rocket` items via RMB.
    - **Mauser 98k Sniper Rifle:**
      * Authentic 3D mesh (`mauser.mdc` -> 141 verts, 131 triangles) and original texture (`mauser.png`).
      * 10-round 7.92mm internal magazine loaded with `ammo_792mm`.
      * RMB toggles optical scope mode (zero bullet spread across 120-block range).
      * Upper-hitbox headshot detection (top 25% of mob box) triggers 2.5x critical multiplier (62.5 damage) with `[HEADSHOT!]` indicator and extra crit particles.
      * 26-tick bolt-action cycling delay with authentic cycling sound.
    - **Ammo & Resource Items:**
      * Authentic 3D models and textures for `ammo_792mm`, `ammo_127mm`, and `panzerfaust_rocket`.
      * All weapons and ammo added to creative tab `WOLFCRAFT_TAB`.
      * Comprehensive Russian (`ru_ru.json`) and English (`en_us.json`) localizations.
      * Registered `PanzerfaustRocketEntity` renderer via `ThrownItemRenderer`.

15. **Arsenal Enhancements, Scaling & Shoulder Launcher Pose (v1.1.1):**
    - **Venom 6-Barrel Gatling Gun Model & Texture Fix:**
      * Extracted full weapon mesh from `pu_venom.mdc` (553 vertices, 477 triangles) restoring all 6 rotating barrels which were previously missing from the body-only `venom.mdc`.
      * Extracted authentic 512x512 HD texture `v4.jpg` from `sp_pak2.pk3` covering both the receiver body and the full 6-barrel assembly.
    - **Panzerfaust Pure Rocket Projectile:**
      * Replaced the previous `ampf.mdc` model (which included parts of a wooden ammo delivery crate) with pure Surface 2 from `models/ammo/rocket/rocket.mdc` (247 vertices, 258 triangles).
      * Excluded 2D billboard engine flare planes; rocket is now a clean aerodynamic missile.
    - **Weapon Scale Rebalancing:**
      * Sten SMG (`sten.json`): Increased third-person scale by +50% (from `0.95` to `1.42`) and first-person scale to `0.85`.
      * Mauser 98k Rifle (`mauser.json`): Doubled third-person scale (from `1.0` to `2.0`) and first-person scale to `0.85`.
      * Panzerfaust (`panzerfaust.json`): Increased third-person scale by +35% (from `1.05` to `1.42`) and first-person scale to `0.85`.
    - **Shoulder-Fired Panzerfaust Stance:**
      * Created `HumanoidModelMixin` injecting into `HumanoidModel.setupAnim(HumanoidRenderState)`.
      * When holding `ModItems.PANZERFAUST`, raises the firing arm straight forward at shoulder level tracking player pitch and head rotation, and bends the support arm across the chest supporting the tube from below.
      * Repositioned `panzerfaust.json` display transforms to rest the launcher tube directly over the player's shoulder.
    - **OBJ Newline Formatting Fix (v1.1.2):**
      * Re-exported `venom.obj` (1584 lines) and `panzerfaust_rocket.obj` (753 lines) with genuine LF/CRLF newlines, resolving the single-line parsing issue where models appeared invisible. Both models now render completely solid and textured.

16. **Venom Overheat Overhaul, 20% First-Person Weapon Upscale & Rich Tooltips (v1.1.3):**
    - **Venom Overheat & Sustained Firing Mechanics:**
      * Separated cooling from firing: Gun only cools down when the barrels have stopped spinning (`spin == 0`). While holding LMB, cooling is suspended.
      * Overheat accumulates reliably during sustained bursts (~5-7 seconds of continuous 1200+ RPM fire) up to 100%.
      * Reaching 100% heat triggers `ModSounds.VENOM_OVERHEAT`, 3.5s cooldown lockout, smoke discharge from barrels, and resets spin speed. Overheat logic applies in both survival and creative modes.
    - **Venom Gatling Third-Person Doubling:**
      * Doubled third-person scale in `venom.json` to `2.2` (was `1.1`), with adjusted hand translation `[0.5, 0.0, -3.5]`. The rotary Gatling cannon now looks truly massive and menacing when carried by players.
    - **+20% First-Person Viewmodel Scale Across All Weapons:**
      * Scaled up `firstperson_righthand` and `firstperson_lefthand` for all weapons by +20% so they feel properly proportioned and weighty in hand:
        - `knife.json`: `0.90` (was 0.75)
        - `mp40.json`: `0.78` (was 0.65)
        - `tesla_gun.json`: `0.78` (was 0.65)
        - `sten.json`: `1.02` (was 0.85)
        - `mauser.json`: `1.02` (was 0.85)
        - `panzerfaust.json`: `1.02` (was 0.85)
        - `venom.json`: `0.85` (was 0.70)
    - **Rich 3-Line Inventory Hover Tooltips:**
      * Implemented `appendHoverText` for all newly ported weapons matching MP40 and Tesla Gun:
        - Sten SMG: Damage (6.5), silenced stealth property, magazine count (32), heat gauge %, controls reminder.
        - Venom Gatling: Damage (9.0), fire rate (1200+ RPM), drum capacity (500), dynamic heat %, controls reminder.
        - Panzerfaust: Explosive splash damage (up to 65.0), terrain destruction note, loaded rocket status (1/1 or 0/1), controls reminder.
        - Mauser 98k: Base damage (25.0), headshot crit damage (62.5), internal magazine (10), optical scope status, controls reminder.

17. **Full Weapon Reload Animations, Rotary Venom Barrels & Tesla Electric Corona Ring (v1.2.0):**
    - **Rotary Venom Gatling 6-Barrel Assembly:**
      * Separated `venom.obj` into static receiver/drum (`venom.obj`) and 6-barrel assembly (`venom_barrel.obj`).
      * Enhanced `ObjItemModel` to bake and dynamically rotate sub-part quads around exact cylinder axis `(0.5868, 0.5000)`.
      * Barrels smoothly spin up when firing/holding LMB, roar at full rotary speed (1200+ RPM), and smoothly decelerate to rest on trigger release.
      * Works seamlessly in first person, third person, item frames, and dropped items.
    - **Animated Tesla Gun Rotor & High-Voltage Electric Corona:**
      * Separated `tesla_gun.obj` into static housing (`tesla_gun.obj`) and front electrode rotor (`tesla_ring.obj`).
      * Front 3-electrode rotor smoothly revolves continuously in idle, accelerating during firing.
      * Created `TeslaCoronaRenderer`: draws 3 real-time jittering high-voltage electric lightning arcs between the 3 rotating electrode tips using Minecraft's native `RenderTypes.lightning()` shader.
      * Renders intense cyan glowing corona ribbons with blazing white-hot cores, perfectly recreating authentic RTCW Tesla Gun visuals.
    - **Procedural First-Person Reload Animations for All Guns (`WeaponAnimationHandler`):**
      * **MP40 & Sten:** Weapon lowers, rolls inward opening mag well, mechanical thud/jolt as fresh magazine is slapped in, bolt cocking pull, then snaps smoothly back to aim.
      * **Mauser 98k:**
        - After each shot: hand lifts bolt handle, pulls backward with cartridge ejection kick, pushes forward chambering round, locks down.
        - From empty: full reload dipping down, double thumb-press bumps into internal magazine, bolt slammed forward.
      * **Panzerfaust:** Lowers heavy tube from shoulder to chest level, sharp forward shove as new warhead clicks into muzzle, hoisted back onto shoulder.
      * **Tesla Gun:** Gun tilts and dips, battery cell snaps into chamber with mechanical lock, raises back to center.
      * **Venom Gatling:** Heavy rotary cannon tilts down, 500-round steel ammo box clamps on with heavy thud; displays heat vibration tremor during overheat.

18. **Eliminated Attack Swing, Added First-Person Player Arm & Iron-Sight Mauser (v1.2.1):**
    - **Vanilla Attack Swing Cancellation:**
      * Injected into `ItemInHandRenderer.applyItemArmAttackTransform` to cancel the violent pickaxe/sword swing when holding any WolfCraft gun.
      * Cleared client-side `player.attackAnim`, `player.swinging`, and `player.swingTime` in client tick when holding firearms.
      * The weapon no longer violently chops downward or jerks like breaking blocks; it remains rock-steady in firing position, allowing recoil, spinning barrels, and reload animations to be cleanly visible.
    - **First-Person Arm Rendering:**
      * Added `@Invoker("renderPlayerArm")` to `ItemInHandRendererMixin` to render the player's textured arm and hand holding the gun in first person, matching classic shooters.
    - **Mauser 98k Iron Sights Overhaul:**
      * Completely removed the optical zoom scope toggle from RMB.
      * Made Mauser a pure infantry bolt-action rifle with authentic iron sights.
      * RMB now directly triggers rapid stripper-clip reload from inventory (no Shift key required), matching all other firearms.
      * Updated hover text tooltip in inventory to reflect pure bolt-action and reload controls.

19. **Cleaned Viewmodel & Eliminated Re-Equip Drop Animation (v1.2.2):**
    - **Removed Floating Player Arm:**
      * Stripped the awkward disconnected player arm box, restoring the clean, authentic 3D view of RTCW weapon models without clunky block limbs.
    - **Fixed Re-Equip Drop on Firing / Ammo Change:**
      * Injected into `ItemInHandRenderer.shouldInstantlyReplaceVisibleItem` to return `true` for all WolfCraft firearms when custom data (ammo count, heat, durability) updates.
      * Prevents Minecraft from triggering the re-equip drop animation (which dipped weapons down off-screen on every shot).
      * Firearms now remain completely locked in shooting stance at all times, with clean, crisp recoil kickback and smooth reload animations.

20. **Comprehensive Gameplay Fixes from User Video Analysis (v1.2.3):**
    - **Strict Semi-Automatic Pistols:**
      * Added `WolfCraftClient.isSemiAuto()` covering Colt 1911 and Luger P08 (and Panzerfaust single-rocket launcher).
      * Continuous firing in `ClientTickEvents.END_CLIENT_TICK` now strictly ignores semi-auto weapons when holding LMB.
      * Pistols now fire exclusively on discrete mouse clicks via `ClientPreAttackCallback`.
    - **Pistol Viewmodel Display & Forward Repositioning:**
      * Re-calibrated first-person right-hand transforms in `colt.json` and `luger.json` (`translation: [-0.85, 0.15, -1.05]`, `scale: [1.45, 1.45, 1.45]`).
      * Shifted pistols forward and upward onto the screen so the entire pistol (frame, grip, slide, sights) is clearly visible.
    - **Eliminated Pistol Disappearance on Shot:**
      * Discovered that `shoot()` in `ColtItem` and `LugerItem` was calling `player.getCooldowns().addCooldown(stack, 4)`.
      * In `ItemInHandRendererMixin`, any active cooldown triggers `applyPistolReload()`, which dropped the pistol downward off-screen for 4 ticks on every single click.
      * Removed cooldown from `shoot()` so pistols remain completely steady in firing stance, only animating during actual RMB reloads.
    - **Muzzle Flash & Particle Trajectory Overhaul:**
      * Implemented `WolfCraftMod.spawnMuzzleFlashAndTracer()` using yaw trigonometry to prevent vector degeneration when looking vertically.
      * Generates small muzzle flame and smoke right at the barrel tip, with straight tracer particles heading directly toward the hit target point.
      * Unified muzzle flash and tracer across all ballistic weapons (Colt, Luger, Thompson, MP40, Sten, Mauser, Venom).
      * Aligned Tesla Gun lightning origin and Panzerfaust rocket launch muzzle offset with shoulder/eye height.
    - **Fixed Tesla Rotor Eccentric Wobble:**
      * Corrected rotation pivot in `WolfCraftClient.java` and `TeslaCoronaRenderer.java` from eccentric `Y = 0.7064` to the true circumcenter `Y = 0.6586` of the 3 rotor electrodes.
      * The rotating ring and high-voltage corona arcs now spin concentrically.
    - **Cleaned Venom Gatling 6-Barrel Mesh:**
      * Stripped static frame bracket faces with radius $r > 0.071$ from `venom_barrel.obj` (leaving 266 cylindrical barrel faces) and merged the static bracket into `venom.obj`.
      * Eliminates the stray rotating artifact from the Gatling assembly.
    - **Added Missing 1.21.11 Item Definitions & Textures:**
      * Created all 9 missing item JSONs in `assets/wolfcraft/items/` for Minecraft 1.21.11 compatibility.
      * Extracted and saved missing `flamethrower.png` and `medkit_large.png` to `textures/item/`.
    - **Flamethrower Audio Smoothing:**
      * Added loop cooldown (~1350ms) in `FlamethrowerItem.java` to prevent rapid overlapping audio spam when holding LMB.

21. **Viewmodel Tweaks & Clean Venom Mesh Re-Export (v1.2.4):**
    - **Venom Gatling Gun Texture & Geometry Fix:**
      * Re-exported `venom.obj` (283 verts, 233 tris) and `venom_barrel.obj` (270 verts, 244 tris) from RTCW `pu_venom.mdc` using clean connected-component partitioning.
      * Completely eliminated the stationary barrel fragment that was improperly merged into the body with wrapped/stretched UV coordinates (which caused the distorted texture artifact in screenshot 1).
      * Barrels rotate around exact circumcenter `(0.5860f, 0.4983f, 0.8f)` with 100% authentic RTCW texture mapping.
    - **Luger P08 Viewmodel Adjustment (Screenshot 2):**
      * Reduced scale from 1.45 to 1.05 (`scale: [1.05, 1.05, 1.05]`).
      * Distanced away from camera into the screen (`translation: [-0.78, 0.05, -1.45]`, `rotation: [-4, 183, -1]`).
    - **Colt 1911 Alignment & Distancing (Screenshot 3):**
      * Leveled out and straightened the pistol (`rotation: [-2, 180, 0]`), eliminating the sideways cant and tilt.
      * Reduced scale from 1.45 to 1.12 (`scale: [1.12, 1.12, 1.12]`).
      * Distanced further away from the player (`translation: [-0.76, 0.04, -1.45]`).
    - **Thompson SMG Upscaling (Screenshot 4):**
      * Increased first-person scale from 1.2 to 1.45 (`scale: [1.45, 1.45, 1.45]`), giving it proper presence and weight in hand (`translation: [-0.85, -0.35, 0.1]`).

22. **Full Localization & Internationalization (i18n) (v1.2.5):**
    - **Replaced Hardcoded Literals:** Replaced all hardcoded text strings across item classes (`ColtItem`, `LugerItem`, `ThompsonItem`, `MP40Item`, `StenItem`, `MauserItem`, `VenomItem`, `FlamethrowerItem`, `TeslaGunItem`, `PanzerfaustItem`, `GrenadeItem`, `MedkitItem`, `KnifeItem`) with standard Minecraft `Component.translatable(...)` calls.
    - **Bilingual Dictionary Parity (116 keys):** Synchronized `assets/wolfcraft/lang/en_us.json` and `assets/wolfcraft/lang/ru_ru.json` with matching 116 localization keys:
      * Weapon tooltips: damage numbers, magazine capacities, ammo calibers (.45 ACP, 9mm, 7.92mm, 12.7mm, fuel canister, Tesla cells, rockets), special mechanics (silencer stealth, headshots, chain lightning, splash radius, burn ticks, medkit healing & regen), and control prompts.
      * In-game action bar alerts: magazine full notices, reloaded counts (survival & creative), empty magazine warnings, barrel overheat & cooling timers, Gatling spin-up cues, headshot popups, and fuel/battery warnings.
    - **Dynamic Client Language Switching:** All tooltips and action bar messages now automatically switch in real-time when toggling between English and Russian in Minecraft language settings.


