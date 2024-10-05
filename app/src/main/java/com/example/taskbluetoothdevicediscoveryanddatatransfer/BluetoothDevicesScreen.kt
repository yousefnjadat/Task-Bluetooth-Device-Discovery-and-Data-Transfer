package com.example.taskbluetoothdevicediscoveryanddatatransfer

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp


@Composable
fun BluetoothDevicesScreen(
    pairedDevices: List<BluetoothDevice>,
    discoveredDevices: List<BluetoothDevice>,
    onScanClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        PairedDevicesListScreen(pairedDevices)

        Divider(color = Color.Gray, thickness = 1.dp)

        DiscoveredDevicesListScreen(discoveredDevices) {
            onScanClick() // Start discovery when the button is clicked
        }
    }
}