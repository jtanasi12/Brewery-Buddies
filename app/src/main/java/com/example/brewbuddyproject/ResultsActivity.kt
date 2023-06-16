package com.example.brewbuddyproject

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.brewbuddyproject.databinding.ActivityResultsBinding
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.Exception

class ResultsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var fireBasedb: FirebaseFirestore

    val BASE_URL = "https://api.openbrewerydb.org/v1/"
    val breweryLocations = ArrayList<Brewery>()
    val sampleSpot = Brewery("Test Brewery", "test street", "test city",
    "CT", "06040", "47", "72", "475-226-1717", "www.google.com", "Micro", "null", 0.0F, "null")

    private val TAG = "ResultsActivity"

    private lateinit var myRecycleAdapter: MyRecycleAdapter

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityResultsBinding
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var locationPermissionGranted = false

    // provides a way to convert a physical address into geographic coordinates (latitude and longitude)
    private lateinit var geocoder: Geocoder


    // an arbitrary number request code to be used when requesting permission to access the device's location.
    private val ACCESS_LOCATION_CODE = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //setContentView(R.layout.activity_results)


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        /*Places.initialize(applicationContext, getString(R.string.maps_api_key))
        placesClient = Places.createClient(this)*/

        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        // Get the Cloud firestore Instance
        fireBasedb = FirebaseFirestore.getInstance()

        //val zipCode = intent.getSerializableExtra("zipCode") as String
       // breweryLocations.add(sampleSpot)

        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        myRecycleAdapter = MyRecycleAdapter(breweryLocations, this)

        // Set the recyclers state to show are in the favoritesAdapter so it'll perform that functionality
        myRecycleAdapter.setState("resultsAdapter")
        recyclerView.adapter = myRecycleAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        //pingBreweryAPI(breweryLocations[0].zip) //temp search location
    } // End of OnCreate Method

    fun pingBreweryAPI(searchString: String) {

        //split string into city and state
        val delim = ","
        val cityStateArray = searchString.split(delim).toTypedArray()
        for(entry in cityStateArray) {
            //entry.replace("\\s".toRegex(), "1")
            entry.trim()
            Log.d(TAG, "StringSplit: entry = ${entry.trim()}")
        }

        val retrofitBuilder = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val breweryLocationsAPI = retrofitBuilder.create(BreweryService::class.java)
        //Log.d(TAG, "pingBreweryAPI: inside pingAPI")
        //breweryLocationsAPI.getBreweryByZip(searchString, 5).enqueue(object : Callback<List<Brewery>?> {
        //breweryLocationsAPI.getBreweryByLocation("38.8977,77.0365", 5).enqueue(object : Callback<List<Brewery>?> {
        breweryLocationsAPI.getBreweryByCityState(cityStateArray[0].trim(), convertStatetoFullName(cityStateArray[1].trim())).enqueue(object : Callback<List<Brewery>?> {

            // If we get a response from the API
            override fun onResponse(call: Call<List<Brewery>?>, response: Response<List<Brewery>?>) {
                Log.d(TAG, "onResponse: $response")

                // Body contains a string of the data pulled from the API, the data is in the order
                // of the Brewery's header constructor, so the data is automatically stored with the
                // appropriate variables
                val body = response.body()
                if(body == null) {
                    Log.d(TAG, "Valid response was not received")
                    return
                }

                breweryLocations.clear()
                breweryLocations.addAll(body)
                fillDataHoles() //this function fills in missing lat/lon from API Results

                //Log.d(TAG, "onResponse: inside onResponse")
                myRecycleAdapter.notifyDataSetChanged()
                if(mMap != null)
                //how do we wait for pingBreweryAPI to finish and update breweryLocations??
                    updatePins()
                else Log.d(TAG, "searchButton: nMap is null")
                //Log.d(TAG, "a: ${breweryLocations[0].street}")
                //Log.d(TAG, "b: ${breweryLocations[1].name}")
                //Log.d(TAG, "c: ${breweryLocations[2].name}")


            }

            override fun onFailure(call: Call<List<Brewery>?>, t: Throwable) {
                Log.d(TAG, "onResponse: $t")
            }
        })
    }

    @Suppress("DEPRECATION")
    fun fillDataHoles() {
        geocoder = Geocoder(this)
        for ( (index, brewery) in breweryLocations.withIndex()) {
            if(brewery.longitude == null || brewery.longitude == null) {
                try {
                    val builder = StringBuilder()
                    builder.append(brewery.street)
                        .append(", ")
                        .append(brewery.city)
                        .append(", ")
                        .append(brewery.state)
                    Log.d(TAG, "fillDataHoles: $builder")
                    val addressInfo = geocoder.getFromLocationName(builder.toString(), 1)
                    if(!addressInfo.isNullOrEmpty()) {
                        breweryLocations[index].latitude = addressInfo[0].latitude.toString()
                        breweryLocations[index].longitude = addressInfo[0].longitude.toString()
                        Log.d(TAG, "fillDataHoles: ${breweryLocations[index].name} has lat: ${breweryLocations[index].latitude} and lon: ${breweryLocations[index].longitude}")
                    }
                }
                catch (e: Exception) {
                    Log.e(TAG, "fillDataHoles: ${e.message}")
                }
            }
        }
    }

    fun updatePins() {
        //add pins to Maps
        mMap.clear()
        //var hartford = LatLng(41.7659,-72.681)
        //Log.d(TAG, "updatePins: inside Update Pins")
        var coordinates = LatLng(38.37250, 90.255)
        for (brewery in breweryLocations) {
            //log these lat/lons and see what we got
            if(brewery.longitude != null) {
                Log.d(TAG, "updatePins: ${brewery.name} with brewtype ${brewery.brewery_type} lon ${brewery.longitude.toDouble()} and lat ${brewery.latitude.toDouble()}")
                coordinates = LatLng(brewery.latitude.toDouble(), brewery.longitude.toDouble())
                mMap.addMarker(MarkerOptions().position(coordinates).title("${brewery.name}"))
            }
        }
        //mMap.addMarker(MarkerOptions().position(hartford).title("Test Location"))
        //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(hartford, 12F))
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(coordinates, 10F)
        mMap.animateCamera(cameraUpdate)
    }

    fun searchButton(view: View) {
        val searchView = findViewById<EditText>(R.id.enter_search)
        val searchString = searchView.text.toString()
        searchView.hideKeyboard()
        Log.d(TAG, "searchButton: inside search button")
        /*if(searchString.isEmpty())
            return*/

        pingBreweryAPI(searchString)
        /*if(mMap != null)
            //how do we wait for pingBreweryAPI to finish and update breweryLocations??
            updatePins()
        else Log.d(TAG, "searchButton: nMap is null")*/
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)
        var hartford = LatLng(41.7659,-72.681)
        //mMap.addMarker(MarkerOptions().position(hartford).title("Hartford"))

        //this of course is happening too soon. no locations returned by API yet
        //if (breweryLocations.isNotEmpty()) {
        /*for (brewery in breweryLocations) {
            //log these lat/lons and see what we got
            Log.d(TAG, "onMapReady: ${brewery.name}, Lat=${brewery.latitude}, Lon=${brewery.longitude}")
            val coordinates = LatLng(brewery.latitude.toDouble(), brewery.longitude.toDouble())
            mMap.addMarker(MarkerOptions().position(coordinates).title("${brewery.name}"))
        }*/
        //}
        //this zoom is not doing anything
        //mMap.moveCamera(CameraUpdateFactory.zoomBy(500.0F))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(hartford))



        // Request location permission and show user's current location on the Map
        getLocationPermission()
        //updatePins()
    }

    /*@SuppressLint("MissingPermission")
    private fun getDeviceLocation() {
        *//*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         *//*
        try {
            if (locationPermissionGranted) {
                val locationResult = fusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Set the map's camera position to the current location of the device.
                        lastKnownLocation = task.result
                        if (lastKnownLocation != null) {
                            mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                LatLng(lastKnownLocation!!.latitude,
                                    lastKnownLocation!!.longitude), DEFAULT_ZOOM.toFloat()))
                        }
                    } else {
                        Log.d(TAG, "Current location is null. Using defaults.")
                        Log.e(TAG, "Exception: %s", task.exception)
                        mMap?.moveCamera(CameraUpdateFactory
                            .newLatLngZoom(defaultLocation, DEFAULT_ZOOM.toFloat()))
                        mMap?.uiSettings?.isMyLocationButtonEnabled = false
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }*/

    private fun getLocationPermission() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            // Permission is granted
            enableUserLocation()
        } else {

            // Permission is not granted
            // show an explanation
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    ACCESS_LOCATION_CODE)

            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    ACCESS_LOCATION_CODE)

                // ACCESS_LOCATION_CODE is an int constant (you decide a number). The callback method gets the
                // result of the request.
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            ACCESS_LOCATION_CODE -> {
                enableUserLocation()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableUserLocation() {
        locationPermissionGranted = true
        mMap.isMyLocationEnabled = true
    }

    private fun View.hideKeyboard() {
        val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
    }

    fun convertStatetoFullName(stateInput: String): String {
        val state = stateInput.toLowerCase()
        Log.d(TAG, "convertStatetoFullName: $state")
        return when(state) {
            "al" -> "alabama"
            "ak" -> "alaska"
            "az" -> "arizona"
            "ar" -> "arkansas"
            "ca" -> "california"
            "co" -> "colorado"
            "ct" -> "connecticut"
            "de" -> "delaware"
            "dc" -> "district of columbia"
            "fl" -> "florida"
            "ga" -> "georgia"
            "hi" -> "hawaii"
            "id" -> "idaho"
            "il" -> "illinois"
            "in" -> "indiana"
            "ia" -> "iowa"
            "ks" -> "kansas"
            "ky" -> "kentucky"
            "la" -> "louisiana"
            "me" -> "maine"
            "md" -> "maryland"
            "ma" -> "massachusetts"
            "mi" -> "michigan"
            "mn" -> "minnesota"
            "ms" -> "mississippi"
            "mo" -> "missouri"
            "mt" -> "montana"
            "ne" -> "nebraska"
            "nv" -> "nevada"
            "nh" -> "new hampshire"
            "nj" -> "new jersey"
            "nm" -> "new mexico"
            "ny" -> "new york"
            "nc" -> "north carolina"
            "nd" -> "north dakota"
            "oh" -> "ohio"
            "ok" -> "oklahoma"
            "or" -> "oregon"
            "pa" -> "pennsylvania"
            "pr" -> "puerto rico"
            "ri" -> "rhode island"
            "sc" -> "south carolina"
            "sd" -> "south dakota"
            "tn" -> "tennessee"
            "tx" -> "texas"
            "ut" -> "utah"
            "vt" -> "vermont"
            "va" -> "virgina"
            "wa" -> "washington"
            "wv" -> "west virginia"
            "wi" -> "wisconsin"
            "wy" -> "wyoming"
            else -> {stateInput} //if invalid input, return original search and generate error
        }
    }

    /*// Call the brewery function
    fun addBreweryButton(view: View){
        // Get the last position in the Brewery List that was last selected
        val position = myRecycleAdapter.getCurrentBrewerySelection()

        if(position != -1){
            addBrewery(breweryLocations[position])
        }
        else
        {
            showDialog("Error", "Please make a selection by clicking a brewery")
        }


    }


    // ************CURRENTLY THIS IS HARDCODED JUST FOR TESTING PURPOSES*******

    // Passing a Brewery and adding it to the database
    private fun addBrewery(passedBrewery : Brewery){


        // Getting an instance of our collection
        val breweryDatabase = fireBasedb.collection("breweries")

        // Getting the auto generated id for the document that we want to create
        val documentId = breweryDatabase.document().id


        // Adding the data
        breweryDatabase.document(documentId).set(passedBrewery)

        showDialog("Success", "${passedBrewery.name} Brewery has been added.")

    }

    fun backButton(view: View){
        finish()
    }

    private fun showDialog(title : String,Message : String){
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(true)
        builder.setTitle(title)
        builder.setMessage(Message)
        builder.show()
    }*/


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

}
