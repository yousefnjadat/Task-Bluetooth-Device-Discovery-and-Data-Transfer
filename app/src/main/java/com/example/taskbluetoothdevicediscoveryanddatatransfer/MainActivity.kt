package com.example.taskbluetoothdevicediscoveryanddatatransfer

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID

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

            BluetoothDevicesScreen(pairedDevices = pairedBluetoothDevices,
                discoveredDevices = discoveredBluetoothDevices,
                onScanClick = {
                    if (!isBluetoothEnabled) requestBluetoothEnabling()
                    else startDiscovery()
                },
                onDeviceClick = { device ->
                    connectToDevice(device)
                } // Pass device connection logic
            )
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
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
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
                        context, "Discovered: ${device.name ?: "Unknown"}", Toast.LENGTH_SHORT
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

        if (bluetoothAdapter.scanMode != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            val requestCode = 1;
            val discoverableIntent: Intent =
                Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                    putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                }
            startActivityForResult(discoverableIntent, requestCode)
        }
        registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        // Ensure location services are enabled
        val locationManager = getSystemService(LocationManager::class.java)
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            bluetoothAdapter.startDiscovery()
            registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        } else {
            Toast.makeText(
                this, "Please enable location services to discover devices", Toast.LENGTH_SHORT
            ).show()
        }
    }

    val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        // Check if the device is already paired
        if (device.bondState != BluetoothDevice.BOND_BONDED) {
            println("Device is not paired, initiating pairing...")
            device.createBond() // This will initiate the pairing process
        } else {
            println("Device is already paired, connecting...")
            // Proceed to connect if the device is already paired
            initiateBluetoothConnection(device)
        }
    }

    @SuppressLint("MissingPermission")
    private fun initiateBluetoothConnection(device: BluetoothDevice) {
        val socket: BluetoothSocket? = try {
            device.createRfcommSocketToServiceRecord(MY_UUID) // Use the correct UUID
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }

        socket?.let {
            try {
                val bluetoothAdapter:BluetoothAdapter? = getBluetoothAdapter()
                // Cancel discovery because it will slow down the connection
                bluetoothAdapter?.cancelDiscovery()

                it.connect() // Blocking call to connect
                println("Connected to ${device.name}")

                // Now you can start data transfer with input/output streams
                val inputStream = it.inputStream
                val outputStream = it.outputStream

                // Example: Send data to the connected device
                val message = "Hello Device!"
                outputStream.write(message.toByteArray())
            } catch (e: IOException) {
                e.printStackTrace()
                println("Failed to connect to ${device.name}")
                try {
                    it.close()
                } catch (closeException: IOException) {
                    closeException.printStackTrace()
                }
            }
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
