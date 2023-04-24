package com.example.weatherapijetpack.data

import android.content.SharedPreferences


class SharedPreferencesCity {
    var sharedPreferences: SharedPreferences? = null
    fun savePref( value: String) {
        sharedPreferences?.edit()?.putString(KEY, value)?.apply()
    }
    fun getCity(): String {
        return sharedPreferences?.getString(KEY, "London") ?: "London"
    }
}
const val KEY = "key"