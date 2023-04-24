package com.example.weatherapijetpack

import android.Manifest

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.weatherapijetpack.data.SharedPreferencesCity
import com.example.weatherapijetpack.data.WeatherModel
import com.example.weatherapijetpack.helpers.*
import com.example.weatherapijetpack.screens.DialogSearch
import com.example.weatherapijetpack.screens.MainCard
import com.example.weatherapijetpack.screens.TabLayout
import com.example.weatherapijetpack.ui.theme.WeatherAPIJetpackTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    private val cityState = mutableStateOf("")
    private lateinit var sharedPreferences: SharedPreferencesCity
    private val dialogSettings = mutableStateOf(false)
    private lateinit var fLocationClient: FusedLocationProviderClient
    private lateinit var pLauncher: ActivityResultLauncher<String>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        init()
        setContent {
            WeatherAPIJetpackTheme {
                val checkLocationState = remember {
                    dialogSettings
                }
                DialogSettings(checkLocationState = checkLocationState)


                val city = remember {
                    cityState
                }
                getLocation(city)
                val day = remember {
                    mutableStateOf(
                        WeatherModel(
                            "",
                            "",
                            "",
                            "",
                            "",
                            "",
                            "",
                            ""
                        )
                    )
                }
                val daysList = remember {
                    mutableStateOf(listOf<WeatherModel>())
                }
                val dialogState = remember {
                    mutableStateOf(false)
                }
                DialogSearch(dialogState, onClick = {
                        city.value = it
                        requestWeatherData(
                            it,
                            this,
                            day,
                            daysList
                        )
                })

                requestWeatherData(
                    city.value,
                    this,
                    day,
                    daysList
                )
                Image(
                    painter = painterResource(id = R.drawable.sky), contentDescription = "img_1",
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.5f),
                    contentScale = ContentScale.Crop
                )
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    MainCard(
                        day,
                        onClickSync = {
                            requestWeatherData(
                                city.value,
                                this@MainActivity,
                                day,
                                daysList
                            )
                        },
                        onClickSearch = {
                            dialogState.value = true
                        }
                    )
                    TabLayout(daysList, day)
                }
            }


        }
    }

    override fun onResume() {
        super.onResume()
        if (!isLocationEnabled()) dialogSettings.value = true
    }

    override fun onDestroy() {
        super.onDestroy()
        sharedPreferences.savePref(cityState.value)
    }
    private fun init(){
        fLocationClient = LocationServices.getFusedLocationProviderClient(this)
        checkPermission()
        sharedPreferences = SharedPreferencesCity()
        sharedPreferences.sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE)
        cityState.value = sharedPreferences.getCity()
    }
    // get data from api by city name or coordinates
    private fun requestWeatherData(
        city: String,
        context: Context,
        curDay: MutableState<WeatherModel>,
        list: MutableState<List<WeatherModel>>
    ) {
        val url =
            "https://api.weatherapi.com/v1/forecast.json?key=$BASE_URL&q=$city&days=3&aqi=no&alerts=no"
        val queue = Volley.newRequestQueue(context)
        val request = StringRequest(
            Request.Method.GET, url, { response ->
                val jsonObject = JSONObject(response)
                val nextDays = parseDaysArrays(jsonObject)
                list.value = nextDays
                val currentDay = parseCurrentData(jsonObject, nextDays[0])
                curDay.value = currentDay
            }, {
                Toast.makeText(
                    this,
                    "I couldn't find it =(",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
        queue.add(request)
    }

    //transform JSON to ArrayList<WeatherModel>
    private fun parseDaysArrays(obj: JSONObject): ArrayList<WeatherModel> {
        val list = ArrayList<WeatherModel>()
        val array = obj.getJSONObject(FORECAST).getJSONArray(FORECASTDAY)
        val cityName = obj.getJSONObject(LOCATION).getString(NAME)
        for (i in 0 until array.length()) {
            val item = array[i] as JSONObject
            val day = item.getJSONObject(DAY)
            val condition = day.getJSONObject(CONDITION)
            val weatherModel = WeatherModel(
                cityName,
                item.getString(DATE),
                condition.getString(TEXT),
                "",
                day.getString(MAXTEMP_C).toFloat().toInt().toString(),
                day.getString(MINTEMP_C).toFloat().toInt().toString(),
                "https:" + condition.getString(ICON),
                item.getJSONArray(HOUR).toString()
            )
            list.add(weatherModel)
        }
        return list
    }

    //transform JSON to our WeatherModel
    private fun parseCurrentData(obj: JSONObject, weatherModel: WeatherModel): WeatherModel {
        val current = obj.getJSONObject(CURRENT)
        val condition = current.getJSONObject(CONDITION)
        return WeatherModel(
            weatherModel.city,
            current.getString(LAST_UPDATED),
            condition.getString(TEXT),
            current.getString(TEMP_C),
            weatherModel.maxTemp,
            weatherModel.minTemp,
            "https:" + condition.getString(ICON),
            weatherModel.hours
        )
    }

    private fun isPermissionGranted(p:String): Boolean{
        return ContextCompat.checkSelfPermission(
            this, p
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun permissionListener() {
        pLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            if (it) getLocation(cityState)
        }
    }

    private fun checkPermission() {
        if (!isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
            permissionListener()
            pLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun getLocation(city:MutableState<String>) {
        val cancellationToken = CancellationTokenSource()
        if (!(ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED)
        ) {
            fLocationClient
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationToken.token)
                .addOnCompleteListener {
                    if (it.result!=null) city.value = "${it.result.latitude}, ${it.result.longitude}"
                }
        }
    }

    private fun isLocationEnabled(): Boolean {
        val lm = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }


    @Composable
    fun DialogSettings(checkLocationState: MutableState<Boolean>) {
        if (checkLocationState.value) {
            AlertDialog(onDismissRequest = { checkLocationState.value = false },
                confirmButton = {
                    TextButton(onClick = {
                        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        checkLocationState.value = false
                    }) {
                        Text(text = "Open Settings")
                    }
                }, dismissButton = {
                    TextButton(onClick = { checkLocationState.value = false }) {
                        Text(text = "Cancel")
                    }
                }, title = {
                    Text(text = "Do you want to open settings?")
                })
        }

    }
}



