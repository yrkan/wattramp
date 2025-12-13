# WattRamp

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Platform](https://img.shields.io/badge/Platform-Karoo%202%20%7C%20Karoo%203-orange.svg)](https://www.hammerhead.io)

Free FTP testing extension for Hammerhead Karoo cycling computers. No subscriptions, no accounts, works offline.

## Features

- **Three FTP Test Protocols**
  - **Ramp Test**: Progressive power increase until failure (~20 min)
  - **20-Minute Test**: Classic FTP protocol with max sustained effort (~60 min)
  - **8-Minute Test**: Two max effort intervals with recovery (~50 min)

- **Real-time Data Fields**
  - Current interval with progress bar (2x1 graphical)
  - Target power, test progress, power zone indicators
  - Deviation from target, elapsed time, average/max power
  - Live FTP prediction during ramp test

- **In-Ride Alerts**
  - Interval change notifications
  - Countdown warnings before transitions
  - Motivational messages
  - Test completion with calculated FTP

- **Offline Operation**
  - No internet or external accounts required
  - All data stored locally on device

## Installation

### From APK

1. Download the latest APK from releases
2. Transfer to your Karoo via USB or web dashboard
3. Install using file manager on device

### Build from Source

Requirements:
- Android Studio or JDK 17+
- Gradle 8.x
- GitHub account (for Karoo SDK access)

#### 1. Configure GitHub Packages access

The Karoo Extension SDK is hosted on GitHub Packages which requires authentication. Create a [GitHub Personal Access Token](https://github.com/settings/tokens) with `read:packages` scope.

Add to `~/.gradle/gradle.properties` (global) or project `local.properties`:

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_GITHUB_TOKEN
```

#### 2. Build

```bash
git clone https://github.com/yrkan/wattramp.git
cd wattramp
./gradlew assembleDebug
```

APK location: `app/build/outputs/apk/debug/app-debug.apk`

## Usage

1. **Setup**: Open WattRamp app on Karoo, configure your current FTP and test preferences
2. **Add Data Fields**: In ride profile setup, add WattRamp data fields to your screen
3. **Start Ride**: Begin recording a ride
4. **Run Test**: Open WattRamp app during ride and select a test protocol
5. **Follow Prompts**: Watch data fields for target power and interval changes
6. **View Results**: After test completion, see calculated FTP and save to history

## FTP Calculation

| Protocol | Formula | Default Coefficient |
|----------|---------|---------------------|
| Ramp Test | Max 1-min Power × k | 0.75 |
| 20-Minute Test | 20-min Average × k | 0.95 |
| 8-Minute Test | Avg(8min₁, 8min₂) × k | 0.90 |

Coefficient can be adjusted in settings (conservative/standard/aggressive).

## Data Fields

| Field | Size | Description |
|-------|------|-------------|
| Current Interval | 2x1 | Graphical widget with interval name, target, progress |
| Target Power | 1x1 | Current target wattage |
| Test Progress | 1x1 | Step number or time progress |
| Power Zone | 1x1 | Zone indicator relative to target |
| Deviation | 1x1 | Watts above/below target |
| Elapsed Time | 1x1 | Total test duration |
| Average Power | 1x1 | Session average |
| Max Power | 1x1 | Session maximum |
| FTP Prediction | 1x1 | Live FTP estimate (Ramp test) |

## Settings

- **Current FTP**: Your baseline FTP for zone calculations
- **Ramp Start Power**: Initial wattage for ramp test (default: 100W)
- **Ramp Step**: Power increase per minute (default: 20W)
- **Warmup/Cooldown Duration**: Configurable warmup and cooldown periods
- **Sound Alerts**: Enable/disable audio notifications
- **Screen Wake**: Wake screen on important alerts
- **Motivational Messages**: Show encouragement during test
- **Language**: Multiple languages supported
- **Theme**: Orange or Blue color scheme

## Requirements

- Hammerhead Karoo 2 or Karoo 3
- Power meter (required for FTP testing)

## Privacy

WattRamp is designed with privacy in mind:

- **All data stays on your device** — no external servers, no cloud sync
- **No accounts required** — just install and use
- **No analytics or tracking** — we don't collect any usage data
- **Open source** — verify our privacy practices yourself

Read our full [Privacy Policy](https://wattramp.com/privacy).

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## Support

- **Issues**: [GitHub Issues](https://github.com/yrkan/wattramp/issues)
- **Email**: [info@wattramp.com](mailto:info@wattramp.com)
- **Website**: [wattramp.com](https://wattramp.com)

## License

MIT License — see [LICENSE](LICENSE) file for details.

## Acknowledgments

- Built with [Karoo Extension SDK](https://github.com/hammerheadnav/karoo-ext)
- FTP protocols based on established cycling science methodologies

---

**Disclaimer**: WattRamp is not affiliated with Hammerhead or SRAM. Karoo is a trademark of Hammerhead.
