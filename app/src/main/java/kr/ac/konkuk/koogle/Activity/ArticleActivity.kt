package kr.ac.konkuk.koogle.Activity

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.target.ViewTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kr.ac.konkuk.koogle.Adapter.ArticleImageAdapter
import kr.ac.konkuk.koogle.Adapter.TagAdapter
import kr.ac.konkuk.koogle.DBKeys
import kr.ac.konkuk.koogle.DBKeys.Companion.ARTICLE_ID
import kr.ac.konkuk.koogle.DBKeys.Companion.ARTICLE_IMAGE_FILE_NAME
import kr.ac.konkuk.koogle.DBKeys.Companion.ARTICLE_IMAGE_PATH
import kr.ac.konkuk.koogle.DBKeys.Companion.ARTICLE_TITLE
import kr.ac.konkuk.koogle.DBKeys.Companion.CURRENT_NUMBER
import kr.ac.konkuk.koogle.DBKeys.Companion.DB_ARTICLES
import kr.ac.konkuk.koogle.DBKeys.Companion.DB_BLOCK_USERS
import kr.ac.konkuk.koogle.DBKeys.Companion.DB_GROUPS
import kr.ac.konkuk.koogle.DBKeys.Companion.DB_MAIN_TAGS
import kr.ac.konkuk.koogle.DBKeys.Companion.DB_USERS
import kr.ac.konkuk.koogle.DBKeys.Companion.GROUP_ID
import kr.ac.konkuk.koogle.DBKeys.Companion.TAG_INDEX
import kr.ac.konkuk.koogle.DBKeys.Companion.USER_EMAIL
import kr.ac.konkuk.koogle.DBKeys.Companion.USER_ID
import kr.ac.konkuk.koogle.DBKeys.Companion.USER_NAME
import kr.ac.konkuk.koogle.DBKeys.Companion.USER_PROFILE_IMAGE_URL
import kr.ac.konkuk.koogle.Fragment.CommunityFragment
import kr.ac.konkuk.koogle.Model.ArticleModel
import kr.ac.konkuk.koogle.Model.Entity.SearchResultEntity
import kr.ac.konkuk.koogle.Model.TagModel
import kr.ac.konkuk.koogle.Model.UserModel
import kr.ac.konkuk.koogle.databinding.ActivityArticleBinding
import java.text.SimpleDateFormat
import java.util.*
import kr.ac.konkuk.koogle.R


class ArticleActivity : AppCompatActivity() {
    lateinit var binding: ActivityArticleBinding

    val scope = CoroutineScope(Dispatchers.Main)

    private lateinit var writerId:String
    private lateinit var writerName:String
    private lateinit var articleId:String
    private lateinit var articleTitle:String

    private lateinit var currentUserId:String
    private lateinit var currentUserName:String
    private lateinit var currentUserProfileImage:String

    private lateinit var recruitmentNumber:String
    private lateinit var currentNumber:String

    private var userIdList:MutableList<String> = mutableListOf()
    private var fileNameList: ArrayList<String> = arrayListOf()

    private lateinit var imageAdapter: ArticleImageAdapter
    private lateinit var tagRecyclerAdapter : TagAdapter

    private lateinit var mapInfo:SearchResultEntity

    private val auth: FirebaseAuth by lazy {
        Firebase.auth
    }
    private val firebaseUser = auth.currentUser!!

    private val currentUserRef: DatabaseReference by lazy {
        Firebase.database.reference.child(DB_USERS).child(firebaseUser.uid)
    }

    private val currentUserGroupRef: DatabaseReference by lazy {
        currentUserRef.child(DB_GROUPS).child(articleId)
    }

    private val currentArticleRef: DatabaseReference by lazy{
        Firebase.database.reference.child(DB_ARTICLES).child(articleId)
    }

    private val currentGroupRef: DatabaseReference by lazy {
        Firebase.database.reference.child(DB_GROUPS).child(articleId)
    }

    private val currentGroupUsersRef: DatabaseReference by lazy {
        Firebase.database.reference.child(DB_GROUPS).child(articleId).child(DB_USERS)
    }

    private val currentGroupUserRef: DatabaseReference by lazy {
        Firebase.database.reference.child(DB_GROUPS).child(articleId).child(DB_USERS).child(currentUserId)
    }

    private val currentUserBlockRef: DatabaseReference by lazy {
        currentUserRef.child(DB_BLOCK_USERS)
    }

    private val storage: FirebaseStorage by lazy {
        Firebase.storage
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArticleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initDB()
        initViews()
        initImageRecyclerView()
        initRecyclerView()
        initButton()
    }

