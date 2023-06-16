package com.example.brewbuddyproject

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface BreweryService {

    //base_url = https://api.openbrewerydb.org
    //we need = /v1/breweries?by_dist=38.8977,77.0365&per_page=3
    //or        /v1/breweries?by_postal=06040&per_page=1
    //CCSU Long/Lat = 41.6930770,-72.7664217
    @GET("breweries")
    fun getBreweryByLocation(@Query("by_dist") location: String,
                             @Query("per_page") numberResults: Int): Call<List<Brewery>>
    @GET("breweries")
    fun getBreweryByZip(@Query("by_postal") zip: String,
                        @Query("per_page") numberResults: Int): Call<List<Brewery>>

    @GET("breweries")
    fun getBreweryByCityState(@Query("by_city") city: String,
                              @Query("by_state") state: String): Call<List<Brewery>>
}