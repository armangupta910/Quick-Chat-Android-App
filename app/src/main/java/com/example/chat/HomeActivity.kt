package com.example.chat

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.zxing.BarcodeFormat
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import com.journeyapps.barcodescanner.BarcodeEncoder

class HomeActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)

        val profileImageView: ImageView = findViewById(R.id.profile)

        // Get the Google account
        val account: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(this)

        if (account != null) {
            // Get the profile image URL
            val profileImageUrl = account.photoUrl

            // Load the image into ImageView using Glide
            Glide.with(this)
                .load(profileImageUrl)
                .placeholder(R.mipmap.ic_launcher) // Optional placeholder
                .error(R.mipmap.ic_launcher) // Optional error image
                .circleCrop() // Optional for circular crop
                .into(profileImageView)
        }

        // Get Firebase UID
        val currentUser = FirebaseAuth.getInstance().currentUser

        findViewById<Button>(R.id.chat).setOnClickListener{

            val friendUID:String =findViewById<EditText>(R.id.friendUID).text.toString()
            val intent = Intent(this,ChatActivity::class.java)

            intent.putExtra("friendUID",friendUID)
            startActivity(intent)
        }


        findViewById<ImageView>(R.id.logout).setOnClickListener{
            sharedPreferences.edit().clear().apply()
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this,SignInActivity::class.java))
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val result: IntentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {


                // You can now use the scanned content for your purpose
            } else {
                // Scanning cancelled
                Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    }
}