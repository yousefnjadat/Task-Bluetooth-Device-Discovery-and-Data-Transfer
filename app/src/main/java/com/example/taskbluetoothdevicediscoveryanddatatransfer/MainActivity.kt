package com.example.taskbluetoothdevicediscoveryanddatatransfer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.CallSuper
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.taskbluetoothdevicediscoveryanddatatransfer.ui.theme.TaskBluetoothDeviceDiscoveryAndDataTransferTheme
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    private val STRATEGY = Strategy.P2P_CLUSTER
    private var opponentEndpointId: String? = null
    private lateinit var connectionsClient: ConnectionsClient
    private val REQUEST_CODE_REQUIRED_PERMISSIONS = 1
    private val txtLogState = mutableStateOf("")
    val name = Random.nextInt().toString()

    val REQUIRED_PERMISSIONS =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.NEARBY_WIFI_DEVICES, // Required for Android 13+
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
            TaskBluetoothDeviceDiscoveryAndDataTransferTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val txtLog by txtLogState
                    var textField by remember { mutableStateOf("Hello") }
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier.weight(weight = 0.3f, fill = true)
                        ) {
                            Button(onClick = {
                                startAdvertising()
                                startDiscovery()
                            }) {
                                Text(text = "Start Advertising")
                            }
                            TextField(value = textField,
                                onValueChange = { textField = it },
                                label = { Text("Label") })
                            Row {
                                Button(onClick = {
                                    sendData(textField)
                                }) {
                                    Text(text = "Send Data")
                                }
                                Button(onClick = {
                                    txtLogState.value = ""
                                }) {
                                    Text(text = "Clear Log")
                                }
                            }
                        }
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .weight(weight = 0.7f, fill = true)
                        ) {
                            Text(txtLog)
                        }
                    }
                }
            }
        }
    }

    @CallSuper
    override fun onStart() {
        super.onStart()
        // Request permission for NEARBY_WIFI_DEVICES on Android 13+.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES),
                    REQUEST_CODE_REQUIRED_PERMISSIONS
                )
            }
        }
        // Request other necessary permissions
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
                updateText("message: $message")
                Log.e("POC", message)
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            if (update.status == PayloadTransferUpdate.Status.SUCCESS) {
                Log.e("POC", endpointId)
            }
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            connectionsClient.acceptConnection(endpointId, payloadCallback)
            updateText("connected to: $endpointId")
            Log.e("POC", "Opponent (${info.endpointName})")
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                opponentEndpointId = endpointId
                connectionsClient.stopAdvertising()
                connectionsClient.stopDiscovery()
                updateText("connected to: $endpointId successfully")
                Log.e("POC", "$endpointId Connected")
            } else {
                updateText("connection failed: $endpointId")
            }
        }

        override fun onDisconnected(endpointId: String) {
            updateText("disconnected: $endpointId")
            Log.e("POC", "Disconnected")
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
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
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
