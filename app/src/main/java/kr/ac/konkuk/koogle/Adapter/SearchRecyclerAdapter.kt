package kr.ac.konkuk.koogle.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kr.ac.konkuk.koogle.Model.Entity.SearchResultEntity
import kr.ac.konkuk.koogle.databinding.ItemSearchResultBinding

class SearchRecyclerAdapter : RecyclerView.Adapter<SearchRecyclerAdapter.SearchResultItemViewHolder>() {

    private var searchResultList: List<SearchResultEntity> = listOf()
    private lateinit var searchResultClickListener: (SearchResultEntity) -> Unit

    inner class SearchResultItemViewHolder(
        private val binding: ItemSearchResultBinding,
        val searchResultClickListener: (SearchResultEntity) -> Unit
    ): RecyclerView.ViewHolder(binding.root) {

        fun bindData(data: SearchResultEntity) = with(binding) {
            //제목
            textTextView.text = "${data.fullAddress}"
            //부제목
            subtextTextView.text = "${data.name}"
        }

        fun bindViews(data: SearchResultEntity) {
            binding.root.setOnClickListener {
                searchResultClickListener(data)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultItemViewHolder {
        val view = ItemSearchResultBinding.inflate(LayoutInflater.from(parent.context),parent,false)

        return SearchResultItemViewHolder(view, searchResultClickListener)
    }

    override fun onBindViewHolder(holder: SearchResultItemViewHolder, position: Int) {
        holder.bindData(searchResultList[position])
        holder.bindViews(searchResultList[position])
    }

    override fun getItemCount(): Int = searchResultList.size

    fun setSearchResultList(searchResultList: List<SearchResultEntity>, searchResultClickListener: (SearchResultEntity) -> Unit){
        this.searchResultList = searchResultList
        this.searchResultClickListener = searchResultClickListener
        //set 하고 새로고침
        notifyDataSetChanged()
    }
}
