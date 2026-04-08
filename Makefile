export ANDROID_HOME := $(HOME)/android-sdk

PKG := com.example.fit

.PHONY: build test install install-phone check-phone clean emulator run release distribute

build:
	./gradlew assembleDebug
	rm -f $(HOME)/Downloads/Fit.apk
	cp app/build/outputs/apk/debug/app-debug.apk $(HOME)/Downloads/Fit.apk
	@echo "APK copied to ~/Downloads/Fit.apk"

test:
	./gradlew testDebugUnitTest

install:
	adb install app/build/outputs/apk/debug/app-debug.apk

check-phone:
	@tmpf=$$(mktemp); \
	adb devices -l 2>/dev/null | tail -n +2 | grep -v '^$$' | grep 'model:' | \
		sed 's/.*model://;s/ .*//' | sort -u > "$$tmpf"; \
	while read -r mdl; do \
		line=$$(adb devices -l </dev/null 2>/dev/null | grep "model:$$mdl " | head -1); \
		serial=$$(echo "$$line" | sed 's/  *device .*//'); \
		model=$$(adb -s "$$serial" shell getprop ro.product.model </dev/null 2>/dev/null | tr -d '\r'); \
		brand=$$(adb -s "$$serial" shell getprop ro.product.brand </dev/null 2>/dev/null | tr -d '\r'); \
		version=$$(adb -s "$$serial" shell dumpsys package $(PKG) </dev/null 2>/dev/null | grep versionName | head -1 | awk -F= '{print $$2}' | tr -d '\r'); \
		echo "$$brand $$model ($$serial) — installed: v$$version"; \
	done < "$$tmpf"; \
	rm -f "$$tmpf"

install-phone: build
	@tmpf=$$(mktemp); \
	adb devices -l 2>/dev/null | tail -n +2 | grep -v '^$$' | grep 'model:' | \
		sed 's/.*model://;s/ .*//' | sort -u > "$$tmpf"; \
	while read -r mdl; do \
		line=$$(adb devices -l </dev/null 2>/dev/null | grep "model:$$mdl " | head -1); \
		serial=$$(echo "$$line" | sed 's/  *device .*//'); \
		model=$$(adb -s "$$serial" shell getprop ro.product.model </dev/null 2>/dev/null | tr -d '\r'); \
		echo "Installing on $$model ($$serial)..."; \
		adb -s "$$serial" install -r app/build/outputs/apk/debug/app-debug.apk </dev/null && \
			echo "  done" || \
			echo "  FAILED"; \
	done < "$$tmpf"; \
	rm -f "$$tmpf"

clean:
	./gradlew clean

emulator:
	$(ANDROID_HOME)/emulator/emulator -avd Pixel_API_34 &

run: build
	adb wait-for-device
	adb install app/build/outputs/apk/debug/app-debug.apk
	adb shell am start -n $(PKG)/.LoginActivity

APP_ID := 1:490456704105:android:31512a2d6ade89fa4f6ef8
RELEASE_APK := app/build/outputs/apk/release/app-release.apk

release:
	./gradlew assembleRelease
	cp $(RELEASE_APK) $(HOME)/Downloads/Fit.apk
	@echo "Release APK at ~/Downloads/Fit.apk"

distribute: release
	firebase appdistribution:distribute $(RELEASE_APK) \
		--app $(APP_ID) \
		--groups "fit-app-testers" \
		--release-notes "$(NOTES)"
