# Magic Grid - Production Android Puzzle Game

A modern, polished puzzle game built with Kotlin and Android Studio. Similar to Flow Free, players connect colored pairs on a grid without overlapping paths.

## 🎮 Game Features

### Core Gameplay
- **Grid-Based Puzzle**: Connect matching color pairs on a grid
- **No Overlapping**: Paths cannot cross or overlap
- **Complete Grid**: Fill all cells to complete a level
- **Progressive Difficulty**: 10+ carefully designed levels

### Game Modes
- **Campaign**: 10 unique levels with increasing difficulty
- **Level Select**: Choose any unlocked level to play
- **Progress Tracking**: Save your progress automatically

### Features
- ✅ Smooth drag-to-draw interaction
- ✅ Undo functionality
- ✅ Reset level button
- ✅ Hint system
- ✅ Move counter & timer
- ✅ Level progression
- ✅ Best score tracking
- ✅ Modern, polished UI with animations

## 📁 Project Structure

```
Magic-grid/
├── app/
│   ├── src/main/
│   │   ├── kotlin/com/magicgrid/
│   │   │   ├── game/
│   │   │   │   ├── MenuActivity.kt
│   │   │   │   ├── LevelSelectActivity.kt
│   │   │   │   └── GameActivity.kt
│   │   │   ├── controller/
│   │   │   │   ├── GameManager.kt
│   │   │   │   └── GameState.kt
│   │   │   ├── logic/
│   │   │   │   ├── GameBoard.kt
│   │   │   │   ├── Cell.kt
│   │   │   │   └── Path.kt
│   │   │   ├── data/
│   │   │   │   ├── Level.kt
│   │   │   │   ├── LevelRepository.kt
│   │   │   │   └── ProgressManager.kt
│   │   │   └── ui/
│   │   │       └── GameView.kt
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   ├── activity_menu.xml
│   │   │   │   ├── activity_level_select.xml
│   │   │   │   └── activity_game.xml
│   │   │   ├── drawable/
│   │   │   │   ├── gradient_background.xml
│   │   │   │   ├── button_primary.xml
│   │   │   │   └── button_secondary.xml
│   │   │   └── values/
│   │   │       ├── colors.xml
│   │   │       ├── strings.xml
│   │   │       └── styles.xml
│   │   └── AndroidManifest.xml
│   ├── build.gradle
│   └── proguard-rules.pro
├── build.gradle
├── settings.gradle
└── README.md
```

## 🏗️ Architecture

The project follows a **clean layered architecture** with strict separation of concerns:

### 1. **UI Layer** (`game/` + `ui/`)
- `MenuActivity`: Home screen with navigation
- `LevelSelectActivity`: Level selection grid
- `GameActivity`: Main gameplay screen
- `GameView`: Custom Canvas-based rendering

**Responsibility**: Display and handle user interaction only

### 2. **Controller Layer** (`controller/`)
- `GameManager`: Central controller managing game flow
- `GameState`: Immutable data class for current state

**Responsibility**: Mediate between UI and Logic, handle player input, manage state transitions

### 3. **Logic Layer** (`logic/`)
- `GameBoard`: Grid management and validation logic
- `Cell`: Individual grid unit
- `Path`: Represents a connected path for a color

**Responsibility**: Game rules, validation, state manipulation - NO UI code

### 4. **Data Layer** (`data/`)
- `Level`: Level definition (size, endpoints)
- `LevelRepository`: Provides predefined levels
- `ProgressManager`: Saves/loads progress with SharedPreferences

**Responsibility**: Data access and persistence

## 🎯 Key Design Principles

1. **No UI in Logic**: GameBoard has zero knowledge of rendering
2. **No Logic in UI**: Activities don't make game decisions
3. **Separation of Concerns**: Each class has one responsibility
4. **Data Classes**: Used for immutable data (Level, GameState)
5. **Listener Pattern**: GameStateListener for reactive updates
6. **Provider Pattern**: LevelRepository for data access

## 🚀 Getting Started

