package org.telegram.custom

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast

/**
 * create by ysz on 2023/7/14
 */

@SuppressLint("StaticFieldLeak")
lateinit var app:Context

fun runUi(callBack: () -> Unit) {
    Handler(Looper.getMainLooper()).post(callBack)
}

fun runUi(delay: Long, callBack: () -> Unit) {
    Handler(Looper.getMainLooper()).postDelayed(callBack, delay)
}

fun showToast(str: String) {
    Toast.makeText(app, str, Toast.LENGTH_SHORT).show()
}

/**
 * 执行回调函数, 将它抛出的异常记录到 Xposed 的日志里
 */
inline fun runSafe(func: () -> Unit) {
    try { func() } catch (t: Throwable) {
        Log.e("HookUtils", "runSafe error",t);
    }
}

fun Activity.addCustomView(view: View, block: (FrameLayout.LayoutParams.() -> Unit) = {}) {
    val parent = findViewById<ViewGroup>(android.R.id.content)
    Log.i(TAG, "addView parent${parent}")
    parent?.apply {
        Log.i(TAG, "addView Child count=${childCount}")
        if(indexOfChild(view) != -1){
            return
        }
        view.bringToFront()
        addView(
            view,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply(block)
        )
    }
}

fun Activity.removeCustomView(view: View?) {
    view?:return
    val parent = findViewById<ViewGroup>(android.R.id.content)
    Log.i(TAG, "remove parent${parent}, act=${this}")
    parent?.apply {
        removeView(view)
    }
}

val Int.dp get() = ((app.resources.displayMetrics.density ?: 1f) * this + 0.5f).toInt()

fun View?.asActivity(): Activity? {
    this?:return null
    var context: Context? = context
    while (context is ContextWrapper) {
        if (context is Activity) {
            return context
        }
        context = context.baseContext
    }
    return null
}

