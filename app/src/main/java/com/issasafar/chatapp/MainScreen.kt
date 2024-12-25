package com.issasafar.chatapp

import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.issasafar.chatapp.data.MessageUnit
import com.issasafar.chatapp.data.decrypt
import com.issasafar.chatapp.ui.theme.ChatappTheme
import com.issasafar.chatapp.viewmodels.ChatStateHolder
import com.issasafar.chatapp.viewmodels.ConnectionType
import com.journeyapps.barcodescanner.CaptureActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.random.nextInt
import android.graphics.Color as androidColor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    chatStateHolder: State<ChatStateHolder>,
    onChipSelected: (ConnectionType) -> Unit,
    onConfigureClicked: (Pair<String, String>) -> Unit,
    messages: List<MessageUnit> = listOf(),
    onSendClick: (String, Boolean, String) -> Unit = { _, _, _ -> },
    deviceIp: () -> String,
    onDecryptionError: (e: RuntimeException) -> Unit = {}
) {
    val port = chatStateHolder.value.port
    val name = chatStateHolder.value.userName
    val ip = chatStateHolder.value.ip
    var displayName by remember { mutableStateOf("") }
    var ipPortTextValue by remember { mutableStateOf("") }
    var ipPortTextHolderValue by remember { mutableStateOf("") }
    var showEcho by remember { mutableStateOf(false) }
    val filteredMessages = if (showEcho) messages else messages.filter { !it.isEchoMessage }
    ipPortTextHolderValue = if (deviceIp() != "") "${deviceIp()}:12345" else deviceIp()
    val listState = rememberLazyListState()

    LaunchedEffect(filteredMessages.size) {
        if (filteredMessages.isNotEmpty()) {
            listState.animateScrollToItem(filteredMessages.size - 1, 0)
        }
    }

    BottomSheetScaffold(

        sheetPeekHeight = 500.dp, sheetContent = {
            Box(
                modifier = modifier.fillMaxSize()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 4.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    contentPadding = PaddingValues(bottom = 72.dp, top = 8.dp)
                ) {

                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(ConnectionType.entries) {
                                FilterChip(selected = chatStateHolder.value.connectionType == it,
                                    onClick = {
                                        onChipSelected(it)
                                    },
                                    label = { Text(it.name) })
                            }
                        }
                    }
                    item {
                        this@BottomSheetScaffold.AnimatedVisibility(filteredMessages.isNotEmpty()) {

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(if (!showEcho) "Hide echo messages" else "Show echo messages")
                                Switch(checked = showEcho, onCheckedChange = {
                                    showEcho = it
                                })
                            }
                        }
                    }
                    item {
                        this@BottomSheetScaffold.AnimatedVisibility(filteredMessages.isEmpty()) {
                            Text(
                                "No messages yet",
                                modifier = Modifier.padding(vertical = 128.dp, horizontal = 16.dp)
                            )
                        }
                    }

                    items(items = filteredMessages, key = { it.id }) {
                        var isMine by remember { mutableStateOf(false) }
                        isMine = it.senderIp == deviceIp()
                        var showIp by remember { mutableStateOf(false) }
                        var showTime by remember { mutableStateOf(false) }
                        val onRowClicked = {
                            showIp = !showIp
                        }
                        val onBubbleClicked = {
                            showTime = !showTime
                        }
                        ChatBubble(
                            modifier = Modifier.animateItemPlacement(),
                            isMine = isMine,
                            showTime = showTime,
                            showIp = showIp,
                            messageUnit = it,
                            onBubbleClicked = onBubbleClicked,
                            onRowClicked = onRowClicked,
                            onDecryptionError = onDecryptionError
                        )
                    }
                }

                Box(
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    this@BottomSheetScaffold.AnimatedVisibility(
                        visible = chatStateHolder.value.connectionType == ConnectionType.CLIENT,
                        enter = slideInVertically { it },
                        exit = slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = spring(stiffness = Spring.StiffnessMedium)
                        )
                    ) {
                        SendBar(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 2.dp, vertical = 8.dp),
                            onSendClick = onSendClick
                        )
                    }
                }
            }
        }) {
        val scrollState = rememberLazyListState()
        var qrCodeBitmap by remember { mutableStateOf<Bitmap?>(null) }
        var showQrCode by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        val mColor = 0xF7F2F9
        val showQrCodeClicked = {
            onConfigureClicked("" to ipPortTextHolderValue)
            showQrCode = false

            if (ipPortTextValue.isEmpty()) {
                val txt = Gson().toJson(ipPortTextHolderValue)
                qrCodeBitmap = generateQRCodeBitmap(text = txt, color = mColor)
            } else {
                val txt = Gson().toJson(ipPortTextValue)
                qrCodeBitmap = generateQRCodeBitmap(text = txt, color = mColor)
            }
            showQrCode = true
            scope.launch {
                delay(500)
                scrollState.animateScrollToItem(scrollState.layoutInfo.totalItemsCount - 1)
            }

        }
        val context = LocalContext.current
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val data = result.data
            val rawResult = data?.getStringExtra("SCAN_RESULT")
            rawResult?.let {
                try {
                    val ipPort = Gson().fromJson(it, String::class.java)
                    onConfigureClicked(displayName to ipPort)
                } catch (e: RuntimeException) {
                    Toast.makeText(context, "Error Scanning QR-code", Toast.LENGTH_SHORT).show()
                }
            }
        }
        val scanQrCodeClicked = {
            val intent = Intent(context, PortraitCaptureActivity::class.java)
            launcher.launch(intent)
        }
        var doManualConfigure by remember { mutableStateOf(false) }
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text("Manual configuration")
                    Switch(
                        checked = doManualConfigure,
                        onCheckedChange = { doManualConfigure = !doManualConfigure })
                }
            }
            item {
                AnimatedVisibility(chatStateHolder.value.connectionType == ConnectionType.SERVER) {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = {
                            displayName = it
                        },
                        placeholder = { Text(text = "Display Name", color = Color.LightGray) },
                        modifier = Modifier.padding(bottom = 8.dp),
                        singleLine = true,
                        textStyle = TextStyle(textDirection = TextDirection.Content)
                    )
                }
            }
            item {
                AnimatedVisibility(doManualConfigure) {
                    OutlinedTextField(value = ipPortTextValue, onValueChange = {
                        ipPortTextValue = it
                    }, placeholder = {
                        Text(
                            text = if (ipPortTextHolderValue != "") ipPortTextHolderValue else "xx.xx.xx.xx:port",
                            color = Color.LightGray
                        )
                    }, modifier = Modifier.padding(bottom = 8.dp), singleLine = true
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedVisibility(doManualConfigure) {
                        Button(
                            onClick = { onConfigureClicked(displayName to ipPortTextValue) },
                        ) {
                            Text(if (chatStateHolder.value.connectionType == ConnectionType.SERVER) "Start Server" else "Connect to Server")
                        }
                    }
                    Button(
                        onClick = { if (chatStateHolder.value.connectionType == ConnectionType.SERVER) showQrCodeClicked() else scanQrCodeClicked() },
                    ) {
                        Text(if (chatStateHolder.value.connectionType == ConnectionType.SERVER) "Generate Qr" else "Scan Qr")
                    }
                }
                AnimatedVisibility(name != "") {
                    Text(text = if (name != "") "Display Name: $name" else "")
                }
                AnimatedVisibility(ip != "") {
                    Text(text = if (ip != "") "Ip Address: $ip:$port" else "")
                }
                AnimatedVisibility(showQrCode) {
                    QRCodeBox(qrCodeBitmap = qrCodeBitmap)
                }



            }
            item {
                Spacer(modifier = Modifier.height(500.dp))
            }
        }
    }
}