### Prerequisites
- Android Studio 2022.1+
- Android SDK 34+
- Kotlin 1.9.0+

### Setup
1. Clone or download the project
2. Open in Android Studio
3. Let Gradle sync dependencies
4. Build and run on emulator or device

### Build from Command Line
```bash
cd Magic-grid
./gradlew clean build
./gradlew installDebug
```

## 🎮 How to Play

1. **Select Level**: Choose a level from the menu
2. **Connect Pairs**: Tap and drag to connect matching colors
3. **Fill Grid**: All cells must be filled with no overlapping paths
4. **Win**: Complete all connections and fill the entire grid

### Controls
- **Drag**: Connect paths
- **Reset**: Clear the level and start over
- **Undo**: Undo the last move
- **Hint**: Get a suggestion for stuck situations

## 📊 Level System

### Difficulty Progression
- **Easy** (Levels 1-5): 5x5 grid, 2-4 colors
- **Medium** (Levels 6-8): 6x6 grid, 3-5 colors
- **Hard** (Levels 9-10): 7x7 grid, 5+ colors

### Predefined Levels
The game includes 10 hand-crafted levels with varying difficulty. Extend by adding more to `LevelRepository::initializeLevels()`.

## 💾 Save System

Progress is automatically saved using SharedPreferences:
- **Completed Levels**: Set of level IDs
- **Level Scores**: Best move count per level
- **Level Times**: Best time per level
- **Current Level**: Last played level

Reset via `ProgressManager.resetProgress()`.

## 🎨 Visual Design

### Color Palette
- Primary: `#6200EE` (Purple)
- Secondary: `#03DAC6` (Teal)
- Background: Gradient from `#6200EE` to `#1A0033`

### Path Colors
- Red, Blue, Green, Orange, Purple, Yellow, Pink, Cyan

### UI Elements
- Rounded corners (8dp radius)
- Ripple effects on buttons
- Smooth animations
- Modern material design

## ⚡ Performance Optimizations

1. **Efficient Rendering**: Only draw changed cells (can be optimized further)
2. **Touch Handling**: Direct path validation without unnecessary redraws
3. **Memory**: Reuse Paint objects
4. **Data Structures**: Use HashMaps for O(1) color lookups

## 🔧 Extending the Game

### Adding New Levels
Edit `LevelRepository.initializeLevels()`:
```kotlin
levels.add(
    Level(
        id = 11,
        name = "Custom Level",
        gridSize = 5,
        endpoints = listOf(
            Level.ColorEndpoint(0xFFFF5252.toInt(), 0, 0, 4, 4),
            Level.ColorEndpoint(0xFF2196F3.toInt(), 0, 4, 4, 0)
        )
    )
)
```

### Custom Colors
Update `colors.xml` in `res/values/`.

### New Features
- Add listeners in `GameStateListener` interface
- Extend `GameManager` for new game modes
- Override `GameView` for custom rendering

## 📝 Code Quality

- Well-documented with KDoc comments
- Kotlin best practices and idioms
- No null pointer exceptions (safe calls, null checks)
- Data classes for immutable models
- Extension functions where appropriate

## 🐛 Known Limitations & Future Improvements

### Current Limitations
- Hint system is basic (shows next cell only)
- No animation for winning
- Timer doesn't pause on pause
- No pause functionality

### Future Enhancements
- **Animations**: Toast win messages with confetti
- **Difficulty Presets**: Choose grid size and color count
- **Leaderboard**: Cloud sync with Firebase
- **Themes**: Dark mode, seasonal themes
- **Sound**: Background music and SFX
- **Pause/Resume**: Handle activity lifecycle properly
- **Achievements**: Badge system
- **Analytics**: Track player behavior

## 📄 License

This is a sample educational project demonstrating Android game development best practices.

## 🤝 Contributing

Feel free to fork and improve! Key areas:
- More levels
- Better hint algorithm
- Animation polish
- Performance profiling

---

**Built with ❤️ using Kotlin and Android Studio**
