package com.example.taskbluetoothdevicediscoveryanddatatransfer

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import android.widget.Toast

class MainActivity : ComponentActivity() {
    private val REQUEST_ENABLE_BT = 1
    private val REQUEST_PERMISSIONS = 2

    private val pairedBluetoothDevices = mutableStateListOf<BluetoothDevice>()
    private val discoveredBluetoothDevices = mutableStateListOf<BluetoothDevice>()

    // Required permissions for Bluetooth operations
    private val requiredPermissions = arrayOf(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.ACCESS_FINE_LOCATION // Needed for discovery in some cases
    )

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Get Bluetooth adapter
            val bluetoothAdapter: BluetoothAdapter? = getBluetoothAdapter()

            // Request to enable Bluetooth if not enabled
            if (bluetoothAdapter?.isEnabled == false) {
                requestBluetoothEnabling()
            }

            // Check for permissions and load paired devices
            if (hasRequiredPermissions()) {
                loadPairedDevices()
            } else {
                requestBluetoothPermissions()
            }

            // Start discovery when permissions are granted and Bluetooth is enabled
            if (hasRequiredPermissions() && bluetoothAdapter?.isEnabled == true) {
                startDiscovery()
            }

            // UI to display devices and handle scanning
            BluetoothDevicesScreen(pairedBluetoothDevices, discoveredBluetoothDevices) {
                if (bluetoothAdapter?.isEnabled == true && hasRequiredPermissions()) {
                    startDiscovery() // Start discovery when button clicked
                } else {
                    Toast.makeText(this, "Bluetooth not enabled or permissions missing", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /** Get the BluetoothAdapter */
    private fun getBluetoothAdapter(): BluetoothAdapter? {
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        return bluetoothManager.adapter
    }

    /** Request to enable Bluetooth */
    private fun requestBluetoothEnabling() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
    }

    /** Check if all required permissions are granted */
    private fun hasRequiredPermissions(): Boolean {
        return requiredPermissions.all {
            ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /** Request Bluetooth permissions */
    private fun requestBluetoothPermissions() {
        ActivityCompat.requestPermissions(this, requiredPermissions, REQUEST_PERMISSIONS)
    }

    /** BroadcastReceiver to handle discovered Bluetooth devices */
    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            val action: String = intent.action ?: return
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device: BluetoothDevice? =
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                device?.let {
                    if (!discoveredBluetoothDevices.contains(it)) {
                        discoveredBluetoothDevices.add(it) // Add the discovered device
                        Toast.makeText(
                            context, "Discovered: ${it.name ?: "Unknown"}", Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    /** Load paired Bluetooth devices */
    @SuppressLint("MissingPermission")
    private fun loadPairedDevices() {
        val bluetoothAdapter: BluetoothAdapter = getBluetoothAdapter()!!
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
        pairedDevices?.let {
            pairedBluetoothDevices.clear()
            pairedBluetoothDevices.addAll(it)
        }
    }

    /** Start Bluetooth device discovery */
    @SuppressLint("MissingPermission")
    private fun startDiscovery() {
        val bluetoothAdapter: BluetoothAdapter = getBluetoothAdapter()!!
        discoveredBluetoothDevices.clear() // Clear before discovery

        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery() // Cancel ongoing discovery
        }

        bluetoothAdapter.startDiscovery()
        registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
    }

    /** Stop discovery and unregister receiver on destroy */
    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        val bluetoothAdapter: BluetoothAdapter = getBluetoothAdapter()!!
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }
        unregisterReceiver(receiver)
    }

    /** Handle Bluetooth enable result */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            startDiscovery() // Start discovery if Bluetooth is enabled
        }
    }

    /** Handle permission result */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            loadPairedDevices()
            startDiscovery() // Start discovery if permissions granted
        } else {
            Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show()
        }
    }
}
