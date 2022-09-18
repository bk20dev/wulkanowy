package io.github.wulkanowy.utils

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle

object WidgetSizeProvider {
    /**
     * Returns the size of the given widget, or zeros
     * if the options do not contain appropriate values
     *
     * @param context The widget context
     * @param options The appWidgetOptions associated with the measured widget
     *
     * @return The calculated size
     */
    fun getSize(context: Context, options: Bundle): Pair<Int, Int> {
        val sizeBounds = getSizeBounds(options)
        val orientation = context.resources.configuration.orientation
        val isPortrait = orientation == Configuration.ORIENTATION_PORTRAIT

        return if (isPortrait) {
            sizeBounds.minWidth to sizeBounds.maxHeight
        } else {
            sizeBounds.maxWidth to sizeBounds.minHeight
        }
    }

    /**
     * Reads the size-related information from the appWidgetOptions bundle
     */
    private fun getSizeBounds(options: Bundle): WidgetSizeBounds {
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val maxWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        val maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        return WidgetSizeBounds(minWidth, maxWidth, minHeight, maxHeight)
    }

    /**
     * Stores information about the size of a particular widget
     */
    private data class WidgetSizeBounds(
        val minWidth: Int,
        val maxWidth: Int,
        val minHeight: Int,
        val maxHeight: Int,
    )
}
