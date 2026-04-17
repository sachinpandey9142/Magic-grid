# Development Guide - Magic Grid

This guide will help you set up, build, and extend the Magic Grid game.

## Environment Setup

### System Requirements
- **Windows/macOS/Linux**
- **JDK 11+** (included with Android Studio)
- **Android Studio 2022.1+** (Flamingo or newer)
- **SDK API Level 34 (Android 14)**

### Initial Setup

1. **Install Android Studio**
   - Download from https://developer.android.com/studio
   - Install by following the official guide
   - Launch Android Studio and complete SDK Manager setup

2. **Clone/Extract the Project**
   ```bash
   # If you have git
   git clone <repository-url>
   cd Magic-grid
   
   # Or extract the provided ZIP/archive
   ```

3. **Open in Android Studio**
   - File → Open → Select the project root (Magic-grid folder)
   - Wait for Gradle sync to complete
   - This will download all dependencies (~1-2 hours on first run)

4. **Create/Update local.properties**
   ```properties
   # Windows
   sdk.dir=C:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
   
   # macOS
   sdk.dir=/Users/YourName/Library/Android/sdk
   
   # Linux
   sdk.dir=/home/yourname/Android/Sdk
   ```

5. **Setup Android Virtual Device (AVD)**
   - Tools → Device Manager → Create Virtual Device
   - Select Pixel 5a with Android 14 API 34
   - Allocate 4GB+ RAM for smooth gameplay

## Building the Project

### From Android Studio
1. Build → Clean Project
2. Build → Rebuild Project
3. Run → Run 'app'
4. Select your AVD or connected device
5. Click OK to deploy

### From Command Line
```bash
# Clean build
./gradlew clean

# Build APK (debug version)
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Run with logging
./gradlew installDebug logcat
```

### Build Output
- **Debug APK**: `app/build/outputs/apk/debug/app-debug.apk`
- **Release APK**: `app/build/outputs/apk/release/app-release.apk` (requires signing key)

## Project Structure Deep Dive

### Game Flow
```
MenuActivity (Home Screen)
    ↓
LevelSelectActivity (Choose Level)
    ↓
GameActivity (Gameplay)
    ├─ GameManager (Controller)
    ├─ GameView (Custom Rendering)
    ├─ GameBoard (Logic)
    └─ ProgressManager (Persistence)
```

### Class Responsibilities

#### UI Layer
- **MenuActivity**: Navigation hub
  - Guides to different screens
  - Loads last played level
  
- **LevelSelectActivity**: Grid of levels
  - Shows locked/unlocked states
  - Handles level selection

- **GameActivity**: Main game screen
  - Manages game lifecycle
  - Updates UI during gameplay
  - Shows win dialog

- **GameView**: Custom Canvas rendering
  - Draws grid and paths
  - Handles touch input
  - Direct Canvas drawing for performance

#### Controller Layer
- **GameManager**: Central orchestrator
  - Bridges UI and logic
  - Manages game state
  - Validates moves
  - Handles undo/reset
  - Notifies listeners of state changes

- **GameState**: Immutable state snapshot
  - Move count, time, level ID
  - Win status

#### Logic Layer
- **GameBoard**: Grid management
  - Path validation
  - Cell occupancy tracking
  - Win condition checking
  - Rules enforcement

- **Cell**: Individual grid unit
  - Row/column position
  - Color (if endpoint)
  - Path color (if occupied)

- **Path**: Color connection
  - Start and end points
  - Cells in sequence
  - Completion status

#### Data Layer
- **Level**: Level definition
  - Grid size
  - Endpoint positions
  - Difficulty rating

- **LevelRepository**: Level provider
  - 10 predefined levels
  - Level lookup by ID
  - Progression system

- **ProgressManager**: Persistence
  - Saves completed levels
  - Tracks best moves/times
  - SharedPreferences backed

## Extending the Game

### Adding a New Level

Edit `LevelRepository.kt`:

```kotlin
private fun initializeLevels() {
    // ... existing levels ...
    
    levels.add(
        Level(
            id = 11,
            name = "My Custom Level",
            gridSize = 5,
            endpoints = listOf(
                Level.ColorEndpoint(
                    color = 0xFFFF5252.toInt(),  // Red
                    startRow = 0, startCol = 0,   // Top-left
                    endRow = 4, endCol = 4        // Bottom-right
                ),
                Level.ColorEndpoint(
                    color = 0xFF2196F3.toInt(),  // Blue
                    startRow = 0, startCol = 4,   // Top-right
                    endRow = 4, endCol = 0        // Bottom-left
                )
            )
        )
    )
}
```

**Tips for level design:**
- Make first few moves obvious
- Gradually increase complexity
- Ensure there's a valid solution
- Test thoroughly before release

### Adding a New Game Mode

1. **Create new Activity**: `GameModeActivity.kt`
2. **Extend GameManager** or create `GameModeManager`
3. **Update MenuActivity** with button
4. **Create layout** XML

Example: Timed Mode, Daily Challenge, Survival Mode

### Customizing Colors

Edit `res/values/colors.xml`:

```xml
<color name="path_custom">#FF6D00</color>  <!-- Add new color -->
```

