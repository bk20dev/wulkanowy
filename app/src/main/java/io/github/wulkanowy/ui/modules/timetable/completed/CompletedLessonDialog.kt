package io.github.wulkanowy.ui.modules.timetable.completed

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.wulkanowy.data.db.entities.CompletedLesson
import io.github.wulkanowy.databinding.DialogLessonCompletedBinding
import io.github.wulkanowy.utils.serializable

class CompletedLessonDialog : DialogFragment() {

    private var _binding: DialogLessonCompletedBinding? = null
    private val binding get() = _binding!!

    private var dialogView: View? = null

    private lateinit var completedLesson: CompletedLesson

    companion object {

        private const val ARGUMENT_KEY = "Item"

        fun newInstance(lesson: CompletedLesson) = CompletedLessonDialog().apply {
            arguments = bundleOf(ARGUMENT_KEY to lesson)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
        completedLesson = requireArguments().serializable(ARGUMENT_KEY)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dialogView = DialogLessonCompletedBinding.inflate(layoutInflater).apply { _binding = this }.root
        return MaterialAlertDialogBuilder(requireContext(), theme)
            .setView(dialogView)
            .create()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = dialogView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            completedLessonDialogSubjectValue.text = completedLesson.subject
            completedLessonDialogTopicValue.text = completedLesson.topic
            completedLessonDialogTeacherValue.text = completedLesson.teacher
            completedLessonDialogAbsenceValue.text = completedLesson.absence
            completedLessonDialogChangesValue.text = completedLesson.substitution
            completedLessonDialogResourcesValue.text = completedLesson.resources
        }

        completedLesson.substitution.let {
            if (it.isBlank()) {
                with(binding) {
                    completedLessonDialogChangesTitle.visibility = View.GONE
                    completedLessonDialogChangesValue.visibility = View.GONE
                }
            } else binding.completedLessonDialogChangesValue.text = it
        }

        completedLesson.absence.let {
            if (it.isBlank()) {
                with(binding) {
                    completedLessonDialogAbsenceTitle.visibility = View.GONE
                    completedLessonDialogAbsenceValue.visibility = View.GONE
                }
            } else binding.completedLessonDialogAbsenceValue.text = it
        }

        completedLesson.resources.let {
            if (it.isBlank()) {
                with(binding) {
                    completedLessonDialogResourcesTitle.visibility = View.GONE
                    completedLessonDialogResourcesValue.visibility = View.GONE
                }
            } else binding.completedLessonDialogResourcesValue.text = it
        }

        binding.completedLessonDialogClose.setOnClickListener { dismiss() }
    }
}
