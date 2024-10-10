package com.example.taskbluetoothdevicediscoveryanddatatransfer


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.unit.dp
import com.example.taskbluetoothdevicediscoveryanddatatransfer.data.ChatMessage
import java.util.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(messages: List<ChatMessage>, onSendMessage: (String) -> Unit) {
    var message by remember { mutableStateOf("") }

    // Create a ScrollState for the LazyColumn
    val scrollState = rememberLazyListState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        LazyColumn(
            state = scrollState, // Set the scroll state
            modifier = Modifier
                .weight(1f)
                .windowInsetsPadding(WindowInsets.statusBars)
                .imePadding()
        ) {
            items(messages) { message ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
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

        // Use LaunchedEffect to scroll to the last item when messages change
        LaunchedEffect(messages) {
            if (messages.isNotEmpty()) {
                scrollState.animateScrollToItem(messages.size - 1)
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
