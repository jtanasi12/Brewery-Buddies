package com.example.brewbuddyproject

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)


        supportActionBar?.hide()

        // Creates the animation
        val splash_image = findViewById<ImageView>(R.id.splash_image).animate().apply {
            duration = 1000 // 1 second
            rotationX(360f)

        }.start()

        Handler(Looper.getMainLooper()).postDelayed( {
            val myIntent = Intent(this, RegisterActivity::class.java)
            startActivity(myIntent) // We then call the Register Activity and this is because
            // the splash screen is only ever called before the Register. So now in this design
            // the splash screen will always call RegisterActivity
            finish()
        }, 2000) // 2 second delay
    }
}