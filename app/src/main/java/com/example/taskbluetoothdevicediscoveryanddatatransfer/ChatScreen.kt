package com.example.taskbluetoothdevicediscoveryanddatatransfer


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import java.util.*

data class ChatMessage(val text: String, val time: String, val isSent: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(messages: List<ChatMessage>, onSendMessage: (String) -> Unit) {
    var message by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(messages) { message ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = if (message.isSent) Arrangement.End else Arrangement.Start
                ) {
                    Text(
                        text = "${message.text} (${message.time})",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .background(if (message.isSent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
                            .padding(8.dp)
                            .clip(MaterialTheme.shapes.medium)
                    )
                }
            }
        }
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = message,
                onValueChange = { message = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message") }
            )
            Button(onClick = {
                onSendMessage(message)
                message = ""
            }) {
                Text("Send")
            }
        }
    }
}