    private fun initImageRecyclerView() {
        binding.photoImageRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        imageAdapter = ArticleImageAdapter()
        imageAdapter.itemClickListener = object : ArticleImageAdapter.OnItemClickListener {
            override fun onItemClick(holder: ArticleImageAdapter.ViewHolder, uri: Uri) {
                val intent = Intent(this@ArticleActivity, ImageViewActivity::class.java)
                intent.putExtra("url", uri.toString())
                startActivity(intent)
            }
        }
        binding.photoImageRecyclerView.adapter = imageAdapter
    }

    private fun initViews() {
       setSupportActionBar(binding.articleToolbar)
        val actionBar = supportActionBar!!
        actionBar.apply{
            setDisplayShowCustomEnabled(true)
            setDisplayShowTitleEnabled(false) //?????? ????????? ?????????
            setDisplayHomeAsUpEnabled(true) // ???????????? ???????????? ?????? ????????????
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        //??? ???????????? ?????? ????????? ??? ??? ?????????
        if(::writerId.isInitialized){
            if(firebaseUser.uid == writerId){
                val menuInflater = menuInflater
                menuInflater.inflate(R.menu.article_admin_option_menu, menu)
            }
            else{
                val menuInflater = menuInflater
                menuInflater.inflate(R.menu.article_option_menu, menu)
            }
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.updateArticle -> {
                val intent = Intent(this, EditArticleActivity::class.java)
                //???????????? ??? ??????(??? ?????? ??????, ??? ??????) ?????? ?????????????????? ????????? ???????????? -> ??????????????? ??????
                //?????? ??? ???????????? ????????? ???????????? ????????? ?????? -> ?????? ?????????
                intent.putExtra(ARTICLE_INFO, articleId)
                Log.i("ArticleActivity", "onOptionsItemSelected: $articleId")
                startActivity(intent)
                finish()
            }
            R.id.deleteArticle -> {
                //dialog ?????? ????????? ?????? ??????
                val ad = AlertDialog.Builder(this@ArticleActivity)
                ad.setMessage("?????? ?????? ?????????????????????????")
                ad.setPositiveButton(
                    "?????????"
                ) { dialog, _ ->
                    dialog.dismiss()
                }
                ad.setNegativeButton(
                    "???"
                ) { dialog, _ ->
                    //?????? ?????? ?????? ??????
                    deleteArticle()
                    val intent = Intent(this@ArticleActivity,MainActivity::class.java)
                    startActivity(intent)
                    finish()
                    dialog.dismiss()
                }
                ad.show()
            }
            R.id.userBlock -> {
                //dialog ?????? ????????? ?????? ??????
                val ad = AlertDialog.Builder(this@ArticleActivity)
                ad.setMessage("?????? ????????? ????????????????????????? \n??????????????? ?????? ????????? ?????? ??? ??? ????????????.")
                ad.setPositiveButton(
                    "??????"
                ) { dialog, _ ->
                    dialog.dismiss()
                }
                ad.setNegativeButton(
                    "??????"
                ) { dialog, _ ->
                    //?????? ?????? ?????? ??????
                    userBlock(writerId, writerName)
                    Toast.makeText(this@ArticleActivity, "?????? ????????? ?????????????????????", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@ArticleActivity,MainActivity::class.java)
                    startActivity(intent)
                    finish()
                    dialog.dismiss()
                }
                ad.show()
            }
            else -> {
                //????????????
                finish()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun userBlock(writerId: String, writerName:String) {
        val blockId = currentUserBlockRef.push().key.toString()

        val block = mutableMapOf<String, Any>()
        block[USER_ID] = writerId
        block[USER_NAME] = writerName

        currentUserBlockRef.child(blockId).updateChildren(block)
    }


    private fun deleteArticle() {
        currentArticleRef.setValue(null)
        currentGroupRef.setValue(null)
        //????????? ??????????????? ??????
        currentUserGroupRef.setValue(null)
        val storageRef = storage.reference.child(ARTICLE_IMAGE_PATH)
        for (name in fileNameList) {
            storageRef.child(name).delete()
        }
    }

    private fun initButton() {
        binding.contactButton.setOnClickListener {
            currentUserId = firebaseUser.uid

            //?????? ?????? ????????? ??? ??? ??????
            if(writerId == currentUserId){
                Toast.makeText(this, "?????? ????????? ??? ?????????.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            //???????????? ?????? ????????? ?????????????????? ??????
            else if (currentUserId in userIdList){
                Toast.makeText(this, "?????? ????????? ?????????????????????", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            else{
                //?????? ?????? ????????? ?????? ?????? ??????
                //????????? ?????? + 1 ?????? ???????????? ?????? ?????? ??????
                if (currentNumber.toInt() + 1 <= recruitmentNumber.toInt())
                {
                    val ad = AlertDialog.Builder(this@ArticleActivity)
                    ad.setMessage("????????? ?????????????????????????")
                    ad.setPositiveButton(
                        "??????"
                    ) { dialog, _ ->
                        dialog.dismiss()
                    }
                    ad.setNegativeButton(
                        "????????????"
                    ) { _, _ ->
                        //?????? ?????? ?????? ??????
                        showProgress()
                        joinGroup(currentUserId, currentUserName, currentUserProfileImage)
                    }
                    ad.show()
                }
                else {
                    Toast.makeText(this, "??????????????? ?????????????????????", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }
        }

        binding.showMapButton.setOnClickListener {
            val intent = Intent(this, CheckMapActivity::class.java)
            intent.putExtra(MAP_INFO, mapInfo)
            startActivity(intent)
        }

        //???????????? ????????? ????????? ???????????? ??? ??? ??????
        binding.profileImageView.setOnClickListener {
            val intent = Intent(this, CheckProfileActivity::class.java)
            intent.putExtra(WRITER_INFO, writerId)
            Log.i("ArticleActivity", "writerId: $writerId")
            startActivity(intent)
        }
    }

    private fun initDB() {
        val intent = intent
        articleId = intent.getStringExtra(ARTICLE_ID).toString()
        Log.d("ArticleActivity", "articleId: $articleId")
        //?????????????????? ????????????????????? ?????? ????????????

        scope.launch {
            binding.progressBar.visibility = View.VISIBLE
            CoroutineScope(Dispatchers.IO).async {
                currentArticleRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    @SuppressLint("SimpleDateFormat", "SetTextI18n")
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val articleModel: ArticleModel? = snapshot.getValue(ArticleModel::class.java)
                            val format = SimpleDateFormat("MM??? dd???")

                            if (articleModel != null) {
                                writerId = articleModel.writerId
                                writerName = articleModel.writerName
                                articleTitle = articleModel.articleTitle
                                if (articleModel.articleImageUrl.isEmpty()) {
                                    val scrollParams = LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        0,
                                        9.0f
                                    )
                                    binding.contentScrollView.layoutParams = scrollParams
                                    binding.contentScrollView.scrollBarFadeDuration = 0
                                    binding.contentScrollView.isAlwaysDrawnWithCacheEnabled = true
                                    binding.photoImageRecyclerView.visibility = View.GONE

                                } else {
                                    val scrollParams = LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        0,
                                        6.0f
                                    )
                                    binding.contentScrollView.layoutParams = scrollParams
                                    binding.photoImageRecyclerView.visibility = View.VISIBLE

                                    fileNameList = articleModel.articleImageFileName
                                    initImageRecyclerView()
                                    for (uri in articleModel.articleImageUrl) {
                                        Log.i("uri", Uri.parse(uri).toString())
                                        imageAdapter.addItem(Uri.parse(uri))
                                    }
                                }

                                if (articleModel.writerProfileImageUrl.isEmpty()) {
                                    binding.profileImageView.setImageResource(R.drawable.profile_image)
                                } else {
                                    Glide.with(binding.profileImageView)
                                        .load(articleModel.writerProfileImageUrl)
                                        .into(binding.profileImageView)
                                }

                                //?????? ?????? ????????? ???????????? ??????
                                if (articleModel.desiredLocation != null){
                                    mapInfo = articleModel.desiredLocation
                                    binding.locationTextView.text = articleModel.desiredLocation.fullAddress
                                }
                                else {
                                    //????????? ???????????? ????????? ??????
                                    //?????? ?????? ??????????????? ?????? ????????? ??????
                                    binding.locationInfoLayout.visibility =  View.GONE
                                }
                                binding.writerNameTextView.text = articleModel.writerName
                                binding.titleTextView.text = articleModel.articleTitle
                                binding.recruitmentNumberTextView.text = articleModel.recruitmentNumber.toString()+"???"
                                val date = Date(articleModel.articleCreatedAt)
                                binding.dateTextView.text = format.format(date).toString()
                                binding.contentTextView.text = articleModel.articleContent
                                recruitmentNumber = articleModel.recruitmentNumber.toString()
                                currentNumber = articleModel.currentNumber.toString()

                                Glide.with(binding.articleThumbnailBackground)
                                    .load(articleModel.articleThumbnailImageUrl)
                                    .into(binding.articleThumbnailBackground)
                                binding.articleThumbnailBackground.alpha = 0.5f
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@ArticleActivity, "????????? ?????? ??????", Toast.LENGTH_SHORT).show()
                    }
                })

                currentGroupUsersRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        userIdList.clear()

                        //????????? ?????? ???????????? ???????????? ?????????
                        for (snapshot in dataSnapshot.children) { //???????????? ?????? ????????? List??? ????????????.
                            val userModel = snapshot.getValue(UserModel::class.java)
                            if (userModel != null) {
                                Log.i("ArticleActivity", "userModel: $userModel")
                                userIdList.add(userModel.userId)
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {

                    }

                })

                currentUserRef.addListenerForSingleValueEvent(object: ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val userModel: UserModel? = snapshot.getValue(UserModel::class.java)
                        if (userModel != null) {
                            Log.d("onDataChange", "userName: ${userModel.userName}")
                            currentUserName = userModel.userName
                            currentUserProfileImage = userModel.userProfileImageUrl
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.d("onCancelled: ", "??????????????? ??????")
                    }
                })
            }.await()
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun joinGroup(userId: String, userName: String, userProfileImage: String) {

        val user = mutableMapOf<String, Any>()
        user[USER_ID] = userId
        user[USER_NAME] = userName
        user[USER_PROFILE_IMAGE_URL] = userProfileImage

        currentGroupUserRef.setValue(user)
//        currentGroupUserRef.updateChildren(user)

        //????????? ??? ???????????? ?????? ?????? ??????
        val group = mutableMapOf<String, Any>()
        group[CURRENT_NUMBER] = currentNumber.toInt() + 1
        currentGroupRef.updateChildren(group)

        val article = mutableMapOf<String, Any>()
        article[CURRENT_NUMBER] = currentNumber.toInt() + 1
        currentArticleRef.updateChildren(article)

        val userGroup = mutableMapOf<String,Any>()
        userGroup[GROUP_ID] = articleId
        userGroup[ARTICLE_TITLE] = articleTitle
        currentUserGroupRef.updateChildren(userGroup)

        hideProgress()

        Toast.makeText(this, "???????????? ?????????????????????. ????????? ??????????????????.", Toast.LENGTH_SHORT).show()

        finish()
    }

    // ?????????????????? ?????????
    private fun initRecyclerView(){
        // DB ?????? ????????? ?????? ????????? ?????????
        val tagData: ArrayList<TagModel> = arrayListOf()
        currentArticleRef.child(DB_MAIN_TAGS).orderByChild(TAG_INDEX)
            .limitToFirst(MAXSHOWTAG).addListenerForSingleValueEvent(object:ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    for(s in snapshot.children){
                        val newSubTag = arrayListOf<String>()
                        for(st in s.child(DBKeys.SUB_TAGS).children){
                            newSubTag.add(st.key.toString())
                        }
                        tagData.add(
                            TagModel(
                                s.key.toString(), newSubTag,
                                s.child(DBKeys.TAG_VALUE).value.toString().toInt(),
                                s.child(DBKeys.TAG_TYPE).value.toString().toInt()
                            ))
                    }
                    // ?????? ????????? ?????? ?????? RecyclerView ??? ??????????????? ????????? ????????? ?????? ????????? ??????
                    initTagRecyclerView(tagData)
                }
                override fun onCancelled(error: DatabaseError) {
                    return
                }
            })
        initTagRecyclerView(arrayListOf())
    }

    // ?????? ?????? ?????????????????? ??????
    private fun initTagRecyclerView(data: ArrayList<TagModel>){
        binding.tagRecyclerView.layoutManager = LinearLayoutManager(this)

        tagRecyclerAdapter = TagAdapter(this, data, false)
        // ??????????????? ???????????? ??? ????????? ??????
        tagRecyclerAdapter.subTagClickListener = object : TagAdapter.OnItemClickListener {
            override fun onItemClick(
                holder: TagAdapter.DefaultViewHolder,
                view: EditText,
                data: TagModel,
                position: Int
            ) {
                // ??????????????? ??????
                val intent = Intent()
                intent.putExtra("tag", view.text.toString())
                setResult(CommunityFragment.REQUEST_ARTICLE, intent)
                finish()
            }
        }
        binding.tagRecyclerView.adapter = tagRecyclerAdapter
    }

    private fun showProgress() {
        binding.progressBar.isVisible = true
    }

    private fun hideProgress() {
        binding.progressBar.isVisible = false
    }

    companion object {
        const val MAP_INFO = "MAP_INFO"
        const val ARTICLE_INFO = "ARTICLE_INFO"
        const val WRITER_INFO = "WRITER_INFO"
        // ??????????????? ????????? ????????? ??????
        const val MAXSHOWTAG = 10

    }
}