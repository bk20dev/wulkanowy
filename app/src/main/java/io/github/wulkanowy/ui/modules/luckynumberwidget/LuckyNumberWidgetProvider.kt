package io.github.wulkanowy.ui.modules.luckynumberwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import dagger.hilt.android.AndroidEntryPoint
import io.github.wulkanowy.R
import io.github.wulkanowy.data.Resource
import io.github.wulkanowy.data.dataOrNull
import io.github.wulkanowy.data.db.SharedPrefProvider
import io.github.wulkanowy.data.db.entities.LuckyNumber
import io.github.wulkanowy.data.exceptions.NoCurrentStudentException
import io.github.wulkanowy.data.repositories.LuckyNumberRepository
import io.github.wulkanowy.data.repositories.StudentRepository
import io.github.wulkanowy.data.toFirstResult
import io.github.wulkanowy.ui.modules.Destination
import io.github.wulkanowy.ui.modules.splash.SplashActivity
import io.github.wulkanowy.utils.PendingIntentCompat
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class LuckyNumberWidgetProvider : AppWidgetProvider() {

    @Inject
    lateinit var studentRepository: StudentRepository

    @Inject
    lateinit var luckyNumberRepository: LuckyNumberRepository

    @Inject
    lateinit var sharedPref: SharedPrefProvider

    companion object {

        const val LUCKY_NUMBER_PENDING_INTENT_ID = 200

        fun getStudentWidgetKey(appWidgetId: Int) = "lucky_number_widget_student_$appWidgetId"

        fun getThemeWidgetKey(appWidgetId: Int) = "lucky_number_widget_theme_$appWidgetId"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray?
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        val appIntent = PendingIntent.getActivity(
            context,
            LUCKY_NUMBER_PENDING_INTENT_ID,
            SplashActivity.getStartIntent(context, Destination.LuckyNumber),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntentCompat.FLAG_IMMUTABLE
        )

        appWidgetIds?.forEach { widgetId ->
            val studentId = sharedPref.getLong(getStudentWidgetKey(widgetId), 0)
            val luckyNumberResource = getLuckyNumber(studentId, widgetId)
            val luckyNumber = luckyNumberResource.dataOrNull?.luckyNumber?.toString()

            if (luckyNumberResource is Resource.Error) {
                Timber.e("Error loading lucky number for widget", luckyNumberResource.error)
            }

            val remoteView = RemoteViews(context.packageName, R.layout.widget_luckynumber)
                .apply {
                    setTextViewText(R.id.luckyNumberWidgetValue, luckyNumber ?: "-")
                    setOnClickPendingIntent(R.id.luckyNumberWidgetContainer, appIntent)
                    // TODO: Add lucky number history interaction
                }
            appWidgetManager.updateAppWidget(widgetId, remoteView)
        }
    }

    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds?.forEach { appWidgetId ->
            with(sharedPref) {
                delete(getStudentWidgetKey(appWidgetId))
                delete(getThemeWidgetKey(appWidgetId))
            }
        }
    }

    private fun getLuckyNumber(studentId: Long, appWidgetId: Int) = runBlocking {
        try {
            val students = studentRepository.getSavedStudents()
            val student = students.singleOrNull { it.student.id == studentId }?.student
            val currentStudent = when {
                student != null -> student
                studentId != 0L && studentRepository.isCurrentStudentSet() -> {
                    studentRepository.getCurrentStudent(false).also {
                        sharedPref.putLong(getStudentWidgetKey(appWidgetId), it.id)
                    }
                }
                else -> null
            }

            if (currentStudent != null) {
                luckyNumberRepository.getLuckyNumber(currentStudent, forceRefresh = false)
                    .toFirstResult()
            } else {
                Resource.Success<LuckyNumber?>(null)
            }
        } catch (e: Exception) {
            if (e.cause !is NoCurrentStudentException) {
                Timber.e(e, "An error has occurred in lucky number provider")
            }
            Resource.Error(e)
        }
    }
}
