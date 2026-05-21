.PHONY: dev dev-server dev-web dev-android dev-all codegen server-test android-test web-test test \
	lint-sdui-warn \
	stop stop-server stop-web stop-android \
	ios-test ios-test-clean ios-build ios-demo-project ios-run ios-run-max ios-stop ios-fixtures-sync ios-sim-preflight

# Warn if Java composers reintroduce put("trigger", "onTap") — should use onActivate.
lint-sdui-warn:
	@./scripts/warn-onTap-in-composers.sh

# ── iOS build / test config ──────────────────────────────────
IOS_SCHEME        ?= SduiCore
IOS_DEMO_SCHEME   ?= SduiDemo
IOS_DEMO_BUNDLE   ?= com.nba.sdui.demo
IOS_SIM_NAME      ?= iPhone SE (3rd generation)
# Use iPhone SE (3rd gen) for lighter resource usage on constrained hardware
IOS_DESTINATION   ?= platform=iOS Simulator,name=$(IOS_SIM_NAME),OS=latest
# Set SDUI_DISABLE_ABLY=0 on the command line to re-enable ably-cocoa
# (only works on arm64 simulators; x86_64 simulators hit the ably module
# map bug and must keep the default of 1).
SDUI_DISABLE_ABLY ?= 1
XCBEAUTIFY        := $(shell command -v xcbeautify 2>/dev/null)
ifeq ($(XCBEAUTIFY),)
    IOS_PIPE := cat
else
    IOS_PIPE := xcbeautify
endif

# ── Android SDK paths ────────────────────────────────────────
ANDROID_SDK ?= $(or $(ANDROID_HOME),$(HOME)/Library/Android/sdk)
ADB          = $(ANDROID_SDK)/platform-tools/adb
EMU          = $(ANDROID_SDK)/emulator/emulator
AVD_NAME    ?= $(shell $(EMU) -list-avds 2>/dev/null | head -1)
# Lightweight defaults for local SDUI dev (override: make dev-android EMU_MEMORY=2048)
UNAME_M     := $(shell uname -m)
ifeq ($(UNAME_M),arm64)
EMU_GPU     ?= host
else
EMU_GPU     ?= swiftshader_indirect
endif
EMU_MEMORY  ?= 1536
EMU_CORES   ?= 1
# Omit -no-snapshot-load so Quick Boot reuses state after the first cold boot.
EMU_FLAGS   ?= -memory $(EMU_MEMORY) -cores $(EMU_CORES) -gpu $(EMU_GPU) -no-boot-anim -no-audio


# ── Codegen ──────────────────────────────────────────────────
codegen:
	@echo "=== Running codegen ==="
	@cd codegen && bash generate.sh
	@cd codegen && ./gradlew generateJsonSchema2Pojo
	@echo "=== Codegen complete ==="

server-test:
	@echo "=== Running server tests ==="
	@cd server && ./gradlew test

android-test:
	@echo "=== Running Android sdui-core JVM tests ==="
	@cd android && ./gradlew :sdui-core:testDebugUnitTest

web-test:
	@echo "=== Running web vitest suite ==="
	@cd web && npm test -- --run

test: server-test android-test web-test ios-test
	@echo "=== All platform tests passed ==="

# ── iOS fixtures sync ───────────────────────────────────────
ios-fixtures-sync:
	@echo "=== Syncing iOS fixtures from schema/examples ==="
	@mkdir -p ios/Tests/SduiCoreTests/Fixtures
	@rsync -a --include '*/' --include '*.json' --exclude '*' schema/examples/ ios/Tests/SduiCoreTests/Fixtures/
	@echo "=== iOS fixtures synced ==="

# ── Dev helpers (open in separate Terminal tabs) ─────────────
dev:
	@echo "=== Starting full stack ==="
	@$(MAKE) dev-server
	@$(MAKE) dev-web
	@echo "Waiting 5s for server to start..."


dev-server:
	@osascript -e 'tell application "Terminal" to do script "cd \"$(PWD)/server\" && ./gradlew bootRun"' >/dev/null

