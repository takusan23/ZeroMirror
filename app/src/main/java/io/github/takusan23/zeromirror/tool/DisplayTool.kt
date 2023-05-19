package io.github.takusan23.zeromirror.tool

import android.app.Activity
import androidx.window.layout.WindowMetricsCalculator

/** 画面サイズを出す */
object DisplayTool {

    /**
     * 画面のサイズを出す
     *
     * @param activity [Activity]
     * @return 縦 横 の Pair
     */
    fun getDisplaySize(activity: Activity): Pair<Int, Int> {
        val metrics = WindowMetricsCalculator.getOrCreate().computeMaximumWindowMetrics(activity)
        val height = metrics.bounds.height()
        val width = metrics.bounds.width()
        return (height to width)
    }
}