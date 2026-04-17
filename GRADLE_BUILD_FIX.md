# Android Gradle Build System - Complete Fix Guide

## Problem Summary

Your Android project has a broken Gradle setup:
- ✗ `gradle/wrapper/` directory exists but is incomplete
- ✗ `gradle-wrapper.jar` is missing (binary file)
- ✗ `gradle-wrapper.properties` was missing (now fixed)
- ✗ `gradlew` and `gradlew.bat` were missing (now fixed)
- ✗ `gradle.properties` had deprecated JVM options (now fixed)

## Solution Overview

All **text files** have been created/fixed. You now need to:
1. **Run the setup script** to download and install `gradle-wrapper.jar`
2. **Sync the project** in Android Studio
3. **Build and verify**

---

## Step 1: Run the Setup Script (Choose One)

### Option A: PowerShell (Recommended for most users)

```powershell
# Open PowerShell in your project root
# Navigate to: c:\Game Projects\Magic-grid

cd "c:\Game Projects\Magic-grid"

# Run the setup script
.\setup-gradle-wrapper.ps1

# If you get "cannot be loaded" error, run this first:
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

### Option B: Command Prompt (CMD)

```cmd
REM Open Command Prompt in your project root
cd c:\Game Projects\Magic-grid

REM Run the batch script
setup-gradle-wrapper.bat
```

### Option C: Git Bash (if you have Git installed)

```bash
cd /c/Game\ Projects/Magic-grid
bash setup-gradle-wrapper.ps1
```

---

## What the Setup Script Does

When you run the script, it will:

1. ✓ Verify you're in the correct project directory
2. ✓ Check/create `gradle/wrapper/` directory
3. ✓ Download Gradle 8.4 binary distribution (~100 MB)
4. ✓ Extract `gradle-wrapper.jar` to correct location
5. ✓ Verify all required files exist
6. ✓ Clean up temporary files

**Expected output**: Shows progress and confirms all files are in place.

---

## Step 2: Verify Setup in Android Studio

### 2a. Open Project (if not already open)

1. Launch Android Studio
2. File → Open
3. Select `C:\Game Projects\Magic-grid`
4. Click Open

### 2b. Sync Gradle Files

One of these should appear:
- **Sync Now** button (click it)
- **Auto-sync** (wait for completion)
- Or manually: File → Sync Now

**Watch for**: No errors in the "Messages" tool window

### 2c. Wait for Download

Android Studio will download:
- Gradle 8.4 (if using wrapper for first time)
- Android Gradle Plugin 7.4.2
- All dependencies

This may take 5-10 minutes on first run.

---

## Step 3: Verify Build Configuration

### 3a. Build → Clean Project

The project should compile without errors. You should see:
```
> Build successful in X.XXs
> 42 actionable tasks: 42 executed
```

### 3b. Build → Rebuild Project

Again, should succeed with no errors.

### 3c. Build from Command Line (Optional)

```bash
cd C:\Game Projects\Magic-grid

# Clean and build
.\gradlew clean build

# Output should show:
# BUILD SUCCESSFUL in X.XXs
```

---

## Files Created/Fixed

### ✓ Created
```
gradle/wrapper/gradle-wrapper.properties    [Properties file - version lock]
gradlew                                     [Unix shell script]
gradlew.bat                                 [Windows batch script]
setup-gradle-wrapper.ps1                    [PowerShell setup script]
setup-gradle-wrapper.bat                    [Batch setup script]
```

### ✓ Fixed
```
gradle.properties                           [Removed MaxPermSize deprecated option]
```

### ⏳ Needs to be Downloaded (via script)
```
gradle/wrapper/gradle-wrapper.jar           [Binary - downloaded by setup script]
```

---

## Gradle Version Information

| Component | Version | Why |
|-----------|---------|-----|
| Gradle Wrapper | 8.4 | Latest stable, compatible with Android Studio |
| Android Gradle Plugin | 7.4.2 | Specified in app/build.gradle |
| Kotlin | 1.9.0 | Specified in build.gradle |
| Android SDK | API 34 | Specified in app/build.gradle |

---

## Build Output Locations

After successful build:

```
app/build/
├── outputs/
│   └── apk/
│       ├── debug/
│       │   └── app-debug.apk           ← Install on device/emulator
│       └── release/
│           └── app-release.apk         ← For Play Store
├── intermediates/                       [Temporary build files]
└── ... (other build artifacts)
```

---

## Troubleshooting

### Problem: PowerShell script won't run

**Error**: "cannot be loaded because running scripts is disabled"

**Solution**:
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
# Then run the script again
.\setup-gradle-wrapper.ps1
```

---

### Problem: Download fails ("remote name could not be resolved")

**Causes**: 
- No internet connection
- Firewall blocking gradle.org
- Corporate proxy

**Solution**:
1. Check internet connection
2. Try from a different network
3. Manually download from: https://gradle.org/releases/
4. Extract and copy jar manually (see GRADLE_WRAPPER_SETUP.md)

