# Game Algorithm Documentation

## Board State Management

### Grid Representation
```
Grid: Array[rows][cols] of Cell

Cell {
  row: Int              // 0 to rows-1
  col: Int              // 0 to cols-1
  color: Int?           // Endpoint color or null
  pathColor: Int?       // Current path color occupying this cell
  isEndpoint: Boolean   // Whether this cell is an endpoint
}
```

### Path Format
```
Path {
  color: Int            // The color this path represents
  startRow, startCol    // Starting endpoint position
  endRow, endCol        // Ending endpoint position
  cells: List[Cell]     // Ordered list of cells forming the path
}
```

## Validation Algorithm

### Can Add Cell to Path

```
canAddToPath(color, fromRow, fromCol, toRow, toCol) -> Boolean {
  1. Check bounds: toRow in [0, rows) AND toCol in [0, cols)
     Return false if out of bounds
  
  2. Check adjacency: |toRow - fromRow| + |toCol - fromCol| == 1
     (Only allow up, down, left, right - not diagonal)
     Return false if not adjacent
  
  3. Get target cell
  
  4. Check for overlap:
     If targetCell.pathColor != null AND targetCell.pathColor != color
       Return false (can't overlap with different path)
  
  5. Check endpoint collision:
     If targetCell.isEndpoint AND targetCell.color != color
       Return false (can't go through another color's endpoint)
  
  6. Check revisit (except destination):
     If path contains (toRow, toCol) AND NOT (toRow == path.endRow AND toCol == path.endCol)
       Return false (can't revisit own path)
  
  7. Return true (cell is valid for path)
}
```

### Win Condition

```
isGameWon() -> Boolean {
  // All paths must be complete
  For each color in colors:
    If path[color].isComplete() == false
      Return false
  
  // All cells must be filled
  For each cell in grid:
    If cell.isFilled() == false  // color != null OR pathColor != null
      Return false
  
  Return true
}
```

## Example Gameplay Walkthrough

### Level Example: 5x5 Grid, 2 Colors

```
Initial State:
  R . . . B          (R = Red endpoint, B = Blue endpoint)
  . . . . .
  . . . . .
  . . . . .
  B . . . R

Goal: Connect all cells, no overlap
```

### Step 1: Start Red Path (from 0,0)
```
Before:
  R . . . B
  . . . . .
  . . . . .
  . . . . .
  B . . . R

After onCellSelected(RED, 0, 0):
  ✓ Path is empty, so must start from startRow==0, startCol==0
  ✓ onCellSelected(RED, 0, 0) is valid
  ✓ RED path now contains: [Cell(0,0)]
```

### Step 2: Move Right
```
User drags right to (0,1)

Validation: canAddToPath(RED, 0, 0, 0, 1)
  ✓ In bounds
  ✓ Adjacent (|0-0| + |1-0| = 1)
  ✓ No overlap
  ✓ Not blocked
  ✓ Not revisiting

After onCellSelected(RED, 0, 1):
  RED path now contains: [Cell(0,0), Cell(0,1)]
  Grid[0][1].pathColor = RED
```

### Step 3: Complete Path
```
User drags down and right to reach endpoint at (4,4)

After multiple moves reach (4,4):
  ✓ Cell is endpoint
  ✓ Cell color matches (RED)
  ✓ Path length > 1
  
Check: RED.path.isComplete()
  ✓ lastCell.row == path.endRow (4 == 4)
  ✓ lastCell.col == path.endCol (4 == 4)
  ✓ RED path is complete!
```

### Step 4: Win Condition
```
After all cells filled and all paths complete:

Check isGameWon():
  ✓ RED.isComplete() = true
  ✓ BLUE.isComplete() = true
  ✓ All grid cells filled
  → GAME WON!
```

## Undo Implementation

### State Snapshot
```
save() {
  For each cell(r, c):
    snapshot[(r,c)] = (cell.color, cell.pathColor)
  
  historyStack.push(snapshot)
}

undo() {
  previous = historyStack.pop()
  
  For each cell(r, c):
    cell.color, cell.pathColor = previous[(r,c)]
}
```

## Move Backtracking (Undo Single Move)

```
undoLastMove(color) {
  path = getPath(color)
  
  If path.cells.isEmpty()
    Return false
  
  cell = path.cells.removeLast()
  cell.pathColor = null
  
  Return true
}
```

## Performance Considerations

### Time Complexity
- Path validation: O(1) per cell add
- Win check: O(rows × cols)
- Undo: O(rows × cols)

### Space Complexity
- Grid: O(rows × cols)
- Path storage: O(total_cells)
- History: O(total_moves)

## Algorithms Explained

### Adjacency Check
```kotlin
fun isAdjacent(r1: Int, c1: Int, r2: Int, c2: Int): Boolean {
  val rowDiff = (r2 - r1).coerceIn(-1, 1)
  val colDiff = (c2 - c1).coerceIn(-1, 1)
  return (rowDiff == 0 && colDiff != 0) || (rowDiff != 0 && colDiff == 0)
}
// 4-way adjacency (not diagonal)
```

### Cell Filling
```kotlin
fun isFilled(): Boolean {
  return color != null || pathColor != null
}
// True if either endpoint or path occupies the cell
```

### Grid Completion
```kotlin
fun getCompletionPercentage(): Int {
  val filled = grid.flatten().count { it.isFilled() }
  return (filled * 100) / totalCells
}
```

## Design Decisions

1. **Array-based Grid**: Direct index access O(1)
2. **Path as List**: Preserves order for drawing/validation
3. **Separate color & pathColor**: Distinguishes endpoints from paths
4. **GameBoard isolated**: No knowledge of rendering
5. **GameManager as mediator**: Decouples UI from logic
6. **SharedPreferences**: Lightweight persistence, good for save states

## Future Algorithm Improvements

### Hint Algorithm (A*)
```
fun findPathToEnd(from: Cell, target: Cell, color: Int): List<Cell> {
  // Use A* pathfinding
  // Consider: valid moves, cell occupancy, distance to target
  return nextCell  // Next optimal cell
}
```

### Solver Algorithm (Backtracking)
```
fun solvePuzzle(board: GameBoard): Boolean {
  // Try to solve current board state
  // Recursively fill remaining cells
  // Return if solvable
  
  if (board.isWon()) return true
  
  for color in board.getRemainingColors():
    for nextCell in getValidMoves(color):
      board.addCell(color, nextCell)
      if (solvePuzzle(board))
        return true
      board.removeCell(color)
  
  return false
}
```

---

This documentation explains the core algorithms and data structures. Reference this when extending the game logic.
