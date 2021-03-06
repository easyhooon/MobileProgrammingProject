package kr.ac.konkuk.koogle.Activity

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.activity_add_article.*
import kr.ac.konkuk.koogle.Adapter.AddArticleImageAdapter
import kr.ac.konkuk.koogle.Adapter.TagAdapter
import kr.ac.konkuk.koogle.DBKeys
import kr.ac.konkuk.koogle.DBKeys.Companion.ADMIN_ID
import kr.ac.konkuk.koogle.DBKeys.Companion.ADMIN_NAME
import kr.ac.konkuk.koogle.DBKeys.Companion.ADMIN_PROFILE_IMAGE_URL
import kr.ac.konkuk.koogle.DBKeys.Companion.ARTICLE_CONTENT
import kr.ac.konkuk.koogle.DBKeys.Companion.ARTICLE_CREATED_AT
import kr.ac.konkuk.koogle.DBKeys.Companion.ARTICLE_ID
import kr.ac.konkuk.koogle.DBKeys.Companion.ARTICLE_IMAGE_FILE_NAME
import kr.ac.konkuk.koogle.DBKeys.Companion.ARTICLE_IMAGE_PATH
import kr.ac.konkuk.koogle.DBKeys.Companion.ARTICLE_IMAGE_URL
import kr.ac.konkuk.koogle.DBKeys.Companion.ARTICLE_THUMBNAIL_IMAGE_URL
import kr.ac.konkuk.koogle.DBKeys.Companion.ARTICLE_TITLE
import kr.ac.konkuk.koogle.DBKeys.Companion.CURRENT_NUMBER
import kr.ac.konkuk.koogle.DBKeys.Companion.DB_ARTICLES
import kr.ac.konkuk.koogle.DBKeys.Companion.DB_GROUPS
import kr.ac.konkuk.koogle.DBKeys.Companion.DB_MAIN_TAGS
import kr.ac.konkuk.koogle.DBKeys.Companion.DB_USERS
import kr.ac.konkuk.koogle.DBKeys.Companion.DESIRED_LOCATION
import kr.ac.konkuk.koogle.DBKeys.Companion.GROUP_ID
import kr.ac.konkuk.koogle.DBKeys.Companion.GROUP_LAST_CHAT
import kr.ac.konkuk.koogle.DBKeys.Companion.GROUP_LAST_CHAT_CREATED_AT
import kr.ac.konkuk.koogle.DBKeys.Companion.RECRUITMENT_NUMBER
import kr.ac.konkuk.koogle.DBKeys.Companion.USER_ID
import kr.ac.konkuk.koogle.DBKeys.Companion.USER_NAME
import kr.ac.konkuk.koogle.DBKeys.Companion.USER_PROFILE_IMAGE_URL
import kr.ac.konkuk.koogle.DBKeys.Companion.WRITER_ID
import kr.ac.konkuk.koogle.DBKeys.Companion.WRITER_NAME
import kr.ac.konkuk.koogle.DBKeys.Companion.WRITER_PROFILE_IMAGE_URL
import kr.ac.konkuk.koogle.Model.Entity.SearchResultEntity
import kr.ac.konkuk.koogle.Model.TagModel
import kr.ac.konkuk.koogle.Model.UserModel
import kr.ac.konkuk.koogle.databinding.ActivityAddArticleBinding

class AddArticleActivity : AppCompatActivity() {

    lateinit var binding: ActivityAddArticleBinding

    lateinit var writerProfileImageUrl: String

    lateinit var articleId: String

    lateinit var writerId: String

    private lateinit var writerName:String

    private lateinit var searchResult: SearchResultEntity

    private var isFirstImageUpdate = true
    private var selectedUriList: ArrayList<Uri> = arrayListOf()
    private var fileNameList: ArrayList<String> = arrayListOf()
    private var downloadedUrlList: ArrayList<String> = arrayListOf()