dev-web:
	@osascript -e 'tell application "Terminal" to do script "cd \"$(PWD)/web\" && npm run dev"' >/dev/null

# ── Android (auto-launches emulator if none connected) ───────
dev-android:
	@if $(ADB) devices 2>/dev/null | tail -n +2 | grep -qw 'device'; then \
		echo "=== Device/emulator already connected ==="; \
	else \
		if [ -z "$(AVD_NAME)" ]; then \
			echo "ERROR: No AVDs found. See docs/sdui-setup.md Step 9 (Android Studio UI or: brew install --cask android-commandlinetools)."; \
			exit 1; \
		fi; \
		echo "=== Launching emulator: $(AVD_NAME) ($(EMU_MEMORY)MB, $(EMU_CORES) core(s), gpu=$(EMU_GPU)) ==="; \
		$(EMU) -avd "$(AVD_NAME)" $(EMU_FLAGS) & \
		sleep 3; \
		$(ADB) start-server >/dev/null 2>&1; \
		echo "Waiting for emulator to boot..."; \
		$(ADB) wait-for-device; \
		while [ "$$($(ADB) shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" != "1" ]; do sleep 1; done; \
		echo "=== Emulator ready ==="; \
	fi
	@echo "=== Building & installing Android app ==="
	@cd android && ./gradlew installDebug
	@echo "=== Launching app ==="
	@$(ADB) shell am force-stop com.nba.sdui.app 2>/dev/null || true
	@$(ADB) shell am start -n com.nba.sdui.app/.MainActivity
	@echo "=== Tailing logs (Ctrl-C to stop) ==="
	@sleep 3 && $(ADB) logcat --pid=$$($(ADB) shell pidof com.nba.sdui.app) -s SduiScreenViewModel:* SectionRouter:* ActionHandler:* FormRenderer:* AblyChannelManager:* DataBindingResolver:* SduiRepository:* SDUI:* AndroidRuntime:E *:F

# ── Stop targets ─────────────────────────────────────────────
stop: stop-server stop-web stop-android
	@echo "=== All services stopped ==="

stop-server:
	@echo "=== Stopping server ==="
	@pkill -f 'gradlew bootRun' 2>/dev/null || true
	@pkill -f 'org.springframework.boot' 2>/dev/null || true
	@lsof -ti:8080 | xargs kill 2>/dev/null || true
	@echo "Server stopped"

stop-web:
	@echo "=== Stopping web ==="
	@pkill -f 'vite' 2>/dev/null || true
	@lsof -ti:5173 | xargs kill 2>/dev/null || true
	@echo "Web stopped"

stop-android:
	@echo "=== Stopping Android ==="
	@$(ADB) shell am force-stop com.nba.sdui.app 2>/dev/null || true
	@$(ADB) emu kill 2>/dev/null || true
	@echo "Android stopped"

# ── iOS ──────────────────────────────────────────────────────
# `make ios-test` runs the SduiCore XCTest suite on the iOS simulator
# and pipes output through xcbeautify (if installed) so errors appear as
# `file:line: error:` instead of Xcode's default noise. `brew install
# xcbeautify` to enable — without it we fall back to raw xcodebuild.
ios-sim-preflight:
	@if ! xcrun simctl list runtimes | grep -Eq '^iOS '; then \
		echo "ERROR: No iOS Simulator runtime is installed for Xcode."; \
		echo "       Install one from Xcode > Settings > Components, then re-run make ios-run."; \
		exit 1; \
	fi
	@if ! xcrun simctl list devices available | grep -Fq "$(IOS_SIM_NAME) ("; then \
		echo "ERROR: Simulator '$(IOS_SIM_NAME)' is not available."; \
		echo "       Available simulators:"; \
		xcrun simctl list devices available | sed 's/^/         /'; \
		echo "       Override with: make ios-run IOS_SIM_NAME=\"<available simulator>\""; \
		exit 1; \
	fi

