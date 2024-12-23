package com.example.chat

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import ua.naiksoftware.stomp.Stomp
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent


class ChatActivity : AppCompatActivity() {

    private lateinit var messageInput: EditText
    private lateinit var sendButton: ImageView

    private lateinit var friendUID: String
    private lateinit var roomID:String

    private lateinit var stompClient: StompClient
    private val disposables = CompositeDisposable()

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private val messageList = mutableListOf<Message>()
    private val gravities = mutableListOf<Int>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

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


        friendUID = intent.getStringExtra("friendUID").toString()
//        Toast.makeText(this,"$friendUID",Toast.LENGTH_SHORT).show()

        val uid:String = FirebaseAuth.getInstance().currentUser?.email.toString()

        roomID = (minOf(friendUID, uid) + maxOf(friendUID, uid)).lowercase()

//        Toast.makeText(this,"${roomID}",Toast.LENGTH_SHORT).show()

//        roomID = "ABCD"

        messageInput = findViewById(R.id.message_input)
        sendButton = findViewById(R.id.send)

        // Initialize the WebSocket client
        initStompClient()

        // Send button action
        sendButton.setOnClickListener {
            val sender = FirebaseAuth.getInstance().currentUser?.email.toString()
            val content = messageInput.text.toString().trim()
            if (sender.isNotEmpty() && content.isNotEmpty()) {
                sendMessage(sender, content)
                messageInput.text.clear()
            }
        }

        chatRecyclerView = findViewById(R.id.chat_recycler_view)
        chatAdapter = ChatAdapter(messageList, gravities)
        chatRecyclerView.adapter = chatAdapter
        chatRecyclerView.layoutManager = LinearLayoutManager(this)



    }

    private fun displayMessage(message: String) {
        try {
            val jsonObject = org.json.JSONObject(message)
            val sender = jsonObject.getString("sender")
            val content = jsonObject.getString("content")

            val x:Int
            if(sender.toString() == FirebaseAuth.getInstance().currentUser?.email.toString()){
                x  = 1
            }
            else{
                x = 0
            }

            // Add the message to the list and update the RecyclerView
            val newMessage = Message(sender, content)
            chatAdapter.addMessage(newMessage, x)

            // Scroll to the latest message
            chatRecyclerView.scrollToPosition(messageList.size - 1)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing message: $message", e)
        }
    }



    private fun initStompClient() {
        // Initialize Stomp client with your backend WebSocket URL
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, "ws://<server-url>:<port-number>/ws-chat")
        stompClient.connect()

        // Handle WebSocket connection
        disposables.add(
            stompClient.lifecycle()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ lifecycleEvent ->
                    when (lifecycleEvent.type) {
                        LifecycleEvent.Type.OPENED -> {
                            Toast.makeText(this, "Connected to the chat!", Toast.LENGTH_SHORT).show()
                            Log.d(TAG, "WebSocket connection opened.")
                        }
                        LifecycleEvent.Type.ERROR -> {
                            Log.e(TAG, "Connection error: ${lifecycleEvent.exception?.message}", lifecycleEvent.exception)
                        }
                        LifecycleEvent.Type.CLOSED -> {
                            Toast.makeText(this, "Disconnected from the chat!", Toast.LENGTH_SHORT).show()
                            Log.w(TAG, "WebSocket connection closed.")
                            if (lifecycleEvent.exception != null) {
                                Log.e(TAG, "Disconnection reason: ${lifecycleEvent.exception.message}", lifecycleEvent.exception)
                            }
                        }
                        LifecycleEvent.Type.FAILED_SERVER_HEARTBEAT -> {
                            Log.w(TAG, "Server heartbeat failed.")
                        }
                    }
                }, { error ->
                    Log.e(TAG, "Lifecycle error: ${error.message}", error)
                })
        )

        // Subscribe to topic for receiving messages
        disposables.add(
            stompClient.topic("/topic/${roomID}/messages")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ topicMessage ->
                    displayMessage(topicMessage.payload)
                    Log.d(TAG, "Message received: ${topicMessage.payload}")
                }, { error ->
                    Log.e(TAG, "Subscription error: ${error.message}", error)
                })
        )
    }


    private fun sendMessage(sender: String, content: String) {
        val message = """
            {
                "sender": "$sender",
                "content": "$content",
                "recipient": "AllUsers"
            }
        """.trimIndent()

        // Send message to the server
        disposables.add(
            stompClient.send("/app/${roomID}/sendMessage", message)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
//                    appendMessageToUI("${findViewById<EditText>(R.id.sender_input).text} : $content")
                }, { error ->
//                    appendMessageToUI("Error sending message: ${error.message}")
                })
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        // Disconnect and clean up resources
        stompClient.disconnect()
        disposables.clear()
        Toast.makeText(this,"Disconnected from the chat!",Toast.LENGTH_SHORT).show()
    }


}
