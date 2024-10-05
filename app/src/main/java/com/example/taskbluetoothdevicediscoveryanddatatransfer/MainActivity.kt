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
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import android.widget.Toast

class MainActivity : ComponentActivity() {
    private val REQUEST_PERMISSIONS = 2
    private val pairedBluetoothDevices = mutableStateListOf<BluetoothDevice>()
    private val discoveredBluetoothDevices = mutableStateListOf<BluetoothDevice>()

    // Required permissions for Bluetooth operations
    private val requiredPermissions = arrayOf(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.BLUETOOTH,
    )

    // Launcher for enabling Bluetooth
    private lateinit var enableBluetoothLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Bluetooth enabling launcher
        enableBluetoothLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    loadPairedDevices()
                    startDiscovery()
                } else {
                    println("Bluetooth not enabled")
                }
            }

        setContent {
            val bluetoothAdapter: BluetoothAdapter? = getBluetoothAdapter()
            var isBluetoothEnabled by remember { mutableStateOf(bluetoothAdapter?.isEnabled == true) }

            // Check Bluetooth state and permissions on startup
            LaunchedEffect(true) {
                if (hasRequiredPermissions()) {
                    if (!isBluetoothEnabled) requestBluetoothEnabling()
                    else {
                        loadPairedDevices()
                        startDiscovery()
                    }
                } else {
                    requestBluetoothPermissions()
                }
            }

            BluetoothDevicesScreen(pairedBluetoothDevices, discoveredBluetoothDevices) {
                if (!isBluetoothEnabled) requestBluetoothEnabling()
                else startDiscovery()
            }
        }
    }

    // Get the BluetoothAdapter
    private fun getBluetoothAdapter(): BluetoothAdapter? {
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        return bluetoothManager.adapter
    }

    // Request to enable Bluetooth
    private fun requestBluetoothEnabling() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        enableBluetoothLauncher.launch(enableBtIntent)
    }

    // Check if all required permissions are granted
    private fun hasRequiredPermissions(): Boolean {
        return requiredPermissions.all {
            ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Request Bluetooth permissions
    private fun requestBluetoothPermissions() {
        ActivityCompat.requestPermissions(this, requiredPermissions, REQUEST_PERMISSIONS)
    }

    // Handle permission request result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            val bluetoothAdapter = getBluetoothAdapter()
            if (bluetoothAdapter?.isEnabled == false) requestBluetoothEnabling()
            else {
                loadPairedDevices()
                startDiscovery()
            }
        } else {
            println("Permissions denied")
        }
    }

    // BroadcastReceiver to handle discovered Bluetooth devices
    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BluetoothDevice.ACTION_FOUND) {
                val device: BluetoothDevice =
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) ?: return
                if (!discoveredBluetoothDevices.contains(device)) {
                    discoveredBluetoothDevices.add(device)
                    Toast.makeText(
                        context,
                        "Discovered: ${device.name ?: "Unknown"}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Load paired Bluetooth devices
    @SuppressLint("MissingPermission")
    private fun loadPairedDevices() {
        val pairedDevices: Set<BluetoothDevice>? = getBluetoothAdapter()?.bondedDevices
        pairedDevices?.let {
            pairedBluetoothDevices.clear()
            pairedBluetoothDevices.addAll(it)
        }
    }

    // Start Bluetooth device discovery
    @SuppressLint("MissingPermission")
    private fun startDiscovery() {
        val bluetoothAdapter = getBluetoothAdapter() ?: return
        discoveredBluetoothDevices.clear()
        if (bluetoothAdapter.isDiscovering) bluetoothAdapter.cancelDiscovery()
        bluetoothAdapter.startDiscovery()
        registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        // Ensure location services are enabled
        val locationManager = getSystemService(LocationManager::class.java)
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            bluetoothAdapter.startDiscovery()
            registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        } else {
            Toast.makeText(
                this,
                "Please enable location services to discover devices",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Unregister receiver and cancel discovery on destroy
    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        getBluetoothAdapter()?.cancelDiscovery()
    }
}
