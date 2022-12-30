package io.github.wulkanowy.ui.modules.note

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.wulkanowy.R
import io.github.wulkanowy.data.db.entities.Note
import io.github.wulkanowy.databinding.DialogNoteBinding
import io.github.wulkanowy.sdk.scrapper.notes.NoteCategory
import io.github.wulkanowy.utils.getThemeAttrColor
import io.github.wulkanowy.utils.serializable
import io.github.wulkanowy.utils.toFormattedString

class NoteDialog : DialogFragment() {

    private var _binding: DialogNoteBinding? = null
    private val binding get() = _binding!!

    private var dialogView: View? = null

    private lateinit var note: Note

    companion object {

        private const val ARGUMENT_KEY = "Item"

        fun newInstance(note: Note) = NoteDialog().apply {
            arguments = bundleOf(ARGUMENT_KEY to note)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
        note = requireArguments().serializable(ARGUMENT_KEY)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dialogView = DialogNoteBinding.inflate(layoutInflater).apply { _binding = this }.root
        return MaterialAlertDialogBuilder(requireContext(), theme)
            .setView(dialogView)
            .create()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = dialogView

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            noteDialogDateValue.text = note.date.toFormattedString()
            noteDialogCategoryValue.text = note.category
            noteDialogTeacherValue.text = note.teacher
            noteDialogContentValue.text = note.content
        }

        if (note.isPointsShow) {
            with(binding.noteDialogPointsValue) {
                text = "${if (note.points > 0) "+" else ""}${note.points}"
                setTextColor(
                    when (NoteCategory.getByValue(note.categoryType)) {
                        NoteCategory.POSITIVE -> ContextCompat.getColor(
                            requireContext(),
                            R.color.note_positive
                        )
                        NoteCategory.NEGATIVE -> ContextCompat.getColor(
                            requireContext(),
                            R.color.note_negative
                        )
                        else -> requireContext().getThemeAttrColor(android.R.attr.textColorPrimary)
                    }
                )
            }
        }

        binding.noteDialogClose.setOnClickListener { dismiss() }
    }
}