Use in code:
```kotlin
Level.ColorEndpoint(
    color = context.getColor(R.color.path_custom),
    ...
)
```

### Enhancing the Hint System

Current hint in `GameActivity.showHint()` is simple. Improve with:

```kotlin
fun getSmartHint(color: Int): Pair<Int, Int>? {
    val path = gameManager.board.getPath(color) ?: return null
    
    if (path.cells.isEmpty()) {
        return Pair(path.startRow, path.startCol)
    }
    
    // BFS to find path to endpoint
    val lastCell = path.cells.last()
    return findPathToEnd(lastCell, path.endRow, path.endCol, color)
}

private fun findPathToEnd(
    from: Cell,
    targetRow: Int,
    targetCol: Int,
    color: Int
): Pair<Int, Int>? {
    // Implement breadth-first search
    // Return next cell on shortest path
    return null // Stub
}
```

## Testing

### Manual Testing Checklist

- [ ] All levels load without crashing
- [ ] Paths cannot overlap
- [ ] Win condition triggers correctly
- [ ] Reset clears the board
- [ ] Undo works for multiple moves
- [ ] Progress saves between sessions
- [ ] UI is responsive during drag
- [ ] No memory leaks during gameplay
- [ ] Back button behaves correctly
- [ ] Device rotation handled gracefully

### UI/UX Testing
- [ ] Text sizes readable at various screen densities
- [ ] Buttons easily tappable (48dp minimum)
- [ ] Colors have sufficient contrast
- [ ] Animations don't cause lag
- [ ] Touch responsiveness is immediate

## Performance Optimization

### Current Optimizations
- Single Paint objects (reused)
- Canvas drawing (fast raster)
- Map-based cell access (O(1))

### Potential Improvements

1. **Partial Invalidation**
   ```kotlin
   // Instead of invalidate() (whole screen redraw)
   invalidate(left, top, right, bottom)  // Only redraw affected area
   ```

2. **Hardware Acceleration**
   - Enable in manifest (already done)
   - Use: `setLayerType(LAYER_TYPE_HARDWARE, null)`

3. **Object Pooling**
   - Reuse Path objects
   - Cache Cell allocations

4. **Caching**
   - Pre-compute cell positions
   - Cache path drawing paths

## Debugging

### Enable Logging
Add to `GameManager`:
```kotlin
private fun log(message: String) {
    Log.d("GameManager", message)
}
```

Use:
```kotlin
log("Board state: ${board}")
log("Path complete: ${path.isComplete()}")
```

### Android Studio Debugger
1. Set breakpoint (click line number)
2. Run → Debug 'app'
3. Execution will pause at breakpoint
4. Inspect variables in "Variables" pane

### Logcat Filters
```
// View only game logs
adb logcat | grep GameManager

// View only errors
adb logcat *:E
```

## Running Tests (Optional)

Create unit tests in `app/src/test/`:

```kotlin
// GameBoardTest.kt
class GameBoardTest {
    @Test
    fun testCellValidation() {
        val board = GameBoard(5, 5)
        board.setEndpoint(0, 0, Color.RED)
        
        assertTrue(board.canAddToPath(Color.RED, 0, 0, 0, 1))
        assertFalse(board.canAddToPath(Color.RED, 0, 0, 2, 2)) // Not adjacent
    }
}
```

Run: `./gradlew test`

## Distribution

### Prepare Release Build
1. Create signing key:
   ```bash
   keytool -genkey -v -keystore release.jks \
     -keyalg RSA -keysize 2048 -validity 10000 \
     -alias magic-grid
   ```

2. Edit `build.gradle`:
   ```gradle
   signingConfigs {
       release {
           storeFile file('release.jks')
           storePassword "your-password"
           keyAlias "magic-grid"
           keyPassword "your-password"
       }
   }
   buildTypes {
       release {
           signingConfig signingConfigs.release
           minifyEnabled true
       }
   }
   ```

3. Build: `./gradlew assembleRelease`

### Upload to Play Store
- Create account at https://play.google.com/apps/publish
- Create app listing
- Upload signed APK
- Follow Google Play policies

## Troubleshooting

### Common Issues

**Gradle sync fails**
- File → Invalidate Caches → Invalidate and Restart
- Delete `.gradle` folder
- Delete `.idea` folder

**Emulator is slow**
- Allocate more RAM in AVD config
- Enable GPU acceleration if available
- Use higher API level (25+)

**App crashes on startup**
- Check logcat for exceptions
- Ensure all resources exist
- Verify AndroidManifest.xml activities

**Touch input not responsive**
- Check `GameView.onTouchEvent()` returns `true`
- Verify GameManager is properly initialized
- Check touch coordinates math

## Resources

- **Android Documentation**: https://developer.android.com/docs
- **Kotlin Documentation**: https://kotlinlang.org/docs
- **Material Design**: https://material.io/design
- **Canvas Drawing**: https://developer.android.com/guide/topics/graphics/custom-views

## Next Steps

1. Build and run the project
2. Play through all levels
3. Review the code structure
4. Add a new level
5. Implement a custom feature

Happy coding! 🚀
