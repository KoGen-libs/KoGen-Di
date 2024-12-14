package kz.evko.kogen_di

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kz.evko.kogen_di.KoGenComponentFactory.inject
import kz.evko.kogen_di.annotations.KoGenComponent
import kz.evko.kogen_di.ui.theme.KoGenDITheme

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
fun Greeting(modifier: Modifier = Modifier, name: Masuika = inject()) {
    Text(
        text = "Hello ${name.sayHello()}!",
        modifier = modifier
    )
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
    override fun getName(): String = "Weee"
}

interface MasuikasName {
    fun sayHello(): String
}

@KoGenComponent(singleton = true)
class Masuika: MasuikasName {
    override fun sayHello(): String {
        return "Hello masuika"
    }
}
