package com.example.wifidirectchat2

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.wifidirectchat2.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), WiFiDirectChatApplication.Contract {
    private lateinit var binding: ActivityMainBinding
    private var handler: Handler? = null

    private val app by lazy { this.application as WiFiDirectChatApplication }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        handler = Handler(Looper.getMainLooper()) {
            val source: String = if (it.what == 2) "server" else "client"
            val length = it.arg1
            if (length <= 0) return@Handler true
            val msg: ByteArray = it.obj as ByteArray
            binding.textViewText.append("$source: ${String(msg)}\n")
            return@Handler true
        }

        binding.buttonSend.setOnClickListener {
            val message = binding.editTextMessage.text.toString()
            binding.textViewText.append("me: ${message}\n")
            app.sendMessage(message)

            binding.editTextMessage.text.clear()
        }

        binding.buttonStart.setOnClickListener {
            app.startServer(this)
        }

        binding.buttonJoin.setOnClickListener {
            app.join(this)
        }
    }

    override fun startServer() {
        TODO("Not yet implemented")
    }

    override fun showToast(msg: String, length: Int) {
        Toast.makeText(this, msg, length).show()
    }

    override fun requestPermission() {
        TODO("Not yet implemented")
    }

    override fun getHandler(): Handler {
        return handler!!
    }
}