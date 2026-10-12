# A New Corps — Green Shield (Fabric 1.20.1)

Independent add-on prototype for Minecraft Java 1.20.1 / Fabric. It does not modify the original A New Corps JAR.

## Current prototype behavior
- Press **G** to toggle the cubic barrier (key is configurable in Minecraft Controls).
- A cube with sharp 90-degree corners is indicated by green particles along its 12 edges.
- The server removes projectile entities that enter the cube.
- Other entities found inside the cube are pushed outside its nearest face and their velocity is cancelled.
- The active barrier also emits green particles server-side so nearby players can see its edges.
- User-supplied texture images are included in `assets/green_shield/textures/entity/` as source assets.

## Important limitations
- This is source code, not a prebuilt/tested JAR. Build it with Java 17 and Gradle installed by running `gradle build`; the output should be in `build/libs/`.
- The supplied PNGs are included but are not yet mapped onto solid translucent cube faces; this prototype renders the shape with particles.
- Protection is implemented by server-side entity/projectile handling, but has not been tested in a live game. Some modded projectile types may not extend Minecraft's standard `ProjectileEntity` class.
- This add-on is standalone and does not yet call A New Corps/Palladium powers. Install Fabric API and the dependencies required by A New Corps as well.

## Build / install
1. Install Java 17 and a Gradle installation.
2. In this folder, run `gradle build`.
3. Copy `build/libs/a-new-corps-green-shield-0.1.0.jar` to your Minecraft `mods` folder.
4. Install Fabric API and the dependencies needed by your A New Corps setup.
5. Launch Minecraft 1.20.1 Fabric and use **G** to toggle the barrier.

## Texture assets
The provided textures are kept unchanged under `src/main/resources/assets/green_shield/textures/entity/` for the next rendering pass.