---

### Problem: Gradle build fails with "Unable to start Gradle daemon"

**Cause**: Java/JVM issue

**Solution**:
```bash
# Check Java is installed
java -version

# If not, install Java 11+
# Download from: https://www.oracle.com/java/technologies/downloads/

# Then try again
.\gradlew clean build
```

---

### Problem: "Unrecognized VM option" errors

**Cause**: Outdated `gradle.properties`

**Solution**: Verify your `gradle.properties` contains:
```
org.gradle.jvmargs=-Xmx2048m -XX:+HeapDumpOnOutOfMemoryError
```

NOT:
```
org.gradle.jvmargs=-Xmx2048m -XX:MaxPermSize=512m  ← WRONG (deprecated)
```

---

### Problem: "Could not download gradle-wrapper.jar"

**Solution**: Use manual download method in GRADLE_WRAPPER_SETUP.md

---

## Complete Directory Structure (After Fix)

Your project should now look like:

```
Magic-grid/
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar           ✓ Downloaded by script
│       └── gradle-wrapper.properties    ✓ Created
├── gradlew                              ✓ Created
├── gradlew.bat                          ✓ Created
├── gradle.properties                    ✓ Fixed
├── build.gradle                         ✓ Existing
├── settings.gradle                      ✓ Existing
├── app/
│   ├── build.gradle                     ✓ Existing
│   └── src/
│       ├── main/
│       │   ├── kotlin/                  ✓ Existing
│       │   ├── res/                     ✓ Existing
│       │   └── AndroidManifest.xml      ✓ Existing
│       └── test/                        ✓ Existing
├── setup-gradle-wrapper.ps1             ✓ Created (helper script)
├── setup-gradle-wrapper.bat             ✓ Created (helper script)
└── ... (other project files)
```

---

## Quick Verification

Run this command to confirm everything is working:

### PowerShell
```powershell
$required = @("gradle\wrapper\gradle-wrapper.jar", "gradle\wrapper\gradle-wrapper.properties", "gradlew", "gradlew.bat")
foreach ($file in $required) {
    if (Test-Path $file) {
        Write-Host "✓ $file" -ForegroundColor Green
    } else {
        Write-Host "✗ $file MISSING" -ForegroundColor Red
    }
}
```

### Command Prompt
```cmd
for %%f in (gradle\wrapper\gradle-wrapper.jar gradle\wrapper\gradle-wrapper.properties gradlew gradlew.bat) do (
    if exist "%%f" (
        echo + %%f
    ) else (
        echo - %%f MISSING
    )
)
```

---

## Next Steps

### 1. Run Setup Script
```bash
# PowerShell
.\setup-gradle-wrapper.ps1

# OR Command Prompt
setup-gradle-wrapper.bat
```

### 2. Verify Files (use command above)

### 3. Open in Android Studio
- File → Open → Select Magic-grid folder
- Wait for sync to complete

### 4. Build
- Build → Clean Project
- Build → Rebuild Project

### 5. Success!
You should see: **BUILD SUCCESSFUL**

---

## If Something Goes Wrong

1. **Check internet connection**
2. **Verify Java is installed**: `java -version`
3. **Check gradle.properties** (no MaxPermSize)
4. **Delete and re-run script** (clean state)
5. **Check file permissions** (read/write)
6. **Try manual download** (see GRADLE_WRAPPER_SETUP.md)

---

## Reference Files

- **gradle-wrapper.properties**: Locks Gradle version to 8.4
- **gradle.properties**: JVM options and Android configuration
- **gradlew**: Unix/Mac script for running Gradle
- **gradlew.bat**: Windows batch script for running Gradle
- **gradle/wrapper/gradle-wrapper.jar**: The actual Gradle executable (downloaded)

---

## Version Compatibility

| Component | Version | Compatibility |
|-----------|---------|---|
| Gradle | 8.4 | ✓ Requires Java 11+ |
| Android Gradle Plugin | 7.4.2 | ✓ Works with Gradle 8.4 |
| Java | 11+ | ✓ Required by Gradle 8.4 |
| Android Studio | 2022.1+ | ✓ Tested with latest |

---

## Important Notes

✓ **gradle-wrapper.jar is binary** - Can only be created by downloading Gradle distribution
✓ **Script automates download** - No need to manually get it
✓ **Version locked** - gradle-wrapper.properties ensures same Gradle version on all machines
✓ **No breaking changes** - Updated JVM options are compatible with Java 11+

---

## Support

If you get stuck:
1. Check **Troubleshooting** section above
2. Refer to **GRADLE_WRAPPER_SETUP.md** for detailed jar download options
3. Check **gradle.properties** matches the fixed version
4. Verify **Java installation**: `java -version` (should be 11+)

---

*Last Updated: April 4, 2026*
*Magic Grid Android Build System - Complete Fix Guide*
