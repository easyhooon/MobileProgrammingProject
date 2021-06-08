package kr.ac.konkuk.koogle.Adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView
import kr.ac.konkuk.koogle.Model.ChatModel
import kr.ac.konkuk.koogle.R
import kr.ac.konkuk.koogle.databinding.ItemLeftChatBinding
import kr.ac.konkuk.koogle.databinding.ItemRightChatBinding
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(val context: Context, private val chatList: MutableList<ChatModel>) :
    RecyclerView.Adapter<ChatAdapter.BaseViewHolder<*>>() {

    companion object {
        const val LEFT_CHAT = 1
        const val RIGHT_CHAT = 2
    }


    abstract class BaseViewHolder<T>(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(item: T)
    }

    inner class LeftViewHolder(itemView: View) : BaseViewHolder<ChatModel>(itemView) {

        @SuppressLint("SimpleDateFormat")
        val format = SimpleDateFormat("a HH:mm", Locale.KOREA)

        val tv_writerName = itemView.findViewById<TextView>(R.id.writerNameTextView)
        val tv_chatContent = itemView.findViewById<TextView>(R.id.contentTextView)
        val tv_createTime = itemView.findViewById<TextView>(R.id.timeTextView)
        val iv_writerProfileImage = itemView.findViewById<CircleImageView>(R.id.writerProfileImage)


        override fun bind(chatModel: ChatModel) {
            val time = Date(chatModel.chatCreatedAt)

            tv_writerName.text = chatModel.writerName
            tv_chatContent.text = chatModel.chatContent
            tv_createTime.text = format.format(time).toString()

            if (chatModel.writerProfileImageUrl.isNotEmpty()) {
                Glide.with(iv_writerProfileImage)
                    .load(chatModel.writerProfileImageUrl)
                    .into(iv_writerProfileImage)


//            fun bind(item: ChatModel){
//                tv_writerName.text = item.writerName
//                tv_chatContent.text = item.chatContent
//                tv_createTime.text = format.format(time).toString()
//
//                if(chatModel.writerProfileImageUrl.isNotEmpty()){
//                    Glide.with(iv_writerProfileImage)
//                        .load(chatModel.writerProfileImageUrl)
//                        .into(iv_writerProfileImage)
//                }
//
//            }
//            binding.writerNameTextView.text = chatModel.writerName
//            binding.contentTextView.text = chatModel.chatContent
//            binding.timeTextView.text = format.format(time).toString()
//
//            if(chatModel.writerProfileImageUrl.isNotEmpty()){
//                Glide.with(binding.writerProfileImage)
//                    .load(chatModel.writerProfileImageUrl)
//                    .into(binding.writerProfileImage)
//            }
            }
        }
    }

    inner class RightViewHolder(itemView: View) : BaseViewHolder<ChatModel>(itemView) {

        @SuppressLint("SimpleDateFormat")
        val format = SimpleDateFormat("a HH:mm", Locale.KOREA)


        val tv_chatContent = itemView.findViewById<TextView>(R.id.contentTextView)
        val tv_createTime = itemView.findViewById<TextView>(R.id.timeTextView)
        override fun bind(chatModel: ChatModel) {
            val time = Date(chatModel.chatCreatedAt)

            tv_chatContent.text = chatModel.chatContent
            tv_createTime.text = format.format(time).toString()
        }

//            fun bind(item: ChatModel){
//                tv_chatContent.text = item.chatContent
//                tv_createTime.text = format.format(time).toString()
//            }

//            binding.contentTextView.text = chatModel.chatContent
//            binding.timeTextView.text = format.format(time).toString()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<*> {
        //context 는 parent 에 있다
        return when (viewType) {
            LEFT_CHAT -> {
                val view =
                    LayoutInflater.from(context).inflate(R.layout.item_left_chat, parent, false)
                LeftViewHolder(view)
            }
            RIGHT_CHAT -> {
                val view =
                    LayoutInflater.from(context).inflate(R.layout.item_right_chat, parent, false)
                RightViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }

    }

    override fun onBindViewHolder(holder: BaseViewHolder<*>, position: Int) {
        val element = chatList[position]
        when (holder) {
            is LeftViewHolder -> {
                holder.bind(element)
            }

            is RightViewHolder -> {
                holder.bind(element)
            }
            else -> throw IllegalArgumentException()
        }

    }

    override fun getItemViewType(position: Int): Int {
        return chatList[position].viewType
    }

//    companion object {
//        val diffUtil = object : DiffUtil.ItemCallback<ChatModel>() {
//            override fun areItemsTheSame(oldModel: ChatModel, newModel: ChatModel): Boolean {
//                return oldModel.chatCreatedAt == newModel.chatCreatedAt
//            }
//
//            override fun areContentsTheSame(oldModel: ChatModel, newModel: ChatModel): Boolean {
//                return oldModel == newModel
//            }
//        }
//    }

    override fun getItemCount(): Int {
        return chatList.size
    }

}