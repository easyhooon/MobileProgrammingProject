package kr.ac.konkuk.koogle.Fragment

import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.text.TextUtils
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kr.ac.konkuk.koogle.Activity.AddArticleActivity
import kr.ac.konkuk.koogle.Activity.ArticleActivity
import kr.ac.konkuk.koogle.Activity.LogInActivity
import kr.ac.konkuk.koogle.Adapter.CommunityAdapter
import kr.ac.konkuk.koogle.DBKeys
import kr.ac.konkuk.koogle.DBKeys.Companion.ARTICLE_CONTENT
import kr.ac.konkuk.koogle.DBKeys.Companion.ARTICLE_ID
import kr.ac.konkuk.koogle.DBKeys.Companion.ARTICLE_TITLE
import kr.ac.konkuk.koogle.DBKeys.Companion.DB_ARTICLES
import kr.ac.konkuk.koogle.DBKeys.Companion.DB_BLOCK_USERS
import kr.ac.konkuk.koogle.DBKeys.Companion.DB_MAIN_TAGS
import kr.ac.konkuk.koogle.DBKeys.Companion.DB_USERS
import kr.ac.konkuk.koogle.DBKeys.Companion.SUB_TAGS
import kr.ac.konkuk.koogle.Model.ArticleModel
import kr.ac.konkuk.koogle.Model.BlockUserModel
import kr.ac.konkuk.koogle.Model.GroupModel
import kr.ac.konkuk.koogle.Model.TagModel
import kr.ac.konkuk.koogle.R
import kr.ac.konkuk.koogle.databinding.FragmentCommunityBinding


class CommunityFragment : Fragment(R.layout.fragment_community) {
    private var binding: FragmentCommunityBinding? = null

    private lateinit var communityAdapter: CommunityAdapter

    private var blockList = mutableListOf<String>()

    private val articleList = mutableListOf<ArticleModel>()
    private var searchedArticleList = mutableListOf<ArticleModel>()

    private val auth: FirebaseAuth by lazy {
        Firebase.auth
    }

    private val firebaseUser = auth.currentUser!!
    private val articleRef: DatabaseReference by lazy {
        Firebase.database.reference.child(DB_ARTICLES)
    }
    private val currentUserRef: DatabaseReference by lazy {
        Firebase.database.reference.child(DB_USERS).child(firebaseUser.uid)
    }

    private val currentUserBlockRef: DatabaseReference by lazy {
        currentUserRef.child(DB_BLOCK_USERS)
    }

    private val listener = object : ChildEventListener {
        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            //model ????????? ????????? ??????????????? ????????????
            val articleModel = snapshot.getValue(ArticleModel::class.java)
            articleModel ?: return

            if (!blockList.contains(articleModel.writerId)) {
                //???????????? ?????? ???????????????
                articleList.add(0, articleModel)
            }

            communityAdapter.submitList(articleList)
            communityAdapter.notifyDataSetChanged()
        }

        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
        override fun onChildRemoved(snapshot: DataSnapshot) {}
        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
        override fun onCancelled(error: DatabaseError) {}
    }

