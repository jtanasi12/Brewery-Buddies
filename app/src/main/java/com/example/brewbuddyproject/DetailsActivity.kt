package com.example.brewbuddyproject

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

class DetailsActivity : AppCompatActivity() {

    // Globals
    private val TAG = "DetailsActivity"
    private lateinit var myRecycleAdapter: MyRecycleAdapter
    private lateinit var fireBasedb: FirebaseFirestore
    private var recyclerPosition : Int = 0
    private var currentUser: String? = null
    private var passedBrewery : Brewery? = null
    private var foundID: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)

        /*val brewName = intent.getStringExtra("brewName")
        val brewStreet = intent.getStringExtra("brewStreet")
        val brewCity = intent.getStringExtra("brewCity")
        val brewPhone = intent.getStringExtra("brewPhone")
        val brewWebsite = intent.getStringExtra("brewWebsite")*/

        passedBrewery = intent.getSerializableExtra("passedBrewery") as? Brewery


        // Get the Cloud firestore Instance
        fireBasedb = FirebaseFirestore.getInstance()

      //  val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
     //   myRecycleAdapter = MyRecycleAdapter(breweryLocations, this)
       // recyclerView.adapter = myRecycleAdapter


        /*Log.d(TAG, "breweryName: $brewName")
        Log.d(TAG, "breweryStreet: $brewStreet")
        Log.d(TAG, "breweryCity: $brewCity")
        Log.d(TAG, "breweryPhone: $brewPhone")*/

        findViewById<TextView>(R.id.details_brewery_name).text = passedBrewery?.name.toString()
        findViewById<TextView>(R.id.details_brewery_addresss).text = "${passedBrewery?.street}, ${passedBrewery?.city} ${passedBrewery?.state}"
        findViewById<TextView>(R.id.details_brewery_phone_number).text = "Phone: ${passedBrewery?.phone}"
        findViewById<TextView>(R.id.details_brewery_type).text = passedBrewery?.brewery_type
        //if brewery found in favorites, pull rating and comments
        fireBasedb.collection("breweries")
            .orderBy("name")
            .get()
            .addOnSuccessListener { documents ->

                for(document in documents) {
                    if (document.get("name") != null) {
                        Log.d(TAG, "firebaseDB: document.get(\"name\") = ${document.get("name")}")
                        Log.d(TAG, "firebaseDB:  passedBrewery.name = ${passedBrewery?.name}")
                        if (document.get("name") == passedBrewery?.name) {
                            Log.d(TAG, "fireBaseDB: found matching brewery at ${document.id}")
                            foundID = document.id
                            findViewById<Button>(R.id.details_brewery_add_to_favs_button).text = "Update Favorite"
                            Log.d(TAG, "fireBaseDB: ${document.get("rating")}")
                            Log.d(TAG, "fireBaseDB: ${document.get("comments")}")
                            val tempFloat: Double? = document.getDouble("rating")
                            findViewById<RatingBar>(R.id.details_brewery_rating).rating = tempFloat?.toFloat()!!
                            findViewById<EditText>(R.id.details_brewery_details_notes).setText(document.get("comments") as String)
                        }
                    }
                }
            }



    }

    fun backButton(view: View) {
        Log.d(TAG, "backButton: inside")
        finish()
    }

    fun websiteButton(view: View){
        val i = Intent(Intent.ACTION_VIEW, Uri.parse(intent.getStringExtra("brewWebsite")))
        startActivity(i)
    }


    fun addBreweryButton(view: View){
        // Get the last position in the Brewery List that was last selected
        val position = recyclerPosition



        if(position != -1){
            addBrewery(passedBrewery)
        }
        else
        {
            showDialog("Error", "Please make a selection by clicking a brewery")
        }


    }

    // Passing a Brewery and adding it to the database
    private fun addBrewery(passedBrewery : Brewery?){

        // Get the current users email
        currentUser = getCurrentUser()

        // Attach the curent users email address to the dataobject
        if (passedBrewery != null) {
            passedBrewery.user = currentUser.toString() // Add this to the Data Object
            passedBrewery.rating = findViewById<RatingBar>(R.id.details_brewery_rating).rating
            passedBrewery.comments = findViewById<EditText>(R.id.details_brewery_details_notes).text.toString()
        }
        // Getting an instance of our collection
        val breweryDatabase = fireBasedb.collection("breweries")

        // Getting the auto generated id for the document that we want to create
        val documentId = breweryDatabase.document().id


        // Adding the data
        if (passedBrewery != null) {
            if(foundID != null)
                breweryDatabase.document(foundID!!).set(passedBrewery)
            else {
                foundID = documentId
                breweryDatabase.document(documentId).set(passedBrewery)
            }
        }

        if (passedBrewery != null) {
            showDialog("Success", "${passedBrewery.name} Brewery has been added.")
        }

    }

    private fun showDialog(title : String,Message : String){
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(true)
        builder.setTitle(title)
        builder.setMessage(Message)
        builder.show()
    }

    // Return the current users email
    private fun getCurrentUser() : String? {
        return Firebase.auth.currentUser?.email.toString()
    }

    
}