@Preview
@Composable
fun QRCodeBitmap() {
    val bitmap = generateQRCodeBitmap("this is sample")
    ChatappTheme {
        QRCodeBox(bitmap)
    }
}

@Composable
fun QRCodeBox(qrCodeBitmap: Bitmap?) {
    Box(contentAlignment = Alignment.Center) {
        Image(
            bitmap = qrCodeBitmap!!.asImageBitmap(),
            contentDescription = "QR Code",
            modifier = Modifier
                .clip(RoundedCornerShape(25.dp))
        )
        Image(
            painter = painterResource(R.drawable.app_logo),
            contentDescription = null,
            modifier = Modifier
                .size(25.dp)
                .clip(RoundedCornerShape(5.dp))
        )
    }
}

@Composable
fun QRCodeScanner(onResult: (String) -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val rawResult = data?.getStringExtra("SCAN_RESULT")
        rawResult?.let(onResult)
    }

    Button(onClick = {
        val intent = Intent(context, CaptureActivity::class.java)
        launcher.launch(intent)
    }) {
        Text("Scan QR Code")
    }
}


fun generateQRCodeBitmap(text: String, color: Int = androidColor.WHITE): Bitmap? {
    return try {
        val size = 512
        val bits = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bits[x, y]) androidColor.BLACK else color)
            }
        }
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

}