//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        // Inflate the layout for this fragment
//        binding = FragmentCommunityBinding.inflate(layoutInflater, container, false)
//
//        //???????????? ????????? ????????? ?????? ?????? ??????????????? ?????? ?????? ???????????? ?????????
//
//        initRecyclerView()
//        initButton()
//
//        //???????????? ?????????
//        //addSingleValueListener -> ?????????, 1?????? ??????
//        //addChildEventListener -> ?????? ?????????????????? ?????? ???????????? ?????????????????? ???????????????.
//        //activity ??? ?????? activity ??? ???????????? ???????????? ??? ???????????? view ??? ??? destroy ???
//        //fragment ??? ???????????? ??????????????? onviewcreated ??? ?????????????????? ???????????? ???????????? ???????????????
//        //????????? eventlistener ??? ???????????? ????????? ????????? viewcreated ???????????? attach ??? ?????? destroy ??? ???????????? remove ??? ????????? ????????? ??????
//        articleRef.addChildEventListener(listener)
//
//        return binding!!.root
//    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentCommunityBinding.bind(view)

        //???????????? ????????? ????????? ?????? ?????? ??????????????? ?????? ?????? ???????????? ?????????

        if(auth.currentUser != null) {
            Log.i("Community fragment", "onViewCreated: ${firebaseUser.uid}")
            initDB()
//            initRecyclerView()
//            initButton()
//
//            //???????????? ?????????
//            //addSingleValueListener -> ?????????, 1?????? ??????
//            //addChildEventListener -> ?????? ?????????????????? ?????? ???????????? ?????????????????? ???????????????.
//            //activity ??? ?????? activity ??? ???????????? ???????????? ??? ???????????? view ??? ??? destroy ???
//            //fragment ??? ???????????? ??????????????? onviewcreated ??? ?????????????????? ???????????? ???????????? ???????????????
//            //????????? eventlistener ??? ???????????? ????????? ????????? viewcreated ???????????? attach ??? ?????? destroy ??? ???????????? remove ??? ????????? ????????? ??????
//            articleRef.addChildEventListener(listener)
        }
        else {
            val intent = Intent(context, LogInActivity::class.java)
            activity?.startActivity(intent)
        }
    }

    private fun initDB() {
        currentUserBlockRef.addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for(snapshot in dataSnapshot.children){
                    val blockUserModel = snapshot.getValue(BlockUserModel::class.java)
                    if (blockUserModel != null) {
                        blockList.add(blockUserModel.userId)
                    }
                }
                initRecyclerView()
                initButton()

                //???????????? ?????????
                //addSingleValueListener -> ?????????, 1?????? ??????
                //addChildEventListener -> ?????? ?????????????????? ?????? ???????????? ?????????????????? ???????????????.
                //activity ??? ?????? activity ??? ???????????? ???????????? ??? ???????????? view ??? ??? destroy ???
                //fragment ??? ???????????? ??????????????? onviewcreated ??? ?????????????????? ???????????? ???????????? ???????????????
                //????????? eventlistener ??? ???????????? ????????? ????????? viewcreated ???????????? attach ??? ?????? destroy ??? ???????????? remove ??? ????????? ????????? ??????
                articleRef.addChildEventListener(listener)
            }


            override fun onCancelled(error: DatabaseError) {

            }

        })
    }

    private fun initRecyclerView() {
        articleList.clear()
        //????????? ??????
        communityAdapter = CommunityAdapter(onItemClicked = { articleModel ->
            if (auth.currentUser != null) {
                val intent = Intent(context, ArticleActivity::class.java)
                intent.putExtra(ARTICLE_ID, articleModel.articleId)

                //fragment?????? ?????? ??????????????? ????????? ??????
                Log.d("CommunityFragment", "articleId: ${articleModel.articleId}")
                startActivityForResult(intent, REQUEST_ARTICLE)
            } else {
                //???????????? ?????? ??????
                Toast.makeText(context, "????????? ??? ??????????????????", Toast.LENGTH_LONG).show()
            }
        })

        binding!!.articleRecyclerView.layoutManager = LinearLayoutManager(context)
        binding!!.articleRecyclerView.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayoutManager.VERTICAL
            )
        )
        binding!!.articleRecyclerView.adapter = communityAdapter
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("jan", "ok")
        // ????????? ????????? ?????? (?????? ??????)
        if(requestCode == REQUEST_ARTICLE){
            val str:String? = data?.getStringExtra("tag")
            Log.d("jan", "go ${str}")
            if(!str.isNullOrBlank()){
                Log.d("jan", str)
                binding!!.searchEditText.setText(str)
                searchArticle()
            }
        }
    }

    private fun searchArticle(){
        val searchText = binding!!.searchEditText.text.toString()
        val articleRef = Firebase.database.reference.child(DB_ARTICLES)

        if (searchText.isEmpty()) {
            articleRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    articleList.clear()
                    for (article in snapshot.children) {
                        articleList.add(0, article.getValue(ArticleModel::class.java)!!)
                    }
                    communityAdapter.submitList(articleList)
                    communityAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {}

            })
        } else {
            val position = binding!!.searchSpinner.selectedItemPosition

            if (position == 0) {
                searchedArticleList.clear()
                articleRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (article in snapshot.children) {
                            for (tag in article.child(DB_MAIN_TAGS).children) {
                                var isContain = false
                                if (searchText in tag.key.toString()) {
                                    searchedArticleList.add(
                                        0,
                                        article.getValue(ArticleModel::class.java)!!
                                    )
                                    break
                                }
                                for (subtag in tag.child(SUB_TAGS).children) {
                                    if (searchText in subtag.key.toString()) {
                                        searchedArticleList.add(
                                            0,
                                            article.getValue(ArticleModel::class.java)!!
                                        )
                                        isContain = true
                                        break
                                    }
                                }
                                if (isContain) {
                                    break
                                }
                            }
                        }
                        communityAdapter.submitList(searchedArticleList)
                        communityAdapter.notifyDataSetChanged()
                    }

                    override fun onCancelled(error: DatabaseError) {}

                })
            } else if (position == 1) {
                searchedArticleList.clear()
                articleRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (article in snapshot.children) {
                            if (searchText in article.child(ARTICLE_TITLE).value.toString()) {
                                searchedArticleList.add(
                                    0,
                                    article.getValue(ArticleModel::class.java)!!
                                )
                            }
                        }
                        communityAdapter.submitList(searchedArticleList)
                        communityAdapter.notifyDataSetChanged()
                    }

                    override fun onCancelled(error: DatabaseError) {}

                })
            } else {
                searchedArticleList.clear()
                articleRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (article in snapshot.children) {
                            if (searchText in article.child(ARTICLE_CONTENT).value.toString()) {
                                searchedArticleList.add(
                                    0,
                                    article.getValue(ArticleModel::class.java)!!
                                )
                            }
                        }
                        communityAdapter.submitList(searchedArticleList)
                        communityAdapter.notifyDataSetChanged()
                    }

                    override fun onCancelled(error: DatabaseError) {}

                })
            }
        }
    }

    private fun initButton() {
        val spinner = binding!!.searchSpinner
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.search_spinner,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }

        binding!!.btnAddArticle.setOnClickListener {

            context?.let {
                if (auth.currentUser != null) {
                    startActivity(Intent(it, AddArticleActivity::class.java))
                } else {
                    Toast.makeText(context, "????????? ??? ??????????????????", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(it, LogInActivity::class.java))
                }

                //????????? ??????
                //startActivity(Intent(requireContext(),ArticleAddActivity::class.java))
            }
        }

        binding!!.searchImageView.setOnClickListener {
             searchArticle()
        }
    }

//    override fun onResume() {
//        super.onResume()
//
//        //view??? ?????? ??????????????? ?????? ?????? ??????
//        communityAdapter.notifyDataSetChanged()
//    }

    override fun onDestroyView() {
        super.onDestroyView()

        articleRef.removeEventListener(listener)
    }

    companion object{
        const val REQUEST_ARTICLE = 177
    }
}