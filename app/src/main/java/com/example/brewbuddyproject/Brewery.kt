package com.example.brewbuddyproject

import com.bumptech.glide.Glide.init
import com.google.gson.annotations.SerializedName
import java.io.Serializable

/*data class BreweryLocations(
    val results: List<Brewery>
)*/

data class Brewery(

    val name: String,
    val street: String,
    val city: String,
    @SerializedName("state_province") val state: String,
    @SerializedName("postal_code") val zip: String,
    var longitude: String,
    var latitude: String,
    val phone: String,
    val website_url: String,
    val brewery_type: String,
    var user : String?,
    var rating : Float?,
    var comments : String?
) : Serializable
