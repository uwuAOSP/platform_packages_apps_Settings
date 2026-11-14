/* SPDX-License-Identifier: Apache-2.0 */
package com.android.settings.spa.about

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import com.android.settings.R
import java.util.WeakHashMap
import kotlin.random.Random

internal object KtmBeanRain {
    private class State(var taps: Int = 0, var last: Long = 0, var rain: Rain? = null)
    private val states = WeakHashMap<Activity, State>()
    private fun activity(context: Context): Activity? = when (context) {
        is Activity -> context
        is ContextWrapper -> activity(context.baseContext)
        else -> null
    }
    fun tap(context: Context) {
        val host = activity(context) ?: return
        val state = states.getOrPut(host) { State() }
        val now = SystemClock.uptimeMillis()
        state.taps = if (now - state.last > 1500) 1 else state.taps + 1
        state.last = now
        if (state.taps < 7) return
        state.taps = 0
        if (!ValueAnimator.areAnimatorsEnabled() || state.rain != null) {
            return
        }
        val root = host.window.decorView as? ViewGroup ?: return
        if (root.width == 0 || root.height == 0) return
        val rain = runCatching { Rain(host, root) }.getOrNull() ?: return
        state.rain = rain
        rain.finish = { if (state.rain === rain) state.rain = null }
        root.addView(rain, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        rain.bringToFront()
        rain.scheduleStart()
    }
    fun dismiss(context: Context) {
        val host = activity(context) ?: return
        states.remove(host)?.rain?.stop()
    }
    private class Rain(context: Context, private val root: ViewGroup) : View(context) {
        private data class Bean(val x: Float, val size: Float, val delay: Float, val spin: Float, val lifetime: Float)
        private val random = Random(SystemClock.uptimeMillis())
        private val beans = List(30) {
            Bean(random.nextFloat(), 56f + random.nextFloat() * 56f,
                random.nextFloat() * 0.50f, (random.nextFloat() - 0.5f) * 300f,
                0.38f + random.nextFloat() * 0.12f)
        }
        private val image: Drawable = ImageDecoder.decodeDrawable(
            ImageDecoder.createSource(resources, R.raw.ktm_bean))
        private val density = resources.displayMetrics.density
        private var progress = 0f
        private var ended = false
        private var started = false
        var finish: (() -> Unit)? = null
        private val startTask = Runnable { start() }
        private val layoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (width > 0 && height > 0) start()
        }
        fun scheduleStart() {
            if (ended) return
            addOnLayoutChangeListener(layoutListener)
            post(startTask)
        }
        private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 6500
            interpolator = LinearInterpolator()
            addUpdateListener { progress = it.animatedValue as Float; invalidate() }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) { stop() }
            })
        }
        init {
            setWillNotDraw(false)
            isClickable = false
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
            image.callback = this
        }
        fun start() {
            if (started || ended || !isAttachedToWindow || width <= 0 || height <= 0) return
            started = true
            removeCallbacks(startTask)
            removeOnLayoutChangeListener(layoutListener)
            (image as? AnimatedImageDrawable)?.start()
            animator.start()
        }
        fun stop() {
            if (ended) return
            ended = true
            removeCallbacks(startTask)
            removeOnLayoutChangeListener(layoutListener)
            animator.removeAllUpdateListeners()
            animator.removeAllListeners()
            animator.cancel()
            (image as? AnimatedImageDrawable)?.stop()
            image.callback = null
            root.removeView(this)
            val onFinish = finish
            finish = null
            onFinish?.invoke()
        }
        override fun verifyDrawable(who: Drawable) = who === image || super.verifyDrawable(who)
        override fun onDetachedFromWindow() { stop(); super.onDetachedFromWindow() }
        override fun onDraw(canvas: Canvas) {
            for (bean in beans) {
                val p = (progress - bean.delay) / bean.lifetime
                if (p < 0f || p > 1f) continue
                val size = bean.size * density
                val x = bean.x * (width - size).coerceAtLeast(0f)
                val y = -size + p * (height + size * 2)
                val saved = canvas.save()
                canvas.translate(x, y)
                canvas.rotate(bean.spin * p, size / 2, size / 2)
                image.setBounds(0, 0, size.toInt(), size.toInt())
                image.draw(canvas)
                canvas.restoreToCount(saved)
            }
        }
    }
}
