package kr.ac.konkuk.koogle.Activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.*
import kr.ac.konkuk.koogle.Activity.AddArticleActivity.Companion.SEARCH_RESULT_FINAL
import kr.ac.konkuk.koogle.Activity.MapActivity.Companion.SEARCH_RESULT_INFO
import kr.ac.konkuk.koogle.Adapter.SearchRecyclerAdapter
import kr.ac.konkuk.koogle.Model.Entity.LocationLatLngEntity
import kr.ac.konkuk.koogle.Model.Entity.SearchResultEntity
import kr.ac.konkuk.koogle.Utility.RetrofitUtil
import kr.ac.konkuk.koogle.databinding.ActivityLocationSearchBinding
import kr.ac.konkuk.locationsearchmapapp.Response.Search.Poi
import kr.ac.konkuk.locationsearchmapapp.Response.Search.Pois
import java.lang.Exception
import kotlin.coroutines.CoroutineContext

class LocationSearchActivity : AppCompatActivity(), CoroutineScope {
    private lateinit var job: Job

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private lateinit var binding: ActivityLocationSearchBinding
    private lateinit var adapter: SearchRecyclerAdapter

    private lateinit var searchResult: SearchResultEntity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocationSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        job = Job()

        initAdapter()
        initViews()
        //클릭하는 시점에 api를 호출
        bindViews()
        initData()
    }

    private fun initAdapter() {
        adapter = SearchRecyclerAdapter()
    }

    private fun initViews() {
        binding.emptyResultTextView.isVisible = false
        binding.locationRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.locationRecyclerView.addItemDecoration(
            DividerItemDecoration(
                this,
                LinearLayoutManager.VERTICAL
            )
        )
        binding.locationRecyclerView.adapter = adapter
    }

    private fun initData() {
        adapter.notifyDataSetChanged()
    }
    private fun bindViews() = with(binding) {
        binding.searchEditText.addTextChangedListener {
            if (it.toString() == ""){
                binding.searchButton.isEnabled = false
            }
            else{
                binding.searchButton.isEnabled = true
            }
        }

        searchButton.setOnClickListener {
            searchKeyword(searchEditText.text.toString())
        }
    }

    private fun setData(pois: Pois) {
        val dataList = pois.poi.map {
            SearchResultEntity(
                fullAddress = makeMainAddress(it),
                name = it.name ?: "",
                locationLatLng = LocationLatLngEntity(
//                    lng와 lat의 중심점 위치를 명시
                    it.noorLat,
                    it.noorLon
                )
            )
        }
        adapter.setSearchResultList(dataList) {
//            Toast.makeText(this, "빌딩이름 : {${it.name} 주소 : ${it.fullAddress}} 위도 : ${it.locationLatLng} ", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MapActivity::class.java)
            intent.putExtra(SEARCH_RESULT_INFO, it)
            startActivityForResult(intent, CHOICE_LOCATION_REQUEST_CODE)
        }
    }

    private fun searchKeyword(keywordString: String) {
        launch(coroutineContext) {
            try {
                //IO 쓰레드로 변환
                withContext(Dispatchers.IO) {
                    //api 호출
                    val response = RetrofitUtil.apiService.getSearchLocation(
                        keyword = keywordString
                    )
                    if (response.isSuccessful) {
                        val body = response.body()
                        withContext(Dispatchers.Main) {
                            Log.e("list", body.toString())
                            body?.let { searchResponseSchema ->
                                setData(searchResponseSchema.searchPoiInfo.pois)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@LocationSearchActivity,
                    "검색하는 과정에서 에러가 발생했습니다.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    //문자열 보정
    private fun makeMainAddress(poi: Poi): String =
        if (poi.secondNo?.trim().isNullOrEmpty()) {
            (poi.upperAddrName?.trim() ?: "") + " " +
                    (poi.middleAddrName?.trim() ?: "") + " " +
                    (poi.lowerAddrName?.trim() ?: "") + " " +
                    (poi.detailAddrName?.trim() ?: "") + " " +
                    poi.firstNo?.trim()
        } else {
            (poi.upperAddrName?.trim() ?: "") + " " +
                    (poi.middleAddrName?.trim() ?: "") + " " +
                    (poi.lowerAddrName?.trim() ?: "") + " " +
                    (poi.detailAddrName?.trim() ?: "") + " " +
                    (poi.firstNo?.trim() ?: "") + " " +
                    poi.secondNo?.trim()
        }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == CHOICE_LOCATION_REQUEST_CODE){
            if (resultCode == RESULT_OK){
                searchResult = data?.getParcelableExtra(SEARCH_RESULT_MID)!!
                data.putExtra(SEARCH_RESULT_FINAL, searchResult)
                setResult(RESULT_OK, data)
                finish()
            }
        }
        else {

        }
    }

    companion object {
        const val CHOICE_LOCATION_REQUEST_CODE = 1000
        const val SEARCH_RESULT_MID = "SEARCH_RESULT_MID"
    }
}