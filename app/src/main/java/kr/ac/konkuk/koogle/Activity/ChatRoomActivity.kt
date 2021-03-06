package kr.ac.konkuk.koogle.Activity

import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_chat_room.*
import kr.ac.konkuk.koogle.Adapter.ChatAdapter
import kr.ac.konkuk.koogle.DBKeys.Companion.ADMIN_ID
import kr.ac.konkuk.koogle.DBKeys.Companion.CHAT_CONTENT
import kr.ac.konkuk.koogle.DBKeys.Companion.CHAT_CREATED_AT
import kr.ac.konkuk.koogle.DBKeys.Companion.CHAT_ID
import kr.ac.konkuk.koogle.DBKeys.Companion.CURRENT_NUMBER
import kr.ac.konkuk.koogle.DBKeys.Companion.DB_ARTICLES
import kr.ac.konkuk.koogle.DBKeys.Companion.DB_GROUPS
import kr.ac.konkuk.koogle.DBKeys.Companion.DB_MESSAGES
import kr.ac.konkuk.koogle.DBKeys.Companion.DB_USERS
import kr.ac.konkuk.koogle.DBKeys.Companion.GROUP_ID
import kr.ac.konkuk.koogle.DBKeys.Companion.GROUP_LAST_CHAT
import kr.ac.konkuk.koogle.DBKeys.Companion.GROUP_LAST_CHAT_CREATED_AT
import kr.ac.konkuk.koogle.DBKeys.Companion.LEFT_CHAT
import kr.ac.konkuk.koogle.DBKeys.Companion.RIGHT_CHAT
import kr.ac.konkuk.koogle.DBKeys.Companion.WRITER_ID
import kr.ac.konkuk.koogle.DBKeys.Companion.WRITER_NAME
import kr.ac.konkuk.koogle.DBKeys.Companion.WRITER_PROFILE_IMAGE_URL
import kr.ac.konkuk.koogle.Model.ChatModel
import kr.ac.konkuk.koogle.Model.GroupModel
import kr.ac.konkuk.koogle.Model.UserModel
import kr.ac.konkuk.koogle.R
import kr.ac.konkuk.koogle.databinding.ActivityChatRoomBinding

class ChatRoomActivity : AppCompatActivity() {

    lateinit var binding: ActivityChatRoomBinding

    private var userIdList: MutableList<String> = mutableListOf()

    private lateinit var writerName: String

    private lateinit var writerId: String

    private lateinit var writerProfileImageUrl: String

    private lateinit var chatId: String

    private lateinit var groupId: String

    private lateinit var currentNumber: String

    private lateinit var adminId: String

    private val auth: FirebaseAuth by lazy {
        Firebase.auth
    }

    private val firebaseUser = auth.currentUser!!

    private val userRef: DatabaseReference by lazy {
        Firebase.database.reference.child(DB_USERS)
    }

    private val currentArticleRef: DatabaseReference by lazy {
        Firebase.database.reference.child(DB_ARTICLES).child(groupId)
    }

    private val currentGroupRef: DatabaseReference by lazy {
        Firebase.database.reference.child(DB_GROUPS).child(groupId)
    }

    private val currentUserRef: DatabaseReference by lazy {
        Firebase.database.reference.child(DB_USERS).child(firebaseUser.uid)
    }

    //?????? ?????? ??????
    private val currentGroupUsersRef: DatabaseReference by lazy {
        Firebase.database.reference.child(DB_GROUPS).child(groupId).child(DB_USERS)
    }

    //?????? ???????????? ????????????
    private val currentGroupUserRef: DatabaseReference by lazy {
        Firebase.database.reference.child(DB_GROUPS).child(groupId).child(DB_USERS)
            .child(firebaseUser.uid)
    }

    private val chatRef: DatabaseReference by lazy {
        currentGroupRef.child(DB_MESSAGES)
    }

    private val currentChatRef: DatabaseReference by lazy {
        chatRef.child(chatId)
    }

    private val currentUserGroupRef: DatabaseReference by lazy {
        Firebase.database.reference.child(DB_USERS).child(firebaseUser.uid).child(DB_GROUPS)
    }

    private val chatList = mutableListOf<ChatModel>()

    private val chatAdapter = ChatAdapter(this, chatList)

