package com.example.wifidirectchat2

import android.app.Application
import android.os.Handler
import android.util.Log
import android.widget.Toast
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.UnknownHostException
import java.util.UUID

class WiFiDirectChatApplication : Application() {
    private var isServerStarted = false
    private var serverThread: Thread? = null
    private var serverSocket: ServerSocket? = null
    private var listOfClients = mutableListOf<ClientHandler>()

    private var socket: Socket? = null
    private lateinit var serverReader: DataInputStream
    private lateinit var serverWriter: DataOutputStream
    private var handler: Handler? = null

    interface Contract {
        fun startServer()
        fun showToast(msg: String, length: Int = Toast.LENGTH_SHORT)
        fun requestPermission()

        fun getHandler(): Handler
    }

    fun startServer(act: Contract) {
        Log.d(TAG, "startServer: ")
        if (isServerStarted) {
            act.showToast("server was already started")
            return
        }

        Log.d(TAG, "startServer: starting...")

        handler = act.getHandler()

        serverThread = Thread {
            try {
                serverSocket = ServerSocket(SERVER_PORT)
                Log.d(TAG, "startServer: serverSocket created: ${serverSocket?.inetAddress}")
                while (true) {
                    val socket = serverSocket!!.accept()
                    addClient(socket)

                    showToastFromThread("client connected")
                    Log.d(TAG, "startServer: client connected")
                }
            } catch (e: IOException) {
                Log.d(TAG, "startServer: thread exception: ${e.message}")
                showToastFromThread("startServer: thread exception: ${e.message}")
            }
        }.apply {
            isDaemon = true
            start()
        }

        isServerStarted = true

    }

    fun stop() {
        Log.d(TAG, "stop: ")
        if (serverSocket != null) serverSocket!!.close()
        if (serverThread != null) serverThread!!.interrupt()
        if (socket != null) socket!!.close()
    }

    fun join(act: Contract, address: String, port: Int = SERVER_PORT) {
        Log.d(TAG, "join: $address:$port")
        if (handler == null) handler = act.getHandler()

        Thread {
            try {
                socket = Socket(address, port)
                Log.d(TAG, "join: socket created: $socket")
                serverReader = DataInputStream(socket!!.getInputStream())
                serverWriter = DataOutputStream(socket!!.getOutputStream())

                while (true) {
                    processMessage(serverReader.readUTF())
                    showToastFromThread("client: received message from server")
                    Log.d(TAG, "join: client received a message")
                }
            } catch (e: IllegalArgumentException) {
                Log.d(TAG, "join: exception: ${e.message}")
                showToastFromThread("join: exception: ${e.message}")
            } catch (e: SecurityException) {
                Log.d(TAG, "join: exception: ${e.message}")
                showToastFromThread("join: exception: ${e.message}")
            } catch (e: UnknownHostException) {
                Log.d(TAG, "join: exception: ${e.message}")
                showToastFromThread("join: exception: ${e.message}")
            } catch (e: IOException) {
                Log.d(TAG, "join: exception: ${e.message}")
                showToastFromThread("join: exception: ${e.message}")
            }
        }.apply {
            isDaemon = true
            start()
        }
    }

    private fun showToastFromThread(msg: String) {

    }

    private fun processMessage(msg: String?) {
        Log.d(TAG, "processMessage: $msg")
        val readMsg = handler!!.obtainMessage(
            if (isServerStarted) 1 else 2, 1, -1, msg
        )
        readMsg.sendToTarget()
    }

    fun sendAll(msg: String) {
        Thread{ //in order to bypass android.os.NetworkOnMainThreadException... should be a better solution
            if (isServerStarted) {
                Log.d(TAG, "sendAll: sending message ... from server")
                listOfClients.forEach {
                    it.sendMessage(msg)
                }
            }
            if (socket != null) {
                Log.d(TAG, "sendAll: sending message ... from client")
                serverWriter.writeUTF(msg)
            }
        }.apply {
            start()
        }
    }

    private fun addClient(socket: Socket) {
        listOfClients.add(ClientHandler(socket))
    }

    inner class ClientHandler(val socket: Socket) {
        private lateinit var input: DataInputStream
        private lateinit var output: DataOutputStream
        private var clientConnected = false

        init {
            clientConnected = true
            try {
                input = DataInputStream(socket.getInputStream())
                output = DataOutputStream(socket.getOutputStream())

                sendMessage("Hello, client")
                Thread {
                    try {
                        while (clientConnected) {
                            processMessage(input.readUTF())
                        }

                        socket.close()
                    } catch (e: IOException) {
                        Log.d(TAG, "client handler: exception: ${e.message} ")
                    }

                }.apply {
                    isDaemon = true
                    start()
                }
            } catch (e: IOException) {
                Log.d(TAG, "client handling: exception: ${e.message}")
            }
        }

        fun sendMessage(message: String) {
            try {
                output.writeUTF(message)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        const val TAG = "@@TAG_Application"
        const val name = "WiFiDirectChat"
        val uuid: UUID = UUID.fromString("f69e5342-9de2-11ee-8c90-0242ac120002")

        const val SERVER_PORT = 9876
    }
}