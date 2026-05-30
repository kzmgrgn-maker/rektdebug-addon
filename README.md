# RektDebug Addon — Meteor Client

A Meteor Client addon with 2 modules for detecting block movements and sending alerts.

## Modules

### RektMovement
Scans a configurable radius around the player and detects block state changes in real-time.

**Settings:**
- `radius` — Block scan radius (default: 16)
- `display-duration` — How long detections stay visible in ms (default: 3000)
- `show-coords` — Show coordinates of detected changes
- `chat-log` — Print detections to chat

### RektAlert
Monitors RektMovement and sends sound + chat notifications when changes are detected.

**Settings:**
- `sound-alert` — Play a ping sound on detection
- `chat-alert` — Send a colored chat message
- `sound-volume` — Alert sound volume (0.1–1.0)
- `cooldown` — Minimum ticks between alerts (default: 20)

## Building

### Requirements
- Java 21
- Gradle 8+

```bash
./gradlew build
```

The output `.jar` will be in `build/libs/`. Copy it to your Minecraft `mods/` folder alongside Meteor Client.

## Compatibility
- Minecraft: 1.21.4
- Meteor Client: 0.5.8
- Fabric Loader: 0.16.9
