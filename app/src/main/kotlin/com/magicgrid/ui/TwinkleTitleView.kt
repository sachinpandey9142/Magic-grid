package com.magicgrid.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Region
import android.graphics.Shader
import android.os.Build
import android.util.AttributeSet
import android.view.animation.BounceInterpolator
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.magicgrid.R
import kotlin.math.min
import kotlin.random.Random

/**
 * TextView that renders a luminous title stroke and triggers random twinkle bursts.
 */
class TwinkleTitleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private val random = Random(System.currentTimeMillis())
    private val density = resources.displayMetrics.density

    private val innerStrokeWidthPx = 2.05f * density
    private val outerStrokeWidthPx = 3.7f * density
    private val dotDropDistancePx = 34f * density
    private val dotRectInsetPx = 0.6f * density
    // BounceInterpolator touches ground multiple times; these local-progress checkpoints
    // align with each visible contact point so sound can sync with every bounce.
    private val dotImpactProgressMarkers = floatArrayOf(0.315f, 0.660f, 0.859f, 0.998f)
    private val impactLeadCompensation = 0.018f

    private val twinkleDelayMinMs = 950L
    private val twinkleDelayMaxMs = 2600L

    private val baseStrokeColor: Int by lazy {
        ContextCompat.getColor(context, R.color.menu_title_glow)
    }
    private val fillBottomColor: Int by lazy {
        ContextCompat.getColor(context, R.color.menu_primary_top)
    }

    private var twinkleStrength = 0f
    private var twinkleEnabled = true
    private var pendingTwinkle: Runnable? = null
    private var twinkleAnimator: ValueAnimator? = null
    private var dotRevealAnimator: ValueAnimator? = null

    private var dotRevealActive = false
    private var dotRevealProgress = 0f
    private var dotCharacterIndices: List<Int> = emptyList()
    private var dotImpactDispatched = emptyArray<BooleanArray>()
    private var onDotImpactListener: ((DotImpactEvent) -> Unit)? = null

    private var fillShader: LinearGradient? = null
    private val dotBounceInterpolator = BounceInterpolator()

    private val dotGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val dotCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val dotEdgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 0.9f * density
    }
    private val dotHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private data class DotSpec(
        val centerX: Float,
        val targetCenterY: Float,
        val radius: Float,
        val hideRect: RectF
    )

    data class DotImpactEvent(
        val dotIndex: Int,
        val impactIndex: Int,
        val isFinalImpact: Boolean
    )

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (twinkleEnabled) {
            scheduleNextTwinkle()
        }
    }

    override fun onDetachedFromWindow() {
        clearCallbacksAndAnimators()
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        fillShader = null
    }

    override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
        super.onTextChanged(text, start, before, count)
        dotCharacterIndices = findDotIndices(text?.toString().orEmpty())
    }

    fun pauseTwinkle() {
        twinkleEnabled = false
        pendingTwinkle?.let { removeCallbacks(it) }
        pendingTwinkle = null
        twinkleAnimator?.cancel()
        twinkleAnimator = null
        twinkleStrength = 0f
        invalidate()
    }

    fun resumeTwinkle() {
        if (twinkleEnabled) return
        twinkleEnabled = true
        scheduleNextTwinkle()
    }

    fun prepareDotBounceIntro() {
        dotRevealAnimator?.cancel()

        dotCharacterIndices = findDotIndices(text?.toString().orEmpty())
        dotRevealActive = dotCharacterIndices.isNotEmpty()
        dotRevealProgress = 0f
        dotImpactDispatched = Array(dotCharacterIndices.size) {
            BooleanArray(dotImpactProgressMarkers.size)
        }

        invalidate()
    }

    fun setOnDotImpactListener(listener: ((DotImpactEvent) -> Unit)?) {
        onDotImpactListener = listener
    }

    fun playDotBounceIntro(onEnd: (() -> Unit)? = null) {
        if (!dotRevealActive || dotCharacterIndices.isEmpty()) {
            onEnd?.invoke()
            return
        }

        dotRevealAnimator?.cancel()
        dotRevealAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 680L

            addUpdateListener { animator ->
                dotRevealProgress = animator.animatedValue as Float
                dispatchDotImpactEvents(dotRevealProgress)
                invalidate()
            }

            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    dotRevealProgress = 1f
                    dotRevealActive = false
                    invalidate()
                    onEnd?.invoke()
                }

                override fun onAnimationCancel(animation: Animator) {
                    dotRevealProgress = 1f
                    dotRevealActive = false
                    invalidate()
                }
            })

            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        val titleText = text?.toString().orEmpty()
        if (titleText.isEmpty()) {
            super.onDraw(canvas)
            return
        }

        val originalStyle = paint.style
        val originalStrokeWidth = paint.strokeWidth
        val originalColor = paint.color
        val originalShader = paint.shader
        val originalAlpha = paint.alpha

        val glowMix = (0.28f + (twinkleStrength * 0.72f)).coerceIn(0f, 1f)
        val innerStrokeColor = blendColor(baseStrokeColor, Color.WHITE, glowMix)
        val outerStrokeColor = adjustAlpha(innerStrokeColor, 0.34f + (twinkleStrength * 0.38f))

        if (dotRevealActive) {
            val dotSpecs = calculateDotSpecs(titleText)
            if (dotSpecs.isEmpty()) {
                drawStyledTitle(canvas, innerStrokeColor, outerStrokeColor, 1f)
            } else {
                canvas.save()
                clipOutRects(canvas, dotSpecs.map { it.hideRect })
                drawStyledTitle(canvas, innerStrokeColor, outerStrokeColor, 1f)
                canvas.restore()

                drawBouncingDots(canvas, dotSpecs)
            }
        } else {
            drawStyledTitle(canvas, innerStrokeColor, outerStrokeColor, 1f)
        }

        paint.style = originalStyle
        paint.strokeWidth = originalStrokeWidth
        paint.color = originalColor
        paint.shader = originalShader
        paint.alpha = originalAlpha
    }

    private fun drawStyledTitle(
        canvas: Canvas,
        innerStrokeColor: Int,
        outerStrokeColor: Int,
        alphaFraction: Float
    ) {
        val safeAlphaFraction = alphaFraction.coerceIn(0f, 1f)
        val alphaInt = (safeAlphaFraction * 255).toInt().coerceIn(0, 255)

        paint.style = Paint.Style.STROKE
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeMiter = 12f
        paint.shader = null
        paint.strokeWidth = outerStrokeWidthPx
        paint.color = outerStrokeColor
        paint.alpha = alphaInt
        super.onDraw(canvas)

        paint.style = Paint.Style.STROKE
        paint.shader = null
        paint.strokeWidth = innerStrokeWidthPx
        paint.color = innerStrokeColor
        paint.alpha = alphaInt
        super.onDraw(canvas)

        paint.style = Paint.Style.FILL
        paint.strokeWidth = 0f
        paint.shader = getOrCreateFillShader()
        paint.alpha = alphaInt
        super.onDraw(canvas)
    }

    private fun getOrCreateFillShader(): LinearGradient {
        val cached = fillShader
        if (cached != null) return cached

        val shader = LinearGradient(
            0f,
            0f,
            0f,
            height.toFloat().coerceAtLeast(1f),
            Color.WHITE,
            fillBottomColor,
            Shader.TileMode.CLAMP
        )
        fillShader = shader
        return shader
    }

    private fun calculateDotSpecs(textValue: String): List<DotSpec> {
        if (dotCharacterIndices.isEmpty()) return emptyList()

        val baselineY = baseline.toFloat()
        val fm = paint.fontMetrics
        val textTop = baselineY + fm.ascent
        val textHeight = fm.descent - fm.ascent
        val dotTop = textTop + (textHeight * 0.02f)
        val dotBottom = textTop + (textHeight * 0.31f)

        return dotCharacterIndices.mapNotNull { index ->
            if (index !in textValue.indices) {
                return@mapNotNull null
            }

            val prefixWidth = paint.measureText(textValue, 0, index)
            val charWidth = paint.measureText(textValue, index, index + 1)

            val left = paddingLeft + prefixWidth + dotRectInsetPx
            val right = (paddingLeft + prefixWidth + charWidth - dotRectInsetPx)
                .coerceAtLeast(left + 1f)

            val dotRect = RectF(left, dotTop, right, dotBottom)
            val radius = (min(dotRect.width(), dotRect.height()) * 0.5f).coerceAtLeast(1f)
            val hideRect = RectF(
                dotRect.left - (0.9f * density),
                dotRect.top - (0.9f * density),
                dotRect.right + (0.9f * density),
                dotRect.bottom + (0.9f * density)
            )

            DotSpec(
                centerX = dotRect.centerX(),
                targetCenterY = dotRect.centerY(),
                radius = radius,
                hideRect = hideRect
            )
        }
    }

    private fun drawBouncingDots(canvas: Canvas, dotSpecs: List<DotSpec>) {
        dotSpecs.forEachIndexed { index, dot ->
            val startOffset = index * 0.11f
            val localProgress = if (dotRevealProgress <= startOffset) {
                0f
            } else {
                ((dotRevealProgress - startOffset) / (1f - startOffset)).coerceIn(0f, 1f)
            }

            if (localProgress <= 0f) return@forEachIndexed

            val bounceProgress = dotBounceInterpolator.getInterpolation(localProgress)
            val centerY = dot.targetCenterY - dotDropDistancePx + (dotDropDistancePx * bounceProgress)
            val alpha = (localProgress * 1.8f).coerceIn(0f, 1f)

            dotGlowPaint.color = adjustAlpha(baseStrokeColor, 0.27f * alpha)
            dotCorePaint.color = adjustAlpha(blendColor(Color.WHITE, fillBottomColor, 0.2f), alpha)
            dotEdgePaint.color = adjustAlpha(Color.WHITE, 0.78f * alpha)
            dotHighlightPaint.color = adjustAlpha(Color.WHITE, 0.92f * alpha)

            canvas.drawCircle(dot.centerX, centerY, dot.radius * 1.55f, dotGlowPaint)
            canvas.drawCircle(dot.centerX, centerY, dot.radius, dotCorePaint)
            canvas.drawCircle(dot.centerX, centerY, dot.radius, dotEdgePaint)
            canvas.drawCircle(
                dot.centerX - (dot.radius * 0.28f),
                centerY - (dot.radius * 0.32f),
                dot.radius * 0.3f,
                dotHighlightPaint
            )
        }
    }

    private fun dispatchDotImpactEvents(globalProgress: Float) {
        if (dotImpactDispatched.isEmpty()) return

        for (dotIndex in dotImpactDispatched.indices) {
            val dispatchedForDot = dotImpactDispatched[dotIndex]

            val startOffset = dotIndex * 0.11f
            val localProgress = if (globalProgress <= startOffset) {
                0f
            } else {
                ((globalProgress - startOffset) / (1f - startOffset)).coerceIn(0f, 1f)
            }
            val compensatedProgress = (localProgress + impactLeadCompensation).coerceIn(0f, 1f)

            for (impactIndex in dotImpactProgressMarkers.indices) {
                if (dispatchedForDot[impactIndex]) continue

                if (compensatedProgress >= dotImpactProgressMarkers[impactIndex]) {
                    dispatchedForDot[impactIndex] = true
                    val isFinalImpact =
                        dotIndex == (dotImpactDispatched.size - 1) &&
                            impactIndex == (dotImpactProgressMarkers.size - 1)
                    onDotImpactListener?.invoke(
                        DotImpactEvent(
                            dotIndex = dotIndex,
                            impactIndex = impactIndex,
                            isFinalImpact = isFinalImpact
                        )
                    )
                }
            }
        }
    }

    private fun clipOutRects(canvas: Canvas, rects: List<RectF>) {
        for (rect in rects) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                canvas.clipOutRect(rect)
            } else {
                @Suppress("DEPRECATION")
                canvas.clipRect(rect, Region.Op.DIFFERENCE)
            }
        }
    }

    private fun scheduleNextTwinkle() {
        if (!twinkleEnabled || !isAttachedToWindow) return

        pendingTwinkle?.let { removeCallbacks(it) }

        val delay = random.nextLong(twinkleDelayMinMs, twinkleDelayMaxMs)
        val runnable = Runnable { startTwinkleBurst() }
        pendingTwinkle = runnable
        postDelayed(runnable, delay)
    }

    private fun startTwinkleBurst() {
        if (!twinkleEnabled || !isAttachedToWindow) return

        twinkleAnimator?.cancel()
        twinkleAnimator = ValueAnimator.ofFloat(0f, 1f, 0.35f, 0f).apply {
            duration = 700L
            addUpdateListener { animator ->
                twinkleStrength = animator.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    twinkleStrength = 0f
                    invalidate()
                    scheduleNextTwinkle()
                }

                override fun onAnimationCancel(animation: Animator) {
                    twinkleStrength = 0f
                    invalidate()
                }
            })
            start()
        }
    }

    private fun clearCallbacksAndAnimators() {
        pendingTwinkle?.let { removeCallbacks(it) }
        pendingTwinkle = null

        twinkleAnimator?.cancel()
        twinkleAnimator = null

        dotRevealAnimator?.cancel()
        dotRevealAnimator = null
    }

    private fun findDotIndices(value: String): List<Int> {
        if (value.isEmpty()) return emptyList()

        val dotIndices = mutableListOf<Int>()
        value.forEachIndexed { index, char ->
            if (char == 'i') {
                dotIndices.add(index)
            }
        }
        return dotIndices
    }

    private fun blendColor(from: Int, to: Int, ratio: Float): Int {
        val amount = ratio.coerceIn(0f, 1f)

        val alpha = Color.alpha(from) + ((Color.alpha(to) - Color.alpha(from)) * amount).toInt()
        val red = Color.red(from) + ((Color.red(to) - Color.red(from)) * amount).toInt()
        val green = Color.green(from) + ((Color.green(to) - Color.green(from)) * amount).toInt()
        val blue = Color.blue(from) + ((Color.blue(to) - Color.blue(from)) * amount).toInt()

        return Color.argb(alpha, red, green, blue)
    }

    private fun adjustAlpha(color: Int, alphaFraction: Float): Int {
        val finalAlpha = (Color.alpha(color) * alphaFraction.coerceIn(0f, 1f)).toInt()
        return Color.argb(finalAlpha, Color.red(color), Color.green(color), Color.blue(color))
    }
}