    private val listener = object : ChildEventListener {
        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            //model ????????? ????????? ??????????????? ????????????
            val chatModel = snapshot.getValue(ChatModel::class.java)
            chatModel ?: return

            if (chatModel.writerId == firebaseUser.uid) {
                chatModel.viewType = RIGHT_CHAT
            } else
                chatModel.viewType = LEFT_CHAT

            chatList.add(chatModel)
            chatAdapter.notifyDataSetChanged()
            binding.chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1);
        }

        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
        override fun onChildRemoved(snapshot: DataSnapshot) {}
        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
        override fun onCancelled(error: DatabaseError) {}

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initDB()
        initViews()
        initButton()

        chatRef.addChildEventListener(listener)
    }

    private fun initViews() {
        binding.chatRecyclerView.adapter = chatAdapter
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(this)

        setSupportActionBar(binding.articleToolbar)
        val actionBar = supportActionBar!!
        actionBar.apply {
            setDisplayShowCustomEnabled(true)
            setDisplayShowTitleEnabled(false) //?????? ????????? ?????????
            setDisplayHomeAsUpEnabled(true) // ???????????? ???????????? ?????? ????????????
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if(::adminId.isInitialized){
            if(firebaseUser.uid == adminId){
                val menuInflater = menuInflater
                menuInflater.inflate(R.menu.chat_admin_option_menu, menu)
            }
            else{
                val menuInflater = menuInflater
                menuInflater.inflate(R.menu.chat_option_menu, menu)
            }
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.exitChatRoom -> {
                if (firebaseUser.uid == adminId) {
                    //????????? ??????
                    //???, ?????? ???????????? ??????
                    //dialog ?????? ????????? ?????? ??????
                    val ad = AlertDialog.Builder(this@ChatRoomActivity)
                    ad.setMessage("?????? ?????? ????????? ?????????????????????????")
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
                        val intent = Intent(this@ChatRoomActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                        dialog.dismiss()
                    }
                    ad.show()
                } else {
                    //????????? ?????? ??????
                    //dialog ?????? ????????? ?????? ????????? ?????? ?????????
                    val ad = AlertDialog.Builder(this@ChatRoomActivity)
                    ad.setMessage("?????? ????????? ??????????????????????")
                    ad.setPositiveButton(
                        "?????????"
                    ) { dialog, _ ->
                        dialog.dismiss()
                    }
                    ad.setNegativeButton(
                        "???"
                    ) { dialog, _ ->
                        exitChatRoom()
                        val intent = Intent(this@ChatRoomActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                        dialog.dismiss()
                    }
                    ad.show()
                }

            }

            R.id.adminEvaluate -> {
                val intent = Intent(this@ChatRoomActivity, AddCommentActivity::class.java)
                intent.putExtra(ADMIN_ID, adminId)
                intent.putExtra(GROUP_ID, groupId)
                startActivity(intent)
                finish()
            }
            else -> {
                //????????????
                finish()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun exitChatRoom() {
        //currentNumber ??????
        currentGroupUserRef.setValue(null)
        val group = mutableMapOf<String, Any>()
        group[CURRENT_NUMBER] = currentNumber.toInt() - 1
        currentGroupRef.updateChildren(group)

        val article = mutableMapOf<String, Any>()
        article[CURRENT_NUMBER] = currentNumber.toInt() - 1
        currentArticleRef.updateChildren(article)

        //????????? ?????????????????? ?????? ????????? ??????
        currentUserGroupRef.setValue(null)
    }

    private fun deleteArticle() {
        //?????? ????????? group?????? ???????????? ??? ????????? ???????????????????
        //?????? ????????? ???????????? ?????? ????????? list??? ???????????? ????????? ????????? ????????? ??????

        //?????? ?????? ??????????????? ????????????
        for (userId in userIdList) {
            userRef.child(userId).child(DB_GROUPS).child(groupId).setValue(null)
        }

        currentArticleRef.setValue(null)
        currentGroupRef.setValue(null)
    }

    private fun initButton() {
        binding.messageEditText.addTextChangedListener {
            if (it.toString() == "") {
                binding.sendButton.isEnabled = false
            } else {
                binding.sendButton.isEnabled = true
            }
        }

        binding.sendButton.setOnClickListener {
            writerId = auth.currentUser?.uid.toString()
            val content = binding.messageEditText.text.toString()

            showProgress()

            sendChat(writerId, writerName, writerProfileImageUrl, content)

            binding.messageEditText.text = null
        }
    }

    private fun sendChat(
        writerId: String,
        writerName: String,
        writerProfileImageUrl: String,
        content: String
    ) {
        chatId = chatRef.push().key.toString()
        val message = mutableMapOf<String, Any>()

        message[CHAT_ID] = chatId
        message[WRITER_ID] = writerId
        message[WRITER_NAME] = writerName
        message[WRITER_PROFILE_IMAGE_URL] = writerProfileImageUrl
        message[CHAT_CONTENT] = content
        message[CHAT_CREATED_AT] = System.currentTimeMillis()

        currentChatRef.updateChildren(message)

        val group = mutableMapOf<String, Any>()
        group[GROUP_LAST_CHAT] = content
        group[GROUP_LAST_CHAT_CREATED_AT] = System.currentTimeMillis()

        currentGroupRef.updateChildren(group)

        hideProgress()
    }

    private fun initDB() {
        val intent = intent
        groupId = intent.getStringExtra(GROUP_ID).toString()
        currentGroupRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val groupModel: GroupModel? = snapshot.getValue(GroupModel::class.java)
                if (groupModel != null) {
                    adminId = groupModel.adminId
                    currentNumber = groupModel.currentNumber.toString()
                    binding.chatTitleTextView.text = groupModel.articleTitle
                    binding.currentNumberTextView.text = currentNumber
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("onCancelled: ", "??????????????? ??????")
            }

        })

        currentUserRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userModel: UserModel? = snapshot.getValue(UserModel::class.java)
                if (userModel != null) {
                    Log.d("onDataChange", "userName: ${userModel.userName}")
                    writerName = userModel.userName
                    writerProfileImageUrl = userModel.userProfileImageUrl
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("onCancelled: ", "??????????????? ??????")
            }
        })

        //????????? ?????? ?????? id ????????? ?????????
        currentGroupUsersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (snapshot in dataSnapshot.children) {
                    val userModel = snapshot.getValue(UserModel::class.java)
                    if (userModel != null) {
                        userIdList.add(userModel.userId)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("onCancelled: ", "??????????????? ??????")
            }

        })
    }

    private fun showProgress() {
        binding.progressBar.isVisible = true
    }

    private fun hideProgress() {
        binding.progressBar.isVisible = false
    }
}