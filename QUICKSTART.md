# Magic Grid - Quick Start Guide

Get up and running with Magic Grid in 5 minutes!

## 1️⃣ Prerequisites (1 minute)

✅ **Android Studio** (2022.1+)
- Download: https://developer.android.com/studio
- Install following the wizard

✅ **Android SDK 34**
- Automatically installed with Android Studio
- Or manually via Tools → SDK Manager

✅ **Project Files**
- You have the complete Magic-grid project

## 2️⃣ Import & Setup (2 minutes)

### Option A: Open Existing Project
1. Launch Android Studio
2. File → Open → Select the `Magic-grid` folder
3. Click Open
4. Wait for Gradle sync (⏱️ 2-5 minutes on first run)

### Option B: Clone from Git
```bash
git clone <repo-url>
cd Magic-grid
```
Then open in Android Studio.

## 3️⃣ Create Android Virtual Device (AVD) (optional, 2 minutes)

If you don't have a physical device:

1. Tools → Device Manager
2. Create Virtual Device
3. Select **Pixel 5a** (or any modern phone)
4. Choose **Android 14** (API 34)
5. Name: `Magic_Grid_AVD`
6. Allocate 4GB RAM
7. Click Finish

## 4️⃣ Build & Run (1 minute)

### Via Android Studio
1. Connect your Android device OR start the AVD
2. Build → Clean Project
3. Run → Run 'app'
4. Select your device
5. Click OK
6. ⏱️ App deploys (30-60 seconds)

### Via Command Line
```bash
# Clean and build
./gradlew clean build

# Install to device
./gradlew installDebug

# Run on device
adb shell am start -n com.magicgrid/.game.MenuActivity
```

## 5️⃣ Play!

You should see the **Magic Grid** home screen:
- Tap **Play** → Start from last level
- Tap **Levels** → Choose specific level
- Tap **Settings** → Coming soon
- Tap **Exit** → Close game

### How to Play
1. **Tap and drag** to connect matching colors
2. **Fill ALL cells** with no overlapping paths
3. **Hit Reset** if you make a mistake
4. **Use Undo** to go back one step
5. **Get Hint** when stuck

## 📱 Test Levels Available

- **Level 1-5**: Easy (5×5 grid, 2-4 colors)
- **Level 6-8**: Medium (6×6 grid, 3-5 colors)  
- **Level 9-10**: Hard (7×7 grid, 5+ colors)

## ⚠️ Common Issues & Fixes

### "Gradle sync failed"
```
→ File → Invalidate Caches → Invalidate and Restart
→ Or delete .gradle folder and retry
```

### "App crashes on startup"
```
→ Check Logcat (View → Tool Windows → Logcat)
→ Look for red error messages
→ Most common: SDK not installed (use SDK Manager)
```

### "Emulator is very slow"
```
→ Close other apps
→ Allocate more RAM to AVD
→ Enable GPU acceleration in AVD settings
→ Use a lower API level (Android 12 instead of 14)
```

### "Touch input not working"
```
→ Restart the app
→ Recreate the AVD
→ Test on physical device if available
```

## 🎯 What's Next?

### Learn the Codebase
1. Read [README.md](README.md) for overview
2. Check [DEVELOPMENT.md](DEVELOPMENT.md) for architecture
3. Review [ALGORITHM.md](ALGORITHM.md) for game logic
4. Explore the code: `app/src/main/kotlin/com/magicgrid/`

### Try Extending It
1. **Add a level**: Edit `LevelRepository.kt`
2. **Change colors**: Update `colors.xml`
3. **Add sound**: Extend GameActivity
4. **Custom theme**: Modify gradients in `drawable/`

### Build & Share
```bash
# Create release APK for sharing
./gradlew assembleRelease

# APK location: app/build/outputs/apk/release/app-release.apk
```

## 📚 Key Files Reference

| File | Purpose |
|------|---------|
| `GameActivity.kt` | Main gameplay screen |
| `GameBoard.kt` | Game logic & validation |
| `GameView.kt` | Graphics & touch input |
| `LevelRepository.kt` | Level definitions |
| `activity_game.xml` | Game screen layout |

## 🎓 Project Structure

```
Magic-grid/
├── app/src/main/kotlin/com/magicgrid/
│   ├── game/          ← Activities (UI)
│   ├── ui/            ← GameView (drawing)
│   ├── controller/    ← GameManager (control)
│   ├── logic/         ← GameBoard (rules)
│   └── data/          ← Levels & storage
├── app/src/main/res/
│   ├── layout/        ← XML layouts
│   ├── drawable/      ← Images & shapes
│   └── values/        ← Colors & strings
└── README.md          ← Full documentation
```

## 💡 Pro Tips

- **Try all 10 levels** to understand difficulty progression
- **Check the code** - it's well-documented with comments
- **Use Android Studio's debugger** (Run → Debug) to understand code flow
- **Modify colors** to see instant visual changes
- **Add new levels** to learn the system

## 🚀 You're Ready!

The game is production-quality and fully playable. Enjoy!

Have questions? Check:
- [README.md](README.md) - Full feature list
- [DEVELOPMENT.md](DEVELOPMENT.md) - Deep technical guide
- [ALGORITHM.md](ALGORITHM.md) - Game logic explained

**Happy Gaming! 🎮**

---

*Magic Grid: Where colors connect and grids fill.*
