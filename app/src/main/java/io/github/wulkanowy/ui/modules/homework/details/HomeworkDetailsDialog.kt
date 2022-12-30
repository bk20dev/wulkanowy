package io.github.wulkanowy.ui.modules.homework.details

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import io.github.wulkanowy.R
import io.github.wulkanowy.data.db.entities.Homework
import io.github.wulkanowy.databinding.DialogHomeworkBinding
import io.github.wulkanowy.ui.base.BaseDialogFragment
import io.github.wulkanowy.utils.openInternetBrowser
import io.github.wulkanowy.utils.serializable
import javax.inject.Inject

@AndroidEntryPoint
class HomeworkDetailsDialog : BaseDialogFragment<DialogHomeworkBinding>(), HomeworkDetailsView {

    @Inject
    lateinit var presenter: HomeworkDetailsPresenter

    private var dialogView: View? = null

    @Inject
    lateinit var detailsAdapter: HomeworkDetailsAdapter

    override val homeworkDeleteSuccess: String
        get() = getString(R.string.homework_delete_success)

    private lateinit var homework: Homework

    companion object {

        private const val ARGUMENT_KEY = "Item"

        fun newInstance(homework: Homework) = HomeworkDetailsDialog().apply {
            arguments = bundleOf(ARGUMENT_KEY to homework)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
        homework = requireArguments().serializable(ARGUMENT_KEY)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dialogView = DialogHomeworkBinding.inflate(layoutInflater).apply { _binding = this }.root
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
        presenter.onAttachView(this)
    }

    @SuppressLint("SetTextI18n")
    override fun initView() {
        with(binding) {
            homeworkDialogRead.text =
                view?.context?.getString(if (homework.isDone) R.string.homework_mark_as_undone else R.string.homework_mark_as_done)
            homeworkDialogRead.setOnClickListener { presenter.toggleDone(homework) }
            homeworkDialogClose.setOnClickListener { dismiss() }
        }

        if (presenter.isHomeworkFullscreen) {
            dialog?.window?.setLayout(MATCH_PARENT, MATCH_PARENT)
        } else {
            dialog?.window?.setLayout(WRAP_CONTENT, WRAP_CONTENT)
        }

        with(binding.homeworkDialogRecycler) {
            layoutManager = LinearLayoutManager(context)
            adapter = detailsAdapter.apply {
                onAttachmentClickListener = { context.openInternetBrowser(it, ::showMessage) }
                onFullScreenClickListener = {
                    dialog?.window?.setLayout(MATCH_PARENT, MATCH_PARENT)
                    presenter.isHomeworkFullscreen = true
                }
                onFullScreenExitClickListener = {
                    dialog?.window?.setLayout(WRAP_CONTENT, WRAP_CONTENT)
                    presenter.isHomeworkFullscreen = false
                }
                onDeleteClickListener = { homework -> presenter.deleteHomework(homework) }
                isHomeworkFullscreen = presenter.isHomeworkFullscreen
                homework = this@HomeworkDetailsDialog.homework
            }
        }
    }

    override fun closeDialog() {
        dismiss()
    }

    override fun updateMarkAsDoneLabel(isDone: Boolean) {
        binding.homeworkDialogRead.text =
            view?.context?.getString(if (isDone) R.string.homework_mark_as_undone else R.string.homework_mark_as_done)
    }

    override fun onDestroyView() {
        presenter.onDetachView()
        super.onDestroyView()
    }
}
