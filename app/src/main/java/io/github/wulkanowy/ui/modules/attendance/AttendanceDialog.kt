package io.github.wulkanowy.ui.modules.attendance

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.wulkanowy.data.db.entities.Attendance
import io.github.wulkanowy.databinding.DialogAttendanceBinding
import io.github.wulkanowy.utils.descriptionRes
import io.github.wulkanowy.utils.serializable
import io.github.wulkanowy.utils.toFormattedString

class AttendanceDialog : DialogFragment() {

    private var _binding: DialogAttendanceBinding? = null
    private val binding get() = _binding!!

    private lateinit var attendance: Attendance

    private var dialogView: View? = null

    companion object {

        private const val ARGUMENT_KEY = "Item"

        fun newInstance(exam: Attendance) = AttendanceDialog().apply {
            arguments = bundleOf(ARGUMENT_KEY to exam)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
        attendance = requireArguments().serializable(ARGUMENT_KEY)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dialogView = DialogAttendanceBinding.inflate(layoutInflater).apply { _binding = this }.root
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
            attendanceDialogSubjectValue.text = attendance.subject
            attendanceDialogDescriptionValue.setText(attendance.descriptionRes)
            attendanceDialogDateValue.text = attendance.date.toFormattedString()
            attendanceDialogNumberValue.text = attendance.number.toString()
            attendanceDialogClose.setOnClickListener { dismiss() }
        }
    }
}
