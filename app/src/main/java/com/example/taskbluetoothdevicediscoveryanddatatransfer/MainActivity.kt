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
import android.net.Uri
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
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class MainActivity : ComponentActivity() {
    private val REQUEST_PERMISSIONS = 2
    private val pairedBluetoothDevices = mutableStateListOf<BluetoothDevice>()
    private val discoveredBluetoothDevices = mutableStateListOf<BluetoothDevice>()
    private lateinit var fileUri: Uri // Store the selected file URI for file transfer
    private var connectedDevice: BluetoothDevice? = null // Store the connected device
    private var bluetoothSocket: BluetoothSocket? = null // Store the Bluetooth socket

    // Required permissions for Bluetooth operations
    private val requiredPermissions = arrayOf(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.BLUETOOTH,
    )

    // Launcher for enabling Bluetooth and file picker
    private lateinit var enableBluetoothLauncher: ActivityResultLauncher<Intent>
    private lateinit var filePickerLauncher: ActivityResultLauncher<Intent>

    val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

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

        // Initialize file picker launcher
        filePickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            result?.data?.data?.let { uri ->
                fileUri = uri // Store the URI of the selected file
                Toast.makeText(this, "File selected: $uri", Toast.LENGTH_SHORT).show()
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
                },
                onSendFileClick = {
                    // Trigger file sending once a device is connected
                    if (connectedDevice != null) {
                        // Check if a file URI is selected
                        if (fileUri.path?.isNotEmpty() == true) {
                            sendFile(fileUri)
                        } else {
                            selectFile() // Open file picker if no file is selected
                        }
                    } else {
                        Toast.makeText(this, "Not connected to this device", Toast.LENGTH_SHORT)
                            .show()
                    }
                })
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
            val requestCode = 1
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


    //==================================================================================================================
    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        if (device.bondState != BluetoothDevice.BOND_BONDED) {
            println("Device is not paired, initiating pairing...")
            device.createBond() // This will initiate the pairing process

            // Register a BroadcastReceiver to listen for bond state changes
            val bondReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (BluetoothDevice.ACTION_BOND_STATE_CHANGED == intent.action) {
                        val state = intent.getIntExtra(
                            BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR
                        )
                        if (state == BluetoothDevice.BOND_BONDED) {
                            println("Device paired successfully, now connecting...")
                            initiateBluetoothConnection(device)
                            context.unregisterReceiver(this) // Unregister receiver after use
                        }
                    }
                }
            }
            registerReceiver(bondReceiver, IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED))
        } else {
            println("Device is already paired, connecting...")
            initiateBluetoothConnection(device)
        }
    }


    @SuppressLint("MissingPermission")
    private fun initiateBluetoothConnection(device: BluetoothDevice) {
        var socket: BluetoothSocket? = null
        try {
            // Attempt to create an RFCOMM socket
            socket = device.createRfcommSocketToServiceRecord(MY_UUID)
        } catch (e: IOException) {
            e.printStackTrace()
            println("Error creating RFCOMM socket")
        }

        // Fallback to reflection-based connection in case the standard method fails
        if (socket == null) {
            try {
                socket = device.javaClass.getMethod("createRfcommSocket", Int::class.java)
                    .invoke(device, 1) as BluetoothSocket
                println("Fallback RFCOMM socket created using reflection")
            } catch (e: Exception) {
                e.printStackTrace()
                println("Failed to create fallback RFCOMM socket")
            }
        }

        socket?.let {
            try {
                val bluetoothAdapter: BluetoothAdapter? = getBluetoothAdapter()
                bluetoothAdapter?.cancelDiscovery() // Cancel discovery to improve connection speed

                it.connect() // Blocking call to connect
                println("Connected to ${device.name}")

                connectedDevice = device // Set the connected device
                bluetoothSocket = it // Store the Bluetooth socket

                // Notify the user about successful connection
                Toast.makeText(this, "Connected to ${device.name}", Toast.LENGTH_SHORT).show()

            } catch (e: IOException) {
                e.printStackTrace()
                println("Failed to connect to ${device.name}")

                try {
                    it.close() // Close the socket if the connection fails
                } catch (closeException: IOException) {
                    closeException.printStackTrace()
                }

                // Handle connection failure
                Toast.makeText(this, "Connection failed. Please try again.", Toast.LENGTH_SHORT)
                    .show()
            }
        } ?: println("Bluetooth socket is null, connection failed")
    }


    // Open the file picker to select a file
    private fun selectFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
        }
        filePickerLauncher.launch(intent)
    }

    // Send the selected file to the connected device
    @SuppressLint("MissingPermission")
    private fun sendFile(fileUri: Uri) {
        if (bluetoothSocket == null || !bluetoothSocket!!.isConnected) {
            Toast.makeText(this, "Not connected to any device", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val inputStream: InputStream? = contentResolver.openInputStream(fileUri)
                val outputStream: OutputStream? = bluetoothSocket?.outputStream

                inputStream?.let { input ->
                    outputStream?.let { output ->
                        val buffer = ByteArray(1024) // Buffer size
                        var bytesRead: Int

                        // Send file in chunks
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                        }

                        println("File transfer completed.")
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity, "File transfer failed", Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver) // Unregister the receiver
        bluetoothSocket?.close() // Close the Bluetooth socket
    }
}
