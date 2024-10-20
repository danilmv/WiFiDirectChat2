/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.wifidirectchat2

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.PeerListListener
import android.os.Parcelable
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat


/**
 * A BroadcastReceiver that notifies of important wifi p2p events.
 */
class WiFiDirectBroadcastReceiver
/**
 * @param manager  WifiP2pManager system service
 * @param channel  Wifi p2p channel
 * @param activity activity associated with the receiver
 */(
    private val manager: WifiP2pManager?, private val channel: WifiP2pManager.Channel,
    private val activity: MainActivity
) : BroadcastReceiver() {
    val TAG: String = "@@WiFiDirectBroadcastReceiver"
    val contract = activity as BroadcastContract

    /*
     * (non-Javadoc)
     * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
     * android.content.Intent)
     */
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: " + intent.action)
        val action = intent.action
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION == action) {
            // UI update to indicate wifi p2p status.
            val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi Direct mode is enabled
                activity.setIsWifiP2pEnabled(true)
            } else {
                activity.setIsWifiP2pEnabled(false)
                activity.resetData()
            }
            Log.d(MainActivity.TAG, "P2P state changed - $state")
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION == action) {
            // request available peers from the wifi p2p manager. This is an
            // asynchronous call and the calling activity is notified with a
            // callback on PeerListListener.onPeersAvailable()
            if (manager != null) {
                if (ActivityCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED //                        || ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    Toast.makeText(
                        activity,
                        "No permissions : 3",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }
                
//                val fragment = contract.getListFragment()
//                Log.d(TAG, "onReceive: PEERS_CHANGED... fragment = $fragment")
                
                manager.requestPeers(
                    channel,
                    contract.getListFragment()
//                    (activity.supportFragmentManager.findFragmentByTag(NewDeviceListFragment.TAG)
//                        ?: NewDeviceListFragment()) as PeerListListener
//                    (activity.supportFragmentManager.findFragmentByTag(DeviceListFragment.TAG)
//                        ?: DeviceListFragment()) as PeerListListener
//                    activity.fragmentManager.findFragmentById(R.id.frag_list) as PeerListListener
                )
            }
            Log.d(MainActivity.TAG, "P2P peers changed")
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION == action) {
            if (manager == null) {
                return
            }
            val networkInfo = intent
                .getParcelableExtra<Parcelable>(WifiP2pManager.EXTRA_NETWORK_INFO) as NetworkInfo?
            if ((networkInfo != null) and networkInfo!!.isConnected) {
                // we are connected with the other device, request connection
                // info to find group owner IP
                val fragment = contract.getDetailFragment()
//                    activity
//                    .fragmentManager.findFragmentById(R.id.frag_detail) as DeviceDetailFragment
                manager.requestConnectionInfo(channel, fragment)
            } else {
                // It's a disconnect
                activity.resetData()
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION == action) {
            val fragment = contract.getListFragment()
//                ((activity.supportFragmentManager.findFragmentByTag(NewDeviceListFragment.TAG))
//                    ?: NewDeviceListFragment()) as NewDeviceListFragment
//                (activity.supportFragmentManager.findFragmentByTag(DeviceListFragment.TAG)
//                    ?: DeviceListFragment()) as DeviceListFragment
//                activity.fragmentManager
//                    .findFragmentById(R.id.frag_list) as DeviceListFragment
//            fragment.updateThisDevice(
            fragment.thisDevice =
                (intent.getParcelableExtra<Parcelable>(
                    WifiP2pManager.EXTRA_WIFI_P2P_DEVICE
                ) as WifiP2pDevice?)!!
//            )
        }
    }

    interface BroadcastContract{
        fun getListFragment(): NewDeviceListFragment
        fun getDetailFragment(): NewDeviceDetailFragment
    }
}
