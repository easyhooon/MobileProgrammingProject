package kr.ac.konkuk.koogle.Fragment

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.yuyakaido.android.cardstackview.*
import kr.ac.konkuk.koogle.Activity.ArticleActivity
import kr.ac.konkuk.koogle.Activity.LogInActivity
import kr.ac.konkuk.koogle.Activity.MainActivity
import kr.ac.konkuk.koogle.Adapter.CardAdapter
import kr.ac.konkuk.koogle.DBKeys
import kr.ac.konkuk.koogle.DBKeys.Companion.ARTICLE_ID
import kr.ac.konkuk.koogle.DBKeys.Companion.DB_ARTICLES
import kr.ac.konkuk.koogle.DBKeys.Companion.DB_MAIN_TAGS
import kr.ac.konkuk.koogle.DBKeys.Companion.DB_USERS
import kr.ac.konkuk.koogle.DBKeys.Companion.WRITER_ID
import kr.ac.konkuk.koogle.Model.ArticleModel
import kr.ac.konkuk.koogle.Model.BlockUserModel
import kr.ac.konkuk.koogle.Model.CardModel
import kr.ac.konkuk.koogle.R
import kr.ac.konkuk.koogle.databinding.FragmentCardBinding

class CardFragment : Fragment(R.layout.fragment_card), CardStackListener {

    var binding: FragmentCardBinding? = null

    private lateinit var cardAdapter: CardAdapter

    private val cardList = mutableListOf<CardModel>()

    private var blockList = mutableListOf<String>()

    private val auth: FirebaseAuth by lazy {
        Firebase.auth
    }

    private val firebaseUser = auth.currentUser!!

    private val cardRef: DatabaseReference by lazy {
        Firebase.database.reference.child(DB_ARTICLES)
    }

    private val currentUserRef: DatabaseReference by lazy {
        Firebase.database.reference.child(DB_USERS).child(firebaseUser.uid)
    }

    private val currentUserBlockRef: DatabaseReference by lazy {
        currentUserRef.child(DBKeys.DB_BLOCK_USERS)
    }

    private val listener = object : ChildEventListener {
        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            if (snapshot.child(WRITER_ID).value != firebaseUser.uid) {
                val cardModel = snapshot.getValue(CardModel::class.java)
                    ?: return

                if(!blockList.contains(cardModel.writerId)){
                    cardModel.tagList = snapshot.child(DB_MAIN_TAGS)
                    cardList.add(cardModel)
                }

                cardAdapter.submitList(cardList)
                cardAdapter.notifyDataSetChanged()
            }
        }

        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
        override fun onChildRemoved(snapshot: DataSnapshot) {}
        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
        override fun onCancelled(error: DatabaseError) {}
    }

    private val manager by lazy {
        CardStackLayoutManager(context, this)
    }

//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        // Inflate the layout for this fragment
//        binding = FragmentCardBinding.inflate(layoutInflater, container, false)
//
//        initDB()
//
////        initCardStackView()
////        cardRef.addChildEventListener(listener)
//
//        return binding!!.root
//    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentCardBinding.bind(view)

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
        currentUserBlockRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (snapshot in dataSnapshot.children) {
                    val blockUserModel = snapshot.getValue(BlockUserModel::class.java)
                    if (blockUserModel != null) {
                        blockList.add(blockUserModel.userId)
                    }
                }
                initCardStackView()
                cardRef.addChildEventListener(listener)

            }


            override fun onCancelled(error: DatabaseError) {

            }

        })
    }

    private fun initCardStackView() {
        cardAdapter = CardAdapter(context)

        binding?.cardStackView?.layoutManager = CardStackLayoutManager(context, this)
        binding?.cardStackView?.adapter = cardAdapter

        manager.setStackFrom(StackFrom.Top)
        manager.setTranslationInterval(8.0f)
        manager.setSwipeThreshold(0.1f)

        cardAdapter.itemClickListener = object : CardAdapter.OnItemClickListener {
            override fun onItemChecked(
                holder: CardAdapter.ViewHolder,
                view: View,
                data: CardModel,
                position: Int
            ) {
                if (auth.currentUser != null) {
                    val intent = Intent(context, ArticleActivity::class.java)
                    intent.putExtra(ARTICLE_ID, data.articleId)

                    //fragment ?????? ?????? ??????????????? ????????? ??????
                    activity?.startActivity(intent)
                } else {
                    //???????????? ?????? ??????
                    Toast.makeText(context, "????????? ??? ??????????????????", Toast.LENGTH_LONG).show()
                }
            }

            override fun onItemCanceled(
                holder: CardAdapter.ViewHolder,
                view: View,
                data: CardModel,
                position: Int
            ) {
                //dialog ?????? ????????? ?????? ??????
                val ad = AlertDialog.Builder(context)
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
                    userBlock(data.writerId, data.writerName)
                    Toast.makeText(context, "?????? ????????? ?????????????????????", Toast.LENGTH_SHORT).show()
                    val intent = Intent(context, MainActivity::class.java)
                    startActivity(intent)
                    dialog.dismiss()
                }
                ad.show()

            }
        }
    }

    private fun userBlock(writerId: String, writerName: String) {
        val blockId = currentUserBlockRef.push().key.toString()

        val block = mutableMapOf<String, Any>()
        block[DBKeys.USER_ID] = writerId
        block[DBKeys.USER_NAME] = writerName

        currentUserBlockRef.child(blockId).updateChildren(block)

    }


//    override fun onResume() {
//        super.onResume()
//
//        //view??? ?????? ??????????????? ?????? ?????? ??????
//        cardAdapter.notifyDataSetChanged()
//    }

    override fun onDestroyView() {
        super.onDestroyView()

        cardRef.removeEventListener(listener)
    }

    override fun onCardSwiped(direction: Direction?) {}
    override fun onCardDragging(direction: Direction?, ratio: Float) {}
    override fun onCardRewound() {}
    override fun onCardCanceled() {}
    override fun onCardAppeared(view: View?, position: Int) {}
    override fun onCardDisappeared(view: View?, position: Int) {}
}