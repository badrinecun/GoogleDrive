package com.example.googledrivesignindemo

import android.R.attr.data
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.identity.SignInCredential
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.drive.Drive
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider


class LoginActivity : AppCompatActivity() {
    companion object{
        const val CONST_SIGN_IN =34
    }
    lateinit var btn: Button
    private lateinit var auth:FirebaseAuth
    private lateinit var googleAuth: GoogleSignInClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        btn=findViewById(R.id.btn)
        auth = FirebaseAuth.getInstance()

        btn.setOnClickListener{
            googleSignIn()
        }
        val gso = GoogleSignInOptions
            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Drive.SCOPE_FILE,Drive.SCOPE_APPFOLDER)
            .requestIdToken(getString(R.string.clientid))
            .requestEmail()
            .build()
        googleAuth=   GoogleSignIn.getClient(this@LoginActivity,gso)
    }

    @SuppressLint("SuspiciousIndentation")
    private fun firebaseAuthWithGoogle(idToken:String?){
        val credential = GoogleAuthProvider.getCredential(idToken,null)

            auth.signInWithCredential(credential)
                .addOnCompleteListener(this,
                    OnCompleteListener<AuthResult?> { task ->
                        if (task.isSuccessful) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("sign in success", "signInWithCredential:success")
                            val user = auth.currentUser
                            startActivity(Intent(this@LoginActivity,MainActivity::class.java))
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("sign in fail", "signInWithCredential:failure", task.exception)
                            //updateUI(null)
                        }
                    })

    }
    private fun googleSignIn() {
        val account = GoogleSignIn.getLastSignedInAccount(this@LoginActivity)
        if(account == null){
            val signInIntent = googleAuth.signInIntent
            startActivityForResult(signInIntent, CONST_SIGN_IN)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == CONST_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)

                firebaseAuthWithGoogle(account.idToken)


            }catch(e:ApiException){
             Toast.makeText(this@LoginActivity,"${e}",Toast.LENGTH_LONG).show()
            }
        }
    }
}