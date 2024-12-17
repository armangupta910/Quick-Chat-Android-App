package com.example.chat

import android.content.ContentValues.TAG
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider

class SignInActivity : AppCompatActivity() {

    private val RC_SIGN_IN = 9001
    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        auth = FirebaseAuth.getInstance()
        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)

        // Check if user is already signed in
        val savedEmail = sharedPreferences.getString("email", null)
        if (savedEmail != null) {
            Log.d(TAG, "User already signed in with email: $savedEmail")
            moveToHomeScreen()
        }

        // Configure Google Sign-In
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("556200857805-116m7qfp47n1a7lq7a01b5917rff9dra.apps.googleusercontent.com") // Replace with your client ID
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)

        val signInButton: Button = findViewById(R.id.sign_in_button)
        signInButton.setOnClickListener {
            Log.d(TAG, "Sign-In button clicked. Signing out from Google client to force account selection.")

            // Sign out from GoogleSignInClient to ensure the user selects an account each time
            googleSignInClient.signOut().addOnCompleteListener {
                Log.d(TAG, "GoogleSignInClient sign out completed.")

                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_SIGN_IN)
            }.addOnFailureListener {
                Log.e(TAG, "Failed to sign out from GoogleSignInClient: ${it.message}")
                // Still proceed to sign-in as a fallback
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_SIGN_IN)
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            Log.d(TAG, "Received result from Google Sign-In intent.")
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    Log.d(TAG, "Google Sign-In successful. Account ID: ${account.id}, Email: ${account.email}")
                    firebaseAuthWithGoogle(account)
                } else {
                    Log.e(TAG, "Google Sign-In failed. Account is null.")
                }
            } catch (e: ApiException) {
                Log.e(TAG, "Google Sign-In failed with error code: ${e.statusCode}, Message: ${e.message}")
                Toast.makeText(this, "Google Sign-In failed.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        Log.d(TAG, "Authenticating with Firebase using Google account: ${account.email}")
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Log.d(TAG, "Firebase authentication successful for user: ${user?.email}")
                    saveUserToPreferences(user)
                    moveToHomeScreen()
                } else {
                    Log.e(TAG, "Firebase authentication failed.", task.exception)
                    Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Firebase authentication encountered an error: ${exception.message}")
            }
    }

    private fun saveUserToPreferences(user: FirebaseUser?) {
        user?.let {
            Log.d(TAG, "Saving user details to SharedPreferences. Email: ${it.email}, Name: ${it.displayName}")
            sharedPreferences.edit()
                .putString("email", it.email)
                .putString("name", it.displayName)
                .apply()
        }
    }

    private fun moveToHomeScreen() {
        Log.d(TAG, "Navigating to HomeActivity.")
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}