@Preview()
@Composable
fun ChatBubblePreview() {

    val isMine by remember { mutableStateOf(true) }
    var showIp by remember { mutableStateOf(false) }
    val onRowClicked = {
        showIp = !showIp
    }
    val msg = MessageUnit(
        "issa",
        "this is a sample message for testing the ui \nand i want to make sure it works fine \nbut also it doesn't blames the ui for no reasons.",
        ConnectionType.SERVER,
        "1.2.3.4",
        "sent for you"
    )
    val msg2 = MessageUnit(
        "issa safar", "hello", ConnectionType.SERVER, "1.2.3.4", "sent for you"
    )

    var showDate by remember { mutableStateOf(false) }
    val onBubbleClicked = {
        showDate = !showDate
    }
    ChatappTheme {
        Column {
            ChatBubble(
                modifier = Modifier, isMine, showDate, showIp, msg, onBubbleClicked, onRowClicked
            )
            ChatBubble(modifier = Modifier, false, showDate, showIp, msg2, onBubbleClicked)
            ChatBubble(
                modifier = Modifier,
                false,
                showDate,
                showIp,
                msg2.copy(isEncrypted = true),
                onBubbleClicked
            )
            ChatBubble(
                modifier = Modifier,
                false,
                showDate,
                showIp,
                msg2.copy(description = "how to make this work and fine"),
                onBubbleClicked
            )
            ChatBubble(
                modifier = Modifier,
                true,
                true,
                showIp,
                msg2.copy(description = "how to make this work and fine"),
                onBubbleClicked
            )
            ChatBubble(
                modifier = Modifier,
                false,
                showDate,
                showIp,
                msg2.copy(message = "\uD83D\uDE01"),
                onBubbleClicked
            )
        }
    }
}

