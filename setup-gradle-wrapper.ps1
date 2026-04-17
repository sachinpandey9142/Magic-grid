#!/usr/bin/env powershell

# Magic Grid Android Project - Gradle Wrapper Setup Script
# This script automatically downloads and installs gradle-wrapper.jar

Write-Host "==============================================="
Write-Host "Magic Grid - Gradle Wrapper Setup"
Write-Host "==============================================="
Write-Host ""

# Check if we're in the right directory
if (-not (Test-Path "app\src\main")) {
    Write-Host "ERROR: Not in project root directory!" -ForegroundColor Red
    Write-Host "Please run this script from the Magic-grid project root." -ForegroundColor Red
    exit 1
}

Write-Host "[OK] Detected project root: $(Get-Location)" -ForegroundColor Green
Write-Host ""

# Step 1: Check if gradle/wrapper directory exists
Write-Host "[1/5] Checking gradle/wrapper directory..." -ForegroundColor Cyan
if (-not (Test-Path "gradle\wrapper")) {
    Write-Host "Creating gradle/wrapper directory..."
    New-Item -ItemType Directory -Path "gradle\wrapper" -Force | Out-Null
}
Write-Host "[OK] gradle/wrapper directory ready" -ForegroundColor Green
Write-Host ""

# Step 2: Check if gradle-wrapper.properties exists
Write-Host "[2/5] Checking gradle-wrapper.properties..." -ForegroundColor Cyan
if (-not (Test-Path "gradle\wrapper\gradle-wrapper.properties")) {
    Write-Host "ERROR: gradle-wrapper.properties not found!" -ForegroundColor Red
    exit 1
}
Write-Host "[OK] gradle-wrapper.properties exists" -ForegroundColor Green
Write-Host ""

# Step 3: Download gradle-wrapper.jar (bootstrap JAR)
Write-Host "[3/5] Downloading gradle-wrapper.jar bootstrap JAR..." -ForegroundColor Cyan
$BootstrapJar = "gradle/wrapper/gradle-wrapper.jar"

try {
    Write-Host "Downloading from gradle github..."
    Invoke-WebRequest -Uri "https://raw.githubusercontent.com/gradle/gradle/v8.4.0/gradle/wrapper/gradle-wrapper.jar" -OutFile $BootstrapJar -ErrorAction Stop
    $JarSize = [math]::Round((Get-Item $BootstrapJar).Length / 1KB, 1)
    Write-Host "[OK] Downloaded gradle-wrapper.jar (size: $JarSize KB)" -ForegroundColor Green
} catch {
    Write-Host "ERROR: Failed to download gradle-wrapper.jar!" -ForegroundColor Red
    Write-Host "Details: $_" -ForegroundColor Red
    exit 1
}
Write-Host ""

# Step 4: Download Gradle 8.4 (for build files, optional but recommended)
Write-Host "[4/5] Downloading Gradle 8.4 build distribution..." -ForegroundColor Cyan
$GradleUrl = "https://services.gradle.org/distributions/gradle-8.4-bin.zip"
$ZipFile = "gradle-8.4-bin.zip"

try {
    Write-Host "Downloading Gradle 8.4 from gradle.org..."
    Invoke-WebRequest -Uri $GradleUrl -OutFile $ZipFile -ErrorAction Stop
    $FileSize = [math]::Round((Get-Item $ZipFile).Length / 1MB, 1)
    Write-Host "[OK] Downloaded gradle-8.4-bin.zip (size: $FileSize MB)" -ForegroundColor Green
} catch {
    Write-Host "[WARN] Could not download full Gradle distribution" -ForegroundColor Yellow
    Write-Host "This is optional - the wrapper JAR is sufficient to build" -ForegroundColor Yellow
}
Write-Host ""

# Step 5: Cleanup
Write-Host "[5/5] Cleaning up temporary files..." -ForegroundColor Cyan
try {
    if (Test-Path $ZipFile) {
        Remove-Item $ZipFile -Force
    }
    Write-Host "[OK] Cleanup complete" -ForegroundColor Green
} catch {
    Write-Host "[WARN] Could not delete temporary file" -ForegroundColor Yellow
}
Write-Host ""

# Verify complete structure
Write-Host "==============================================="
Write-Host "Verifying Gradle wrapper structure..."
Write-Host "==============================================="
Write-Host ""

$RequiredFiles = @(
    @{ Path = "gradle\wrapper\gradle-wrapper.jar"; Name = "gradle-wrapper.jar" },
    @{ Path = "gradle\wrapper\gradle-wrapper.properties"; Name = "gradle-wrapper.properties" },
    @{ Path = "gradlew"; Name = "gradlew (Unix script)" },
    @{ Path = "gradlew.bat"; Name = "gradlew.bat (Windows script)" }
)

$AllOk = $true
foreach ($item in $RequiredFiles) {
    if (Test-Path $item.Path) {
        Write-Host "[OK] $($item.Name)" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] $($item.Name) - MISSING" -ForegroundColor Red
        $AllOk = $false
    }
}

Write-Host ""
if ($AllOk) {
    Write-Host "==============================================="
    Write-Host "SUCCESS! Gradle wrapper is ready." -ForegroundColor Green
    Write-Host "==============================================="
    Write-Host ""
    Write-Host "Next steps:"
    Write-Host "1. Open the project in Android Studio"
    Write-Host "2. File - Sync Now (or wait for automatic sync)"
    Write-Host "3. Build - Clean Project"
    Write-Host "4. Build - Rebuild Project"
    Write-Host ""
    Write-Host "Or from command line:"
    Write-Host "  .\gradlew clean build"
    Write-Host ""
} else {
    Write-Host "==============================================="
    Write-Host "ERROR: Setup incomplete." -ForegroundColor Red
    Write-Host "==============================================="
    exit 1
}
