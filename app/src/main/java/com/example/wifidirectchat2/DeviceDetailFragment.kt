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

import android.annotation.SuppressLint
import android.app.Fragment
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.example.wifidirectchat2.DeviceListFragment.DeviceActionListener
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket

//import DeviceListFragment.DeviceActionListener;
/**
 * A fragment that manages a particular peer and allows interaction with device
 * i.e. setting up network connection and transferring data.
 */
class DeviceDetailFragment : Fragment(), ConnectionInfoListener {
    private var mContentView: View? = null
    private var device: WifiP2pDevice? = null
    private var info: WifiP2pInfo? = null
    var progressDialog: ProgressDialog? = null
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle
    ): View? {
        Log.d(TAG, "onCreateView: ")
        mContentView = inflater.inflate(R.layout.device_detail, null)
        mContentView!!.findViewById<Button>(R.id.btn_connect).setOnClickListener {
            val config = WifiP2pConfig()
            config.deviceAddress = device!!.deviceAddress
            config.wps.setup = WpsInfo.PBC
            if (progressDialog != null && progressDialog!!.isShowing) {
                progressDialog!!.dismiss()
            }
            progressDialog = ProgressDialog.show(
                activity,
                "Press back to cancel",
                "Connecting to :" + device!!.deviceAddress,
                true,
                true //                        new DialogInterface.OnCancelListener() {
                //
                //                            @Override
                //                            public void onCancel(DialogInterface dialog) {
                //                                ((DeviceActionListener) getActivity()).cancelDisconnect();
                //                            }
                //                        }
            )
            (activity as DeviceActionListener).connect(config)
        }
        mContentView!!.findViewById<Button>(R.id.btn_disconnect)
            .setOnClickListener { (activity as DeviceActionListener).disconnect() }
        mContentView!!.findViewById<Button>(R.id.btn_start_client)
            .setOnClickListener { // Allow user to pick an image from Gallery or other
                // registered apps
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.setType("image/*")
                startActivityForResult(
                    intent,
                    CHOOSE_FILE_RESULT_CODE
                )
            }
        return mContentView
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        Log.d(TAG, "onActivityResult: ")
        // User has picked an image. Transfer it to group owner i.e peer using
        // FileTransferService.
        val uri = data.data
        val statusText = mContentView!!.findViewById<View>(R.id.status_text) as TextView
        statusText.text = "Sending: $uri"
        Log.d(MainActivity.TAG, "Intent----------- $uri")
        val serviceIntent = Intent(
            activity,
            FileTransferService::class.java
        )
        serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE)
        serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, uri.toString())
        serviceIntent.putExtra(
            FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
            info!!.groupOwnerAddress.hostAddress
        )
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8988)
        activity.startService(serviceIntent)
    }

    override fun onConnectionInfoAvailable(info: WifiP2pInfo) {
        Log.d(TAG, "onConnectionInfoAvailable: ")
        if (progressDialog != null && progressDialog!!.isShowing) {
            progressDialog!!.dismiss()
        }
        this.info = info
        this.view!!.visibility = View.VISIBLE
        // The owner IP is now known.
        var view = mContentView!!.findViewById<View>(R.id.group_owner) as TextView
        view.text = (resources.getString(R.string.group_owner_text)
                + (if ((info.isGroupOwner == true))
            resources.getString(R.string.yes)
        else
            resources.getString(R.string.no)))
        // InetAddress from WifiP2pInfo struct.
        view = mContentView!!.findViewById<View>(R.id.device_info) as TextView
        view.text = "Group Owner IP - " + info.groupOwnerAddress.hostAddress
        // After the group negotiation, we assign the group owner as the file
        // server. The file server is single threaded, single connection server
        // socket.
        if (info.groupFormed && info.isGroupOwner) {
            FileServerAsyncTask(activity, mContentView!!.findViewById(R.id.status_text))
                .execute()
        } else if (info.groupFormed) {
            // The other device acts as the client. In this case, we enable the
            // get file button.
            mContentView!!.findViewById<View>(R.id.btn_start_client).visibility =
                View.VISIBLE
            (mContentView!!.findViewById<View>(R.id.status_text) as TextView).text =
                resources
                    .getString(R.string.client_text)
        }
        // hide the connect button
        mContentView!!.findViewById<View>(R.id.btn_connect).visibility =
            View.GONE
    }

    /**
     * Updates the UI with device data
     *
     * @param device the device to be displayed
     */
    fun showDetails(device: WifiP2pDevice) {
        Log.d(TAG, "showDetails: ${device.deviceName}")
        this.device = device
        this.view!!.visibility = View.VISIBLE
        var view = mContentView!!.findViewById<View>(R.id.device_address) as TextView
        view.text = device.deviceAddress
        view = mContentView!!.findViewById<View>(R.id.device_info) as TextView
        view.text = device.toString()
    }

    /**
     * Clears the UI fields after a disconnect or direct mode disable operation.
     */
    fun resetViews() {
        Log.d(TAG, "resetViews: ")
        if (mContentView == null) return

        mContentView!!.findViewById<View>(R.id.btn_connect).visibility =
            View.VISIBLE
        var view = mContentView!!.findViewById<View>(R.id.device_address) as TextView
        view.setText(R.string.empty)
        view = mContentView!!.findViewById<View>(R.id.device_info) as TextView
        view.setText(R.string.empty)
        view = mContentView!!.findViewById<View>(R.id.group_owner) as TextView
        view.setText(R.string.empty)
        view = mContentView!!.findViewById<View>(R.id.status_text) as TextView
        view.setText(R.string.empty)
        mContentView!!.findViewById<View>(R.id.btn_start_client).visibility =
            View.GONE
        this.view!!.visibility = View.GONE
    }

    /**
     * A simple server socket that accepts connection and writes some data on
     * the stream.
     */
    class FileServerAsyncTask(private val context: Context, statusText: View) :
        AsyncTask<Void?, Void?, String?>() {
        @SuppressLint("StaticFieldLeak")
        private val statusText = statusText as TextView

        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg params: Void?): String? {
            try {
                val serverSocket = ServerSocket(8988)
                Log.d(MainActivity.TAG, "Server: Socket opened")
                val client = serverSocket.accept()
                Log.d(MainActivity.TAG, "Server: connection done")
                val f = File(
                    (Environment.getExternalStorageDirectory().toString() + "/"
                            + context.packageName + "/wifip2pshared-" + System.currentTimeMillis()
                            + ".jpg")
                )
                val dirs = File(f.parent)
                if (!dirs.exists()) dirs.mkdirs()
                f.createNewFile()
                Log.d(MainActivity.TAG, "server: copying files $f")
                val inputstream = client.getInputStream()
                copyFile(inputstream, FileOutputStream(f))
                serverSocket.close()
                return f.absolutePath
            } catch (e: IOException) {
                Log.e(MainActivity.TAG, e.message!!)
                return null
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        override fun onPostExecute(result: String?) {
            Log.d(TAG, "onPostExecute: ")
            if (result != null) {
                statusText.text = "File copied - $result"
                val intent = Intent()
                intent.setAction(Intent.ACTION_VIEW)
                intent.setDataAndType(Uri.parse("file://$result"), "image/*")
                context.startActivity(intent)
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        override fun onPreExecute() {
            Log.d(TAG, "onPreExecute: ")
            statusText.text = "Opening a server socket"
        }
    }

    companion object {
        const val TAG = "DeviceDetailFragment"
        protected const val CHOOSE_FILE_RESULT_CODE: Int = 20

        @JvmStatic
        fun copyFile(inputStream: InputStream, out: OutputStream): Boolean {
            val buf = ByteArray(1024)
            var len: Int
            try {
                while ((inputStream.read(buf).also { len = it }) != -1) {
                    out.write(buf, 0, len)
                }
                out.close()
                inputStream.close()
            } catch (e: IOException) {
                Log.d(MainActivity.TAG, e.toString())
                return false
            }
            return true
        }
    }
}