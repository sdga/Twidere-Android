package org.mariotaku.twidere.view.viewer

import android.content.Context
import android.support.annotation.FloatRange
import android.support.v4.view.ViewCompat
import android.support.v4.widget.ViewDragHelper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup

/**
 * Created by mariotaku on 2017/1/29.
 */
class MediaSwipeCloseContainer(context: Context, attrs: AttributeSet? = null) : ViewGroup(context, attrs) {

    private val dragHelper: ViewDragHelper = ViewDragHelper.create(this, object : ViewDragHelper.Callback() {
        override fun onViewPositionChanged(changedView: View?, left: Int, top: Int, dx: Int, dy: Int) {
            val container = this@MediaSwipeCloseContainer
            container.childTop = top
            container.listener?.onSwipeOffsetChanged(top)
        }

        override fun onViewDragStateChanged(state: Int) {
            val container = this@MediaSwipeCloseContainer
            container.listener?.onSwipeStateChanged(container.dragHelper.viewDragState)
        }

        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            // Only when child can't scroll vertically
            return !child.canScrollVertically(-1) && !child.canScrollVertically(1)
        }

        override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
            val container = this@MediaSwipeCloseContainer
            return top.coerceIn(-container.height, container.height)
        }

        override fun getViewVerticalDragRange(child: View?): Int {
            val container = this@MediaSwipeCloseContainer
            return container.height
        }

        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            val container = this@MediaSwipeCloseContainer
            when {
                yvel > 0 -> {
                    // Settle downward
                    container.dragHelper.settleCapturedViewAt(0, container.height)
                }
                yvel < 0 -> {
                    // Settle upward
                    container.dragHelper.settleCapturedViewAt(0, -container.height)

                }
                else -> when {
                    childTop < -container.height / 2 -> {
                        container.dragHelper.settleCapturedViewAt(0, -container.height)
                    }
                    childTop > container.height / 2 -> {
                        container.dragHelper.settleCapturedViewAt(0, container.height)
                    }
                    else -> {
                        container.dragHelper.settleCapturedViewAt(0, 0)
                    }
                }
            }
            ViewCompat.postInvalidateOnAnimation(container)
        }
    })

    private var childTop: Int = 0

    var listener: Listener? = null

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        measureChildren(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(getDefaultSize(suggestedMinimumWidth, widthMeasureSpec),
                getDefaultSize(suggestedMinimumHeight, heightMeasureSpec))
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != View.GONE) {
                child.layout(0, childTop, child.measuredWidth, childTop + child.measuredHeight)
            }
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (dragHelper.shouldInterceptTouchEvent(ev)) {
            return true
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        dragHelper.processTouchEvent(event)
        return true
    }

    override fun computeScroll() {
        if (dragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this)
        } else if (childTop <= -height || childTop >= height) {
            listener?.onSwipeCloseFinished()
        }
    }

    interface Listener {
        fun onSwipeCloseFinished() {}

        fun onSwipeOffsetChanged(offset: Int) {}

        fun onSwipeStateChanged(state: Int) {}
    }


    var backgroundAlpha: Float
        get() = (background?.alpha ?: 0) / 255f
        set(@FloatRange(from = 0.0, to = 1.0) value) {
            background?.alpha = Math.round(value * 0xFF)
        }
}