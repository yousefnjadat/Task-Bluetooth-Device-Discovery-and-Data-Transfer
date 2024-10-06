package com.example.taskbluetoothdevicediscoveryanddatatransfer

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material.*


@Composable
fun BluetoothDevicesScreen(
    pairedDevices: List<BluetoothDevice>,
    discoveredDevices: List<BluetoothDevice>,
    onScanClick: () -> Unit,
    onDeviceClick: (BluetoothDevice) -> Unit,
    onSendFileClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Button(onClick = onScanClick) {
                Text(text = "Start Scanning")
            }
            Spacer(modifier = Modifier.height(16.dp))
            PairedDevicesListScreen(pairedDevices, onDeviceClick, onSendFileClick)

            Divider(color = Color.Gray, thickness = 1.dp)

            DiscoveredDevicesListScreen(discoveredDevices, onDeviceClick)
        }
    }
}
