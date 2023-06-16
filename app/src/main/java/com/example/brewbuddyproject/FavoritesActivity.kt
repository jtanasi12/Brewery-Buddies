package com.example.brewbuddyproject

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.view.isEmpty
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

class FavoritesActivity : AppCompatActivity() {

    private lateinit var myRecycleAdapter: MyRecycleAdapter
    private lateinit var fireBasedb: FirebaseFirestore
    private val TAG = "FavoritesActivity"
    private val favoriteLocations = ArrayList<Brewery>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        // Set the recyclers state to show are in the favoritesAdapter so it'll perform that functionality


        // Get the Cloud firestore Instance
        fireBasedb = FirebaseFirestore.getInstance()


        // Retrieve data from the Firebase API
        fireBasedb.collection("breweries")
            .orderBy("name")
            .get()
            .addOnSuccessListener { documents ->

                for (document in documents) {

                    if (document.get("user") == getCurrentUser()) {
                        Log.d(TAG, "${document.id} => ${document.data}")

                        Log.d(
                            TAG,
                            "Brewery: ${document.get("name")}, ${document.get("street")}, ${
                                document.get("city")
                            }," +
                                    "${document.get("state")}, ${document.get("zip")}, ${
                                        document.get(
                                            "phone"
                                        )
                                    }, " +
                                    "${document.get("website_url")}  "
                        )

                        // Convert the data pulled into a brewery object inside an array
                        favoriteLocations.add(
                            Brewery(
                                "${document.get("name")}",
                                "${document.get("street")}",
                                "${document.get("city")}",
                                "${document.get("state")}",
                                "${document.get("zip")}",
                                "null",
                                "null",
                                "${document.get("phone")}",
                                "${document.get("website_url")}",
                                "${document.get("brewery_type")}",
                                "null",
                                0.0f,
                                "${document.get("comments")}"
                            )
                        )


                    }

                    // Convert this to the recycler
                    val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
                    myRecycleAdapter = MyRecycleAdapter(favoriteLocations, this)

                    myRecycleAdapter.setState("favoritesAdapter") // Set the state for specific functionality
                    recyclerView.adapter = myRecycleAdapter
                    recyclerView.layoutManager = LinearLayoutManager(this)
                    recyclerView.addItemDecoration(
                        DividerItemDecoration(
                            this,
                            DividerItemDecoration.VERTICAL
                        )
                    )

                }
            }


    }

    // Return the current users email
    private fun getCurrentUser() : String? {
        return Firebase.auth.currentUser?.email.toString()
    }

}
