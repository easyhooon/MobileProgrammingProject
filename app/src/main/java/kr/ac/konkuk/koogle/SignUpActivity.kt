package kr.ac.konkuk.koogle

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kr.ac.konkuk.koogle.databinding.ActivitySignUpBinding

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    lateinit var binding: ActivitySignUpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        initSignUpButton()
    }

    private fun initSignUpButton() {
        binding.signUpButton.setOnClickListener {
            val name = binding.nameEditText.text.toString()
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            if (name.isEmpty()) {
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
                        Toast.makeText(this, SIGN_UP_SUCCESS, Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this, SIGN_UP_FAIL, Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    companion object {
        const val SIGN_UP_SUCCESS = "회원가입을 성공했습니다. 로그인 버튼을 눌러 로그인해주세요."
        const val SIGN_UP_FAIL = "이미 가입한 이메일이거나, 회원가입에 실패했습니다."
    }
}