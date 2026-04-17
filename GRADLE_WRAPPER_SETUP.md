# Gradle Wrapper JAR Fix Guide

This document explains how to obtain the missing `gradle-wrapper.jar` file.

## Option 1: Download Official JAR (Recommended - Windows)

The gradle-wrapper.jar is bundled with Gradle 8.4 distribution. Since this file is a binary, you can download it directly:

### Step 1: Download from Official Source
```bash
# Navigate to your project root
cd c:\Game Projects\Magic-grid

# Download Gradle 8.4 distribution
Invoke-WebRequest -Uri "https://services.gradle.org/distributions/gradle-8.4-bin.zip" -OutFile "gradle-8.4-bin.zip"

# Extract the wrapper jar
Expand-Archive -Path "gradle-8.4-bin.zip" -DestinationPath "."

# Copy the wrapper jar to the correct location
Copy-Item -Path "gradle-8.4\lib\gradle-wrapper.jar" -Destination "gradle\wrapper\gradle-wrapper.jar"

# Clean up
Remove-Item -Path "gradle-8.4" -Recurse
Remove-Item -Path "gradle-8.4-bin.zip"
```

### Step 2: Verify the JAR was copied
```bash
# Check if the file exists
if (Test-Path "gradle\wrapper\gradle-wrapper.jar") {
    Write-Host "✓ gradle-wrapper.jar created successfully"
} else {
    Write-Host "✗ Error: JAR file not found"
}
```

---

## Option 2: Use Pre-generated JAR (Quickest)

If you have a working Gradle installation (Android Studio, or separate Gradle):

```bash
# From project root using installed gradle
gradle wrapper --gradle-version=8.4

# Or if Gradle is not in PATH, use full path
# C:\path\to\gradle\bin\gradle wrapper --gradle-version=8.4
```

This command automatically:
- Downloads the correct gradle-wrapper.jar
- Updates gradle-wrapper.properties
- Creates/updates the wrapper scripts (gradlew, gradlew.bat)

---

## Option 3: Using Android Studio's Built-in Gradle

If you have Android Studio installed:

1. Open a PowerShell terminal in Android Studio (Built-in Terminal)
2. Run command:
```bash
.\gradlew wrapper --gradle-version=8.4
```

Android Studio comes with Gradle, so this should work immediately.

---

## Option 4: Manual Download (If PowerShell Script Fails)

If the automated download doesn't work:

1. Visit: https://gradle.org/releases/
2. Find **Gradle 8.4** (or latest 8.x)
3. Download the **Binary-only** zip
4. Extract the zip file anywhere
5. Copy the JAR:
   - Source: `gradle-8.4/lib/gradle-wrapper.jar`
   - Destination: `gradle/wrapper/gradle-wrapper.jar`

---

## Complete Verification Checklist

After obtaining the JAR, verify the complete structure:

```bash
# Check all required files exist
Write-Host "Checking Gradle wrapper structure..."

$files = @(
    "gradle\wrapper\gradle-wrapper.jar",
    "gradle\wrapper\gradle-wrapper.properties",
    "gradlew",
    "gradlew.bat"
)

foreach ($file in $files) {
    if (Test-Path $file) {
        Write-Host "✓ $file"
    } else {
        Write-Host "✗ $file - MISSING"
    }
}
```

---

## Quick Command for Windows PowerShell

Run this one-liner from your project root:

```powershell
# Download Gradle 8.4, extract jar, cleanup
$projectRoot = Get-Location; `
Invoke-WebRequest -Uri "https://services.gradle.org/distributions/gradle-8.4-bin.zip" -OutFile "gradle-8.4-bin.zip" -Verbose; `
Expand-Archive -Path "gradle-8.4-bin.zip" -DestinationPath "."; `
Copy-Item -Path "gradle-8.4\lib\gradle-wrapper.jar" -Destination "gradle\wrapper\gradle-wrapper.jar" -Verbose; `
Remove-Item -Path "gradle-8.4" -Recurse; `
Remove-Item -Path "gradle-8.4-bin.zip"; `
Write-Host "✓ gradle-wrapper.jar installed successfully"
```

---

## Important Notes

✅ **gradle-wrapper.jar is safe** - It's the official Gradle wrapper from gradle.org
✅ **File size** - Should be ~6-8 MB
✅ **Version lock** - The version 8.4 is locked in gradle-wrapper.properties
✅ **Auto-update** - Once in place, you can update with: `gradlew wrapper --gradle-version=X.X`

---

## Troubleshooting

**Problem**: "Invoke-WebRequest: The remote name could not be resolved"
- **Solution**: Check internet connection, or manually download from gradle.org

**Problem**: "Path does not exist"
- **Solution**: Make sure you're in the correct project directory (c:\Game Projects\Magic-grid\)

**Problem**: "Access denied when copying"
- **Solution**: Run PowerShell as Administrator, or close Android Studio while copying

**Problem**: "gradle wrapper command not found"
- **Solution**: Use Option 1 (manual download) instead of Option 2

---

## Next Steps After JAR Installation

Once gradle-wrapper.jar is in place:

```bash
# Test the build
./gradlew clean build

# Or from Windows Command Prompt
gradlew.bat clean build
```

This will:
1. Download Gradle 8.4 from the URL in gradle-wrapper.properties
2. Download all dependencies defined in build.gradle
3. Compile all Kotlin source files
4. Build the APK

---

*Last Updated: April 2026*
*Gradle Wrapper Setup Guide*
