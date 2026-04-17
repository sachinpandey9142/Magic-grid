@echo off
REM Magic Grid Android Project - Gradle Wrapper Setup Script (Batch Version)
REM This script automatically downloads and installs gradle-wrapper.jar

setlocal enabledelayedexpansion

title Magic Grid - Gradle Wrapper Setup

echo.
echo ===============================================
echo Magic Grid - Gradle Wrapper Setup
echo ===============================================
echo.

REM Check if we're in the right directory
if not exist "app\src\main" (
    echo ERROR: Not in project root directory!
    echo Please run this script from the Magic-grid project root.
    pause
    exit /b 1
)

echo + Detected project root: %CD%
echo.

REM Step 1: Create gradle\wrapper directory
echo [1/5] Checking gradle\wrapper directory...
if not exist "gradle\wrapper" (
    echo Creating gradle\wrapper directory...
    mkdir gradle\wrapper
)
echo + gradle\wrapper directory ready
echo.

REM Step 2: Check gradle-wrapper.properties
echo [2/5] Checking gradle-wrapper.properties...
if not exist "gradle\wrapper\gradle-wrapper.properties" (
    echo ERROR: gradle-wrapper.properties not found!
    pause
    exit /b 1
)
echo + gradle-wrapper.properties exists
echo.

REM Step 3: Download Gradle 8.4
echo [3/5] Downloading Gradle 8.4 (this may take a minute)...
if exist "gradle-8.4-bin.zip" (
    del /q gradle-8.4-bin.zip
)

REM Try using PowerShell for download (more reliable)
powershell -NoProfile -Command "try { Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-8.4-bin.zip' -OutFile 'gradle-8.4-bin.zip' -ErrorAction Stop } catch { exit 1 }" >nul 2>&1

if errorlevel 1 (
    echo.
    echo ERROR: Failed to download Gradle!
    echo.
    echo Please download manually:
    echo 1. Visit: https://gradle.org/releases/
    echo 2. Download Gradle 8.4 (binary-only zip)
    echo 3. Extract to temp folder
    echo 4. Copy gradle-8.4\lib\gradle-wrapper.jar to gradle\wrapper\
    echo.
    pause
    exit /b 1
)

echo + Downloaded gradle-8.4-bin.zip
echo.

REM Step 4: Extract JAR
echo [4/5] Extracting gradle-wrapper.jar...
if exist "gradle-extract-temp" (
    rmdir /s /q gradle-extract-temp
)

mkdir gradle-extract-temp

REM Use PowerShell to extract (more reliable with paths)
powershell -NoProfile -Command "Expand-Archive -Path 'gradle-8.4-bin.zip' -DestinationPath 'gradle-extract-temp'" >nul 2>&1

if errorlevel 1 (
    echo ERROR: Failed to extract zip file!
    pause
    exit /b 1
)

if not exist "gradle-extract-temp\gradle-8.4\lib\gradle-wrapper.jar" (
    echo ERROR: gradle-wrapper.jar not found in extracted archive!
    pause
    exit /b 1
)

copy /y "gradle-extract-temp\gradle-8.4\lib\gradle-wrapper.jar" "gradle\wrapper\gradle-wrapper.jar" >nul

if errorlevel 1 (
    echo ERROR: Failed to copy jar file!
    pause
    exit /b 1
)

echo + Installed gradle-wrapper.jar
echo.

REM Step 5: Cleanup
echo [5/5] Cleaning up temporary files...
if exist "gradle-8.4-bin.zip" (
    del /q gradle-8.4-bin.zip
)
if exist "gradle-extract-temp" (
    rmdir /s /q gradle-extract-temp
)
echo + Cleanup complete
echo.

REM Verify structure
echo ===============================================
echo Verifying Gradle wrapper structure...
echo ===============================================
echo.

set ALLOK=1

if exist "gradle\wrapper\gradle-wrapper.jar" (
    echo + gradle-wrapper.jar
) else (
    echo - gradle-wrapper.jar MISSING
    set ALLOK=0
)

if exist "gradle\wrapper\gradle-wrapper.properties" (
    echo + gradle-wrapper.properties
) else (
    echo - gradle-wrapper.properties MISSING
    set ALLOK=0
)

if exist "gradlew" (
    echo + gradlew (Unix script)
) else (
    echo - gradlew MISSING
    set ALLOK=0
)

if exist "gradlew.bat" (
    echo + gradlew.bat (Windows script)
) else (
    echo - gradlew.bat MISSING
    set ALLOK=0
)

echo.

if %ALLOK% equ 1 (
    echo ===============================================
    echo SUCCESS! Gradle wrapper is ready.
    echo ===============================================
    echo.
    echo Next steps:
    echo 1. Open the project in Android Studio
    echo 2. File - Sync Now (or wait for automatic sync!
    echo 3. Build - Clean Project
    echo 4. Build - Rebuild Project
    echo.
    echo Or from command line:
    echo   gradlew.bat clean build
    echo.
) else (
    echo ===============================================
    echo ERROR: Setup incomplete.
    echo ===============================================
    exit /b 1
)

pause
