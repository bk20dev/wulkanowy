package io.github.wulkanowy.ui.modules.schoolannouncement

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.wulkanowy.data.db.entities.SchoolAnnouncement
import io.github.wulkanowy.databinding.DialogSchoolAnnouncementBinding
import io.github.wulkanowy.utils.parseUonetHtml
import io.github.wulkanowy.utils.serializable
import io.github.wulkanowy.utils.toFormattedString

class SchoolAnnouncementDialog : DialogFragment() {

    private var _binding: DialogSchoolAnnouncementBinding? = null
    private val binding get() = _binding!!

    private var dialogView: View? = null

    private lateinit var announcement: SchoolAnnouncement

    companion object {

        private const val ARGUMENT_KEY = "item"

        fun newInstance(announcement: SchoolAnnouncement) = SchoolAnnouncementDialog().apply {
            arguments = bundleOf(ARGUMENT_KEY to announcement)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
        announcement = requireArguments().serializable(ARGUMENT_KEY)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dialogView = DialogSchoolAnnouncementBinding.inflate(layoutInflater).apply { _binding = this }.root
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
            announcementDialogSubjectValue.text = announcement.subject
            announcementDialogDateValue.text = announcement.date.toFormattedString()
            announcementDialogDescriptionValue.text = announcement.content.parseUonetHtml()

            announcementDialogClose.setOnClickListener { dismiss() }
        }
    }
}
