package kz.evko.kogen_di

import android.app.Application
import android.content.Context
import kz.evko.kogen_di.annotations.KoGenBin

class MyApplication : Application() {

    fun provideContext(): Context = applicationContext
}

@KoGenBin
fun baseUrl(): String = "https://api.example.com/"