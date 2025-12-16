# WattRamp

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Platform](https://img.shields.io/badge/Platform-Karoo%202%20%7C%20Karoo%203-orange.svg)](https://www.hammerhead.io)
[![Release](https://img.shields.io/github/v/release/yrkan/wattramp)](https://github.com/yrkan/wattramp/releases)
[![Downloads](https://img.shields.io/github/downloads/yrkan/wattramp/total)](https://github.com/yrkan/wattramp/releases)

Free FTP testing extension for Hammerhead Karoo cycling computers. No subscriptions, no accounts, works completely offline.

## Features

### Three FTP Test Protocols

| Protocol | Duration | Best For | Formula |
|----------|----------|----------|---------|
| **Ramp Test** | ~20 min | Quick assessment, time-crunched athletes | Max 1-min × 0.75 |
| **20-Minute Test** | ~60 min | Gold standard, accurate results | 20-min avg × 0.95 |
| **8-Minute Test** | ~50 min | Balance of accuracy and time | Avg of two 8-min × 0.90 |

### Real-time Data Fields

9 data fields available for your ride screen:

| Field | Size | Description |
|-------|------|-------------|
| **Current Interval** | 2x1 | Graphical widget with interval name, target power, progress bar |
| **Target Power** | 1x1 | Current target wattage or "MAX" for max effort intervals |
| **Test Progress** | 1x1 | Step number (Ramp) or percentage complete |
| **Power Zone** | 1x1 | Zone indicator: IN ZONE / TOO LOW / TOO HIGH |
| **Deviation** | 1x1 | Watts above/below target (e.g., +15W, -8W) |
| **Elapsed Time** | 1x1 | Total test duration |
| **Average Power** | 1x1 | Session average power |
| **Max Power** | 1x1 | Session maximum power |
| **FTP Prediction** | 1x1 | Live FTP estimate (updates during Ramp test) |

### In-Ride Alerts

- **Interval Changes**: Visual + audio notification when phases change
- **Countdown Warnings**: 30-second and 10-second warnings before transitions
- **Motivational Messages**: "HALFWAY!", "PUSH HARDER!" encouragement
- **Low Cadence Warning**: Alert when cadence drops below optimal range
- **Test Completion**: Final FTP result with comparison to previous

### Sound Alerts

Audible beep patterns using Karoo's internal beeper:
- Single beep: Interval changes
- Double beep: Test start / completion
- Triple beep: Final 30 seconds

## Installation

### From GitHub Releases (Recommended)

1. Download `wattramp-x.x.x.apk` from [Releases](https://github.com/yrkan/wattramp/releases)
2. Transfer to Karoo:
   - **Option A**: Use [Hammerhead Web Dashboard](https://dashboard.hammerhead.io) → Apps → Sideload
   - **Option B**: Connect Karoo via USB, copy APK to device, install via file manager
3. Launch WattRamp from app drawer

### Build from Source

**Requirements:**
- Android Studio Arctic Fox+ or JDK 17+
- Gradle 8.x

**Setup GitHub Packages access** (Karoo SDK requires authentication):

1. Create [GitHub Personal Access Token](https://github.com/settings/tokens) with `read:packages` scope
2. Add to `~/.gradle/gradle.properties`:
```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_GITHUB_TOKEN
```

**Build:**
```bash
git clone https://github.com/yrkan/wattramp.git
cd wattramp
./gradlew assembleRelease
```

APK: `app/build/outputs/apk/release/wattramp-x.x.x.apk`

## Usage Guide

### Initial Setup

1. **Open WattRamp** on your Karoo
2. **Set your current FTP** (used for warmup/recovery zone calculations)
3. **Configure test parameters** (optional):
   - Ramp start power (default: 100W)
   - Ramp step increment (default: 20W/min)
   - Warmup/cooldown duration
   - Sound and screen wake preferences

### Adding Data Fields

1. Go to **Profiles** → Select your ride profile → **Edit**
2. Add a new page or edit existing
3. Tap empty slot → **More Data** → **WattRamp**
4. Select desired fields (recommended: Current Interval 2x1 + 2-3 numeric fields)

### Running a Test

1. **Start a ride** (WattRamp requires an active recording)
2. **Open WattRamp app** during ride
3. **Select test protocol** (Ramp / 20-min / 8-min)
4. **Follow the intervals**:
   - Watch target power on data fields
   - Stay in zone (green = good, red = too low/high)
   - Listen for audio cues
5. **Complete the test**:
   - Ramp: Continue until you can't maintain power (auto-detects failure)
   - 20-min / 8-min: Follow all intervals to completion
6. **View results** and optionally save to history

### Test Protocol Details

#### Ramp Test
```
[5 min Warmup @ 50%] → [Ramp: +20W every minute until failure] → [5 min Cooldown]
```
- Starts at configurable power (default 100W)
- Increases by configurable step (default 20W) every minute
- Auto-ends when power drops >30% below target for 10+ seconds
- FTP = Max 1-minute average × 0.75

#### 20-Minute Test
```
[20 min Warmup] → [5 min Blow-out @ 105%] → [5 min Recovery] → [20 min MAX EFFORT] → [10 min Cooldown]
```
- Classic protocol for accurate FTP measurement
- Requires pacing strategy for 20-min effort
- FTP = 20-minute average × 0.95

#### 8-Minute Test
```
[15 min Warmup] → [8 min MAX #1] → [10 min Recovery] → [8 min MAX #2] → [10 min Cooldown]
```
- Two efforts allow for pacing adjustment
- Average of both efforts used
- FTP = Average of two 8-min efforts × 0.90

## Settings

| Setting | Description | Default |
|---------|-------------|---------|
| **Current FTP** | Your baseline FTP for zone calculations | 200W |
| **Ramp Start Power** | Initial power for ramp test | 100W |
| **Ramp Step** | Power increase per minute | 20W |
| **Warmup Duration** | Warmup period length | 5 min |
| **Cooldown Duration** | Cooldown period length | 5 min |
| **FTP Calculation Method** | Conservative (0.72) / Standard / Aggressive (0.77) | Standard |
| **Sound Alerts** | Enable beep notifications | On |
| **Screen Wake** | Wake screen on important alerts | On |
| **Motivational Messages** | Show encouragement during test | On |
| **Language** | UI language | System |
| **Theme** | Orange or Blue color scheme | Orange |

## Architecture

```
io.github.wattramp/
├── WattRampExtension.kt     # KarooExtension service entry point
├── MainActivity.kt          # Settings UI (Jetpack Compose)
├── MainViewModel.kt         # State management for UI
├── datatypes/               # 9 Karoo data field implementations
│   ├── CurrentIntervalDataType.kt  # 2x1 graphical widget
│   ├── TargetPowerDataType.kt
│   ├── TestProgressDataType.kt
│   └── ...
├── protocols/               # FTP test protocol implementations
│   ├── TestProtocol.kt      # Base interface + common logic
│   ├── RampTest.kt
│   ├── TwentyMinTest.kt
│   └── EightMinTest.kt
├── engine/
│   ├── TestEngine.kt        # State machine, Karoo stream handling
│   ├── TestState.kt         # Sealed class for test states
│   └── AlertManager.kt      # In-ride alert management
├── data/
│   ├── PreferencesRepository.kt  # DataStore persistence
│   └── TestHistory.kt       # Test result storage
└── ui/                      # Compose UI screens
```

### Key Technical Details

- **Thread Safety**: All shared state uses `AtomicInteger`, `AtomicLong`, `AtomicReference`, and `Mutex`
- **Memory Management**: Bounded data structures prevent unbounded growth (max 4000 power samples)
- **Karoo SDK**: Uses `KarooSystemService` for power/HR/cadence streams, alerts, and beeps
- **State Flow**: Reactive state management with Kotlin `StateFlow`

## Troubleshooting

### Data fields show "--" or 0
- Ensure a ride is actively recording
- Start a test from the WattRamp app
- Check that power meter is connected

### No sound alerts
- Verify "Sound Alerts" is enabled in settings
- Check Karoo system volume
- Some older Karoo firmware may have beeper limitations

### Test doesn't auto-end (Ramp)
- Power must drop >30% below target for 10+ consecutive seconds
- Ensure power meter is transmitting consistently
- You can manually stop via the app if needed

### FTP seems too high/low
- Try different calculation methods in settings (Conservative/Aggressive)
- Ensure you gave maximum effort during test intervals
- Compare with other FTP tests for validation

## Requirements

- **Device**: Hammerhead Karoo 2 or Karoo 3
- **Sensors**: Power meter (required)
- **Optional**: Heart rate monitor (for HR zone display)

## Privacy

WattRamp respects your privacy:

- **100% Offline**: No internet connection required or used
- **Local Storage Only**: All data stays on your device
- **No Accounts**: No registration, login, or cloud sync
- **No Analytics**: Zero tracking or telemetry
- **Open Source**: Verify everything yourself

## Changelog

### v1.3.0
- Thread safety improvements for Karoo 3 stability
- Implemented sound alerts (PlayBeepPattern)
- Fixed HR zones using actual user max HR from profile
- Fixed progress calculation for 20-min and 8-min tests
- Memory optimization with bounded data structures
- R8 minification (APK size: 17MB → 2.1MB)

### v1.2.2
- Added configurable warmup/cooldown duration
- Karoo settings integration

### v1.2.1
- Registered all 9 data types
- Localization fixes

### v1.2.0
- Performance improvements
- Added MainViewModel for state management

## Contributing

Contributions welcome! Please:

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

## Support

- **Issues**: [GitHub Issues](https://github.com/yrkan/wattramp/issues)
- **Discussions**: [GitHub Discussions](https://github.com/yrkan/wattramp/discussions)

## License

MIT License — see [LICENSE](LICENSE) file.

## Acknowledgments

- Built with [Karoo Extension SDK](https://github.com/hammerheadnav/karoo-ext)
- FTP protocols based on established cycling science (Coggan, Allen, Friel)

---

**Disclaimer**: WattRamp is an independent project, not affiliated with Hammerhead or SRAM. Karoo is a trademark of Hammerhead.
