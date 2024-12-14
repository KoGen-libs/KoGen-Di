package kz.evko.kogen_di

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kz.evko.kogen_di.KoGenComponentFactory.inject
import kz.evko.kogen_di.annotations.KoGenComponent
import kz.evko.kogen_di.ui.theme.KoGenDITheme
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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

@Composable
fun Greeting(modifier: Modifier = Modifier, nameUseCase: NameUseCase = inject()) {
    var name by remember { mutableStateOf(nameUseCase.getName()) }
    Column(modifier = modifier) {
        Text(
            text = "Hello ${name}!",
        )

        Button(onClick = {
            name = nameUseCase.getName()
        }) {
            Text(text = "Update Name")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview(
    source: ApiSource = inject()
) {
    KoGenDITheme {
        Greeting()
    }
}

interface ApiSource {
    fun getName(): String
}

@KoGenComponent
class ApiSourceImpl : ApiSource {
    override fun getName(): String = UUID.randomUUID().toString()
}

@KoGenComponent(singleton = false)
class NameService(private val source: ApiSource) {
    private var name: String? = null

    fun getName(): String {
        return if (name != null) {
            name!!
        } else {
            name = source.getName()
            name!!
        }
    }

}

interface NameUseCase {
    fun getName(): String
}

@KoGenComponent
class NameUseCaseImpl(private val service: NameService) : NameUseCase {
    override fun getName(): String = service.getName()
}