ios-test: ios-fixtures-sync ios-sim-preflight
	@if [ -z "$(XCBEAUTIFY)" ]; then \
		echo "(tip: brew install xcbeautify for human-readable output)"; \
	fi
	@cd ios && set -o pipefail && SDUI_DISABLE_ABLY=$(SDUI_DISABLE_ABLY) xcodebuild test \
		-scheme "$(IOS_SCHEME)" \
		-destination "$(IOS_DESTINATION)" \
		-skipMacroValidation | $(IOS_PIPE)

# Clean slate: nuke DerivedData before running. Use when a
# cached swiftmodule is causing stale diagnostics.
ios-test-clean:
	@echo "=== Wiping iOS DerivedData ==="
	@rm -rf ~/Library/Developer/Xcode/DerivedData/ios-*
	@$(MAKE) ios-test

# Build only (no tests) — faster signal while iterating on library code.
ios-build: ios-fixtures-sync ios-sim-preflight
	@cd ios && set -o pipefail && SDUI_DISABLE_ABLY=$(SDUI_DISABLE_ABLY) xcodebuild build \
		-scheme "$(IOS_SCHEME)" \
		-destination "$(IOS_DESTINATION)" \
		-skipMacroValidation | $(IOS_PIPE)

# `make ios-run` builds and launches the SduiDemo host app in the iOS
# simulator. Mirrors `make dev-android`. Ably stays disabled on the x86_64
# simulator (Intel hosts) — set SDUI_DISABLE_ABLY=0 on arm64 to enable it.
#
# Pre-req: `make dev-server` so the app has something to hit at localhost:8080.
ios-demo-project:
	@echo "=== Regenerating SduiDemo.xcodeproj ==="
	@cd ios/SduiDemo && xcodegen generate --quiet

