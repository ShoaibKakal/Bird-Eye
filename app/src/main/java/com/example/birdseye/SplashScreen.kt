package com.example.birdseye

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity


class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)


        val background: Thread = object : Thread() {
            override fun run() {
                try {
                    // Thread will sleep for 3 seconds
                    sleep((3 * 1000).toLong())

                    // After 3 seconds redirect to another intent
                    val i = Intent(baseContext, LocationPermissionActivity::class.java)
                    startActivity(i)

                    //Remove activity
                    finish()
                } catch (e: Exception) {
                }
            }
        }
        // start thread
        background.start()
    }
}