@Composable
fun ChatBubble(
    modifier: Modifier = Modifier,
    isMine: Boolean,
    showTime: Boolean,
    showIp: Boolean,
    messageUnit: MessageUnit,
    onBubbleClicked: () -> Unit,
    onRowClicked: () -> Unit = {},
    onDecryptionError: (e: RuntimeException) -> Unit = {}
) {
    val message = messageUnit.message.trim()
    val emojiCount = message.count { it.isSurrogate() }
    val isAllEmoji = emojiCount == message.length
    val scope = rememberCoroutineScope()

    val messageFontSize: TextUnit = when {
        (emojiCount / 2 == 1) && isAllEmoji -> {
            64.sp
        }

        (emojiCount / 2 == 2) && isAllEmoji -> {
            42.sp
        }

        (emojiCount / 2 in 3..4) && isAllEmoji -> {
            32.sp
        }

        (emojiCount / 2 in 5..8) && isAllEmoji -> {
            24.sp
        }

        else -> {
            12.sp
        }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = {
                    onRowClicked()
                })
            }, horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        val interactionState = remember { MutableInteractionSource() }
        val normalCorner = 8.dp
        val smallCorner = 2.dp
        Card(
            modifier = Modifier
                .animateContentSize()
                .widthIn(max = 390.dp)
                .width(IntrinsicSize.Max)
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .clip(
                    if (isMine) RoundedCornerShape(
                        topEnd = smallCorner,
                        topStart = normalCorner,
                        bottomEnd = normalCorner,
                        bottomStart = normalCorner
                    ) else RoundedCornerShape(
                        topEnd = normalCorner,
                        topStart = smallCorner,
                        bottomStart = normalCorner,
                        bottomEnd = normalCorner
                    )
                )
                .background(
                    if (!isMine) MaterialTheme.colorScheme.tertiaryContainer else CardDefaults.cardColors().containerColor
                )
                .clickable(
                    indication = null, interactionSource = interactionState
                ) { onBubbleClicked() },
            colors = if (!isMine) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer) else CardDefaults.cardColors()
        ) {

            val infiniteTransition = rememberInfiniteTransition(label = "infinite transition")
            val animatedColor by infiniteTransition.animateColor(
                initialValue = Color(0xFF60DDAD),
                targetValue = Color(0xFF4285F4),
                animationSpec = infiniteRepeatable(
                    tween(1000), RepeatMode.Reverse
                ),
                label = "color"
            )
            var showDecryptionDialog by remember { mutableStateOf(false) }
            var password by remember { mutableStateOf("") }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    messageUnit.owner,
                    lineHeight = 1.1.sp,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isMine) animatedColor else LocalContentColor.current,
                    modifier = Modifier.align(if (isMine) Alignment.TopEnd else Alignment.TopStart),
                    style = TextStyle(textDirection = TextDirection.Content)
                )
            }
            Column(
                modifier = Modifier
                    .padding(
                        horizontal = 8.dp, vertical = 2.dp
                    )
                    .fillMaxWidth(),
                horizontalAlignment = if (isAllEmoji) Alignment.CenterHorizontally else Alignment.Start

            ) {

                if (messageUnit.isEncrypted) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showDecryptionDialog = true }) {
                            Icon(
                                painter = painterResource(R.drawable.baseline_lock_24),
                                tint = animatedColor,
                                contentDescription = null
                            )
                        }
                    }
                } else {
                    Text(
                        messageUnit.message,
                        fontSize = messageFontSize,
                        style = TextStyle(textDirection = TextDirection.Content)
                    )
                }
                AnimatedVisibility(
                    showIp, enter = fadeIn(tween(400)) + expandVertically(
                        tween(
                            400
                        )
                    )

                ) {
                    Text(
                        messageUnit.description,
                        fontSize = 8.sp,
                        style = TextStyle(textDirection = TextDirection.Content)
                    )
                }
            }
            AnimatedVisibility(showDecryptionDialog) {
                AlertDialog(
                    title = {
                        Text("Configure Decryption")
                    },
                    text = {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it.trim() },
                            placeholder = { Text("Password") })
                    },
                    onDismissRequest = { showDecryptionDialog = false },
                    confirmButton = {
                        TextButton(onClick = {
                            showDecryptionDialog = false
                            scope.launch {
                                try {
                                    messageUnit.message =
                                        messageUnit.message.decrypt(password = password)
                                    messageUnit.isEncrypted = false
                                } catch (e: RuntimeException) {
                                    onDecryptionError(e)
                                }
                            }
                        }) {
                            Text(text = "Confirm")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDecryptionDialog = false }) {
                            Text(text = "Dismiss")
                        }
                    }
                )

            }
            AnimatedVisibility(
                showTime,
                exit = fadeOut() + shrinkVertically(
                    animationSpec = spring(
                        dampingRatio = 0.3f, stiffness = Spring.StiffnessMedium
                    )
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (!isMine) Alignment.BottomEnd else Alignment.BottomStart
                ) {
                    Text(
                        text = messageUnit.time,
                        fontSize = 8.sp,
                        lineHeight = 2.sp,
                        modifier = Modifier.padding(2.dp)
                    )
                }
            }
        }
    }
}


@Preview
@Composable
fun SendBarPreview() {
    ChatappTheme {
        SendBar()
    }
}

