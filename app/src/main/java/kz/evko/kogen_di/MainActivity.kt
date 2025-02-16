package kz.evko.kogen_di

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import kz.evko.kogen.di.koGenViewModel
import kz.evko.kogen.di.setApplicationContext
import kz.evko.kogen_di.annotations.KoGenComponent
import kz.evko.kogen_di.annotations.KoGenViewModel
import kz.evko.kogen_di.test.NameUseCase
import kz.evko.kogen_di.ui.theme.KoGenDITheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setApplicationContext(applicationContext)
        setContent {
            KoGenDITheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@KoGenViewModel
class MainViewModel(
    private val nameUseCase: NameUseCase,
) : ViewModel() {
    fun getName(): String = nameUseCase.getName()
}

@Composable
fun Greeting(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = koGenViewModel(),
) {
    var name by remember { mutableStateOf(viewModel.getName()) }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Hello ${name}!",
        )

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            onClick = {
                name = viewModel.getName()
            }) {
            Text(text = "Update Name")
        }
    }
}

interface ApiSource {
    fun getName(): String
}

@KoGenComponent
class ApiSourceImpl(
    private val baseUrl: String,
) : ApiSource {
    override fun getName(): String = baseUrl//UUID.randomUUID().toString()
}

@KoGenComponent(singleton = true)
class NameService(private val source: ApiSource) {
    private var name: String? = null

    fun getName(): String {
        return source.getName()
    }
}
