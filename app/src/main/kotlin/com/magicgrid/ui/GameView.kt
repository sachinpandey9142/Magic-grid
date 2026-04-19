package com.magicgrid.ui

import android.content.Context
import android.graphics.*
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.SystemClock
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import com.magicgrid.controller.GameManager
import kotlin.math.min
import kotlin.math.sin

/**
 * Custom view for rendering the game grid, cells, paths, and handling touch input.
 * Handles all visual rendering and user interaction.
 */
class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private lateinit var gameManager: GameManager

    // Drawing properties
    private var cellSize = 0f
    private var gridStartX = 0f
    private var gridStartY = 0f
    private var gridPaddingX = 40f
    private var gridPaddingY = 40f
    private var inputEnabled = true

    private val frameRect = RectF()
    private val frameCornerRadius = 20f
    private val cellRect = RectF()
    private val cellHighlightRect = RectF()

    private val frameOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val frameMediumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val frameInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }

    private val frameInnerLightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val cellHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(15, 255, 255, 255)
    }

    // Paint objects - Premium neon theme with thick glowing paths
    private val gridPaint = Paint().apply {
        style = Paint.Style.STROKE
    }

    private val cellBackgroundPaint = Paint().apply {
        style = Paint.Style.FILL
    }

    private val cellBorderPaint = Paint().apply {
        strokeWidth = 0.5f  // Very thin border
        style = Paint.Style.STROKE
        color = Color.WHITE
    }

    // THICK neon path paint - premium bold lines
    private val pathPaint = Paint().apply {
        strokeWidth = 16f  // MUCH THICKER - premium neon tube effect
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    // Multi-layer glow effect for neon vibrancy
    private val glowPaint = Paint().apply {
        strokeWidth = 16f  // Match path for glow
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }
    
    // Outer glow for maximum neon effect
    private val outerGlowPaint = Paint().apply {
        strokeWidth = 20f  // Wide outer glow
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    private val endpointPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val endpointBorderPaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeWidth = 2f  // Thinner border
        color = Color.WHITE
    }

    // Touch tracking - improved with interpolation to prevent skipping cells
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var lastGridRow = -1
    private var lastGridCol = -1
    private var currentTouchColor: Int? = null
    private var isPathActive = false  // Track if we're actively drawing a path
    
    // Completion animation tracking
    private var completedPathAnimationTime = 0f
    private var completedPathColor: Int? = null
    private val COMPLETION_ANIMATION_DURATION = 600f  // ms

    private data class HintOverlay(
        val color: Int,
        val route: List<Pair<Int, Int>>
    )

    private var hintOverlay: HintOverlay? = null
    private var hintOverlayRemainingTime = 0f
    private val HINT_OVERLAY_DURATION = 2200f
    private var pathToneGenerator: ToneGenerator? = null
    private var pathCompleteToneGenerator: ToneGenerator? = null
    private var pendingPathCompleteTailTone: Runnable? = null
    private var lastConnectToneAtMs = 0L
    private val CONNECT_TONE_MIN_INTERVAL_MS = 42L

    private val hintGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val hintLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val hintCellOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val hintCellInnerOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    fun setGameManager(gm: GameManager) {
        gameManager = gm
        if (width > 0 && height > 0) {
            updateBoardGeometry(width, height)
        }
        gameManager.addListener(object : GameManager.GameStateListener {
            override fun onGameStateChanged() {
                postInvalidateOnAnimation()
            }

            override fun onGameWon() {
                postInvalidateOnAnimation()
            }
            
            override fun onPathCompleted(color: Int) {
                // Trigger completion animation when path is locked
                triggerCompletionAnimation(color)
                playPathCompleteTone()
            }
        })
        postInvalidateOnAnimation()
    }

    fun setInputEnabled(enabled: Boolean) {
        if (inputEnabled == enabled) return

        inputEnabled = enabled
        if (!enabled) {
            isPathActive = false
            currentTouchColor = null
        }
        postInvalidateOnAnimation()
    }

    fun showHintRoute(route: List<Pair<Int, Int>>, color: Int) {
        if (route.size < 2) return

        hintOverlay = HintOverlay(
            color = color,
            route = route.toList()
        )
        hintOverlayRemainingTime = HINT_OVERLAY_DURATION
        postInvalidateOnAnimation()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        // Make it square-ish with aspect ratio
        val size = min(width, height)
        setMeasuredDimension(width, size)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (!::gameManager.isInitialized) return

        updateBoardGeometry(w, h)
    }

    private fun updateBoardGeometry(width: Int, height: Int) {
        if (!::gameManager.isInitialized) return

        // Calculate grid dimensions
        val availableWidth = width - (gridPaddingX * 2)
        val availableHeight = height - (gridPaddingY * 2)

        cellSize = minOf(availableWidth / gameManager.board.cols, availableHeight / gameManager.board.rows)
        if (cellSize <= 0f) return

        // Center the grid
        val totalGridWidth = cellSize * gameManager.board.cols
        val totalGridHeight = cellSize * gameManager.board.rows

        gridStartX = (width - totalGridWidth) / 2
        gridStartY = (height - totalGridHeight) / 2

        updateFrameRenderingResources()
    }

    private fun updateFrameRenderingResources() {
        val cols = gameManager.board.cols
        val rows = gameManager.board.rows
        val padding = 12f

        val frameLeft = gridStartX - padding
        val frameTop = gridStartY - padding
        val frameRight = gridStartX + cols * cellSize + padding
        val frameBottom = gridStartY + rows * cellSize + padding

        frameRect.set(frameLeft, frameTop, frameRight, frameBottom)

        frameOuterPaint.shader = LinearGradient(
            frameLeft,
            frameTop,
            frameRight,
            frameBottom,
            intArrayOf(
                Color.argb(40, 179, 0, 255),
                Color.argb(40, 255, 20, 147),
                Color.argb(40, 255, 107, 0),
                Color.argb(40, 0, 153, 255)
            ),
            null,
            Shader.TileMode.CLAMP
        )
        frameOuterPaint.setShadowLayer(40f, 0f, 0f, Color.argb(60, 179, 0, 255))

        frameMediumPaint.shader = LinearGradient(
            frameLeft,
            frameTop,
            frameRight,
            frameBottom,
            intArrayOf(
                Color.argb(100, 179, 0, 255),
                Color.argb(100, 255, 20, 147),
                Color.argb(100, 255, 107, 0),
                Color.argb(100, 0, 153, 255)
            ),
            null,
            Shader.TileMode.CLAMP
        )
        frameMediumPaint.setShadowLayer(24f, 0f, 0f, Color.argb(120, 100, 50, 255))

        frameInnerPaint.shader = LinearGradient(
            frameLeft,
            frameTop,
            frameRight,
            frameBottom,
            intArrayOf(
                Color.parseColor("#B300FF"),
                Color.parseColor("#FF1493"),
                Color.parseColor("#FF6B00"),
                Color.parseColor("#0099FF")
            ),
            null,
            Shader.TileMode.CLAMP
        )
        frameInnerPaint.setShadowLayer(12f, 0f, 0f, Color.argb(200, 255, 50, 200))

        val centerX = (frameLeft + frameRight) / 2
        val centerY = (frameTop + frameBottom) / 2
        val maxDist = maxOf(frameRight - frameLeft, frameBottom - frameTop) / 2
        frameInnerLightPaint.shader = RadialGradient(
            centerX,
            centerY,
            maxDist,
            intArrayOf(
                Color.argb(3, 100, 200, 255),
                Color.argb(0, 100, 200, 255)
            ),
            null,
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!::gameManager.isInitialized || cellSize <= 0f) return

        // Update completion animation
        updateCompletionAnimation()
        updateHintOverlayAnimation()

        // Draw grid (no background - cosmic background shows through)
        drawGrid(canvas)

        // Draw neon frame around grid
        drawFrame(canvas)

        // Draw cells with paths
        drawCells(canvas)

        // Draw connecting lines/paths
        drawPaths(canvas)

        // Draw endpoints on top so lines visually connect from underneath the dots.
        drawEndpoints(canvas)

        // Draw hint guidance on top of paths
        drawHintOverlay(canvas)
        
        // Continue animation if needed
        if (completedPathAnimationTime > 0f || hintOverlayRemainingTime > 0f) {
            postInvalidateOnAnimation()
        }
    }

    /**
     * Draw the grid lines
     */
    private fun drawGrid(canvas: Canvas) {
        val cols = gameManager.board.cols
        val rows = gameManager.board.rows

        gridPaint.color = Color.parseColor("#2A2A3A")
        gridPaint.alpha = 100  // Subtle grid

        // Draw vertical lines
        for (c in 0..cols) {
            val x = gridStartX + c * cellSize
            canvas.drawLine(x, gridStartY, x, gridStartY + rows * cellSize, gridPaint)
        }

        // Draw horizontal lines
        for (r in 0..rows) {
            val y = gridStartY + r * cellSize
            canvas.drawLine(gridStartX, y, gridStartX + cols * cellSize, y, gridPaint)
        }
    }

    /**
     * Draw premium neon glowing frame with multi-layer glow effects
     * Layer 1: Soft outer glow
     * Layer 2: Medium glow
     * Layer 3: Sharp inner brightness gradient
     * Plus: Inner grid radial light
     */
    private fun drawFrame(canvas: Canvas) {
        canvas.drawRoundRect(frameRect, frameCornerRadius, frameCornerRadius, frameOuterPaint)
        canvas.drawRoundRect(frameRect, frameCornerRadius, frameCornerRadius, frameMediumPaint)
        canvas.drawRoundRect(frameRect, frameCornerRadius, frameCornerRadius, frameInnerPaint)
        canvas.drawRoundRect(frameRect, frameCornerRadius, frameCornerRadius, frameInnerLightPaint)
    }

    /**
     * Draw individual cells with TRUE glassmorphism - very light, transparent tiles
     * Background must be clearly visible through tiles
     */
    private fun drawCells(canvas: Canvas) {
        val cols = gameManager.board.cols
        val rows = gameManager.board.rows
        val cornerRadius = 10f  // 10dp rounded corners

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val cell = gameManager.board.getCell(r, c) ?: continue

                // Add spacing between cells (5dp)
                val spacing = 5f
                val left = gridStartX + c * cellSize + spacing / 2
                val top = gridStartY + r * cellSize + spacing / 2
                val right = left + cellSize - spacing
                val bottom = top + cellSize - spacing

                // LAYER 1: Base glass fill - VERY light and transparent
                // rgba(255, 255, 255, 0.08) - allows background to shine through clearly
                if (cell.pathColor != null) {
                    // Filled cell - light white tint with path color overlay
                    cellBackgroundPaint.color = Color.WHITE
                    cellBackgroundPaint.alpha = 20  // ~8% opacity - almost invisible white base
                    cellRect.set(left, top, right, bottom)
                    canvas.drawRoundRect(cellRect, cornerRadius, cornerRadius, cellBackgroundPaint)
                    
                    // Add path color as subtle tint on top
                    val pathAlpha = 25  // ~10% path color tint
                    val pathRGB = cell.pathColor!!
                    cellBackgroundPaint.color = Color.argb(pathAlpha, Color.red(pathRGB), Color.green(pathRGB), Color.blue(pathRGB))
                    canvas.drawRoundRect(cellRect, cornerRadius, cornerRadius, cellBackgroundPaint)
                } else if (!cell.isEndpoint) {
                    // Empty cell - very light white glass
                    cellBackgroundPaint.color = Color.argb(20, 255, 255, 255)  // ~8% opacity: rgba(255,255,255,0.08)
                    cellRect.set(left, top, right, bottom)
                    canvas.drawRoundRect(cellRect, cornerRadius, cornerRadius, cellBackgroundPaint)
                }

                // LAYER 2: Border - thin white stroke that defines the tile
                // rgba(255,255,255,0.12) - subtle but visible border
                cellBorderPaint.color = Color.argb(31, 255, 255, 255)  // ~12% opacity for subtle border
                cellBorderPaint.strokeWidth = 0.5f
                cellRect.set(left, top, right, bottom)
                canvas.drawRoundRect(cellRect, cornerRadius, cornerRadius, cellBorderPaint)
                
                // Draw thin highlight strip at top
                cellHighlightRect.set(left + 0.5f, top + 0.5f, right - 0.5f, top + cellSize * 0.15f)
                canvas.drawRoundRect(cellHighlightRect, cornerRadius, cornerRadius, cellHighlightPaint)
            }
        }
    }

    /**
     * Draw endpoints (colored circles at start/end points) with soft glow
     */
    private fun drawEndpoints(canvas: Canvas) {
        val cols = gameManager.board.cols
        val rows = gameManager.board.rows

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val cell = gameManager.board.getCell(r, c) ?: continue

                if (cell.isEndpoint && cell.color != null) {
                    val centerX = gridStartX + c * cellSize + cellSize / 2
                    val centerY = gridStartY + r * cellSize + cellSize / 2
                    
                    val nodeColor = cell.color!!
                    val glowColor = getPremiumGlowColor(nodeColor)
                    
                    // Check if this is a completed path that should animate
                    var radius = cellSize * 0.32f
                    var glowIntensity = 0.6f
                    
                    if (completedPathColor == nodeColor && completedPathAnimationTime > 0f) {
                        // Pulse animation: 1.0 -> 1.2 -> 1.0
                        val progress = (completedPathAnimationTime / COMPLETION_ANIMATION_DURATION).coerceIn(0f, 1f)
                        val pulseFactor = 1.0f + 0.2f * kotlin.math.sin(progress * kotlin.math.PI).toFloat()
                        radius *= pulseFactor
                        glowIntensity = 1.0f  // Stronger glow during completion
                    }

                    // LAYER 1: Soft glow halo
                    glowPaint.color = glowColor
                    glowPaint.alpha = (100 * glowIntensity).toInt()
                    glowPaint.setShadowLayer(8f, 0f, 0f, glowColor)
                    canvas.drawCircle(centerX, centerY, radius * 1.4f, glowPaint)

                    // LAYER 2: Main endpoint circle
                    endpointPaint.color = nodeColor
                    endpointPaint.alpha = 255
                    canvas.drawCircle(centerX, centerY, radius, endpointPaint)

                    // LAYER 3: Inner highlight - bright white shine
                    endpointPaint.color = Color.WHITE
                    endpointPaint.alpha = 100
                    canvas.drawCircle(centerX - radius * 0.25f, centerY - radius * 0.25f, radius * 0.28f, endpointPaint)

                    // LAYER 4: Border outline
                    endpointBorderPaint.color = Color.WHITE
                    endpointBorderPaint.alpha = 180
                    endpointBorderPaint.strokeWidth = 2f
                    canvas.drawCircle(centerX, centerY, radius, endpointBorderPaint)

                    // Clear shadow
                    glowPaint.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
                }
            }
        }
    }

    /**
     * Draw paths connecting cells with thin neon glow and smooth curves
     */
    /**
     * Draw paths connecting cells with PREMIUM NEON GLOW EFFECT
     * 3-layer glow system: outer aura → medium glow → bright core
     * Adaptive to cosmic purple/pink theme
     */
    private fun drawPaths(canvas: Canvas) {
        for (color in gameManager.board.colors) {
            val path = gameManager.board.getPath(color) ?: continue
            if (path.cells.isEmpty()) continue

            val glowColor = getPremiumGlowColor(color)
            
            // Adjust glow opacity based on path activity
            val isActivePath = (currentTouchColor == color && isPathActive)
            val isCompletedPulse = (completedPathColor == color && completedPathAnimationTime > 0f)
            val completionProgress =
                (completedPathAnimationTime / COMPLETION_ANIMATION_DURATION).coerceIn(0f, 1f)
            val completionPulse = if (isCompletedPulse) {
                1f + 0.24f * kotlin.math.sin(completionProgress * kotlin.math.PI).toFloat()
            } else {
                1f
            }

            val outerStroke = if (isCompletedPulse) 24f * completionPulse else 20f
            val mediumStroke = if (isCompletedPulse) 20f * completionPulse else 16f
            val coreStroke = if (isCompletedPulse) 18f * completionPulse else 16f
            
            // Draw only the valid path segments (not crossed-out parts)
            // The path.cells list is maintained in order, so we draw all segments
            
            // LAYER 1: OUTER NEON AURA - Extra wide, very vibrant
            outerGlowPaint.strokeWidth = outerStroke
            outerGlowPaint.color = glowColor
            outerGlowPaint.alpha = when {
                isCompletedPulse -> 230
                isActivePath -> 200
                else -> 140
            }
            outerGlowPaint.setShadowLayer(18f, 0f, 0f, glowColor)  // Massive shadow glow

            for (i in 0 until path.cells.size - 1) {
                val currentCell = path.cells[i]
                val nextCell = path.cells[i + 1]

                val x1 = gridStartX + currentCell.col * cellSize + cellSize / 2
                val y1 = gridStartY + currentCell.row * cellSize + cellSize / 2
                val x2 = gridStartX + nextCell.col * cellSize + cellSize / 2
                val y2 = gridStartY + nextCell.row * cellSize + cellSize / 2

                canvas.drawLine(x1, y1, x2, y2, outerGlowPaint)
            }

            // LAYER 2: MEDIUM GLOW - Colorful bloom
            glowPaint.strokeWidth = mediumStroke
            glowPaint.color = glowColor
            glowPaint.alpha = when {
                isCompletedPulse -> 200
                isActivePath -> 160
                else -> 100
            }
            glowPaint.setShadowLayer(10f, 0f, 0f, glowColor)

            for (i in 0 until path.cells.size - 1) {
                val currentCell = path.cells[i]
                val nextCell = path.cells[i + 1]

                val x1 = gridStartX + currentCell.col * cellSize + cellSize / 2
                val y1 = gridStartY + currentCell.row * cellSize + cellSize / 2
                val x2 = gridStartX + nextCell.col * cellSize + cellSize / 2
                val y2 = gridStartY + nextCell.row * cellSize + cellSize / 2

                canvas.drawLine(x1, y1, x2, y2, glowPaint)
            }

            // LAYER 3: BRIGHT CORE - Thick neon line
            pathPaint.strokeWidth = coreStroke
            pathPaint.color = color
            pathPaint.alpha = when {
                isCompletedPulse -> 255
                isActivePath -> 255
                else -> 240
            }
            pathPaint.setShadowLayer(6f, 0f, 0f, glowColor)

            for (i in 0 until path.cells.size - 1) {
                val currentCell = path.cells[i]
                val nextCell = path.cells[i + 1]

                val x1 = gridStartX + currentCell.col * cellSize + cellSize / 2
                val y1 = gridStartY + currentCell.row * cellSize + cellSize / 2
                val x2 = gridStartX + nextCell.col * cellSize + cellSize / 2
                val y2 = gridStartY + nextCell.row * cellSize + cellSize / 2

                canvas.drawLine(x1, y1, x2, y2, pathPaint)
            }

            // Clear shadow effects
            pathPaint.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
            glowPaint.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
            outerGlowPaint.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)

            // Restore defaults for next draw pass.
            outerGlowPaint.strokeWidth = 20f
            glowPaint.strokeWidth = 16f
            pathPaint.strokeWidth = 16f
        }
    }

    /**
     * Get premium neon glow color for paths and endpoints
     * Vibrant cosmic colors: purple, pink, cyan, magenta - matching starry background
     */
    private fun getPremiumGlowColor(color: Int): Int {
        return when (color) {
            // Red/Pink family → Vibrant Magenta
            Color.parseColor("#FF006E") -> Color.parseColor("#FF10FF")  // Hot magenta
            Color.parseColor("#FF3B3B") -> Color.parseColor("#FF0080")  // Bright magenta
            
            // Cyan/Blue family → Electric Cyan
            Color.parseColor("#00D9FF") -> Color.parseColor("#00FFFF")  // Bright cyan
            Color.parseColor("#0099FF") -> Color.parseColor("#00E5FF")  // Electric blue
            
            // Green family → Neon Green
            Color.parseColor("#39FF14") -> Color.parseColor("#39FF14")  // Neon green
            Color.parseColor("#00FF9C") -> Color.parseColor("#00FFE0")  // Aqua green
            
            // Yellow/Orange family → Golden Neon
            Color.parseColor("#FFA500") -> Color.parseColor("#FFD700")  // Golden
            Color.parseColor("#FFD93B") -> Color.parseColor("#FFFF00")  // Bright yellow
            
            // Purple/Magenta family → Deep Purple Neon
            Color.parseColor("#B700FF") -> Color.parseColor("#DA70D6")  // Orchid
            Color.parseColor("#FF4DFF") -> Color.parseColor("#FF1493")  // Deep pink
            
            // Default: Vibrant cosmic purple/pink glow
            else -> {
                val r = Color.red(color)
                val g = Color.green(color)
                val b = Color.blue(color)
                
                // Create vibrant neon version with enhanced saturation
                val maxComponent = maxOf(r, g, b)
                val scale = if (maxComponent > 0) 255f / maxComponent else 1f
                
                Color.rgb(
                    minOf((r * 1.4 * scale).toInt(), 255),
                    minOf((g * 1.2 * scale).toInt(), 255),
                    minOf((b * 1.6 * scale).toInt(), 255)  // Enhance blue for cosmic feel
                )
            }
        }
    }

    /**
     * Update completion animation - tracks pulse animation for recently completed paths
     */
    private fun updateCompletionAnimation() {
        if (completedPathAnimationTime > 0f) {
            completedPathAnimationTime -= 16f  // ~60fps
            if (completedPathAnimationTime <= 0f) {
                completedPathAnimationTime = 0f
                completedPathColor = null
            }
        }
    }

    private fun updateHintOverlayAnimation() {
        if (hintOverlayRemainingTime > 0f) {
            hintOverlayRemainingTime -= 16f
            if (hintOverlayRemainingTime <= 0f) {
                hintOverlayRemainingTime = 0f
                hintOverlay = null
            }
        }
    }

    private fun drawHintOverlay(canvas: Canvas) {
        val overlay = hintOverlay ?: return
        if (hintOverlayRemainingTime <= 0f) return
        if (overlay.route.size < 2) return

        val elapsed = (HINT_OVERLAY_DURATION - hintOverlayRemainingTime).coerceAtLeast(0f)
        val progress = (elapsed / HINT_OVERLAY_DURATION).coerceIn(0f, 1f)
        val fade = (1f - (progress * 0.4f)).coerceIn(0f, 1f)
        val pulse = (0.76f + (0.24f * sin(elapsed / 140f))).coerceIn(0.45f, 1f)

        val glowColor = getPremiumGlowColor(overlay.color)

        hintGlowPaint.color = glowColor
        hintGlowPaint.alpha = (130f * pulse * fade).toInt().coerceIn(0, 255)
        hintGlowPaint.strokeWidth = maxOf(9f, cellSize * 0.24f)
        hintGlowPaint.setShadowLayer(maxOf(10f, cellSize * 0.15f), 0f, 0f, glowColor)

        hintLinePaint.color = Color.WHITE
        hintLinePaint.alpha = (235f * fade).toInt().coerceIn(0, 255)
        hintLinePaint.strokeWidth = maxOf(3.2f, cellSize * 0.09f)
        hintLinePaint.pathEffect = DashPathEffect(
            floatArrayOf(maxOf(10f, cellSize * 0.3f), maxOf(6f, cellSize * 0.2f)),
            elapsed / 2.4f
        )

        for (index in 0 until overlay.route.lastIndex) {
            val from = overlay.route[index]
            val to = overlay.route[index + 1]
            val (fromX, fromY) = gridToScreenCoordinates(from.first, from.second)
            val (toX, toY) = gridToScreenCoordinates(to.first, to.second)

            canvas.drawLine(fromX, fromY, toX, toY, hintGlowPaint)
            canvas.drawLine(fromX, fromY, toX, toY, hintLinePaint)
        }

        hintLinePaint.pathEffect = null
        hintGlowPaint.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)

        overlay.route.distinct().forEach { point ->
            drawHintCellOutline(canvas, point.first, point.second, glowColor, pulse, fade)
        }
    }

    private fun drawHintCellOutline(
        canvas: Canvas,
        row: Int,
        col: Int,
        glowColor: Int,
        pulse: Float,
        fade: Float
    ) {
        val spacing = 5f
        val left = gridStartX + col * cellSize + spacing / 2
        val top = gridStartY + row * cellSize + spacing / 2
        val right = left + cellSize - spacing
        val bottom = top + cellSize - spacing
        val radius = 10f

        cellRect.set(left, top, right, bottom)

        hintCellOutlinePaint.color = glowColor
        hintCellOutlinePaint.alpha = (220f * fade).toInt().coerceIn(0, 255)
        hintCellOutlinePaint.strokeWidth = maxOf(3.8f, cellSize * 0.085f)
        hintCellOutlinePaint.setShadowLayer(maxOf(6f, cellSize * 0.1f), 0f, 0f, glowColor)
        canvas.drawRoundRect(cellRect, radius, radius, hintCellOutlinePaint)

        hintCellInnerOutlinePaint.color = Color.WHITE
        hintCellInnerOutlinePaint.alpha = (140f * pulse * fade).toInt().coerceIn(0, 255)
        hintCellInnerOutlinePaint.strokeWidth = maxOf(1.7f, cellSize * 0.04f)
        canvas.drawRoundRect(cellRect, radius, radius, hintCellInnerOutlinePaint)

        hintCellOutlinePaint.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
    }

    /**
     * Trigger completion animation for a path - called when path is completed
     */
    fun triggerCompletionAnimation(color: Int) {
        completedPathColor = color
        completedPathAnimationTime = COMPLETION_ANIMATION_DURATION
        postInvalidateOnAnimation()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!::gameManager.isInitialized || !inputEnabled) return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                return handleTouchDown(event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> {
                return handleTouchMove(event.x, event.y)
            }
            MotionEvent.ACTION_UP -> {
                val handled = handleTouchUp()
                if (handled) {
                    performClick()
                }
                return handled
            }
            MotionEvent.ACTION_CANCEL -> {
                return handleTouchUp()
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pendingPathCompleteTailTone?.let { removeCallbacks(it) }
        pendingPathCompleteTailTone = null
        pathToneGenerator?.release()
        pathToneGenerator = null
        pathCompleteToneGenerator?.release()
        pathCompleteToneGenerator = null
    }

    /**
     * Handle touch down - start a new path from an endpoint
     */
    private fun handleTouchDown(x: Float, y: Float): Boolean {
        if (hintOverlay != null) {
            hintOverlay = null
            hintOverlayRemainingTime = 0f
        }

        lastTouchX = x
        lastTouchY = y

        val (row, col) = screenToGridCoordinates(x, y)
        lastGridRow = row
        lastGridCol = col

        if (row < 0 || col < 0) return false

        val cell = gameManager.board.getCell(row, col) ?: return false

        // Allow starting from either an endpoint OR an existing path cell (resume)
        val touchedColor = when {
            cell.isEndpoint && cell.color != null -> cell.color
            cell.pathColor != null -> cell.pathColor
            else -> null
        }

        if (touchedColor == null) return false

        currentTouchColor = touchedColor
        val path = gameManager.board.getPath(touchedColor) ?: return false

        // Don't allow interaction with locked (completed) paths
        if (path.isLocked) return false

        // Start or continue the path from this point (endpoint or any path cell)
        isPathActive = gameManager.onCellSelected(touchedColor, row, col)
        triggerHaptic(isPathActive)
        if (isPathActive) {
            playConnectTone()
        }
        postInvalidateOnAnimation()
        return true
    }

    /**
     * Handle touch move - interpolate between cells with intelligent backtracking
     * Improved to be more responsive and smooth
     */
    private fun handleTouchMove(x: Float, y: Float): Boolean {
        val activeColor = currentTouchColor ?: return false
        if (!isPathActive) return false

        val (row, col) = screenToGridCoordinates(x, y)

        // Only process if we've moved to a different cell
        if (row == lastGridRow && col == lastGridCol) {
            lastTouchX = x
            lastTouchY = y
            return true
        }

        // Check if path is locked (completed) - don't allow modifications
        val path = gameManager.board.getPath(activeColor) ?: return false
        if (path.isLocked) {
            triggerHaptic(false)
            return false
        }

        // Interpolate along the grid to catch all cells
        val cellsToProcess = interpolatePath(lastGridRow, lastGridCol, row, col)

        // Try to add/remove cells based on the path
        var processedAny = false
        var attemptedAny = false
        for ((r, c) in cellsToProcess) {
            if (r < 0 || c < 0) continue
            attemptedAny = true

            val result = gameManager.onCellSelected(activeColor, r, c)
            if (result) {
                processedAny = true
                lastGridRow = r
                lastGridCol = c
            }
        }

        lastTouchX = x
        lastTouchY = y
        
        if (processedAny) {
            triggerHaptic(true)
            playConnectTone()
            postInvalidateOnAnimation()
        } else if (attemptedAny) {
            triggerHaptic(false)
        }
        
        return true
    }

    private fun playConnectTone() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastConnectToneAtMs < CONNECT_TONE_MIN_INTERVAL_MS) {
            return
        }

        lastConnectToneAtMs = now

        if (pathToneGenerator == null) {
            pathToneGenerator = runCatching {
                ToneGenerator(AudioManager.STREAM_MUSIC, 55)
            }.getOrNull()
        }

        pathToneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 28)
    }

    private fun playPathCompleteTone() {
        if (pathCompleteToneGenerator == null) {
            pathCompleteToneGenerator = runCatching {
                ToneGenerator(AudioManager.STREAM_MUSIC, 85)
            }.getOrNull()
        }

        val generator = pathCompleteToneGenerator ?: return
        pendingPathCompleteTailTone?.let { removeCallbacks(it) }

        // Two-part chime so dot-to-dot completion sounds distinct and satisfying.
        generator.startTone(ToneGenerator.TONE_PROP_ACK, 120)
        val tailTone = Runnable {
            pathCompleteToneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 145)
        }
        pendingPathCompleteTailTone = tailTone
        postDelayed(tailTone, 120L)
    }

    /**
     * Handle touch up - finalize the path interaction
     */
    private fun handleTouchUp(): Boolean {
        isPathActive = false
        postInvalidateOnAnimation()
        return true
    }

    /**
     * Interpolate between two grid positions to find all cells that should be visited
     * Improved algorithm for smoother and more predictable path drawing
     */
    private fun interpolatePath(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int): List<Pair<Int, Int>> {
        val path = mutableListOf<Pair<Int, Int>>()

        if (fromRow < 0 || fromCol < 0) {
            // Starting position - just return target
            path.add(Pair(toRow, toCol))
            return path
        }

        // If we haven't moved, return empty
        if (fromRow == toRow && fromCol == toCol) {
            return path
        }

        val rowDist = toRow - fromRow
        val colDist = toCol - fromCol

        // Get absolute distances
        val absRowDist = kotlin.math.abs(rowDist)
        val absColDist = kotlin.math.abs(colDist)

        // Direction to move
        val rowStep = when {
            rowDist > 0 -> 1
            rowDist < 0 -> -1
            else -> 0
        }

        val colStep = when {
            colDist > 0 -> 1
            colDist < 0 -> -1
            else -> 0
        }

        var currentRow = fromRow
        var currentCol = fromCol

        // Use primary axis (whichever has more distance) then secondary axis
        // This creates more predictable L-shaped or straight paths
        if (absRowDist >= absColDist) {
            // Move rows first
            repeat(absRowDist) {
                currentRow += rowStep
                path.add(Pair(currentRow, currentCol))
            }
            // Then move columns
            repeat(absColDist) {
                currentCol += colStep
                path.add(Pair(currentRow, currentCol))
            }
        } else {
            // Move columns first
            repeat(absColDist) {
                currentCol += colStep
                path.add(Pair(currentRow, currentCol))
            }
            // Then move rows
            repeat(absRowDist) {
                currentRow += rowStep
                path.add(Pair(currentRow, currentCol))
            }
        }

        return path
    }

    /**
     * Convert screen coordinates to grid coordinates
     */
    private fun screenToGridCoordinates(screenX: Float, screenY: Float): Pair<Int, Int> {
        if (cellSize <= 0f) return Pair(-1, -1)

        val col = ((screenX - gridStartX) / cellSize).toInt().coerceIn(0, gameManager.board.cols - 1)
        val row = ((screenY - gridStartY) / cellSize).toInt().coerceIn(0, gameManager.board.rows - 1)
        return Pair(row, col)
    }

    /**
     * Convert grid coordinates to screen coordinates
     */
    private fun gridToScreenCoordinates(row: Int, col: Int): Pair<Float, Float> {
        val screenX = gridStartX + col * cellSize + cellSize / 2
        val screenY = gridStartY + row * cellSize + cellSize / 2
        return Pair(screenX, screenY)
    }

    /**
     * Get the cell size for external calculations
     */
    fun getCellSize(): Float = cellSize

    /**
     * Get grid start position
     */
    fun getGridStartPosition(): Pair<Float, Float> = Pair(gridStartX, gridStartY)

    private fun triggerHaptic(validMove: Boolean) {
        val effect = if (validMove) {
            HapticFeedbackConstants.CLOCK_TICK
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }
        performHapticFeedback(effect)
    }
}