    private lateinit var imageAdapter: AddArticleImageAdapter
    private lateinit var tagRecyclerAdapter : TagAdapter

    private val auth: FirebaseAuth by lazy {
        Firebase.auth
    }
    private val firebaseUser = auth.currentUser!!

    private val storage: FirebaseStorage by lazy {
        Firebase.storage
    }

    private val currentUserRef: DatabaseReference by lazy {
        userRef.child(firebaseUser.uid)
    }
    private val articleRef: DatabaseReference by lazy {
        Firebase.database.reference.child(DB_ARTICLES)
    }
    private val groupRef: DatabaseReference by lazy {
        Firebase.database.reference.child(DB_GROUPS)
    }
    private val userRef: DatabaseReference by lazy {
        Firebase.database.reference.child(DB_USERS)
    }
    private val userGroupRef: DatabaseReference by lazy {
        Firebase.database.reference.child(DB_USERS).child(firebaseUser.uid).child(DB_GROUPS).child(articleId)
    }
    private val currentGroupRef: DatabaseReference by lazy {
        groupRef.child(articleId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddArticleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initDB()
        initButton()
        initImageRecyclerView()
        initRecyclerView()
    }

    private fun initDB() {
        currentUserRef.addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val userModel: UserModel? = snapshot.getValue(UserModel::class.java)
                if (userModel != null) {
                    writerName = userModel.userName
                    writerProfileImageUrl = userModel.userProfileImageUrl
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("onCancelled: ", "??????????????? ??????")
            }

        })
    }

    private fun initButton() {
        binding.imageAddButton.setOnClickListener {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    //Permission ??? ????????? ?????? ??????, ContentProvider ??? ?????????
                    startContentProvider()
                }
                //????????? ????????? ????????? ??????
                shouldShowRequestPermissionRationale(android.Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                    showPermissionContextPopup()
                }
                else -> {
                    requestPermissions(
                        arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                        1010
                    )
                }
            }
        }

        // ??? ?????? ?????? ?????? ?????????
        binding.submitButton.setOnClickListener {
            writerId = firebaseUser.uid
            articleId = articleRef.push().key.toString()
            val articleTitle = binding.titleEditText.text.toString()
            val articleContent = binding.contentEditText.text.toString()
            val recruitmentNumberText = binding.recruitmentNumberEditText.text.toString()


            if (articleTitle.isEmpty()) {
                Toast.makeText(this, "?????? ????????? ??????????????????.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (articleContent.isEmpty()) {
                Toast.makeText(this, "?????? ????????? ??????????????????.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (recruitmentNumberText.isEmpty()) {
                Toast.makeText(this, "??????????????? ??????????????????.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            showProgress()

            val recruitmentNumber = recruitmentNumberText.toInt()
            //????????? ???????????? ????????? ????????? ????????? ??????
            selectedUriList = imageAdapter.getUriList()

            if (selectedUriList.isNotEmpty()) {
                for ((i, uri) in selectedUriList.withIndex()) {
                    uploadPhoto(uri, i,
                        //?????? ?????????
                        successHandler = { uploadedUri ->
                            updateImage(uploadedUri)
                        },
                        errorHandler = {
                            Toast.makeText(this, "?????? ???????????? ??????????????????.", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
            uploadArticle(
                writerId,
                writerName,
                writerProfileImageUrl,
                articleId,
                articleTitle,
                articleContent,
                recruitmentNumber,
                downloadedUrlList,
                tagRecyclerAdapter.data
            )
            createChatRoom(
                writerId,
                writerName,
                writerProfileImageUrl,
                articleTitle,
                recruitmentNumber
            )

            addUserGroup(articleId, articleTitle)
        }

        binding.backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.locationAddButton.setOnClickListener {
            val intent = Intent(this, LocationSearchActivity::class.java)
            startActivityForResult(intent, LOCATION_SEARCH_REQUEST_CODE)
        }

        binding.tagAddButton.setOnClickListener {
            val intent = Intent(this, AddNewTagActivity::class.java)
            startActivityForResult(intent, AddArticleActivity.TAG_ADD_REQUEST_CODE)
        }
    }

    private fun addUserGroup(articleId: String, articleTitle: String) {
        val userGroup = mutableMapOf<String, Any>()
        userGroup[GROUP_ID] = articleId
        userGroup[ARTICLE_TITLE] = articleTitle
        userGroupRef.updateChildren(userGroup)
    }

    private fun createChatRoom(
        adminId: String,
        adminName: String,
        adminProfileImageUrl: String,
        articleTitle: String,
        recruitmentNumber: Int)
    {
        val group = mutableMapOf<String, Any>()

        group[GROUP_ID] = articleId
        group[ARTICLE_TITLE] = articleTitle
        group[ADMIN_ID] = adminId
        group[ADMIN_NAME] = adminName
        group[ADMIN_PROFILE_IMAGE_URL] = adminProfileImageUrl
        group[RECRUITMENT_NUMBER] = recruitmentNumber
        group[CURRENT_NUMBER] = 1
        group[GROUP_LAST_CHAT] = ""
        group[GROUP_LAST_CHAT_CREATED_AT] = 0

//        currentGroupRef.setValue(group)
        currentGroupRef.updateChildren(group)

        //????????? ?????? ?????? ??????(admin)??? ???????????? ???????????? ??????

        //???????????? ?????? ref??? ????????? ref??? ??????
        val currentGroupUserRef = currentGroupRef.child(DB_USERS).child(adminId)
        val user = mutableMapOf<String, Any>()
        user[USER_ID] = adminId
        user[USER_NAME] = adminName
        user[USER_PROFILE_IMAGE_URL] = adminProfileImageUrl

//        currentGroupUserRef.setValue(user)
        currentGroupUserRef.updateChildren(user)

        hideProgress()
        finish()

    }

    private fun uploadPhoto(uri: Uri, num: Int, successHandler: (String) -> Unit, errorHandler: () -> Unit) {
        //???????????? ????????? ?????? ?????????
        //????????? ?????? ?????? ????????? ????????? ??????
        val fileName = "${System.currentTimeMillis()}.jpg"
        fileNameList.add(fileName)
        storage.reference.child(ARTICLE_IMAGE_PATH).child(fileName)
            .putFile(uri)
            //??????????????? ????????? ??????
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    storage.reference.child(ARTICLE_IMAGE_PATH).child(fileName).downloadUrl
                        .addOnSuccessListener { uploadedUri ->
                            successHandler(uploadedUri.toString())
                        }.addOnFailureListener {
                            errorHandler()
                        }
                } else {
                    errorHandler()
                }
            }
    }
    private fun uploadArticle(
        writerId: String,
        writerName: String,
        writerProfileImageUrl: String,
        articleId: String,
        articleTitle: String,
        articleContent: String,
        recruitmentNumber: Int,
        uriList: ArrayList<String>,
        tagList: MutableList<TagModel>
    ) {
        val currentArticleRef = articleRef.child(articleId)
        val article = mutableMapOf<String, Any>()

        article[ARTICLE_ID] = articleId
        article[ARTICLE_TITLE] = articleTitle
        article[ARTICLE_CONTENT] = articleContent
        article[ARTICLE_IMAGE_URL] = uriList
        article[ARTICLE_CREATED_AT] = System.currentTimeMillis()
        article[WRITER_ID] = writerId
        article[WRITER_NAME] = writerName
        article[WRITER_PROFILE_IMAGE_URL] = writerProfileImageUrl
        article[RECRUITMENT_NUMBER] = recruitmentNumber
        article[CURRENT_NUMBER] = 1
        article[ARTICLE_IMAGE_FILE_NAME] = fileNameList
        if (::searchResult.isInitialized)
            article[DESIRED_LOCATION] = searchResult

        // ?????? ?????? ??????
        if(!tagList.isNullOrEmpty()){
            val tags = mutableMapOf<String, Any>()
            for((j, value) in tagList.withIndex()){
                val newTag = mutableMapOf<String, Any>()
                val newSubTag = mutableMapOf<String, Any>()
                for ((i, s) in value.sub_tag_list.withIndex()) {
                    val content = s.split(" ")
                    // ?????? ?????? ?????? ?????? ?????? ????????? ????????? ????????????.
                    if(content[0]==null || content[0]==""|| content[0]==" ")
                        continue
                    newSubTag[content[0]] = i
                }
                newTag[DBKeys.TAG_INDEX] = j
                newTag[DBKeys.SUB_TAGS] = newSubTag
                newTag[DBKeys.TAG_TYPE] = value.tag_type
                newTag[DBKeys.TAG_VALUE] = value.value
                tags[value.main_tag_name] = newTag
            }
            article[DB_MAIN_TAGS] = tags
            pushDBTag()
        }

//        currentArticleRef.updateChildren(article)
        currentArticleRef.setValue(article)

        hideProgress()
        finish()
    }

    // ?????? Tag DB ??? Tag ???????????? ?????? (?????? ?????? ??????)
    private fun pushDBTag(){
        var tagRef = Firebase.database.reference.child(DBKeys.DB_MAIN_TAGS)

        for ((key, value) in tagRecyclerAdapter.data) {
            tagRef.child(key)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        // DB ??? ?????? ?????? ??????: ?????? ?????? ??????
                        if (snapshot.childrenCount > 0) {
                            // ?????? ?????? ??????????????? ??????(=??????)
                            tagRef.child(key).child(DBKeys.USED).setValue(
                                snapshot.child(DBKeys.USED).value.toString().toInt() - 1
                            )
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
        }
    }

    private fun updateImage(uri: String) {
        if (isFirstImageUpdate) {
            articleRef.child(articleId).child(ARTICLE_THUMBNAIL_IMAGE_URL).setValue(uri)
            isFirstImageUpdate = false
        }
        val imageRef = articleRef.child(articleId).child(ARTICLE_IMAGE_URL)
        downloadedUrlList.add(uri)
        imageRef.setValue(downloadedUrlList)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1010 ->
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startContentProvider()
                } else {
                    Toast.makeText(this, "????????? ?????????????????????", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun startContentProvider() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.type = "image/*"
        startActivityForResult(Intent.createChooser(intent, ""), CONTENT_PROVIDER_REQUEST_CODE)
    }

    private fun showProgress() {
        binding.progressBar.isVisible = true
    }

    private fun hideProgress() {
        binding.progressBar.isVisible = false
    }
    // new Tag Activity ????????? ???????????? ???????????? ?????????????????? ???????????? ??????
    // resultData: new Tag Activity ????????? ???????????? Data
    private fun tossToAdapter(resultData: HashMap<String, TagModel>? = null){

        var newList:ArrayList<TagModel> = arrayListOf()
        for((key, value) in resultData!!){
            newList.add(value)
        }
        tagRecyclerAdapter.updateData(newList)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            TAG_ADD_REQUEST_CODE -> {
                // ??? ?????? ?????? ?????????????????? ???????????? ??????
                val data = data?.extras?.getSerializable("selectedTags")
                if(data!=null)
                    tossToAdapter(data  as HashMap<String, TagModel>)
            }
            CONTENT_PROVIDER_REQUEST_CODE -> {
                //data ?????? ????????? uri ??? ????????????
                //?????? null ??????
                if (data?.data != null && data?.clipData == null) {
                    initImageRecyclerView()
                    val uri = data.data

                    if (uri != null) {
                        imageAdapter.addItem(uri)
                    }

                    if (binding.imageRecyclerView.visibility == View.GONE)
                        binding.imageRecyclerView.visibility = View.VISIBLE
                }

                if (data?.clipData != null) {
                    initImageRecyclerView()

                    val clipData = data.clipData!!

                    when (clipData.itemCount) {
                        in 2..9 -> {
                            for (i in 0 until clipData.itemCount) {
                                imageAdapter.addItem(clipData.getItemAt(i).uri)
                            }
                        }
                        else -> {
                            Toast.makeText(this, "????????? ?????? 10??? ?????? ???????????????.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    if (binding.imageRecyclerView.visibility == View.GONE)
                        binding.imageRecyclerView.visibility = View.VISIBLE
                }
//                else {
//                    Toast.makeText(this, "????????? ???????????? ???????????????.", Toast.LENGTH_SHORT).show()
//                }
            }
            LOCATION_SEARCH_REQUEST_CODE -> {
                if(resultCode == RESULT_OK){
                    searchResult = data?.getParcelableExtra(SEARCH_RESULT_FINAL)!!
                    binding.locationTextView.text = searchResult.fullAddress
                }
            }
        }
    }

    //????????? ??????
    private fun showPermissionContextPopup() {
        AlertDialog.Builder(this)
            .setTitle("????????? ???????????????.")
            .setMessage("????????? ???????????? ?????? ???????????????.")
            .setPositiveButton("??????") { _, _ ->
                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 1010)
            }
            .create()
            .show()
    }

    private fun initImageRecyclerView() {
        binding.imageRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL,  false)
        imageAdapter = AddArticleImageAdapter(selectedUriList)
        imageAdapter.itemClickListener = object : AddArticleImageAdapter.OnItemClickListener {
            override fun OnItemClick(
                holder: AddArticleImageAdapter.ViewHolder
            ) {
                imageAdapter.removeItem(holder.adapterPosition)
                if (imageAdapter.getUriList().isEmpty())
                    binding.imageRecyclerView.visibility = View.GONE
            }
        }
        binding.imageRecyclerView.adapter = imageAdapter

        val simpleCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT,
            ItemTouchHelper.UP or ItemTouchHelper.DOWN) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                imageAdapter.moveItem(viewHolder.adapterPosition, target.adapterPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        }

        val itemTouchHelper = ItemTouchHelper(simpleCallback)
        itemTouchHelper.attachToRecyclerView(binding.imageRecyclerView)
    }

    // ?????? ?????? ?????????????????? ?????????
    private fun initRecyclerView(){
        initTagRecyclerView(arrayListOf())
    }

    // ?????? ?????? ?????????????????? ?????????
    private fun initTagRecyclerView(data: ArrayList<TagModel>){
        binding.tagRecyclerView.layoutManager = LinearLayoutManager(this)

        tagRecyclerAdapter = TagAdapter(this, data, true)
        // ??????????????? ???????????? ??? ????????? ??????
        tagRecyclerAdapter.subTagClickListener = object : TagAdapter.OnItemClickListener {
            override fun onItemClick(
                holder: TagAdapter.DefaultViewHolder,
                view: EditText,
                data: TagModel,
                position: Int
            ) {
            }
        }
        binding.tagRecyclerView.adapter = tagRecyclerAdapter
        val simpleCallBack = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.DOWN or ItemTouchHelper.UP,
            ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                tagRecyclerAdapter.moveItem(viewHolder.adapterPosition, target.adapterPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                tagRecyclerAdapter.removeItem(viewHolder.adapterPosition)
            }
        }
        val itemTouchHelper = ItemTouchHelper(simpleCallBack)
        itemTouchHelper.attachToRecyclerView(binding.tagRecyclerView)
    }

    companion object {
        const val LOCATION_SEARCH_REQUEST_CODE = 100
        const val CONTENT_PROVIDER_REQUEST_CODE = 200
        const val TAG_ADD_REQUEST_CODE = 300
        const val SEARCH_RESULT_FINAL = "SEARCH_RESULT_FINAL"
    }
}