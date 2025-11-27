package com.example.mobileapp.presentation.util

import android.view.View
import android.view.animation.AnimationUtils
import com.example.mobileapp.R

fun View.fadeIn() {
    if (visibility == View.VISIBLE) return
    val animation = AnimationUtils.loadAnimation(context, R.anim.fade_in)
    startAnimation(animation)
    visibility = View.VISIBLE
}

fun View.fadeOut() {
    if (visibility == View.GONE || visibility == View.INVISIBLE) return
    val animation = AnimationUtils.loadAnimation(context, R.anim.fade_out)
    startAnimation(animation)
    visibility = View.GONE
}

fun View.visibleIf(condition: Boolean) {
    if (condition) fadeIn() else fadeOut()
}
