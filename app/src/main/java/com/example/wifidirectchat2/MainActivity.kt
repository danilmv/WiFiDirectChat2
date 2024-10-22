package com.example.wifidirectchat2

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.ActionListener
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.wifidirectchat2.databinding.ActivityMainBinding
import java.net.InetAddress

/**
 * An activity that uses WiFi Direct APIs to discover and connect with available
 * devices. WiFi Direct APIs are asynchronous and rely on callback mechanism
 * using interfaces to notify the application of operation success or failure.
 * The application should also register a BroadcastReceiver for notification of
 * WiFi state related events.
 */
class MainActivity :
    AppCompatActivity(),
    WiFiDirectChatApplication.Contract,
    WifiP2pManager.ChannelListener,
    NewDeviceListFragment.DeviceActionListener,
    WiFiDirectBroadcastReceiver.BroadcastContract {
    private lateinit var binding: ActivityMainBinding
    private var handler: Handler? = null

    private val app by lazy { this.application as WiFiDirectChatApplication }


    private val intentFilter = IntentFilter()
    private lateinit var channel: WifiP2pManager.Channel
    private lateinit var manager: WifiP2pManager
    private lateinit var receiver: WiFiDirectBroadcastReceiver

    private var isWifiP2pEnabled: Boolean = false
    private var retryChannel = false

    private val fragmentList = NewDeviceListFragment()
    private val fragmentDetail = NewDeviceDetailFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate: ")
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        handler = Handler(Looper.getMainLooper()) {
            val source: String = if (it.what == 2) "server" else "client"
            val length = it.arg1
            if (length <= 0) return@Handler true
//            val msg: ByteArray = it.obj as ByteArray
            val msg: String = it.obj as String
            binding.textViewText.append("$source: ${msg}\n")
            return@Handler true
        }

        binding.buttonSend.setOnClickListener {
            val message = binding.editTextMessage.text.toString()
            binding.textViewText.append("me: ${message}\n")
            app.sendAll(message)

            binding.editTextMessage.text.clear()
        }

//        binding.buttonStart.setOnClickListener {
//            app.startServer(this)
//        }
//
//        binding.buttonJoin.setOnClickListener {
//            app.join(this)
//        }


        // Indicates a change in the Wi-Fi Direct status.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)

        // Indicates a change in the list of available peers.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)

        // Indicates the state of Wi-Fi Direct connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)

        // Indicates this device's details have changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)

        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager.initialize(this, mainLooper, null)

        showFragments()
    }

    private fun showFragments() {
        Log.d(TAG, "showFragments: ")
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.frag_list, fragmentList)
            .replace(R.id.frag_detail, fragmentDetail)
            .commit()
    }

    override fun startServer() {
        Log.d(TAG, "startServer: ")
        app.startServer(this)
    }

    override fun join(address: String) {
        Log.d(TAG, "join: ")
        app.join(this, address)
    }

    override fun showToast(msg: String, length: Int) {
        Log.d(TAG, "showToast: ")
        Toast.makeText(this, msg, length).show()
    }

    override fun requestPermission() {
        Log.d(TAG, "requestPermission: ")
        TODO("Not yet implemented")
    }

    override fun getHandler(): Handler {
        return handler!!
    }

    override fun onResume() {
        Log.d(TAG, "onResume: ")
        super.onResume()
        receiver = WiFiDirectBroadcastReceiver(manager, channel, this)
        registerReceiver(receiver, intentFilter)
    }

    override fun onPause() {
        Log.d(TAG, "onPause: ")
        super.onPause()
        unregisterReceiver(receiver)
    }

    override fun onStop() {
        super.onStop()
        app.stop()
    }

    fun setIsWifiP2pEnabled(isWifiP2pEnabled: Boolean) {
        Log.d(TAG, "setIsWifiP2pEnabled: $isWifiP2pEnabled")
        this.isWifiP2pEnabled = isWifiP2pEnabled
    }

    /**
     * Remove all peers and clear all fields. This is called on
     * BroadcastReceiver receiving a state change event.
     */
    fun resetData() {
        Log.d(TAG, "resetData: ")
//        val fragmentList = //DeviceListFragment()
////            (supportFragmentManager.findFragmentByTag(DeviceListFragment.TAG)
////                ?: DeviceListFragment()) as DeviceListFragment
//            (supportFragmentManager.findFragmentByTag(NewDeviceListFragment.TAG)
//                ?: NewDeviceListFragment()) as NewDeviceListFragment
////            fragmentManager
////                .findFragmentById(R.id.frag_list) as DeviceListFragment
//        val fragmentDetails = //DeviceDetailFragment()
//            (supportFragmentManager.findFragmentByTag(DeviceDetailFragment.TAG)
//                ?: DeviceDetailFragment()) as DeviceDetailFragment
////            fragmentManager
////                .findFragmentById(R.id.frag_detail) as DeviceDetailFragment
        fragmentList.clearPeers()
        fragmentDetail.resetViews()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        Log.d(TAG, "onCreateOptionsMenu: ")
        val inflater = menuInflater
        inflater.inflate(R.menu.action_items, menu)
        return true
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d(TAG, "onOptionsItemSelected: ${item.itemId} ")
        when (item.itemId) {
            R.id.atn_direct_enable -> {
                if (manager != null && channel != null) {
                    // Since this is the system wireless settings activity, it's
                    // not going to send us a result. We will be notified by
                    // WiFiDeviceBroadcastReceiver instead.
                    startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                } else {
                    Log.e(TAG, "channel or manager is null")
                }
                return true
            }

            R.id.atn_direct_discover -> {
                if (!isWifiP2pEnabled) {
                    Toast.makeText(
                        this, R.string.p2p_off_warning,
                        Toast.LENGTH_SHORT
                    ).show()
                    return true
                }
//                val fragment = fragmentManager
//                    .findFragmentById(R.id.frag_list) as DeviceListFragment
//                val fragment: NewDeviceListFragment =
//                    (supportFragmentManager.findFragmentByTag(NewDeviceListFragment.TAG)
//                        ?: NewDeviceListFragment()) as NewDeviceListFragment

//                fragment.onInitiateDiscovery(this)
                if (
                    ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
//                    ||
//                    ActivityCompat.checkSelfPermission(
//                        this,
//                        Manifest.permission.NEARBY_WIFI_DEVICES
//                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    this.requestPermissions(
                        arrayOf(
                            Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.CHANGE_WIFI_STATE,
                            Manifest.permission.INTERNET,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.NEARBY_WIFI_DEVICES,
//                            "android.permission.NEARBY_WIFI_DEVICES",
                            Manifest.permission.CHANGE_NETWORK_STATE,
                            Manifest.permission.READ_PHONE_STATE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        ), 1
                    )
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    Toast.makeText(
                        this@MainActivity,
                        "No permission : 1.... requesting",
                        Toast.LENGTH_SHORT
                    ).show()
                    return false
                }
                manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        Toast.makeText(
                            this@MainActivity, "Discovery Initiated",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    override fun onFailure(reasonCode: Int) {
                        Toast.makeText(
                            this@MainActivity,
                            "Discovery Failed : $reasonCode",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        var i = 0
        for (permission in permissions) {

            val result = ActivityCompat.checkSelfPermission(
                this,
                permission
            )
            Toast.makeText(
                this@MainActivity,
                "permission request: $permission result: ${grantResults[i++]} $result",
                Toast.LENGTH_SHORT
            ).show()
        }

    }

    override fun showDetails(device: WifiP2pDevice?) {
        Log.d(TAG, "showDetails: ")
//        val fragment = fragmentManager
//            .findFragmentById(R.id.frag_detail) as DeviceDetailFragment
        fragmentDetail.showDetails(device!!)
    }

    override fun connect(config: WifiP2pConfig?) {
        Log.d(TAG, "connect: ")
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
//            ||
//            ActivityCompat.checkSelfPermission(
//                this,
//                Manifest.permission.NEARBY_WIFI_DEVICES
//            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Toast.makeText(
                this@MainActivity,
                "No permissions : 2",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        manager.connect(channel, config, object : ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "onSuccess: ")
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
            }

            override fun onFailure(reason: Int) {
                Log.d(TAG, "onFailure: ")
                Toast.makeText(
                    this@MainActivity, "Connect failed. Retry.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    override fun disconnect() {
        Log.d(TAG, "disconnect:... ")
//        val fragment = fragmentManager
//            .findFragmentById(R.id.frag_detail) as DeviceDetailFragment
//        fragment.resetViews()
        manager.removeGroup(channel, object : ActionListener {
            override fun onFailure(reasonCode: Int) {
                Log.d(TAG, "Disconnect failed. Reason :$reasonCode")
            }

            override fun onSuccess() {
                Log.d(TAG, "disconnect ...successful: ")
                fragmentDetail.view?.visibility = View.GONE
            }
        })
    }

    override fun onChannelDisconnected() {
        Log.d(TAG, "onChannelDisconnected: ")
        // we will try once more
        if (manager != null && !retryChannel) {
            Toast.makeText(this, "Channel lost. Trying again", Toast.LENGTH_LONG).show()
            resetData()
            retryChannel = true
            manager.initialize(this, mainLooper, this)
        } else {
            Toast.makeText(
                this,
                "Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun cancelDisconnect() {
        Log.d(TAG, "cancelDisconnect: ")
        /*
         * A cancel abort request by user. Disconnect i.e. removeGroup if
         * already connected. Else, request WifiP2pManager to abort the ongoing
         * request
         */
        if (manager != null) {
//            val fragment = (supportFragmentManager.findFragmentByTag(NewDeviceListFragment.TAG)
//                ?: NewDeviceListFragment()) as NewDeviceListFragment
//                fragmentManager
//                .findFragmentById(R.id.frag_list) as DeviceListFragment
            if (fragmentList.thisDevice == null
                || fragmentList.thisDevice!!.status == WifiP2pDevice.CONNECTED
            ) {
                disconnect()
            } else if (fragmentList.thisDevice!!.status == WifiP2pDevice.AVAILABLE
                || fragmentList.thisDevice!!.status == WifiP2pDevice.INVITED
            ) {
                manager.cancelConnect(channel, object : ActionListener {
                    override fun onSuccess() {
                        Toast.makeText(
                            this@MainActivity, "Aborting connection",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    override fun onFailure(reasonCode: Int) {
                        Toast.makeText(
                            this@MainActivity,
                            "Connect abort request failed. Reason Code: $reasonCode",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            }
        }
    }

    companion object {
        const val TAG = "@@MainActivity"
    }

    override fun getListFragment() = fragmentList
    override fun getDetailFragment() = fragmentDetail
}