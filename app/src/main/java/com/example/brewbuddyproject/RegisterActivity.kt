package com.example.brewbuddyproject

import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {


    val TAG = "LoginActivity"

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        setContentView(R.layout.register_activity)


        // If the current is not null that means we already have a user logged in and don't need to login, so we launch the main activity
        if (FirebaseAuth.getInstance().currentUser != null) {

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)

            finish() // Kills this activity
        } else { // The current user is null and we need to create authentication for the user


            val signinActivityLauncher =
                registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> // Replace the default (it) with result


                    // Sign in success
                    if (result.resultCode == Activity.RESULT_OK) {


                        val user = FirebaseAuth.getInstance().currentUser

                        startActivity(Intent(this,ActivityLogin::class.java))

                        // Make sure to call finish(), otherwise the user would be able to go back to the activityLogin
                        finish()
                    } // End of If statement

                    // Sign in failed
                    else {


                        val response = IdpResponse.fromResultIntent(result.data)
                        if (response == null) {
                            Log.d(TAG, "loginActivity: the user has cancelled the sign in request")
                        } else {
                            Log.d(TAG, "loginActivity: ${response.error?.errorCode}")
                        }
                    } // End of else statement


                } // End of signinActivityLauncher


            // Login Button Set On Click Listener Event

            findViewById<TextView>(R.id.login_button).setOnClickListener {


                // Email and Google Email authentications
                val loginMethods = arrayListOf(
                    AuthUI.IdpConfig.EmailBuilder().build(),
                    AuthUI.IdpConfig.GoogleBuilder().build()
                )

                // Create a sign in intent
                val signInIntent = AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(loginMethods)
                    .setTosAndPrivacyPolicyUrls("https://example.com", "https://example.com")
                    .setLogo(R.drawable.beer)
                    .setAlwaysShowSignInMethodScreen(true)
                    .setIsSmartLockEnabled(false)
                    .setTheme(R.style.Theme_BrewBuddyProject) // This changes the theme to match our applications theme
                    .build()

                signinActivityLauncher.launch(signInIntent)

            } // End of setOnClickListener


        } // End of else - where the user is null and we need to create Auth.


    } // onCreate




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


} // End of class