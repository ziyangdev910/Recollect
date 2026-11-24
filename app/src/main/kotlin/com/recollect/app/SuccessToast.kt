package com.recollect.app

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import org.ziyangdev910.recollect.R

/**
 * 操作成功的自定义 Toast：绿色对勾 + 文案。
 *
 * 「导入成功」与「保存成功」共用同一样式；只在界面层调用，不参与状态机（R-03）。
 */
fun showImportSuccessToast(context: Context) = showSuccessToast(context, "导入成功")

/** 录入/编辑题目保存成功：绿色对勾 +「保存成功」。 */
fun showSaveSuccessToast(context: Context) = showSuccessToast(context, "保存成功")

private fun showSuccessToast(context: Context, message: String) {
    val density = context.resources.displayMetrics.density
    fun dp(value: Int): Int = (value * density).toInt()

    val container = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(16), dp(10), dp(20), dp(10))
        background = GradientDrawable().apply {
            cornerRadius = dp(24).toFloat()
            setColor(0xE6323232.toInt())
        }
    }
    container.addView(
        ImageView(context).apply {
            setImageResource(R.drawable.ic_import_success_check)
            setColorFilter(0xFF4CAF50.toInt())
            layoutParams = LinearLayout.LayoutParams(dp(22), dp(22))
        },
    )
    container.addView(
        TextView(context).apply {
            text = message
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(dp(10), 0, 0, 0)
        },
    )

    Toast(context.applicationContext).apply {
        view = container
        duration = Toast.LENGTH_SHORT
    }.show()
}
