# SDUI Prototype — Local Dev Setup

## Prerequisites (one-time installs)

### Java 21

```bash
brew install openjdk@21
sudo ln -sfn /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-21.jdk
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 21)' >> ~/.zshrc
source ~/.zshrc
java -version  # should show openjdk 21
```

### Node.js (if not already installed)

```bash
brew install node
```

### Android toolchain

```bash
# Android Studio (optional GUI, useful for AVD management)
brew install --cask android-studio

# Command-line tools (sdkmanager, avdmanager)
brew install android-commandlinetools

# Shell env — add to ~/.zshrc
echo 'export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools' >> ~/.zshrc
echo 'export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH' >> ~/.zshrc
source ~/.zshrc

# Install SDK packages + emulator system image (ARM64 for Apple Silicon)
sdkmanager --install "platform-tools" "emulator" "platforms;android-35" \
  "system-images;android-35;google_apis;arm64-v8a"

# Create the AVD (Pixel 8, API 35)
echo no | avdmanager create avd -n Pixel8_API35 \
  -k "system-images;android-35;google_apis;arm64-v8a" -d "pixel_8"

# Verify
emulator -list-avds  # should show Pixel8_API35
```

### iOS toolchain

> Xcode must be installed from the Mac App Store first.

```bash
# Project generator for SduiDemo.xcodeproj
brew install xcodegen

# Optional: nicer xcodebuild output
brew install xcbeautify

# Create the iPhone 15 Pro Max simulator (iOS 18.5 runtime)
xcrun simctl create "iPhone 15 Pro Max" "iPhone 15 Pro Max" "iOS18.5"

# Verify
xcrun simctl list devices | grep "iPhone 15 Pro Max"
```

### Web dependencies

```bash
cd web && npm install
```

---

## Secrets

Populate `server/.env` before starting the server (file is gitignored):

```
STATS_API_KEY=your_key_here
ABLY_SUBSCRIBE_API_KEY=your_key_here
```

These are read automatically by Spring Boot via `spring-dotenv` on startup.

---

## Running the stack

| What | Command |
|---|---|
| Server + Web only (no Android) | `make dev-no-android` |
| Full stack (server + web + Android) | `make dev` |
| Server only | `make dev-server` |
| Web only | `make dev-web` |
| Android emulator + app | `make dev-android` |
| iOS simulator + app | `make ios-run` |
| Stop everything | `make stop` |

### URLs when running

| Service | URL |
|---|---|
| Spring Boot API | http://localhost:8080 |
| Vite web app | http://localhost:5173 |

---

## Troubleshooting

### `invalid source release: 21`
Java 17 is active instead of 21. Fix:
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

### `No AVDs found`
Either the AVD wasn't created yet (run the `avdmanager create avd` command above),
or `ANDROID_HOME` is not set in the current shell (`source ~/.zshrc`).

### `Unable to find a device matching iPhone 15 Pro Max`
The simulator device doesn't exist yet. Run:
```bash
xcrun simctl create "iPhone 15 Pro Max" "iPhone 15 Pro Max" "iOS18.5"
```

### `xcodegen: command not found`
```bash
brew install xcodegen
```

### iOS destination `OS=latest` picks up a beta runtime
The Makefile pins `OS=18.5` to avoid resolving to iOS 26.x betas.
Override on the command line if needed:
```bash
make ios-run IOS_DESTINATION="platform=iOS Simulator,name=iPhone 15 Pro Max,OS=18.5"
```
