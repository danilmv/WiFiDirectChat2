package com.example.wifidirectchat2

import android.content.Context
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class NewDeviceListFragment : Fragment(), WifiP2pManager.PeerListListener,
    DeviceListAdapter.OnItemClickListener {

    private var actionListener: DeviceActionListener? = null
    private var recycler: RecyclerView? = null


    var adapter = DeviceListAdapter()

    init {
        adapter.listener = this
    }

    private var textThisDeviceName: TextView? = null
    private var textThisDeviceStatus: TextView? = null

    var thisDevice: WifiP2pDevice? = null
        set(value) {
            Log.d(TAG, "thisDevice set: ")
            field = value
            textThisDeviceName?.text = value?.deviceName
            textThisDeviceStatus?.text = value?.status.toString()
        }


    override fun onAttach(context: Context) {
        Log.d(TAG, "onAttach: ")
        super.onAttach(context)
        if (context is DeviceActionListener)
            actionListener = context
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView: ")
        val view = inflater.inflate(R.layout.fragment_device_list, container, false)
        textThisDeviceName = view.findViewById(R.id.my_name)
        textThisDeviceStatus = view.findViewById(R.id.my_status)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "onViewCreated: ")
        super.onViewCreated(view, savedInstanceState)

        recycler = view.findViewById(R.id.recycler_device_list)
        recycler!!.layoutManager = LinearLayoutManager(context)
        recycler!!.adapter = adapter
    }

    companion object {
        const val TAG = "@@NewDeviceListFragment"
    }

    override fun onPeersAvailable(peers: WifiP2pDeviceList) {
        Log.d(TAG, "onPeersAvailable: deviceList size: ${peers.deviceList.size}")
        adapter.updateList(peers)
//        adapter.data = peers.deviceList.toMutableList()
    }

    fun clearPeers() {
        Log.d(TAG, "clearPeers: ")
        adapter.clearData()
    }

//    @SuppressLint("NewApi")
//    fun updateThisDevice(device: WifiP2pDevice) {
//        Log.d(DeviceListFragment.TAG, "updateThisDevice: ${device.deviceName}")
//
//        thisDevice = device
//    }

    interface DeviceActionListener {
        fun showDetails(device: WifiP2pDevice?)
        fun cancelDisconnect()
        fun connect(config: WifiP2pConfig?)
        fun disconnect()
    }

    override fun onItemClick(device: WifiP2pDevice) {
        Log.d(TAG, "onItemClick: ")
        actionListener?.showDetails(device)
    }

}