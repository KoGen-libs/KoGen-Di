package kz.evko.kogen_di

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.MutableStateFlow
import kz.evko.kogen.di.koGenViewModel
import kz.evko.kogen_di.annotations.KoGenViewModel
import kz.evko.kogen_di.ui.theme.KoGenDITheme

class SecondActivity : ComponentActivity() {
    val viewModel: FirstViewModel by koGenViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            KoGenDITheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "firstScreen") {
                    composable("firstScreen") {
                        FirstScreen(navController, viewModel)
                    }
                    composable("secondScreen") {
                        SecondScreen(navController)
                    }
                }
            }
        }
    }
}

@Composable
fun FirstScreen(
    navController: NavHostController,
    viewModel: FirstViewModel,
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color.Cyan),
            ) {
                Image(
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp)
                        .clickable {
                            viewModel.setPage(Pages.Main)
                        },
                    painter = painterResource(R.drawable.ic_launcher_background),
                    contentDescription = "",
                )
                Image(
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp)
                        .clickable {
                            viewModel.setPage(Pages.Second)
                        },
                    painter = painterResource(R.drawable.ic_launcher_background),
                    contentDescription = "",
                )
                Image(
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp)
                        .clickable {
                            viewModel.setPage(Pages.Third)
                        },
                    painter = painterResource(R.drawable.ic_launcher_background),
                    contentDescription = "",
                )
            }
        }
    ) { innerPadding ->
        Page(modifier = Modifier.padding(innerPadding), type = state) {
            navController.navigate("secondScreen")
        }
    }
}

@Composable
private fun Page(modifier: Modifier = Modifier, type: Pages, click: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(type.color)
    ) {
        Button(onClick = click, modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)) {
            Text("Navigate")
        }
    }
}

enum class Pages(val color: Color) {
    Main(Color.Red), Second(Color.Green), Third(Color.Blue),
}

@KoGenViewModel
class FirstViewModel : ViewModel() {
    val state: MutableStateFlow<Pages> = MutableStateFlow(Pages.Main)

    fun setPage(page: Pages) {
        state.value = page
    }
}

@Composable
fun SecondScreen(
    navController: NavHostController,
    viewModel: SecondViewModel = koGenViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
                .background(Color.Green)
        ) {
            Text(state)
        }
    }
}

@KoGenViewModel
class SecondViewModel : ViewModel() {
    val state: MutableStateFlow<String> = MutableStateFlow("Hello world")
}