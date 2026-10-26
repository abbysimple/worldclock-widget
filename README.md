# Flip World Clock

An Android home-screen widget: a flip-card clock for your main time zone, with rows for any
other world clocks you add underneath.

- Tap the widget (or open the app) to manage clocks. You can search any city or country,
  rename a row (e.g. `LAX`), reorder it, remove it, or make it the main clock.
- Every clock is a `TextClock` pinned to a time zone, so the launcher keeps it current.
  There's no background service, no polling and no battery cost, and daylight saving
  time is handled for you.
- The widget resizes vertically to fit more rows.

## Build

```powershell
.\build-apk.ps1            # -> build\WorldClockWidget.apk
.\build-apk.ps1 -Install   # also installs over adb
```

The script calls the SDK tools directly (aapt2, javac, d8, zipalign, apksigner). It needs
JDK 17+, `ANDROID_HOME` (defaults to `C:\Android\sdk`), build-tools 35.0.0 and
platform android-36. The Gradle files are there for building in Android Studio.
