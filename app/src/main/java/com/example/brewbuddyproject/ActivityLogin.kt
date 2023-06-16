package com.example.brewbuddyproject

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ActivityLogin : AppCompatActivity() {

    val TAG = "ActivityLogin"

    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)



        val currentUser = FirebaseAuth.getInstance().currentUser

        // If currentUser is null, open the RegisterActivity
        if (currentUser == null) {

            animatedSplashScreen() // The Splash Screen will call the Register Screen

         //  startRegisterActivity() <-- So this is no longer needed


        } else {
            findViewById<TextView>(R.id.user_name).text = currentUser.displayName
            findViewById<TextView>(R.id.user_email).text = currentUser.email
            Glide.with(this)
                .load(currentUser.photoUrl)
                .placeholder(R.drawable.baseline_person_24)
                .circleCrop()
                .into(findViewById<ImageView>(R.id.username_image))

        }
    }



    // Create the menu option
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    // Handle when the menu options log out is selected, if it's selected - log the user out
    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        return when(item.itemId){
            R.id.action_logout->{

                AuthUI.getInstance().signOut(this)
                    .addOnCompleteListener{task->

                        // If the logout button is clicked, logout the user
                        if(task.isSuccessful){
                          startRegisterActivity() // They need to register/log back in
                        }
                        else
                        {
                           Log.e(TAG, "Task is not successful: ${task.exception}")
                        }
                    }
                true
            }
            else->{
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun startRegisterActivity() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)

        finish()
    }

    fun startButton(view: View){
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)

        finish() // Pop off the stack from Activity login and go back to the main menu which was previously created
    }

    private fun animatedSplashScreen() {
        val intent = Intent(this, SplashActivity::class.java)
        startActivity(intent)

        finish()
    }


}

