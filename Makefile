.PHONY: dev dev-server dev-web codegen

codegen:
	@echo "=== Running codegen ==="
	@cd codegen && bash generate.sh
	@cd codegen && ./gradlew generateJsonSchema2Pojo
	@echo "=== Codegen complete ==="

dev:
	@echo "Opening Terminal sessions for server and web..."
	@$(MAKE) dev-server
	@$(MAKE) dev-web

dev-server:
	@osascript -e 'tell application "Terminal" to do script "cd \"$(PWD)/server\" && ./gradlew bootRun"' >/dev/null

dev-web:
	@osascript -e 'tell application "Terminal" to do script "cd \"$(PWD)/web\" && npm run dev"' >/dev/null

dev-android:
	@echo "=== Building & installing Android app ==="
	@cd android && ./gradlew installDebug
	@echo "=== Launching app on emulator ==="
	@adb shell am force-stop com.nba.sdui.app 2>/dev/null || true
	@adb shell am start -n com.nba.sdui.app/.MainActivity
	@echo "=== Tailing logs (Ctrl-C to stop) ==="
	@sleep 3 && adb logcat --pid=$$(adb shell pidof com.nba.sdui.app) -s SduiScreenVM:* SectionRouter:* ActionHandler:* FormRenderer:* SDUI:* AndroidRuntime:E *:F