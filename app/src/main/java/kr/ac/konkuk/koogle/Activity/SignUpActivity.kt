package kr.ac.konkuk.koogle.Activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kr.ac.konkuk.koogle.DBKeys.Companion.DB_USERS
import kr.ac.konkuk.koogle.DBKeys.Companion.USER_EMAIL
import kr.ac.konkuk.koogle.DBKeys.Companion.USER_ID
import kr.ac.konkuk.koogle.DBKeys.Companion.USER_NAME
import kr.ac.konkuk.koogle.DBKeys.Companion.USER_PROFILE_IMAGE_URL
import kr.ac.konkuk.koogle.databinding.ActivitySignUpBinding

class SignUpActivity : AppCompatActivity() {

    private val auth:FirebaseAuth by lazy {
        Firebase.auth
    }

    lateinit var binding: ActivitySignUpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initSignUpButton()
    }

    private fun initSignUpButton() {
        binding.signUpButton.setOnClickListener {
            val username = binding.nameEditText.text.toString()
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            if (username.isEmpty()) {
                Toast.makeText(this, LogInActivity.ENTER_EMAIL, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (email.isEmpty()) {
                Toast.makeText(this, LogInActivity.ENTER_EMAIL, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                Toast.makeText(this, LogInActivity.ENTER_PASSWORD, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        handleSuccessSignUp(username, email)

                        Toast.makeText(this, SIGN_UP_SUCCESS, Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this, SIGN_UP_FAIL, Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun handleSuccessSignUp(name: String, email: String) {
        if (auth.currentUser == null) {
            startActivity(Intent(this, LogInActivity::class.java))
            return
        } else {
            //currentUser ??? nullable ?????? ????????? ?????? ?????????????????????
            val userId = auth.currentUser?.uid.orEmpty()
            //reference ??? ?????????-> child
            //child ??? ?????? ??????
            //????????? ???????????? ????????? ??????, ????????? ??? ????????? ?????????
            val currentUserRef = Firebase.database.reference.child(DB_USERS).child(userId)
            val user = mutableMapOf<String, Any>()
            user[USER_ID] = userId
            user[USER_NAME] = name
            user[USER_EMAIL] = email
            user[USER_PROFILE_IMAGE_URL] = ""
            currentUserRef.updateChildren(user)

            startActivity(Intent(this, LogInActivity::class.java))
            //?????? ???????????? ??????????????? ??????
            finish()
        }
    }

    companion object {
        const val SIGN_UP_SUCCESS = "??????????????? ??????????????????. ????????? ????????? ?????? ?????????????????????."
        const val SIGN_UP_FAIL = "?????? ????????? ??????????????????, ??????????????? ??????????????????."
    }
}