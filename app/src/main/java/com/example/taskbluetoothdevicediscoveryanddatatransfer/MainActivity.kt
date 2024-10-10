package com.example.taskbluetoothdevicediscoveryanddatatransfer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.CallSuper
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import com.example.taskbluetoothdevicediscoveryanddatatransfer.data.ChatMessage
import com.example.taskbluetoothdevicediscoveryanddatatransfer.ui.theme.TaskBluetoothDeviceDiscoveryAndDataTransferTheme
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import kotlin.random.Random
import java.text.SimpleDateFormat
import java.util.*


@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private val STRATEGY = Strategy.P2P_CLUSTER
    private var opponentEndpointId: String? = null
    private lateinit var connectionsClient: ConnectionsClient
    private val REQUEST_CODE_REQUIRED_PERMISSIONS = 1
    private val txtLogState = mutableStateOf("")
    private val messages = mutableStateListOf<ChatMessage>()
    private var isConnected by mutableStateOf(false)
    val name = Random.nextInt().toString()

    val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.NEARBY_WIFI_DEVICES,
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    } else {
        arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        connectionsClient = Nearby.getConnectionsClient(this)
        setContent {
            val focusManager = LocalFocusManager.current
            TaskBluetoothDeviceDiscoveryAndDataTransferTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            focusManager.clearFocus() // Close keyboard when clicking outside
                        })
                    }) {
                        if (isConnected) {
                            ChatScreen(messages) { messageText ->
                                sendData(messageText) // Send the message
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Button(onClick = {
                                    txtLogState.value = ""
                                    startAdvertising()
                                    startDiscovery()
                                }) {
                                    Text("Start Advertising")
                                }
                                Button(onClick = {
                                    txtLogState.value = ""
                                }) {
                                    Text(text = "Clear Log")
                                }
                                // Your existing UI components...
                                TextField(modifier = Modifier.focusProperties { canFocus = false },
                                    readOnly = true,
                                    value = txtLogState.value,
                                    onValueChange = {
                                        focusManager.clearFocus()
                                        txtLogState.value = it
                                    },
                                    label = { Text("Log") })

                            }
                        }
                    }
                }
            }
        }
    }

    @CallSuper
    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES),
                    REQUEST_CODE_REQUIRED_PERMISSIONS
                )
            }
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS)
        }
    }

    @CallSuper
    override fun onStop() {
        connectionsClient.apply {
            stopAdvertising()
            stopDiscovery()
            stopAllEndpoints()
        }
        super.onStop()
    }

    private fun sendData(textField: String) {
        connectionsClient.sendPayload(
            (opponentEndpointId ?: ""), Payload.fromBytes(textField.toByteArray(Charsets.UTF_8))
        ).addOnFailureListener {
            updateText("sendPayload: failed")
        }.addOnSuccessListener {
            updateText("sendPayload: success $textField")
            val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            messages.add(ChatMessage(textField, currentTime, true)) // Add received message to chat
        }
    }

    private fun startAdvertising() {
        val options = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        updateText("startAdvertising")
        connectionsClient.startAdvertising(
            name, packageName, connectionLifecycleCallback, options
        ).addOnSuccessListener {
            updateText("startAdvertising: Success")
        }.addOnFailureListener {
            updateText("startAdvertising: failed $it")
        }
    }

    private fun updateText(newLineText: String) {
        txtLogState.value += "\n$newLineText"
    }

    private fun startDiscovery() {
        val options = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        connectionsClient.startDiscovery(packageName, endpointDiscoveryCallback, options)
            .addOnSuccessListener {
                updateText("startDiscovery success")
            }.addOnFailureListener {
                updateText("startDiscovery failed: $it")
            }
    }

    private val payloadCallback: PayloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            payload.asBytes()?.let {
                val message = String(it, Charsets.UTF_8)
                val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                messages.add(
                    ChatMessage(
                        message, currentTime, false
                    )
                ) // Add received message to chat
                updateText("message: $message")
                Log.e("onPayloadReceived", message)
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            if (update.status == PayloadTransferUpdate.Status.SUCCESS) {
                Log.e("onPayloadTransferUpdate", endpointId)
            }
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            connectionsClient.acceptConnection(endpointId, payloadCallback)
            updateText("connected to: $endpointId")
            Log.e("onConnectionInitiated", "Opponent (${info.endpointName})")
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                opponentEndpointId = endpointId
                isConnected = true // Update connection state
                connectionsClient.stopAdvertising()
                connectionsClient.stopDiscovery()
                updateText("connected to: $endpointId successfully")
                Log.e("onConnectionResult", "$endpointId Connected")
            } else {
                updateText("connection failed: $endpointId")
            }
        }

        override fun onDisconnected(endpointId: String) {
            isConnected = false // Update connection state
            updateText("disconnected: $endpointId")
            Log.e("onDisconnected", "Disconnected")
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            updateText("onEndpointFound: ${info.endpointName} $endpointId")
            connectionsClient.requestConnection(name, endpointId, connectionLifecycleCallback)
                .addOnFailureListener {
                    updateText("requestConnection failed: $endpointId ${it.message}")
                }.addOnSuccessListener {
                    updateText("requestConnection success: $endpointId")
                }
        }

        override fun onEndpointLost(endpointId: String) {
            updateText("onEndpointLost: $endpointId")
        }
    }

    // Handle the result of the permission request
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_REQUIRED_PERMISSIONS) {
            for (i in permissions.indices) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    updateText("${permissions[i]} permission denied.")
                    return
                }
            }
            // All permissions granted, proceed with discovery or advertising
            startAdvertising()
            startDiscovery()
        }
    }
}
