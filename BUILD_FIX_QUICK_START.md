# 🚀 GRADLE BUILD FIX - Quick Start (5 Minutes)

## Your Problem
```
✗ Gradle wrapper missing
✗ gradle-wrapper.jar not found  
✗ gradle-wrapper.properties not found
✗ gradle.properties has deprecated options (MaxPermSize)
✗ Build fails: "Unable to start Gradle daemon"
```

## Your Solution (3 Steps)

### STEP 1: Run Setup Script (2 minutes)

**Windows PowerShell:**
```powershell
cd "C:\Game Projects\Magic-grid"
.\setup-gradle-wrapper.ps1
```

**Windows Command Prompt:**
```cmd
cd C:\Game Projects\Magic-grid
setup-gradle-wrapper.bat
```

**What happens:**
- Downloads Gradle 8.4 (~100 MB)
- Extracts gradle-wrapper.jar
- Verifies all files
- Cleans up temporary files

✓ **Output**: Should show "SUCCESS! Gradle wrapper is ready."

---

### STEP 2: Open in Android Studio (1 minute)

1. Launch Android Studio
2. File → Open → Select `C:\Game Projects\Magic-grid`
3. Wait for Gradle sync (may ask to download ~500 MB of dependencies)
4. ✓ No red errors in "Messages" window

---

### STEP 3: Build Project (2 minutes)

**Option A: From Android Studio**
- Build → Clean Project
- Build → Rebuild Project
- ✓ You should see "BUILD SUCCESSFUL"

**Option B: From Command Line**
```bash
cd "C:\Game Projects\Magic-grid"
.\gradlew clean build
# ✓ Should return: BUILD SUCCESSFUL in X.XXs
```

---

## What Was Fixed

### ✓ Created Files
```
gradle/wrapper/gradle-wrapper.properties    ← Gradle 8.4 config
gradlew                                     ← Unix/Mac script
gradlew.bat                                 ← Windows script
setup-gradle-wrapper.ps1                    ← Auto-setup (PowerShell)
setup-gradle-wrapper.bat                    ← Auto-setup (batch)
```

### ✓ Fixed Files
```
gradle.properties                           ← Removed deprecated MaxPermSize
```

### ⏳ Downloaded by Script
```
gradle/wrapper/gradle-wrapper.jar           ← The binary (auto-downloaded)
```

---

## If Script Fails

### "Cannot be loaded" error
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
.\setup-gradle-wrapper.ps1
```

### "Internet not working" error
Manually download from: https://gradle.org/releases/
- Download **Gradle 8.4** (binary-only zip)
- Extract anywhere
- Copy `gradle-8.4/lib/gradle-wrapper.jar` to `gradle/wrapper/gradle-wrapper.jar`

### "Java not found" error
```bash
java -version
```
If not installed, download Java 11+ from: https://www.oracle.com/java/technologies/downloads/

---

## Verify It Works

### Quick Check
```bash
DIR gradle\wrapper\gradle-wrapper.jar
REM If file exists, you're good!
```

### Full Build Test
```bash
.\gradlew --version
.\gradlew clean build
```

Should output:
```
Gradle 8.4
...
BUILD SUCCESSFUL in X.XXs
```

---

## Project Structure Now

```
Magic-grid/
├── gradle/wrapper/
│   ├── gradle-wrapper.jar           ✓ BINARY FILE
│   └── gradle-wrapper.properties    ✓ CONFIG FILE
├── gradlew                          ✓ SCRIPT (Unix/Mac)
├── gradlew.bat                      ✓ SCRIPT (Windows)
├── gradle.properties                ✓ FIXED
├── build.gradle                     ✓ EXISTING
├── settings.gradle                  ✓ EXISTING
├── app/
│   ├── build.gradle                 ✓ EXISTING
│   └── src/                         ✓ EXISTING (Kotlin code)
└── ... (other project files)
```

---

## KEY INFO

| Info | Value |
|------|-------|
| **Gradle Version** | 8.4 (latest stable) |
| **Android Gradle Plugin** | 7.4.2 |
| **Kotlin** | 1.9.0 |
| **Min Java** | Java 11 |
| **Android API** | 34 (Android 14) |

---

## Common Issues & Fixes

| Issue | Fix |
|-------|-----|
| Script won't run | Set execution policy (see above) |
| Download fails | Check internet, try manual download |
| Java not found | Install Java 11+ |
| Build still fails | Delete `app/build` folder, try again |
| "MaxPermSize" error | Check gradle.properties (should be fixed) |

---

## File Reference

### gradle-wrapper.properties
```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.4-bin.zip
```
This locks the Gradle version to 8.4.

### gradle.properties (Fixed)
```properties
org.gradle.jvmargs=-Xmx2048m -XX:+HeapDumpOnOutOfMemoryError
# OLD: -XX:MaxPermSize=512m  ← REMOVED (deprecated in Java 8+)
```

### gradlew / gradlew.bat
These are wrapper scripts that:
1. Check if Gradle 8.4 is downloaded
2. Download if missing
3. Run build commands

---

## Next Steps After Build Success

1. ✓ Gradle wrapper is working
2. → Open project in Android Studio
3. → Run app on emulator/device
4. → Play the Magic Grid game!

---

## Documentation

For more details:
- **GRADLE_BUILD_FIX.md** - Complete build system guide
- **GRADLE_WRAPPER_SETUP.md** - Manual JAR installation options
- **README.md** - Project overview
- **DEVELOPMENT.md** - Full development guide

---

## TL;DR

1. Run: `.\setup-gradle-wrapper.ps1` (or `.bat`)
2. Wait for download
3. Open in Android Studio
4. Build → Rebuild Project
5. ✓ Done!

---

*Magic Grid - Gradle Build Fix Guide*
*April 4, 2026*
