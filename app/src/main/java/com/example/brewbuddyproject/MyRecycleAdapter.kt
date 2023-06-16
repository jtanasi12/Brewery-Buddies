package com.example.brewbuddyproject

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore


class MyRecycleAdapter(private val breweryLocations: ArrayList<Brewery>, private val context: Context): RecyclerView.Adapter<MyRecycleAdapter.MyViewHolder>() {

    var counter = 1
    var selectedItemPosition: Int = -1 // -1 means that nothing was selected
    private lateinit var fireBasedb: FirebaseFirestore

    private val TAG = "MyRecycleAdapter"

    // Keeps track if we are in the Favorites or addBrewery State
    // TO separate functionality
    private var state : String? = null

    inner class MyViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val brewName = itemView.findViewById<TextView>(R.id.brew_name)
        val brewStreet = itemView.findViewById<TextView>(R.id.brew_street)
        val brewCity = itemView.findViewById<TextView>(R.id.brew_city_state)
        val brewPhone = itemView.findViewById<TextView>(R.id.brew_phone)
        val brewWebsite = itemView.findViewById<TextView>(R.id.brew_website)
       //we're not adding these fields to recycle view
        /*val brewRating = itemView.findViewById<TextView>(R.id.brew_rating)
        val brewComment = itemView.findViewById<TextView>(R.id.brew_comments)*/


    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_item, parent, false)
        Log.d(TAG, "onCreateViewHolder: ${counter++}")


        return MyViewHolder(view)


    }

    override fun getItemCount(): Int {
        return breweryLocations.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.brewName.text = breweryLocations[position].name
        holder.brewStreet.text = breweryLocations[position].street
        holder.brewCity.text = "${breweryLocations[position].city}, ${breweryLocations[position].state} ${breweryLocations[position].zip}"
        holder.brewPhone.text = breweryLocations[position].phone
        holder.brewWebsite.text = breweryLocations[position].website_url
        //we're not adding these fields, I don't want them in ResultsActivity and it's too complicated to make a new row_item that holds them
        /*if(state == "favoritesAdapter") {
            holder.brewRating.text = "Rating: " + breweryLocations[position].rating.toString()
            holder.brewComment.text = "Comments: " + breweryLocations[position].comments
        }*/




        holder.itemView.setOnClickListener {
            selectedItemPosition = position
            notifyDataSetChanged()

            if(state == "resultsAdapter") {

                val myIntent = Intent(context, DetailsActivity::class.java)

                myIntent.putExtra("brewName", holder.brewName.text)
                myIntent.putExtra("brewStreet", holder.brewStreet.text)
                myIntent.putExtra("brewCity", holder.brewCity.text)
                myIntent.putExtra("brewPhone", holder.brewPhone.text)
                myIntent.putExtra("brewWebsite", holder.brewWebsite.text)
                myIntent.putExtra("position", selectedItemPosition)
                val brewery = breweryLocations[selectedItemPosition]

                myIntent.putExtra("passedBrewery", brewery)

                context.startActivity(myIntent)
            } else{

            } // This implies we are in the favoritesAdapter and this should handle deletion

        }

        // Deletion Code
        holder.itemView.setOnLongClickListener {
            val fireBasedb = FirebaseFirestore.getInstance()
            var foundID:String? = null

            // Retrieve data from the Firebase API
            fireBasedb.collection("breweries")
                .orderBy("name")
                .get()
                .addOnSuccessListener { documents ->

                    for (document in documents) {
                        Log.d(TAG, "document.get(\"name\") = ${document.get("name")}")
                        Log.d(TAG, "breweryLocations[position].name = ${breweryLocations[position].name}")
                        if (document.get("name")==breweryLocations[position].name) {//&&(document.get("user")== Firebase.auth.currentUser?.email))
                            foundID = document.id
                            Log.d(TAG, "foundID = $foundID")
                        }
                    }
                    if(foundID!=null) {
                        val builder = AlertDialog.Builder(context)
                        builder.setCancelable(true)
                        builder.setTitle("Delete Favorite?")
                        builder.setMessage("Are you sure you want to delete ${breweryLocations[position].name}?")
                        builder.setPositiveButton("Yes"){ dialog, which ->
                            fireBasedb.collection("breweries").document(foundID!!).delete()
                            breweryLocations.removeAt(position)
                            //if(state == "favoritesAdapter") //only remove from list in FAVORITES view
                                notifyItemRemoved(position)
                                notifyDataSetChanged()

                            // Check that this is in the favoritesAdapter
                            if(state == "favoritesAdapter"){
                                if(isEmpty()){ // If the arrayList is empty then we want to pop back to the main menu
                                    (context as Activity).finish() // Pop the favorites and go back to main menu if empty
                                }
                            }

                            Toast.makeText(context, "Brewery Deleted!", Toast.LENGTH_SHORT).show()


                        }
                        builder.setNegativeButton("No") { dialog, which ->

                        }
                        builder.show()
                    } else Toast.makeText(context, "This Brewery isn't a favorite", Toast.LENGTH_SHORT).show()
                }
            true
        }


        if(selectedItemPosition == position){
            // Change Current Selection to Orange
            holder.itemView.setBackgroundColor(Color.parseColor("#f28e1c"))

        }
        else
        {
            // All other items will be changed back to a default of White
            holder.itemView.setBackgroundColor(Color.parseColor("#FFFFFF"))

        }
    }

    private fun showDialog(title : String,Message : String){
        val builder = AlertDialog.Builder(context)
        builder.setCancelable(true)
        builder.setTitle(title)
        builder.setMessage(Message)
        builder.setPositiveButton("OK"){ dialog, which ->

        }
        builder.show()
    }

    // Return the name of the current brewery that is selected
    fun getCurrentBrewerySelection() : Int {
        return selectedItemPosition
    }

    fun getState() : String? {
        return state
    }

    // Change the current state that we are in
    fun setState(newState: String){
        state = newState
    }

   private fun isEmpty() : Boolean{

        if(state == "favoritesAdapter"){
            if(breweryLocations.size == 0){
                return true
            }
        }
            return false
    }

}