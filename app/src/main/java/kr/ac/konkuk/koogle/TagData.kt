package kr.ac.konkuk.koogle

import java.io.Serializable

data class TagData (var main_tag_name: String, var sub_tag_list: ArrayList<String>): Serializable{

}