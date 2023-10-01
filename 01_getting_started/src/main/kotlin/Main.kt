import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.*
import java.awt.GraphicsEnvironment
import java.io.File
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

@Composable
@Preview
fun App() {
    // https://engawapg.net/jetpack-compose/2113/remember-tips/
    // https://engawapg.net/jetpack-compose/1457/theme/#i-2
    // https://developers-jp.googleblog.com/2020/11/delegating-delegates-to-kotlin.html
    // プロパティ委譲
    var text by remember { mutableStateOf("Hello, World!") }

    // https://developer.android.com/jetpack/compose/layout?hl=ja#standard-layouts
    // Columnで縦に並べる
    Column(modifier = Modifier.fillMaxWidth(1f)) {
        // Rowで横に並べる
        Row(modifier = Modifier.fillMaxWidth(1f)) {
            // https://qiita.com/maxfie1d/items/4c876ce9e0ad589a1089
            // weightで幅を設定
            Button(onClick = {
                text = "Hello, Desktop1!"
            }, modifier = Modifier.weight(1f)) {
                Text(text)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = {
                text = "Hello, Desktop2!"
            }, modifier = Modifier.weight(1f)) {
                Text(text)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(1f)) {
            // https://qiita.com/maxfie1d/items/4c876ce9e0ad589a1089
            // weightで幅を設定
            Button(onClick = {
                text = "Hello, Desktop3!"
            }, modifier = Modifier.weight(1f)) {
                Text(text)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = {
                text = "Hello, Desktop4!"
            }, modifier = Modifier.weight(1f)) {
                Text(text)
            }
        }
    }
}

class CommandText {
    var stdout by mutableStateOf("")
    var stderr by mutableStateOf("")

    private val command = ArrayList<String>()
    private var isActive = false
    private val timeoutMinutes = 10L

    private var coroutineScope = CoroutineScope(Dispatchers.IO)

    fun append(str: String) {
        command.add(str)
    }

    fun clear() {
        command.clear()
        stdout = ""
        stderr = ""
    }

    fun run() {
        if (isActive) {
            return
        }

        val processBuilder = ProcessBuilder(command)
        processBuilder.also {
            it.redirectOutput(ProcessBuilder.Redirect.PIPE)
            it.redirectError(ProcessBuilder.Redirect.PIPE)
        }

//        https://stackoverflow.com/questions/70655939/java-kotlin-way-to-redirect-command-output-to-both-stdout-and-string
        coroutineScope.launch {
            runCatching {
                processBuilder.start().also {
                    this@CommandText.isActive = true
                    it.inputStream.bufferedReader(Charset.forName("MS932")).run {
                        while (true) {
                            readLine()?.let { line ->
                                stdout += "$line\n"
                            } ?: break
                        }
                    }
                    it.errorStream.bufferedReader(Charset.forName("MS932")).run {
                        while (true) {
                            readLine()?.let { line ->
                                stderr += "$line\n"
                            } ?: break
                        }
                    }
                    it.waitFor(timeoutMinutes, TimeUnit.MINUTES)
                    if (it.isAlive) {
                        it.destroy()
                    }
                    this@CommandText.isActive = false
                }
            }.onSuccess {
            }.onFailure {
                stdout = "command failure."

            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
@Preview
fun MainContent() {
    val command = remember { CommandText() }
    var text by remember { mutableStateOf("ipconfig /all") }
    MaterialTheme (typography = Typography(
        defaultFontFamily  = FontFamily(
            Font("assets/fonts/Myrica.TTC", weight = FontWeight.Normal),
        ),
        body1 = TextStyle(fontFamily = FontFamily(
            Font("assets/fonts/Myrica.TTC", weight = FontWeight.Normal),
        ))
    ),
    ) {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("title") }, navigationIcon = {
//                https://www.gesource.jp/weblog/?p=8591
                    IconButton(
                        onClick = { command.clear() }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Home"
                        )
                    }
                }, modifier = Modifier.fillMaxWidth(1f))
            }, floatingActionButton = {
                Row {

                    // https://techbooster.org/android/ui/18533/
                    FloatingActionButton(onClick = {
                        command.clear()
                        text.split(" ").forEach {
                            command.append(it)
                        }
                        command.run()
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "追加")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    FloatingActionButton(onClick = {}) {
                        Icon(Icons.Filled.Add, contentDescription = "追加")
                    }

                }
            }
        ) {
            Column {
                Row {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("Label") },
                        modifier = Modifier.weight(1f).onKeyEvent {
                            if (it.key == Key.Enter) {
                                text = text.trim()
                                true
                            } else {
                                false
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            command.clear()
                            text.split(" ").forEach {
                                command.append(it)
                            }
                            command.run()
                        })
                    )
                }
                val stateVertical = rememberScrollState(0)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    Column(modifier = Modifier.verticalScroll(stateVertical)) {
                        Text(command.stdout)
                        Text(command.stderr)
                    }
                    VerticalScrollbar(
                        modifier = Modifier.align(Alignment.CenterEnd)
                            .fillMaxHeight(),
                        adapter = rememberScrollbarAdapter(stateVertical)
                    )

                }

            }
        }

    }
}

//https://github.com/JetBrains/compose-multiplatform/issues/1122
// modal
@OptIn(ExperimentalComposeUiApi::class)
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication
    ) {
        //https://github.com/JetBrains/compose-multiplatform/blob/393cfdd6638eee465b3c974fe9e6b3b0a1db57c1/tutorials/Tray_Notifications_MenuBar_new/README.md?plain=1#L140
//        MenuBar {
//            Menu("File", mnemonic = 'F') {
//                Item("Copy", onClick = { action = "Last action: Copy" }, shortcut = KeyShortcut(Key.C, ctrl = true))
//                Item("Paste", onClick = { action = "Last action: Paste" }, shortcut = KeyShortcut(Key.V, ctrl = true))
//
//            }
//        }
        MainContent()
    }
}
