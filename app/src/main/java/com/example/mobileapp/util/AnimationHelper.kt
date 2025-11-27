package com.example.mobileapp.util

import android.view.View
import android.view.animation.AnimationUtils
import com.example.mobileapp.R

object AnimationHelper {
    
    fun fadeIn(view: View) {
        val animation = AnimationUtils.loadAnimation(view.context, R.anim.fade_in)
        view.startAnimation(animation)
        view.visibility = View.VISIBLE
    }
    
    fun fadeOut(view: View) {
        val animation = AnimationUtils.loadAnimation(view.context, R.anim.fade_out)
        animation.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(animation: android.view.animation.Animation?) {}
            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                view.visibility = View.GONE
            }
        })
        view.startAnimation(animation)
    }
    
    fun scaleUp(view: View) {
        val animation = AnimationUtils.loadAnimation(view.context, R.anim.scale_up)
        view.startAnimation(animation)
    }
    
    fun slideInRight(view: View) {
        val animation = AnimationUtils.loadAnimation(view.context, R.anim.slide_in_right)
        view.startAnimation(animation)
        view.visibility = View.VISIBLE
    }
    
    fun slideOutLeft(view: View) {
        val animation = AnimationUtils.loadAnimation(view.context, R.anim.slide_out_left)
        animation.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(animation: android.view.animation.Animation?) {}
            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                view.visibility = View.GONE
            }
        })
        view.startAnimation(animation)
    }
}
