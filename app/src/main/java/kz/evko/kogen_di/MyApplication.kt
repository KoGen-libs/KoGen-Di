package kz.evko.kogen_di

import android.app.Application
import android.content.Context
import kz.evko.kogen_di.annotations.KoGenBin

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        //setApplicationContext(applicationContext)
    }
}

@KoGenBin
fun baseUrl(context: Context): String = "https://api.example.com/".plus(context.getString(R.string.app_name))