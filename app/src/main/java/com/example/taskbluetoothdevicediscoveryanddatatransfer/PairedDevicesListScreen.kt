package com.example.taskbluetoothdevicediscoveryanddatatransfer

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextDecoration

@Composable
fun PairedDevicesListScreen(
    pairedDevices: List<BluetoothDevice>,
    onDeviceClick: (BluetoothDevice) -> Unit, // Add this for connecting
    onSendFileClick: () -> Unit
) {
    val context = LocalContext.current
    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Paired Devices",
        style = MaterialTheme.typography.h6,
        textDecoration = TextDecoration.Underline
    )
    LazyColumn(modifier = Modifier.height(200.dp)) {
        items(pairedDevices.size) { index ->
            val device = pairedDevices[index]
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clickable { onDeviceClick(device) } // Handle device click
            ) {
                if (ActivityCompat.checkSelfPermission(
                        context, Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return@Column
                }
                ClickableText(
                    text = AnnotatedString("${device.name ?: "Unknown Device"} - ${device.address}"),
                    onClick = { onDeviceClick(device) }
                )
                Button(onClick = { onSendFileClick() }) {
                    Text(text = "Send File")
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}

