package com.magicgrid.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.magicgrid.R
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

    private val baseGlowRadiusPx = 5f * resources.displayMetrics.density
    private val maxGlowBoostPx = 13f * resources.displayMetrics.density
    private val strokeWidthPx = 1.7f * resources.displayMetrics.density
    private val maxStrokeBoostPx = 1.1f * resources.displayMetrics.density

    private val twinkleDelayMinMs = 900L
    private val twinkleDelayMaxMs = 2500L

    private val baseStrokeColor: Int by lazy {
        ContextCompat.getColor(context, R.color.menu_title_glow)
    }

    private var twinkleStrength = 0f
    private var twinkleEnabled = true
    private var pendingTwinkle: Runnable? = null
    private var twinkleAnimator: ValueAnimator? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (twinkleEnabled) {
            scheduleNextTwinkle()
        }
    }

    override fun onDetachedFromWindow() {
        clearTwinkleCallbacks()
        super.onDetachedFromWindow()
    }

    fun pauseTwinkle() {
        twinkleEnabled = false
        clearTwinkleCallbacks()
        twinkleStrength = 0f
        invalidate()
    }

    fun resumeTwinkle() {
        if (twinkleEnabled) return
        twinkleEnabled = true
        scheduleNextTwinkle()
    }

    override fun onDraw(canvas: Canvas) {
        if (text.isNullOrEmpty()) {
            super.onDraw(canvas)
            return
        }

        val originalStyle = paint.style
        val originalStrokeWidth = paint.strokeWidth
        val originalColor = currentTextColor

        val glowMix = (0.25f + (twinkleStrength * 0.75f)).coerceIn(0f, 1f)
        val strokeColor = blendColor(baseStrokeColor, Color.WHITE, glowMix)

        paint.style = Paint.Style.STROKE
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeMiter = 10f
        paint.strokeWidth = strokeWidthPx + (twinkleStrength * maxStrokeBoostPx)
        paint.setShadowLayer(
            baseGlowRadiusPx + (twinkleStrength * maxGlowBoostPx),
            0f,
            0f,
            strokeColor
        )
        paint.color = strokeColor
        super.onDraw(canvas)

        paint.clearShadowLayer()
        paint.style = Paint.Style.FILL
        paint.color = originalColor
        super.onDraw(canvas)

        paint.style = originalStyle
        paint.strokeWidth = originalStrokeWidth
        paint.color = originalColor
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
            duration = 640L
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

    private fun clearTwinkleCallbacks() {
        pendingTwinkle?.let { removeCallbacks(it) }
        pendingTwinkle = null

        twinkleAnimator?.cancel()
        twinkleAnimator = null
    }

    private fun blendColor(from: Int, to: Int, ratio: Float): Int {
        val amount = ratio.coerceIn(0f, 1f)

        val alpha = Color.alpha(from) + ((Color.alpha(to) - Color.alpha(from)) * amount).toInt()
        val red = Color.red(from) + ((Color.red(to) - Color.red(from)) * amount).toInt()
        val green = Color.green(from) + ((Color.green(to) - Color.green(from)) * amount).toInt()
        val blue = Color.blue(from) + ((Color.blue(to) - Color.blue(from)) * amount).toInt()

        return Color.argb(alpha, red, green, blue)
    }
}
