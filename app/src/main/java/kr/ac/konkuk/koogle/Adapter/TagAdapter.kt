package kr.ac.konkuk.koogle.Adapter

import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import android.widget.LinearLayout
import androidx.core.view.children
import androidx.core.view.setMargins
import androidx.recyclerview.widget.RecyclerView
import kr.ac.konkuk.koogle.Model.TagModel
import kr.ac.konkuk.koogle.Model.TagType
import kr.ac.konkuk.koogle.R
import java.security.spec.EllipticCurve


/*
    2021-05-27 주예진 작성
    프로필에서 표시되는 태그 Recycler View 의 row adapter
    대분류 태그(제목)와 소분류 태그를 표시
    isSetting: 프로필에서 사용할 것인지 프로필 편집 창에서 사용할 것인지에 따라
    태그를 누를 수 있는 지 여부가 결정됨
 */
class TagAdapter(val context: Context, val data: MutableList<TagModel>,
                 val isSetting: Boolean)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
    interface OnItemClickListener {
        fun onItemClick(holder: DefaultViewHolder, view: EditText, data: TagModel, position: Int)
    }
    var itemClickListener: OnItemClickListener? = null
    var subTagClickListener: OnItemClickListener? = null

    // ProfileActivity 가 아닌 EditProfileActivity 에서만 사용해야 함
    // AddNewTagActivity 에서 새롭게 받아온 것들 넣기
    fun updateData(new_data: ArrayList<TagModel>){
        for(tag in new_data){
            var index = -1
            for((i, d) in data.withIndex()){
                // 이미 동일한 태그가 있을 때
                if(tag.main_tag_name == d.main_tag_name){
                    index = i
                    break
                }
            }
            // 중복 태그는 subTag 만 추가하기
            if(index>-1){
                for(s in tag.sub_tag_list){
                    // 서브태그도 중복 확인 후 추가하기
                    if(!data[index].sub_tag_list.contains(s))
                        data[index].sub_tag_list.add(s)
                }

            }
            // 새로운 태그 추가하기
            else{
                data.add(TagModel(
                    tag.main_tag_name, tag.sub_tag_list, tag.value, tag.tag_type
                ))
            }
        }
        notifyDataSetChanged()
    }

    fun moveItem(oldPos: Int, newPos: Int) {
        val item = data[oldPos]
        data.removeAt(oldPos)
        data.add(newPos, item)
        notifyItemMoved(oldPos, newPos)
    }

    fun removeItem(pos: Int) {
        data.removeAt(pos)
        notifyItemRemoved(pos)
    }

    inner class DefaultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var mainTagText: TextView = itemView.findViewById(R.id.mainTagText)
        var mainTag: String = ""
        var subTagView: LinearLayout = itemView.findViewById(R.id.subTagView)
        var nowIndex: Int = -1
        var editNowSub: Int = -1

        private fun editTagStart(subTagName: String): Boolean{
            if(nowIndex==-1) return false

            // 새로 추가된 태그의 경우
            if(subTagName == " "){
                editNowSub = data[nowIndex].sub_tag_list.size - 1
            }

            for((i, st) in data[nowIndex].sub_tag_list.withIndex()){
                if(subTagName == st){
                    editNowSub = i
                    return true
                }
            }

            for((j, t) in data.withIndex()){

            }
            return false
        }

        private fun editTag(subTagName: String){
            if(subTagName.isNullOrBlank())
                data[nowIndex].sub_tag_list.removeAt(editNowSub)
            else
            data[nowIndex].sub_tag_list[editNowSub] = subTagName
        }

        // SubTag 한 칸을 생성한다.
        fun makeSubTagView(tagName: String): TextView {
            var subTagText = EditText(context)
            subTagText.isFocusable = isSetting
            subTagText.setText(tagName)
            // 모서리가 둥근 태그 스타일 적용(임시)
            subTagText.setTextAppearance(R.style.TAG_STYLE)
            subTagText.setBackgroundResource(R.drawable.layout_tag_background)
            // 태그 간 간격 설정
            val p = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            p.setMargins(5)
            subTagText.layoutParams = p

            // 편집 세팅
            subTagText.addTextChangedListener(object: TextWatcher{
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    if(!editTagStart(subTagText.text.toString()))
                        Log.d("jan", "editTagFail")
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }

                override fun afterTextChanged(s: Editable?) {
                    editTag(subTagText.text.toString())
                    if(s.isNullOrBlank()){
                        for (c in subTagView.children)
                            ((c as ScrollView).getChildAt(0) as LinearLayout).removeView(subTagText)
                    }
                }

            })

            // 클릭 이벤트 설정
            subTagText.setOnClickListener {
                subTagClickListener?.onItemClick(this, subTagText, data[adapterPosition], adapterPosition)
            }

            return subTagText
        }

        private fun getRow(index: Int): LinearLayout{
            return (subTagView.getChildAt(index) as ScrollView).getChildAt(0) as LinearLayout
        }

        // 태그 추가 버튼
        private fun makePlusBtn(): TextView {
            var subTagText = TextView(context)
            subTagText.text = "+"
            // 태그 간 간격 설정
            val p = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            p.setMargins(5)
            subTagText.layoutParams = p
            // 모서리가 둥근 태그 스타일 적용(임시)
            subTagText.setTextAppearance(R.style.TAG_STYLE)
            subTagText.setBackgroundResource(R.drawable.layout_tag_background)
            subTagText.maxLines = 1
            subTagText.ellipsize = TextUtils.TruncateAt.MARQUEE
            subTagText.isSingleLine = true
            subTagText.inputType = InputType.TYPE_CLASS_TEXT

            // 새 태그 추가 기능
            subTagText.setOnClickListener {
                var lastRow: LinearLayout = getRow(subTagView.childCount - 1)
                // 이미 빈 태그가 있으면 추가하지 않음
                if((lastRow.getChildAt(lastRow.childCount - 1) as TextView).text.toString()
                == " ") return@setOnClickListener
                lastRow.addView(makeSubTagView(" "), lastRow.childCount-1)
            }
            return subTagText
        }

        // 소분류 태그 테이블 생성
        fun bind(model: TagModel) {
            var lastRow: LinearLayout
            for (tag in model.sub_tag_list) {
                // row 가 하나도 없으면 새로 만들기
                if (subTagView.childCount == 0) {
                    addRow()
                }
                // 새로운 Table row 를 추가해야 하는지 길이 검사
                lastRow = getRow(subTagView.childCount - 1)
                var len = 0
                val row_len = 26
                val margin = 1
                for (i in lastRow.children) {
                    i as TextView
                    len += i.text.length + margin
                }
                len += tag.length
                if (len > row_len) {
                    addRow()
                }
                lastRow = getRow(subTagView.childCount - 1)

                lastRow.addView(makeSubTagView(tag))
            }
            // 프로필 편집 액티비티의 경우 + 버튼 추가
            if(isSetting){
                lastRow = getRow(subTagView.childCount - 1)
                lastRow.addView(makePlusBtn())

                // 데이터에 추가
                for(d in data){
                    if(d.main_tag_name == mainTag){
                        d.sub_tag_list.add(" ")
                    }
                }
            }

            // 현재 Main Tag 의 View index 구함
            // 리사이클러 뷰 내 위치 변경 시에도 업데이트 되어야 함
            for ((i, t) in data.withIndex()){
                if(t.main_tag_name == mainTag) nowIndex = i
            }

        }

        // 태그 row 추가
        private fun addRow() {
            val row = LinearLayout(context)
            val lp: LinearLayout.LayoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            row.orientation = LinearLayout.HORIZONTAL
            row.layoutParams = lp

            val newScrollView = ScrollView(context)

            val layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            newScrollView.layoutParams = layoutParams

            val linearParams = LinearLayout.LayoutParams(
                800,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            row.orientation = LinearLayout.HORIZONTAL
            row.layoutParams = linearParams

            newScrollView.addView(row)
            subTagView.addView(newScrollView)
        }
    }

    // Tag 밑에 수치형 데이터 가 있는 형태의 row
    inner class ValueViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val mainTagText: TextView = itemView.findViewById(R.id.mainTagText)
        val valueText: TextView = itemView.findViewById(R.id.tagValueText)
        var mainTag: String = ""
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view:View
        return when(viewType){
            TagType.TAG ->{
                view = LayoutInflater.from(context).inflate(R.layout.row_user_tag, parent, false)
                DefaultViewHolder(view)
            }
            TagType.VALUE->{
                view = LayoutInflater.from(context).inflate(R.layout.row_user_value, parent, false)
                ValueViewHolder(view)
            }
            else -> {
                view = LayoutInflater.from(context).inflate(R.layout.row_user_tag, parent, false)
                DefaultViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val tagName: String = data[position].main_tag_name
        if(holder is DefaultViewHolder){
            holder.mainTag = tagName
            holder.mainTagText.text = tagName
            // 바인드 할 때 마다 다시 생성
            holder.subTagView.removeAllViews()
            holder.bind(data[position])
        }else if(holder is ValueViewHolder){
            holder.mainTag = tagName
            holder.mainTagText.text = tagName
            holder.valueText.text = data[position].value.toString()
        }else {
            //(holder as SettingViewHolder)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    // 헤더의 경우 메뉴에 포함되지 않으므로 제외
    override fun getItemViewType(position: Int): Int {
        return data[position].tag_type
    }

}
