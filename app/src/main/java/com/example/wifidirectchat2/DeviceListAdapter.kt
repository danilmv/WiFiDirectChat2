package com.example.wifidirectchat2

import android.annotation.SuppressLint
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder

class DeviceListAdapter : Adapter<DeviceListAdapter.DeviceListViewHolder>() {

    private var data: MutableList<WifiP2pDevice> = ArrayList()
    var currentItem = 0
    var listener: OnItemClickListener? = null

    var currentDevice: WifiP2pDevice? = null

    inner class DeviceListViewHolder(itemView: View) : ViewHolder(itemView) {
        private val textName: TextView = itemView.findViewById(R.id.peer_name)
        private val textStatus: TextView = itemView.findViewById(R.id.peer_status)
        private lateinit var device: WifiP2pDevice

        init {
            Log.d(TAG, "view holder created ")
            itemView.setOnClickListener {
                Log.d(TAG, ": item clicked $textName")
                listener?.onItemClick(device)
                currentItem = adapterPosition
                currentDevice = device
            }
        }

        @SuppressLint("SetTextI18n")
        fun bind(device: WifiP2pDevice) {
            Log.d(TAG, "bind: ${device.deviceName}")
            this.device = device
            textName.text = device.deviceName
            textStatus.text = device.status.toString()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceListViewHolder {
        Log.d(TAG, "onCreateViewHolder: ")
//        return DeviceListViewHolder(DeviceBinding.inflate(LayoutInflater.from(parent.context)))

        return DeviceListViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.device, parent, false)
        )
    }

    override fun getItemCount(): Int {
        Log.d(TAG, "getItemCount: ${data.size}")
        return data.size
    }

    override fun onBindViewHolder(holder: DeviceListViewHolder, position: Int) {
        Log.d(TAG, "onBindViewHolder: ")
        holder.bind(data[position])
    }

    fun updateList(peers: WifiP2pDeviceList) {
        Log.d(TAG, "data set: size: ${peers.deviceList.size} data size: ${data.size}")
        data = peers.deviceList.toMutableList()
//        data.addAll(peers.deviceList.toMutableList())

        data.forEach {
            Log.d(TAG, "updateList: new peer: ${it.deviceName}")
        }

        Log.d(TAG, "updateList: ... data size: ${data.size}....")
        notifyDataSetChanged()
    }

    fun clearData() {
        Log.d(TAG, "clearData: ")
        data.clear()
        notifyDataSetChanged()
    }

    companion object {
        const val TAG = "@@DeviceListAdapter"
    }

    interface OnItemClickListener {
        fun onItemClick(device: WifiP2pDevice)
    }
}