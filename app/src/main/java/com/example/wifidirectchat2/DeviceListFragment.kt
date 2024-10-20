package com.example.wifidirectchat2

import android.app.Activity
import android.app.ListFragment
import android.app.ProgressDialog
import android.content.Context
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager.PeerListListener
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import kotlin.math.log


/**
 * A ListFragment that displays available peers on discovery and requests the
 * parent activity to handle user interaction events
 */
class DeviceListFragment : ListFragment(), PeerListListener {
    private val peers: MutableList<WifiP2pDevice> = ArrayList()

    //    var progressDialog: ProgressDialog? = null
    var mContentView: View? = null

    /**
     * @return this device
     */
    var device: WifiP2pDevice? = null
        private set

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        Log.d(TAG, "onActivityCreated: ")
        super.onActivityCreated(savedInstanceState)
        this.listAdapter = WiFiPeerListAdapter(activity, R.layout.row_devices, peers)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        Log.d(TAG, "onViewCreated: ")
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate: ")
        super.onCreate(savedInstanceState)
    }

    override fun onAttach(activity: Activity?) {
        Log.d(TAG, "onAttach: activity")
        super.onAttach(activity)
    }

    override fun onAttach(context: Context?) {
        Log.d(TAG, "onAttach: context")
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle
    ): View? {
        Log.d(TAG, "onCreateView: ")
        mContentView = inflater.inflate(R.layout.device_list, null)
        return mContentView
    }

    /**
     * Initiate a connection with the peer.
     */
    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        val device = listAdapter.getItem(position) as WifiP2pDevice
        Log.d(TAG, "onListItemClick: ${device.deviceName}")
        (activity as DeviceActionListener).showDetails(device)
    }

    /**
     * Array adapter for ListFragment that maintains WifiP2pDevice list.
     */
    private inner class WiFiPeerListAdapter
    /**
     * @param context
     * @param textViewResourceId
     * @param objects
     */(
        context: Context, textViewResourceId: Int,
        private val items: List<WifiP2pDevice>
    ) :
        ArrayAdapter<WifiP2pDevice?>(context, textViewResourceId, items) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var v = convertView
            if (v == null) {
                val vi = activity.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE
                ) as LayoutInflater
                v = vi.inflate(R.layout.row_devices, null)
            }
            val device = items[position]
            if (device != null) {
                val top = v!!.findViewById<View>(R.id.device_name) as TextView
                val bottom = v.findViewById<View>(R.id.device_details) as TextView
                if (top != null) {
                    top.text = device.deviceName
                }
                if (bottom != null) {
                    bottom.text = getDeviceStatus(device.status)
                }
            }
            return v!!
        }
    }

    /**
     * Update UI for this device.
     *
     * @param device WifiP2pDevice object
     */
    fun updateThisDevice(device: WifiP2pDevice) {
        Log.d(TAG, "updateThisDevice: ${device.deviceName}")
        if (mContentView == null) return
        this.device = device
        var view = mContentView!!.findViewById<View>(R.id.my_name) as TextView
        view.text = device.deviceName
        view = mContentView!!.findViewById<View>(R.id.my_status) as TextView
        view.text = getDeviceStatus(device.status)
    }

    override fun onPeersAvailable(peerList: WifiP2pDeviceList) {
        peers.clear()
        peers.addAll(peerList.deviceList)

        Log.d(TAG, "onPeersAvailable: listAdepter = $listAdapter peers: ${peers.size}")

        if (listAdapter == null) return
//        if (progressDialog != null && progressDialog!!.isShowing) {
//            progressDialog!!.dismiss()
//        }

        (listAdapter as WiFiPeerListAdapter).notifyDataSetChanged()
        if (peers.size == 0) {
            Log.d(MainActivity.TAG, "No devices found")
            return
        }
    }

    fun clearPeers() {
        Log.d(TAG, "clearPeers: ")
        peers.clear()
        if (listAdapter != null)
            (listAdapter as WiFiPeerListAdapter).notifyDataSetChanged()
    }

    /**
     *
     */
    fun onInitiateDiscovery(context: Context) {
        Log.d(TAG, "onInitiateDiscovery: ")
//        if (progressDialog != null && progressDialog!!.isShowing) {
//            progressDialog!!.dismiss()
//        }
//        progressDialog = ProgressDialog.show(
//            activity, "Press back to cancel", "finding peers", true,
//            true
//        ) { }
        if (listAdapter == null)
            this.listAdapter = WiFiPeerListAdapter(context, R.layout.row_devices, peers)
//            this.listAdapter = WiFiPeerListAdapter(activity, R.layout.row_devices, peers)
    }

    /**
     * An interface-callback for the activity to listen to fragment interaction
     * events.
     */
    interface DeviceActionListener {
        fun showDetails(device: WifiP2pDevice?)
        fun cancelDisconnect()
        fun connect(config: WifiP2pConfig?)
        fun disconnect()
    }

    companion object {
        const val TAG = "DeviceListFragment"
        private fun getDeviceStatus(deviceStatus: Int): String {
            Log.d(MainActivity.TAG, "Peer status :$deviceStatus")
            return when (deviceStatus) {
                WifiP2pDevice.AVAILABLE -> "Available"
                WifiP2pDevice.INVITED -> "Invited"
                WifiP2pDevice.CONNECTED -> "Connected"
                WifiP2pDevice.FAILED -> "Failed"
                WifiP2pDevice.UNAVAILABLE -> "Unavailable"
                else -> "Unknown"
            }
        }
    }
}
