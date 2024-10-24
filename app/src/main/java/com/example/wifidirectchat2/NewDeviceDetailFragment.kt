package com.example.wifidirectchat2

import android.content.Context
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.wifidirectchat2.DeviceDetailFragment.Companion
import com.example.wifidirectchat2.DeviceDetailFragment.FileServerAsyncTask
import com.example.wifidirectchat2.NewDeviceListFragment.DeviceActionListener
import kotlin.contracts.contract

class NewDeviceDetailFragment : Fragment(), WifiP2pManager.ConnectionInfoListener {

    private var device: WifiP2pDevice? = null
    private var info: WifiP2pInfo? = null

    private var actionListener: DeviceActionListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView: ")
        val view = inflater.inflate(R.layout.device_detail, container, false)

        view.findViewById<Button>(R.id.btn_connect).setOnClickListener{
            Log.d(TAG, "CONNECT pressed")
            val config = WifiP2pConfig()
            config.deviceAddress = device!!.deviceAddress
            config.wps.setup = WpsInfo.PBC

            actionListener?.connect(config)
        }

        view.findViewById<Button>(R.id.btn_disconnect).setOnClickListener{
            Log.d(TAG, "DISCONNECT pressed")
            actionListener?.disconnect()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "onViewCreated: ")
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onAttach(context: Context) {
        Log.d(TAG, "onAttach: ")
        super.onAttach(context)

        if (context is DeviceActionListener)
            actionListener = context
    }

    companion object {
        const val TAG = "@@NewDeviceDetailFragment"
    }

    override fun onConnectionInfoAvailable(info: WifiP2pInfo?) {
        Log.d(TAG, "onConnectionInfoAvailable: ${info?.groupOwnerAddress} ${info?.groupOwnerAddress?.hostAddress} ${info?.groupOwnerAddress?.address}")
        this.info = info
        view?.visibility = View.VISIBLE
        // The owner IP is now known.
        var view = this.requireView().findViewById<View>(R.id.group_owner) as TextView
        view.text = (resources.getString(R.string.group_owner_text)
                + (
                if (info?.isGroupOwner == true) resources.getString(R.string.yes) else resources.getString(
                    R.string.no
                ))
                )
        // InetAddress from WifiP2pInfo struct.
        view = this.requireView().findViewById<View>(R.id.device_info) as TextView
        view.text = "Group Owner IP - " + info!!.groupOwnerAddress.hostAddress
        // After the group negotiation, we assign the group owner as the file
        // server. The file server is single threaded, single connection server
        // socket.
        if (info.groupFormed && info.isGroupOwner) {
            actionListener?.startServer()
//            FileServerAsyncTask(requireContext(), this.requireView().findViewById(R.id.status_text))
//                .execute()
        } else if (info.groupFormed) {
            actionListener?.join(info!!.groupOwnerAddress.hostAddress)
            // The other device acts as the client. In this case, we enable the
            // get file button.
//            this.requireView().findViewById<View>(R.id.btn_start_client).visibility =
//                View.VISIBLE
//            (this.requireView().findViewById<View>(R.id.status_text) as TextView).text =
//                resources
//                    .getString(R.string.client_text)
        }
        // hide the connect button
        this.requireView().findViewById<View>(R.id.btn_connect).visibility =
            View.GONE
    }

    fun showDetails(device: WifiP2pDevice) {
        Log.d(DeviceDetailFragment.TAG, "showDetails: ${device.deviceName}")
        this.device = device
        this.view?.visibility = View.VISIBLE
        var view = this.requireView().findViewById<View>(R.id.device_address) as TextView
        view.text = device.deviceAddress
        view = this.requireView().findViewById<View>(R.id.device_info) as TextView
        view.text = device.toString()
    }

    fun resetViews() {
        Log.d(TAG, "resetViews: ")

        Log.d(com.example.wifidirectchat2.DeviceDetailFragment.TAG, "resetViews: ")
        if (this.view == null) return

        this.requireView()!!.findViewById<View>(R.id.btn_connect).visibility =
            View.VISIBLE
        var view = this.requireView()!!.findViewById<View>(R.id.device_address) as TextView
        view.setText(R.string.empty)
        view = this.requireView()!!.findViewById<View>(R.id.device_info) as TextView
        view.setText(R.string.empty)
        view = this.requireView()!!.findViewById<View>(R.id.group_owner) as TextView
        view.setText(R.string.empty)
        view = this.requireView()!!.findViewById<View>(R.id.status_text) as TextView
        view.setText(R.string.empty)
        this.requireView()!!.findViewById<View>(R.id.btn_start_client).visibility =
            View.GONE
        this.view?.visibility = View.GONE
    }
}