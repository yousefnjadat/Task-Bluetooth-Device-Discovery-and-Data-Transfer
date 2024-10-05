package com.example.taskbluetoothdevicediscoveryanddatatransfer

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat


@Composable
fun DiscoveredDevicesListScreen(discoveredDevices: List<BluetoothDevice>, onScanClick: () -> Unit) {
    val context = LocalContext.current
    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Discovered Devices",
        style = MaterialTheme.typography.h6,
        textDecoration = TextDecoration.Underline
    )
    // Scan button
    Button(onClick = { onScanClick() }) {
        Text("Scan for Devices")
    }

    Spacer(modifier = Modifier.height(8.dp))
    LazyColumn {
        items(discoveredDevices.size) { index ->
            val device = discoveredDevices[index]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                if (ActivityCompat.checkSelfPermission(
                        context, Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return@Row
                }
                Text(text = "${device.name ?: "Unknown Device"} - ${device.address}")
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
