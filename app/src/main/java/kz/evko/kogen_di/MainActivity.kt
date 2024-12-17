package kz.evko.kogen_di

import android.content.Context
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
import kz.evko.kogen_di.KoGenComponentFactory.inject
import kz.evko.kogen_di.KoGenComponentFactory.setApplicationContext
import kz.evko.kogen_di.annotations.KoGenComponent
import kz.evko.kogen_di.ui.theme.KoGenDITheme
import java.util.UUID

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

@Composable
fun Greeting(modifier: Modifier = Modifier, nameUseCase: NameUseCase = inject()) {
    var name by remember { mutableStateOf(nameUseCase.getName()) }
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
                name = nameUseCase.getName()
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

interface NameUseCase {
    fun getName(): String
}

@KoGenComponent
class NameUseCaseImpl(private val service: NameService) : NameUseCase {
    override fun getName(): String = service.getName()
}
