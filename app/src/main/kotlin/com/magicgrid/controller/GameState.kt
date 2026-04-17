package com.magicgrid.controller

/**
 * Game state class - represents the current state of the game.
 * Can be saved/restored for undo functionality.
 */
data class GameState(
    val levelId: Int,
    val moveCount: Int = 0,
    val elapsedTimeSeconds: Int = 0,
    val isGameOver: Boolean = false,
    val won: Boolean = false
)