ios-run: ios-sim-preflight ios-demo-project
	@if ! curl -sf http://localhost:8080/v1/sdui/demos >/dev/null 2>&1; then \
		echo "WARNING: server not reachable at http://localhost:8080"; \
		echo "         run 'make dev-server' in another terminal first"; \
	fi
	@echo "=== Cleaning package caches ==="
	@# Wipe every SwiftPM-managed Kingfisher registration in the tree so
	@# xcodebuild resolves the package graph from a clean state. Xcode
	@# throws "Cannot open X as Swift Package Proxy because it is already
	@# open as Swift User Managed Package Folder" — which surfaces as
	@# "Missing package product 'Kingfisher'" at link time — whenever two
	@# resolutions coexist within a single xcodebuild run:
	@#   - ios/.build/          : left behind by a raw `swift build` at
	@#                            the library root (SwiftPM CLI cache).
	@#   - ios/.swiftpm/        : left behind by Xcode.app opening
	@#                            ios/Package.swift as a package project
	@#                            (user-managed package view).
	@#   - ios/Package.resolved : written by `make ios-build` /
	@#                            `make ios-test` when xcodebuild runs
	@#                            against ios/Package.swift directly.
	@#                            Its presence next to Package.swift makes
	@#                            Xcode's IDE layer treat the local
	@#                            SduiCore package as a "user-managed"
	@#                            view of its transitive deps, while the
	@#                            demo's xcworkspace Package.resolved
	@#                            tries to resolve the same Kingfisher
	@#                            as a "proxy". Having both is the
	@#                            specific trigger for the error above.
	@#   - ios/SduiDemo/build/  : xcodebuild's per-project derived-data.
	@#                            Full wipe (not just SourcePackages)
	@#                            because stale entries in Build/ and
	@#                            Index.noindex/ can retain package
	@#                            references that survive a checkout
	@#                            refresh.
	@# Everything here is rebuild-safe — SwiftPM and Xcode regenerate
	@# whatever they need on the next xcodebuild invocation. If the
	@# error still recurs after this, `~/Library/Developer/Xcode/
	@# DerivedData/SduiDemo-*` is the remaining place Xcode caches a
	@# user-managed Kingfisher registration; wipe it manually and close
	@# Xcode.app.
	@rm -rf ios/.build ios/.swiftpm ios/Package.resolved ios/SduiDemo/build
	@echo "=== Resolving package graph ==="
	@# Resolution runs in its own xcodebuild process to sidestep an
	@# Xcode 15 IDE-container ordering bug. When resolution and build
	@# share a single xcodebuild invocation, the IDE layer walks into
	@# the freshly-checked-out Kingfisher directory
	@# (build/SourcePackages/checkouts/Kingfisher) and opens it as a
	@# "Swift User Managed Package Folder" (because it's a local
	@# filesystem path containing Package.swift). The proxy layer then
	@# fails to open the same path as a "Swift Package Proxy" (its
	@# view of Kingfisher as a remote-URL dependency) with:
	@#   Cannot open "Kingfisher" as a "Swift Package Proxy" because
	@#   it is already open as a "Swift User Managed Package Folder"
	@# which surfaces at link time as
	@#   Missing package product 'Kingfisher' (in target 'SduiDemo')
	@# The first invocation below populates
	@# build/SourcePackages/{checkouts,workspace-state.json} and exits
	@# before the proxy/user-managed collision window opens. The
	@# second invocation (`xcodebuild build`) then reuses those
	@# pre-resolved artifacts and skips the re-resolution pass via
	@# `-disableAutomaticPackageResolution`, so the IDE container
	@# graph only registers Kingfisher once — as a proxy via the
	@# already-resolved workspace state.
	@# TODO: drop the split once we're on Xcode 16+, which resolves
	@# the ordering bug natively.
	@cd ios/SduiDemo && set -o pipefail && SDUI_DISABLE_ABLY=$(SDUI_DISABLE_ABLY) xcodebuild -resolvePackageDependencies \
		-project SduiDemo.xcodeproj \
		-scheme "$(IOS_DEMO_SCHEME)" \
		-clonedSourcePackagesDirPath build/SourcePackages | $(IOS_PIPE)
	@echo "=== Building $(IOS_DEMO_SCHEME) ==="
	@cd ios/SduiDemo && set -o pipefail && SDUI_DISABLE_ABLY=$(SDUI_DISABLE_ABLY) xcodebuild build \
		-project SduiDemo.xcodeproj \
		-scheme "$(IOS_DEMO_SCHEME)" \
		-destination "$(IOS_DESTINATION)" \
		-derivedDataPath build \
		-clonedSourcePackagesDirPath build/SourcePackages \
		-disableAutomaticPackageResolution \
		-skipMacroValidation | $(IOS_PIPE)
	@echo "=== Booting simulator ==="
	@xcrun simctl boot "$(IOS_SIM_NAME)" 2>/dev/null || true
	@# Reduce graphics quality for better performance on constrained hardware
	@xcrun simctl ui "$(IOS_SIM_NAME)" appearance dark 2>/dev/null || true
	@open -a Simulator
	@echo "=== Installing app ==="
	@APP=$$(find ios/SduiDemo/build/Build/Products -type d -name "$(IOS_DEMO_SCHEME).app" | head -1); \
	 if [ -z "$$APP" ]; then echo "ERROR: SduiDemo.app not found after build"; exit 1; fi; \
	 xcrun simctl install booted "$$APP"
	@echo "=== Launching app ==="
	@xcrun simctl terminate booted $(IOS_DEMO_BUNDLE) 2>/dev/null || true
	@xcrun simctl launch booted $(IOS_DEMO_BUNDLE)
	@echo "=== Tailing logs (Ctrl-C to stop) ==="
	@xcrun simctl spawn booted log stream --level debug \
		--predicate 'subsystem == "com.nba.sdui"'

# Run on iPhone 15 Pro Max for testing larger screens / responsive layouts
ios-run-max:
	@$(MAKE) ios-run IOS_SIM_NAME="iPhone 15 Pro Max"

ios-stop:
	@echo "=== Stopping SduiDemo ==="
	@xcrun simctl terminate booted $(IOS_DEMO_BUNDLE) 2>/dev/null || true
	@echo "Stopped"