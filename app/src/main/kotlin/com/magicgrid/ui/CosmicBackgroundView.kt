package com.magicgrid.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin
import kotlin.random.Random

/**
 * Premium cosmic background with vibrant glittering particles and dynamic glow effects.
 * Creates a magical, sparkling space/galaxy feel that feels alive and premium.
 * 
 * Enhanced Features:
 * - Deep gradient with radial center lighting and vignette
 * - 70+ particles in 3 layers (small, medium, bright sparkles)
 * - Dynamic twinkling: opacity (0.2→1.0) + size pulse (0.8→1.2)
 * - setShadowLayer() glow for real glowing effect
 * - Multiple animation speeds for depth
 * - Soft blue (#3A8DFF), pink (#FF4DFF), and white sparkles
 * - Optimized for 60fps smooth animation
 * - Visually engaging, magical aesthetic
 */
class CosmicBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Particle layers - multiple sizes for depth
    private val smallParticles = mutableListOf<GlitterParticle>()     // Layer 1: very subtle
    private val mediumParticles = mutableListOf<GlitterParticle>()    // Layer 2: visible glow
    private val brightSparkles = mutableListOf<GlitterParticle>()     // Layer 3: strong twinkle

    // Paint objects
    private val gradientPaint = Paint().apply {
        style = Paint.Style.FILL
    }

    private val particlePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        strokeWidth = 0.5f
    }

    private val vignetteOverlayPaint = Paint().apply {
        style = Paint.Style.FILL
    }

    private val radialGlowPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    // Overlay paints for soft bloom glow
    private val blueBloomPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val pinkBloomPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    // Animation tracking
    private var animationTime = 0f
    private var isInitialized = false

    // Rare sparkle effect tracking
    private data class RareSparkleEvent(
        val particleIndex: Int,        // Index in particle list (0-72)
        val startTime: Float,          // When sparkle started (animationTime)
        val duration: Float            // Duration in ms (500-1000ms = 8-16 frames @ 60fps)
    )
    private val activeSparkles = mutableListOf<RareSparkleEvent>()
    private var nextSparkleTime = 0f  // Time when next sparkle should trigger
    private var sparkleRandomInterval = 0f  // Random interval until next sparkle
    private val sparkleRandom = Random(System.currentTimeMillis())

    init {
        startAnimation()
        initializeSparkleTimer()
    }

    /**
     * Enhanced particle class with size pulse and glow tracking
     */
    private data class GlitterParticle(
        val x: Float,
        val y: Float,
        val baseSize: Float,        // Base radius (used for sizing)
        val brightness: Float,      // Base brightness multiplier
        val twinkleSpeed: Float,    // Animation speed (affects both opacity and size)
        val twinklePhase: Float,    // Phase offset for variation
        val color: Int,             // Particle color
        val glowRadius: Float,      // Glow halo size
        val layer: ParticleLayer,   // Which layer (affects rendering)
        val driftX: Float,          // Subtle horizontal drift speed
        val driftY: Float           // Subtle vertical drift speed
    )

    enum class ParticleLayer {
        SMALL,      // Layer 1: tiny, slow, subtle
        MEDIUM,     // Layer 2: regular, medium speed
        BRIGHT      // Layer 3: larger, faster, bright twinkle
    }

    /**
     * Initialize the random timer for rare sparkles
     */
    private fun initializeSparkleTimer() {
        // Next sparkle in 3-8 seconds (180-480 frames @ 60fps)
        sparkleRandomInterval = sparkleRandom.nextFloat() * 300f + 180f
        nextSparkleTime = animationTime + sparkleRandomInterval
    }

    /**
     * Trigger a rare sparkle on select particles
     */
    private fun triggerRareSparkle() {
        val totalParticles = smallParticles.size + mediumParticles.size + brightSparkles.size
        if (totalParticles < 2) return  // Need at least 2 particles
        
        // Select 2-4 random particles to sparkle
        val sparkleCount = sparkleRandom.nextInt(2, 5)  // 2-4 particles
        repeat(sparkleCount) {
            val randomParticleIndex = sparkleRandom.nextInt(totalParticles)
            
            // Duration: 0.5-1 second (30-60 frames @ 60fps)
            val durationFrames = sparkleRandom.nextFloat() * 30f + 30f
            val duration = durationFrames * 16.67f  // Convert to ms
            
            // Add to active sparkles
            activeSparkles.add(
                RareSparkleEvent(
                    particleIndex = randomParticleIndex,
                    startTime = animationTime,
                    duration = duration
                )
            )
        }
        
        // Schedule next sparkle
        initializeSparkleTimer()
    }

    /**
     * Update rare sparkle events
     */
    private fun updateRareSparkles() {
        // Trigger new sparkle if it's time
        if (animationTime >= nextSparkleTime) {
            triggerRareSparkle()
        }
        
        // Remove expired sparkles
        activeSparkles.removeAll { sparkle ->
            (animationTime - sparkle.startTime) > sparkle.duration
        }
    }

    /**
     * Check if a particle (by global index) is currently sparkling
     * Returns brightness boost factor (0.0-1.0)
     */
    private fun getSparkleBoost(globalParticleIndex: Int): Float {
        for (sparkle in activeSparkles) {
            if (sparkle.particleIndex == globalParticleIndex) {
                // Calculate progress through sparkle duration (0.0 to 1.0)
                val elapsed = animationTime - sparkle.startTime
                val progress = (elapsed / sparkle.duration).coerceIn(0f, 1f)
                
                // Fade in and out smoothly: triangle wave
                return if (progress < 0.5f) {
                    progress * 2f  // Fade in: 0 → 1
                } else {
                    (1f - progress) * 2f  // Fade out: 1 → 0
                }
            }
        }
        return 0f  // Not sparkling
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        if (!isInitialized) {
            initializeParticles()
            isInitialized = true
        }
    }

    /**
     * Initialize glittering particles in 3 layers for depth and visual interest
     */
    private fun initializeParticles() {
        smallParticles.clear()
        mediumParticles.clear()
        brightSparkles.clear()

        val random = Random(42)

        // LAYER 1: 45 small, subtle particles (very slow animation)
        repeat(45) {
            addParticle(
                x = random.nextFloat() * width,
                y = random.nextFloat() * height,
                baseSize = random.nextFloat() * 0.5f + 0.2f,  // 0.2-0.7px
                brightness = random.nextFloat() * 0.3f + 0.2f,  // 0.2-0.5 (reduced)
                twinkleSpeed = random.nextFloat() * 0.1f + 0.05f,  // 0.05-0.15 (natural star twinkling, 2.4-7.2 sec)
                colorType = random.nextInt(100),
                glowRadius = 1.5f,
                layer = ParticleLayer.SMALL,
                random = random
            )
        }

        // LAYER 2: 20 medium, visible particles (medium animation)
        repeat(20) {
            addParticle(
                x = random.nextFloat() * width,
                y = random.nextFloat() * height,
                baseSize = random.nextFloat() * 0.8f + 0.6f,  // 0.6-1.4px
                brightness = random.nextFloat() * 0.4f + 0.3f,  // 0.3-0.7 (reduced)
                twinkleSpeed = random.nextFloat() * 0.1f + 0.08f,  // 0.08-0.18 (natural twinkling, 1.7-4.5 sec)
                colorType = random.nextInt(100),
                glowRadius = 2.5f,
                layer = ParticleLayer.MEDIUM,
                random = random
            )
        }

        // LAYER 3: 8 bright sparkles (medium-slow, strong twinkle)
        repeat(8) {
            addParticle(
                x = random.nextFloat() * width,
                y = random.nextFloat() * height,
                baseSize = random.nextFloat() * 1.2f + 0.9f,  // 0.9-2.1px
                brightness = random.nextFloat() * 0.3f + 0.5f,  // 0.5-0.8 (reduced)
                twinkleSpeed = random.nextFloat() * 0.1f + 0.1f,  // 0.1-0.2 (natural twinkling, 1.8-3.6 sec)
                colorType = random.nextInt(100),
                glowRadius = 3.5f,
                layer = ParticleLayer.BRIGHT,
                random = random
            )
        }
    }

    /**
     * Helper to add a particle to the appropriate layer
     */
    private fun addParticle(
        x: Float, y: Float, baseSize: Float, brightness: Float, twinkleSpeed: Float,
        colorType: Int, glowRadius: Float, layer: ParticleLayer, random: Random
    ) {
        val phase = random.nextFloat() * 360f
        
        // Vibrant color distribution
        val color = when {
            colorType < 65 -> Color.parseColor("#FFFFFF")      // White sparkles (65%)
            colorType < 85 -> Color.parseColor("#3A8DFF")      // Soft blue (20%)
            else -> Color.parseColor("#FF4DFF")                 // Pink glow (15%)
        }

        // Very subtle drift (adds life without being noticeable)
        val driftX = (random.nextFloat() - 0.5f) * 0.08f  // ±0.04 pixels per frame
        val driftY = (random.nextFloat() - 0.5f) * 0.08f  // ±0.04 pixels per frame

        val particle = GlitterParticle(
            x = x,
            y = y,
            baseSize = baseSize,
            brightness = brightness,
            twinkleSpeed = twinkleSpeed,
            twinklePhase = phase,
            color = color,
            glowRadius = glowRadius,
            layer = layer,
            driftX = driftX,
            driftY = driftY
        )

        when (layer) {
            ParticleLayer.SMALL -> smallParticles.add(particle)
            ParticleLayer.MEDIUM -> mediumParticles.add(particle)
            ParticleLayer.BRIGHT -> brightSparkles.add(particle)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Layer 1: Gradient base
        drawCosmicGradient(canvas)

        // Layer 2: Radial center glow
        drawRadialGlow(canvas)

        // Layer 3: Vignette edges
        drawVignette(canvas)

        // Layer 4: Soft bloom glows
        drawSoftBlooms(canvas)

        // Layer 5: Glittering particles (in order: small → medium → bright)
        drawGlitteringParticles(canvas)

        // Continue animation
        postInvalidateOnAnimation()
    }

    /**
     * Draw the main cosmic gradient
     */
    private fun drawCosmicGradient(canvas: Canvas) {
        val gradient = LinearGradient(
            0f, 0f,
            0f, height.toFloat(),
            intArrayOf(
                Color.parseColor("#1A0A3E"),
                Color.parseColor("#2A0A5E"),
                Color.parseColor("#3B1A7E"),
                Color.parseColor("#1F0A40"),
                Color.parseColor("#0F0620")
            ),
            floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f),
            Shader.TileMode.CLAMP
        )
        
        gradientPaint.shader = gradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), gradientPaint)
    }

    /**
     * Add radial glow in center for depth
     */
    private fun drawRadialGlow(canvas: Canvas) {
        val centerX = width * 0.5f
        val centerY = height * 0.45f
        val glowRadius = width * 0.7f
        
        val radialGradient = RadialGradient(
            centerX, centerY, glowRadius,
            intArrayOf(
                Color.parseColor("#7B3AAA33"),
                Color.parseColor("#00000000")
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        
        radialGlowPaint.shader = radialGradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), radialGlowPaint)
    }

    /**
     * Add vignette effect to darken edges
     */
    private fun drawVignette(canvas: Canvas) {
        val vignetteGradient = RadialGradient(
            width * 0.5f, height * 0.5f,
            kotlin.math.max(width, height) * 0.7f,
            intArrayOf(
                Color.parseColor("#00000000"),
                Color.parseColor("#4D000000")
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        
        vignetteOverlayPaint.shader = vignetteGradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), vignetteOverlayPaint)
    }

    /**
     * Draw soft bloom glow areas that pulse gently
     */
    private fun drawSoftBlooms(canvas: Canvas) {
        // Blue bloom (top-right)
        val blueFade = 0.15f + 0.1f * sin((animationTime * 0.0003f).toDouble()).toFloat()
        blueBloomPaint.color = Color.parseColor("#3A8DFF")
        blueBloomPaint.alpha = (blueFade * 60).toInt()
        canvas.drawCircle(width * 0.75f, height * 0.25f, width * 0.55f, blueBloomPaint)

        // Pink bloom (bottom-left)
        val pinkFade = 0.1f + 0.08f * sin((animationTime * 0.0004f + 180).toDouble()).toFloat()
        pinkBloomPaint.color = Color.parseColor("#FF4DFF")
        pinkBloomPaint.alpha = (pinkFade * 50).toInt()
        canvas.drawCircle(width * 0.25f, height * 0.75f, width * 0.5f, pinkBloomPaint)
    }

    /**
     * Draw all glittering particles with twinkling and glow
     * Uses shadow layer for realistic glow effect
     */
    private fun drawGlitteringParticles(canvas: Canvas) {
        // Update rare sparkle effects
        updateRareSparkles()
        
        // Draw in order: small → medium → bright (layering)
        // Track global particle index for sparkle lookup
        var particleIndex = 0
        particleIndex = drawParticleLayer(canvas, smallParticles, particleIndex)
        particleIndex = drawParticleLayer(canvas, mediumParticles, particleIndex)
        drawParticleLayer(canvas, brightSparkles, particleIndex)

        // Update animation time
        animationTime += 16f  // ~60fps
    }

    /**
     * Draw a single layer of particles with full twinkling and glow
     * Returns the next global particle index
     */
    private fun drawParticleLayer(canvas: Canvas, particles: List<GlitterParticle>, startIndex: Int): Int {
        var globalIndex = startIndex
        
        for (particle in particles) {
            // Calculate animation based on time and speed
            val normalizedTime = (animationTime * particle.twinkleSpeed) % 360f
            val phase = normalizedTime + particle.twinklePhase
            val pulseFactor = 0.5f + 0.5f * sin((phase * Math.PI / 180.0)).toFloat()

            // Opacity: 0.3 → 0.9 range (more natural, less extreme)
            var opacity = 0.3f + (pulseFactor * 0.6f)
            
            // Apply rare sparkle boost if active
            val sparkleBoost = getSparkleBoost(globalIndex)
            if (sparkleBoost > 0f) {
                // Add subtle brightness boost: +0.15 at peak
                opacity = (opacity + sparkleBoost * 0.15f).coerceIn(0f, 1f)
            }
            
            val alpha = (opacity * 255).toInt()

            // Size pulse: 0.9 → 1.1 range (very subtle)
            val sizePulse = 0.9f + (pulseFactor * 0.2f)
            val currentSize = particle.baseSize * sizePulse

            // Set up particle rendering
            particlePaint.color = particle.color
            particlePaint.alpha = alpha

            // Add glow effect using shadow layer
            val glowColor = when {
                particle.layer == ParticleLayer.BRIGHT -> {
                    // Bright sparkles get stronger glow
                    particle.color  // Same color glow
                }
                particle.layer == ParticleLayer.MEDIUM -> {
                    // Medium particles get subtle glow
                    particle.color
                }
                else -> {
                    // Small particles get minimal glow
                    particle.color
                }
            }

            val glowAlpha = when {
                particle.layer == ParticleLayer.BRIGHT -> (opacity * 100).toInt()   // Reduced from 150
                particle.layer == ParticleLayer.MEDIUM -> (opacity * 50).toInt()   // Reduced from 80
                else -> (opacity * 25).toInt()                                     // Reduced from 40
            }

            // Calculate drift position with very subtle movement
            val driftedX = particle.x + sin((animationTime * particle.driftX * 0.01f) * Math.PI / 180.0).toFloat() * 0.5f
            val driftedY = particle.y + sin((animationTime * particle.driftY * 0.01f) * Math.PI / 180.0).toFloat() * 0.5f

            // Draw with shadow/glow for premium effect (softer blur with improved blending)
            particlePaint.setShadowLayer(
                particle.glowRadius * pulseFactor * 0.8f,  // Softer glow (reduced from full radius)
                0f, 0f,
                Color.argb(glowAlpha, Color.red(glowColor), Color.green(glowColor), Color.blue(glowColor))
            )

            // Draw the particle at drifted position
            canvas.drawCircle(driftedX, driftedY, currentSize, particlePaint)
            
            globalIndex++
        }

        // Clear shadow layer after drawing
        particlePaint.clearShadowLayer()
        
        return globalIndex
    }

    /**
     * Start the animation loop
     */
    private fun startAnimation() {
        postInvalidateOnAnimation()
    }
}

