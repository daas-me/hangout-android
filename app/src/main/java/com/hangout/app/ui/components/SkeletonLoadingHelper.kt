package com.hangout.app.ui.components

import android.animation.ObjectAnimator
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator

/**
 * Helper class to manage skeleton loading animations and visibility
 */
object SkeletonLoadingHelper {

    /**
     * Start shimmer animation on a view and all its children
     */
    fun startShimmer(skeletonView: View) {
        if (skeletonView is ViewGroup) {
            for (i in 0 until skeletonView.childCount) {
                val child = skeletonView.getChildAt(i)
                if (child.tag == "skeleton_item") {
                    startShimmerOnView(child)
                } else if (child is ViewGroup) {
                    startShimmer(child)
                }
            }
        }
    }

    /**
     * Stop shimmer animation on a view
     */
    fun stopShimmer(skeletonView: View) {
        if (skeletonView is ViewGroup) {
            for (i in 0 until skeletonView.childCount) {
                val child = skeletonView.getChildAt(i)
                child.animation?.cancel()
                if (child is ViewGroup) {
                    stopShimmer(child)
                }
            }
        }
    }

    private fun startShimmerOnView(view: View) {
        val shimmerAnimation = ObjectAnimator.ofFloat(view, "alpha", 0.3f, 1f, 0.3f).apply {
            duration = 1500L
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
        }
        shimmerAnimation.start()
    }

    /**
     * Show skeleton and hide content
     */
    fun showSkeleton(skeletonView: View, contentView: View) {
        skeletonView.visibility = View.VISIBLE
        contentView.visibility = View.GONE
        startShimmer(skeletonView)
    }

    /**
     * Hide skeleton and show content
     */
    fun hideSkeleton(skeletonView: View, contentView: View) {
        stopShimmer(skeletonView)
        skeletonView.visibility = View.GONE
        contentView.visibility = View.VISIBLE
    }
}
