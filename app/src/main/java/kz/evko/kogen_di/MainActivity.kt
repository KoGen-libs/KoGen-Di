package kz.evko.kogen_di

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kz.evko.kogen.di.koGenViewModel
import kz.evko.kogen.di.setApplicationContext
import kz.evko.kogen_di.annotations.KoGenComponent
import kz.evko.kogen_di.annotations.KoGenViewModel
import kz.evko.kogen_di.test.NameUseCase
import kz.evko.kogen_di.ui.theme.KoGenDITheme
import java.util.UUID

class MainActivity : ComponentActivity() {
    val viewModel: MainViewModel by koGenViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setApplicationContext(applicationContext)

        setContent {
            KoGenDITheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        modifier = Modifier.padding(innerPadding),
                        viewModel = viewModel,
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

    val state: MutableStateFlow<MainState> = MutableStateFlow(MainState())

    init {
        state.value = state.value.copy(name = nameUseCase.getName())
    }

    fun getName(name: String) {
        state.value = state.value.copy(name = name)
    }
}

data class MainState(
    val name: String = "",
)

@Composable
fun Greeting(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel// = koGenViewModel(),
) {
    val activity = LocalActivity.current

    val state by viewModel.state.collectAsState()
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Hello ${state.name}!",
        )

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            onClick = {
                viewModel.getName("Oh no, the scope workes: \n${UUID.randomUUID()}")
            }) {
            Text(text = "Update Name")
        }

        Button(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
            onClick = {
                activity?.startActivity(Intent(activity, SecondActivity::class.java))
            }) {
            Text("Next")
        }
    }
}

interface ApiSource {
    fun getName(): String
}

@KoGenComponent
class ApiSourceImpl(
) : ApiSource {
    override fun getName(): String = UUID.randomUUID().toString()
}

@KoGenComponent(singleton = true)
class NameService(private val source: ApiSource) {
    private var name: String? = null

    fun getName(): String {
        return source.getName()
    }
}
