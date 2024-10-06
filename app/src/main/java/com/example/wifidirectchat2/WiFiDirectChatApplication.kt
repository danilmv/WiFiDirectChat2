package com.example.wifidirectchat2

import android.app.Application
import android.os.Handler
import android.widget.Toast
import java.util.UUID

class WiFiDirectChatApplication : Application() {

    interface Contract {
        fun startServer()
        fun showToast(msg: String, length: Int = Toast.LENGTH_SHORT)
        fun requestPermission()

        fun getHandler(): Handler
    }

    fun startServer(act: Contract) {
    }

    fun stop() {
    }

    fun join(act: Contract) {
    }

    fun sendMessage(msg: String) {
    }

    companion object {
        const val TAG = "TAG_Application"
        const val name = "WiFiDirectChat"
        val uuid: UUID = UUID.fromString("f69e5342-9de2-11ee-8c90-0242ac120002")
    }
}