@Composable
fun SendBar(
    modifier: Modifier = Modifier, onSendClick: (String, Boolean, String) -> Unit = { _, _, _ -> }
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
    ) {

        val rainbowColors = listOf(
            Color(0xFF9B4D96), // Purple-Pink
            Color(0xFFFF4F00), // Nova Orange
            Color(0xFF00FF66), // Neon Green
            Color(0xFF00D8FF), // Bright Blue
            Color(0xFFFF00FF), // Magenta
            Color(0xFFFF6F61), // Bright Red
            Color(0xFF3B0A45)  // Deep Violet
        )
        val buttonColors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.primary
        )
        val infiniteTransition = rememberInfiniteTransition(label = "rainbow transition")
        val animatedColor by infiniteTransition.animateColor(
            initialValue = rainbowColors.first(),
            targetValue = rainbowColors.last(),
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 4000
                    for (i in rainbowColors.indices) {
                        rainbowColors[i] at (i * 500) // 500ms for each color transition
                    }
                }, repeatMode = RepeatMode.Reverse
            ),
            label = "Rainbow Color"
        )

        val auroraColor by infiniteTransition.animateColor(
            initialValue = Color(0xFF60DDAD),
            targetValue = Color(0xFF4285F4),
            animationSpec = infiniteRepeatable(
                tween(1000), RepeatMode.Reverse
            ),
            label = "color"
        )
        val animatedButtonColor by infiniteTransition.animateColor(
            initialValue = buttonColors.first(),
            targetValue = buttonColors.last(),
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 3000
                    for (i in buttonColors.indices) {
                        buttonColors[i] at (i * 500) // 500ms for each color transition
                    }
                }, repeatMode = RepeatMode.Reverse
            ),
            label = "Rainbow Color"
        )


        var tempMessage by remember { mutableStateOf("") }
        var isButtonAnimated by remember { mutableStateOf(false) }
        val randomFlow = MutableStateFlow(Random.nextInt(5..10))
        val randomLength by randomFlow.collectAsState()
        if (tempMessage.isEmpty()) {
            isButtonAnimated = false
        }
        if (tempMessage.length == randomLength && !isButtonAnimated && tempMessage.trim()
                .isNotEmpty()
        ) {
            isButtonAnimated = true
        }
        LaunchedEffect(Unit) {
            delay(3000)
            randomFlow.emit(Random.nextInt(5..10))
        }
        LaunchedEffect(isButtonAnimated) {
            delay(1000)
            isButtonAnimated = false
        }

        var isFocused by remember { mutableStateOf(false) }
        var isEncrypted by remember { mutableStateOf(false) }
        var showEncryptionDialog by remember { mutableStateOf(false) }
        var password by remember { mutableStateOf("") }
        OutlinedTextField(modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .border(
                if (isFocused) 2.dp else 0.5.dp,
                if (isFocused) animatedColor else MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(30.dp)
            )
            .shadow(4.dp, shape = RoundedCornerShape(30.dp))
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primaryContainer,
                unfocusedBorderColor = MaterialTheme.colorScheme.primary,
                focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                focusedTextColor = MaterialTheme.colorScheme.primary,
            ),
            shape = RoundedCornerShape(30.dp),
            value = tempMessage,
            textStyle = TextStyle(
                textDirection = TextDirection.Content
            ),
            onValueChange = { tempMessage = it },
            placeholder = { Text("Enter message") },
            maxLines = 5,
            leadingIcon = {
                IconButton(onClick = {
                    showEncryptionDialog = !showEncryptionDialog
                }) {
                    Icon(
                        painter = painterResource(if (isEncrypted) R.drawable.baseline_lock_24 else R.drawable.baseline_lock_open_24),
                        contentDescription = null,
                        tint = if (isEncrypted) MaterialTheme.colorScheme.primary else Color.DarkGray
                    )
                }
            },
            trailingIcon = {
                Box(modifier = Modifier.fillMaxHeight()) {
                    Button(
                        onClick = {
                            val msg = tempMessage.trim()
                            tempMessage = ""
                            onSendClick(msg, isEncrypted, password)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isButtonAnimated) animatedButtonColor else Color.Unspecified),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(vertical = 4.dp, horizontal = 8.dp)
                    ) {
                        Text(
                            "send",
                        )
                    }
                }
            })

        //

        AnimatedVisibility(showEncryptionDialog) {
            AlertDialog(onDismissRequest = { isEncrypted = false },
                title = { Text("Configure Encryption") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it.trim() },
                            placeholder = { Text("Password") })
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        isEncrypted = password.isNotEmpty()
                        showEncryptionDialog = false
                    }) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showEncryptionDialog = false
                    }) {
                        Text("Dismiss")
                    }
                })
        }
    }
}


