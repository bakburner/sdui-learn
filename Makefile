.PHONY: dev dev-server dev-web dev-android dev-all codegen stop stop-server stop-web stop-android

# ── Android SDK paths ────────────────────────────────────────
ANDROID_SDK ?= $(or $(ANDROID_HOME),$(HOME)/Library/Android/sdk)
ADB          = $(ANDROID_SDK)/platform-tools/adb
EMU          = $(ANDROID_SDK)/emulator/emulator
AVD_NAME    ?= $(shell $(EMU) -list-avds 2>/dev/null | head -1)

# ── Codegen ──────────────────────────────────────────────────
codegen:
	@echo "=== Running codegen ==="
	@cd codegen && bash generate.sh
	@cd codegen && ./gradlew generateJsonSchema2Pojo
	@echo "=== Codegen complete ==="

# ── Dev helpers (open in separate Terminal tabs) ─────────────
dev:
	@echo "=== Starting full stack ==="
	@$(MAKE) dev-server
	@$(MAKE) dev-web
	@echo "Waiting 5s for server to start..."
	@sleep 5
	@$(MAKE) dev-android

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
			echo "ERROR: No AVDs found. Create one via Android Studio > Device Manager."; \
			exit 1; \
		fi; \
		echo "=== Launching emulator: $(AVD_NAME) ==="; \
		$(EMU) -avd "$(AVD_NAME)" -no-snapshot-load -memory 2048 &>/dev/null & \
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