# SDUI Prototype — Development Setup

Step-by-step local setup for this repo. Pick the path that matches what you are
building (web, Android, iOS, or all three).

**Hard requirement for server and Android:** Java 21 active in the shell where
you run `make` / Gradle.

**macOS installs:** use [Homebrew](https://brew.sh) for every tool and SDK
component in this guide (`brew install` / `brew install --cask`). Do not
download Android zips by hand.

---

## Step 0 — Choose what you are setting up

| Goal | Follow these steps |
|---|---|
| Server + web only | [Steps 1–4](#step-1--check-what-is-already-installed), then [5–7](#step-5--install-web-dependencies) |
| Android app (`make dev-android-local` / `make dev-android-remote`) | Steps 1–4, [8–10](#step-8--android-sdk-on-your-path-both-setup-paths), then 5–7 |
| iOS app (`make dev-ios-local` / `make dev-ios-remote`) | Steps 1–4, [11–12](#step-11--xcode-and-ios-tools), then 5–7 (Android steps 8–10 optional) |
| Full stack | All steps |

You can skip steps you have already done. Re-run the **verify** blocks after
each section to confirm you are ready for the next one.

---

## Step 1 — Check what is already installed

Run:

```bash
java -version
node -v
npm -v
command -v adb
command -v emulator
command -v sdkmanager
command -v avdmanager
command -v xcodebuild
command -v xcodegen
```

| Command | Needed for | Good result |
|---|---|---|
| `java -version` | Server, Android | Reports **21** |
| `node` / `npm` | Web | Both print a version |
| `adb` / `emulator` | Android | Paths under your Android SDK |
| `sdkmanager` / `avdmanager` | Android Path B | Under `$(brew --prefix)/share/android-commandlinetools/...` |
| `xcodebuild` | iOS | Prints Xcode version |
| `xcodegen` | `make dev-ios-local` / `make dev-ios-remote` | Prints a path |

**Verify:** note which rows are missing; the steps below install only what you
need.

---

## Step 2 — Install base tools (macOS)

If Step 1 showed gaps, install with Homebrew:

```bash
brew install openjdk@21 node xcodegen xcbeautify
```

**Android** (`make dev-android-local` / `make dev-android-remote`) — install both casks, then open Android Studio
once so it creates `~/Library/Android/sdk`:

```bash
brew install --cask android-studio android-commandlinetools
open -a "Android Studio"
```

| Step 9 path | What you use |
|---|---|
| **Path A** (default) | `android-studio` → Device Manager UI |
| **Path B** | `android-commandlinetools` → `sdkmanager` / `avdmanager` |

Path B still needs the Studio SDK directory; the command-line cask only adds
the CLI binaries (via Homebrew), not a second SDK root.

**Verify:**

```bash
java -version   # should show 21 after Step 3
node -v
npm -v
```

---

## Step 3 — Point your shell at Java 21

Skip if `java -version` already reports 21.

```bash
sudo ln -sfn /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk \
  /Library/Java/JavaVirtualMachines/openjdk-21.jdk
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 21)' >> ~/.zshrc
source ~/.zshrc
java -version
```

**Verify:** `java -version` shows 21.

---

## Step 4 — Clone the repo and open a shell in it

```bash
cd /path/to/sdui-prototype
```

All `make` targets below assume your current directory is the repo root.

---

## Step 5 — Install web dependencies

```bash
cd web && npm install && cd ..
```

**Verify:** `cd web && npm test -- --run` (optional) or at least `npm -v` works
inside `web/`.

---

## Step 6 — Add server secrets

Create `server/.env`:

```dotenv
STATS_API_KEY=your_key_here
ABLY_SUBSCRIBE_API_KEY=your_key_here
```

Spring Boot reads these at startup. Without them, the server may start but
live-data features will not work.

**Verify:** file exists at `server/.env`.

---

## Step 7 — First run (server + web)

In **two terminals** at the repo root:

```bash
# Terminal 1
make dev-server

# Terminal 2
make dev-web-local
```

| Service | URL |
|---|---|
| API | http://localhost:8080 |
| Web app | http://localhost:5173 |

**Verify:** web app loads in the browser and can reach the API.

You can stop here if you only need server + web.

---

## Step 8 — Android SDK on your PATH (both setup paths)

Skip if you are not using `make dev-android-local` or `make dev-android-remote`.

`make dev-android-local` / `make dev-android-remote` look for the SDK at `$ANDROID_HOME`, or by default at
`$HOME/Library/Android/sdk` on macOS.

**8.1 — Confirm the SDK directory exists**

```bash
ls "$HOME/Library/Android/sdk"
```

You should see `platform-tools`, `emulator`, and `platforms`. If the folder is
missing, install [Android Studio](#step-2--install-base-tools-macos) and open it
once so the SDK is created.

**8.2 — Add SDK tools to your shell**

Put this in `~/.zshrc` (uses Homebrew’s prefix so it works on Apple Silicon and
Intel):

```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
```

For **Path B**, prepend the Homebrew command-line tools (install the cask in
[Step 2](#step-2--install-base-tools-macos)):

```bash
export PATH="$(brew --prefix)/share/android-commandlinetools/cmdline-tools/latest/bin:$PATH"
```

Reload and check:

```bash
source ~/.zshrc
command -v adb
command -v emulator
```

**Verify:** `adb` and `emulator` point under `$ANDROID_HOME`. For Path B, also:

```bash
command -v sdkmanager
command -v avdmanager
```

Both should point under `$(brew --prefix)/share/android-commandlinetools/`.

---

## Step 9 — Android emulator: pick one path

`make dev-android-local` / `make dev-android-remote` need **one AVD** (a named virtual device). Use **either**
the Android Studio UI **or** the terminal — not both unless you want extra
AVDs.

| | Path A — Android Studio (default) | Path B — Command line |
|---|---|---|
| Best if | You prefer GUIs | You want scriptable setup |
| Prereq | `brew install --cask android-studio` | Same + `android-commandlinetools` |
| Tools | Device Manager + SDK Manager | Homebrew `sdkmanager`, `avdmanager` |
| API level | **API 35** (matches app `compileSdk`) | **API 35** |
| Apple Silicon image | **arm64** system image | `…;arm64-v8a` package |
| Intel Mac image | **x86_64** system image | `…;x86_64` package |

After either path, both converge on the same check:

```bash
emulator -list-avds
```

You need at least one name in that list before Step 10.

---

### Path A — Android Studio

Do this if you are **not** using `sdkmanager` / `avdmanager`.

**A.1 — Install SDK packages**

1. Open **Android Studio**.
2. **Android Studio** → **Settings** (macOS) → **Languages & Frameworks** → **Android SDK**.
3. **SDK Platforms** tab → check **Android 15.0 ("VanillaIceCream")** — **API Level 35**.
4. **SDK Tools** tab → check:
   - **Android SDK Platform-Tools**
   - **Android Emulator**
5. Click **Apply** and wait for downloads.

**A.2 — Create a virtual device**

1. **Android Studio** → **Device Manager** (phone icon on the welcome screen, or **View** → **Tool Windows** → **Device Manager** from a project).
2. **+** → **Create Virtual Device**.
3. Pick a phone (e.g. **Pixel 7**) → **Next**.
4. **System Image** → select **API 35** with **Google APIs**:
   - Apple Silicon: choose an **arm64** image (often labeled *arm64-v8a*).
   - Intel Mac: choose **x86_64**.
   - If API 35 is missing, click the download icon next to it, then **Next**.
5. Name the AVD (e.g. `Pixel_API_35`) → **Finish**.

**A.3 — Verify**

```bash
emulator -list-avds
```

Note the exact name (e.g. `Pixel_API_35`). You will pass it to Make as
`AVD_NAME` if it is not the first line listed.

---

### Path B — Command line (`sdkmanager` + `avdmanager`)

Do this if you are **not** using Device Manager in Android Studio.

**B.1 — Install command-line tools (Homebrew)**

```bash
brew install --cask android-commandlinetools
```

Add the [Step 8.2](#82--add-sdk-tools-to-your-shell) `PATH` line for Path B, then reload your shell.

**Verify:**

```bash
command -v sdkmanager
command -v avdmanager
sdkmanager --version
```

Both commands should live under
`$(brew --prefix)/share/android-commandlinetools/cmdline-tools/latest/bin`.
Packages still install into `$ANDROID_HOME` (`~/Library/Android/sdk`).

**B.2 — Accept licenses**

```bash
yes | sdkmanager --licenses
```

**B.3 — Download platform, emulator, and system image**

Use **arm64** on Apple Silicon; **x86_64** on Intel Mac. API **35** matches
this repo’s `compileSdk`.

**Apple Silicon:**

```bash
sdkmanager \
  "platform-tools" \
  "emulator" \
  "platforms;android-35" \
  "system-images;android-35;google_apis;arm64-v8a"
```

**Intel Mac:**

```bash
sdkmanager \
  "platform-tools" \
  "emulator" \
  "platforms;android-35" \
  "system-images;android-35;google_apis;x86_64"
```

**Verify:**

```bash
sdkmanager --list_installed | grep system-images
```

Expect a line containing `android-35` and `google_apis`.

**B.4 — Create the AVD**

**Apple Silicon:**

```bash
avdmanager create avd \
  -n Pixel_API_35 \
  -k "system-images;android-35;google_apis;arm64-v8a" \
  -d pixel_7 \
  --force
```

**Intel Mac:** same command with
`-k "system-images;android-35;google_apis;x86_64"`.

When prompted *Do you wish to create a custom hardware profile*, answer `no`
(or pass extra flags you prefer).

**B.5 — Verify**

```bash
emulator -list-avds
```

Optional smoke test:

```bash
emulator -avd Pixel_API_35 -no-snapshot-load &
adb wait-for-device
adb shell getprop sys.boot_completed   # prints 1 when boot finished
```

---

## Step 10 — Run the Android app

With the server running (`make dev-server` in another terminal):

```bash
make dev-android-local
```

`make dev-android-local` will:

1. Launch the first listed AVD if no device is connected (or use `AVD_NAME`)
2. Build and install the debug APK
3. Start the app and tail logs

Use a specific AVD:

```bash
make dev-android-local AVD_NAME=Pixel_API_35
```

**Verify:** emulator shows the SDUI app; logcat streams in the terminal.

### Lighter emulator (optional)

`make dev-android-local` already uses modest defaults (**1536 MB RAM**, **1 CPU core**,
**no audio**, **host GPU** on Apple Silicon). Override if you need more headroom:

```bash
make dev-android-local EMU_MEMORY=2048 EMU_CORES=2
```

For a smaller AVD (less disk and RAM at rest), create a phone profile one size
down and trim RAM in `~/.android/avd/<name>.avd/config.ini`:

```ini
hw.ramSize=1536
vm.heapSize=128
hw.lcd.width=720
hw.lcd.height=1280
```

Example CLI AVD (Path B) with a smaller device profile:

```bash
avdmanager create avd -n SDUI_Lite_API_35 \
  -k "system-images;android-35;google_apis;arm64-v8a" \
  -d pixel_4 --force
make dev-android-local AVD_NAME=SDUI_Lite_API_35
```

**Lightest option:** a physical device over USB — `make dev-android-local` skips
launching an emulator when `adb devices` already shows one.

---

## Step 11 — Xcode and iOS tools

Skip if you are not using `make dev-ios-local` or `make dev-ios-remote`.

1. Install **Xcode** from the App Store.
2. Select the full Xcode app as the active developer directory:

```bash
sudo xcode-select -s /Applications/Xcode.app/Contents/Developer
xcodebuild -version
```

3. Install helper tools (if missing):

```bash
brew install xcodegen xcbeautify
```

4. Install an iOS simulator runtime and ensure at least one simulator exists.
Use either the Xcode UI or the CLI.

| | Path A — Xcode UI (default) | Path B — Command line |
|---|---|---|
| Best if | You already use Xcode interactively | You want a shell-only setup |
| Runtime install | **Xcode** → **Settings** → **Components** | `xcodebuild -downloadPlatform iOS` |
| Simulator creation | **Xcode** → **Window** → **Devices and Simulators** | `xcrun simctl create ...` |
| Extra first-run step | None | `sudo xcodebuild -runFirstLaunch` |

After either path, both converge on the same check:

```bash
xcrun simctl list runtimes
xcrun simctl list devices available
```

You need at least one `iOS ...` runtime before `make dev-ios-local` / `make dev-ios-remote` can build for a simulator.

### Path A — Xcode UI

1. Open **Xcode** → **Settings** → **Components**.
2. Download any available **iOS xx.x Simulator** runtime.
3. If no simulator exists yet, open **Xcode** → **Window** → **Devices and Simulators** → **Simulators** and create one.

### Path B — Command line (`xcodebuild` + `simctl`)

Run first-launch setup once if this Xcode install has not completed it yet:

```bash
sudo xcodebuild -runFirstLaunch
```

Download the iOS simulator runtime:

```bash
xcodebuild -downloadPlatform iOS
```

If `xcrun simctl list devices available` is still empty after the runtime installs,
create a simulator from the shell:

```bash
xcrun simctl create "iPhone SE (3rd generation)" \
  com.apple.CoreSimulator.SimDeviceType.iPhone-SE-3rd-generation \
  "$(xcrun simctl list runtimes | awk '/^iOS / { print $NF; exit }')"
```

**Verify — runtime + simulator present:**

```bash
xcrun simctl list runtimes
xcrun simctl list devices available
```

If the runtimes list is empty, `make dev-ios-local` / `make dev-ios-remote` cannot boot or build for a simulator yet.

---

## Step 12 — Run the iOS demo

Server must be running first:

```bash
# Terminal 1
make dev-server

# Terminal 2
make dev-ios-local
```

Default simulator is **iPhone SE (3rd generation)**. Override:

```bash
make dev-ios-local IOS_SIM_NAME="iPhone 15 Pro Max"
```

**Verify:** simulator opens with the demo app.

---

## Makefile reference

| Task | Command |
|---|---|
| Server + web | `make dev` |
| Server only | `make dev-server` |
| Web against local server | `make dev-web-local` |
| Web against remote server | `make dev-web-remote` |
| Android against local server | `make dev-android-local` |
| Android against remote server | `make dev-android-remote` |
| iOS demo against local server | `make dev-ios-local` |
| iOS demo against remote server | `make dev-ios-remote` |
| All tests | `make test` |
| Regenerate models after schema change | `make codegen` |
| Stop server + web + Android | `make stop` |

### Android overrides

| Variable | Default | Purpose |
|---|---|---|
| `ANDROID_SDK` | `$ANDROID_HOME` or `~/Library/Android/sdk` | SDK root |
| `AVD_NAME` | First name from `emulator -list-avds` | Emulator to boot |
| `EMU_MEMORY` | `1536` | Emulator RAM (MB) |
| `EMU_CORES` | `1` | Host CPU cores for the VM |
| `EMU_GPU` | `host` (arm64 Mac) / `swiftshader_indirect` (Intel) | Graphics backend |
| `SDUI_ANDROID_LOCAL_SERVER` | `http://10.0.2.2:8080` | Android local-server endpoint (emulator loopback) |
| `SDUI_ANDROID_REMOTE_SERVER` | deployed SDUI server | Android remote endpoint |

### iOS overrides

| Variable | Default | Purpose |
|---|---|---|
| `IOS_SIM_NAME` | `iPhone SE (3rd generation)` | Simulator to boot |
| `IOS_DESTINATION` | `platform=iOS Simulator,name=$(IOS_SIM_NAME),OS=latest` | `xcodebuild` destination |
| `SDUI_DISABLE_ABLY` | `1` | Keeps Ably off on simulator by default |
| `SDUI_IOS_LOCAL_SERVER` | `http://localhost:8080` | iOS local-server endpoint |
| `SDUI_IOS_REMOTE_SERVER` | deployed SDUI server | iOS remote endpoint |

---

## Troubleshooting

### `java -version` is not 21

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
java -version
```

Re-do [Step 3](#step-3--point-your-shell-at-java-21) if needed.

### `adb` or `emulator` not found

SDK is installed but not on `PATH`. Re-do [Step 8b](#8b--put-sdk-tools-on-your-path).

```bash
command -v adb
command -v emulator
```

### `sdkmanager` or `avdmanager` not found

Install the Homebrew cask and add Path B to your `PATH`:

```bash
brew install --cask android-commandlinetools
export PATH="$(brew --prefix)/share/android-commandlinetools/cmdline-tools/latest/bin:$PATH"
command -v sdkmanager
```

See [Step 8.2](#82--add-sdk-tools-to-your-shell) and [Path B, B.1](#path-b--command-line-sdkmanager--avdmanager).

### `make dev-android-local` — No AVDs found

No emulator defined yet. Complete [Step 9](#step-9--android-emulator-pick-one-path) (Path A or Path B), then:

```bash
emulator -list-avds
make dev-android-local AVD_NAME=Pixel_API_35
```

### `sdkmanager` fails with license error

```bash
yes | sdkmanager --licenses
```

### Emulator is slow or will not start on Apple Silicon

- Use an **arm64-v8a** system image, not x86_64.
- Ensure `-d pixel_7` (or another profile from `avdmanager list device`) was used when creating the AVD.

### `make dev-ios-local` — simulator not found

```bash
xcrun simctl list runtimes
xcrun simctl list devices available
make dev-ios-local IOS_SIM_NAME="iPhone 15 Pro"
```

If `xcrun simctl list runtimes` prints no `iOS ...` entries, install an iOS Simulator runtime first.
Use either **Xcode** → **Settings** → **Components** or:

```bash
sudo xcodebuild -runFirstLaunch
xcodebuild -downloadPlatform iOS
```

If the runtime exists but `xcrun simctl list devices available` is still empty, create a simulator in **Xcode** → **Window** → **Devices and Simulators** or with `xcrun simctl create` from [Step 11](#step-11--xcode-and-ios-tools).

### iOS app cannot reach the server

Start the server before `make dev-ios-local`:

```bash
make dev-server
make dev-ios-local
```

### Missing command quick map

| Missing | Fix |
|---|---|
| `java` / wrong version | [Step 2–3](#step-2--install-base-tools-macos) |
| `node` / `npm` | `brew install node` |
| `adb` / `emulator` | [Step 8](#step-8--android-sdk-on-your-path-both-setup-paths) |
| `sdkmanager` / `avdmanager` | `brew install --cask android-commandlinetools` + [Step 8.2](#82--add-sdk-tools-to-your-shell) |
| `xcodebuild` | Install Xcode + `xcode-select` ([Step 11](#step-11--xcode-and-ios-tools)) |
| `xcodegen` | `brew install xcodegen` |
