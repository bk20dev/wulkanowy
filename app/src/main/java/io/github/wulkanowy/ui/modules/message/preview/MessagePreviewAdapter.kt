package io.github.wulkanowy.ui.modules.message.preview

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.wulkanowy.R
import io.github.wulkanowy.data.db.entities.Message
import io.github.wulkanowy.data.db.entities.MessageAttachment
import io.github.wulkanowy.data.db.entities.MessageWithAttachment
import io.github.wulkanowy.data.repositories.message.MessageFolder
import io.github.wulkanowy.databinding.ItemMessageAttachmentBinding
import io.github.wulkanowy.databinding.ItemMessageDividerBinding
import io.github.wulkanowy.databinding.ItemMessagePreviewBinding
import io.github.wulkanowy.utils.openInternetBrowser
import io.github.wulkanowy.utils.toFormattedString
import javax.inject.Inject

class MessagePreviewAdapter @Inject constructor() :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    enum class ViewType(val id: Int) {
        MESSAGE(1),
        DIVIDER(2),
        ATTACHMENT(3)
    }

    var messageWithAttachment: MessageWithAttachment? = null
        set(value) {
            field = value
            attachments = value?.attachments.orEmpty()
        }

    private var attachments: List<MessageAttachment> = emptyList()

    override fun getItemCount() = if (messageWithAttachment == null) 0 else attachments.size + 1 + if (attachments.isNotEmpty()) 1 else 0

    override fun getItemViewType(position: Int) = when (position) {
        0 -> ViewType.MESSAGE.id
        1 -> ViewType.DIVIDER.id
        else -> ViewType.ATTACHMENT.id
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            ViewType.MESSAGE.id -> MessageViewHolder(ItemMessagePreviewBinding.inflate(inflater, parent, false))
            ViewType.DIVIDER.id -> DividerViewHolder(ItemMessageDividerBinding.inflate(inflater, parent, false))
            ViewType.ATTACHMENT.id -> AttachmentViewHolder(ItemMessageAttachmentBinding.inflate(inflater, parent, false))
            else -> throw IllegalStateException()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is MessageViewHolder -> bindMessage(holder, requireNotNull(messageWithAttachment).message)
            is AttachmentViewHolder -> bindAttachment(holder, requireNotNull(messageWithAttachment).attachments[position - 2])
        }
    }

    @SuppressLint("SetTextI18n")
    private fun bindMessage(holder: MessageViewHolder, message: Message) {
        with(holder.binding) {
            messagePreviewSubject.text = message.subject.ifBlank { root.context.getString(R.string.message_no_subject) }
            messagePreviewDate.text = root.context.getString(R.string.message_date, message.date.toFormattedString("yyyy-MM-dd HH:mm:ss"))
            messagePreviewContent.text = message.content
            messagePreviewAuthor.text = if (message.folderId == MessageFolder.SENT.id) "${root.context.getString(R.string.message_to)} ${message.recipient}"
            else "${root.context.getString(R.string.message_from)} ${message.sender}"
        }
    }

    private fun bindAttachment(holder: AttachmentViewHolder, attachment: MessageAttachment) {
        with(holder.binding) {
            messagePreviewAttachment.visibility = View.VISIBLE
            messagePreviewAttachment.text = attachment.filename
            root.setOnClickListener {
                root.context.openInternetBrowser(attachment.url) { }
            }
        }
    }

    class MessageViewHolder(val binding: ItemMessagePreviewBinding) :
        RecyclerView.ViewHolder(binding.root)

    class DividerViewHolder(val binding: ItemMessageDividerBinding) :
        RecyclerView.ViewHolder(binding.root)

    class AttachmentViewHolder(val binding: ItemMessageAttachmentBinding) :
        RecyclerView.ViewHolder(binding.root)
}
