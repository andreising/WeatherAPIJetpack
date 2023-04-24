package com.example.weatherapijetpack.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.weatherapijetpack.data.WeatherModel
import com.example.weatherapijetpack.ui.theme.Blue_bg

@Composable
fun MainList(list: List<WeatherModel>, currentDay: MutableState<WeatherModel>){
    LazyColumn(modifier = Modifier.fillMaxSize()){
        itemsIndexed(
            list
        ) {_, item ->
            ListItem(item, currentDay)
        }
    }
}


@Composable
fun ListItem(weatherModel: WeatherModel, currentDay: MutableState<WeatherModel>) {
    Row(
        modifier = Modifier
            .padding(top = 5.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(5.dp))
            .background(Blue_bg)
            .clickable {
                if (weatherModel.hours.isEmpty()) return@clickable
                currentDay.value = weatherModel
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            Text(text = weatherModel.time)
            Text(
                text = weatherModel.condition,
                color = Color.White
            )
        }
        Text(
            text = weatherModel.currentTemp.ifEmpty { "${weatherModel.maxTemp}°C/${weatherModel.minTemp}°C" },
            color = Color.White,
            fontSize = 25.sp
        )
        AsyncImage(
            modifier = Modifier
                .padding(4.dp)
                .size(40.dp),
            model = weatherModel.imageUrl,
            contentDescription = "img_item"
        )
    }
}

@Composable
fun DialogSearch(boolean: MutableState<Boolean>, onClick: (String)-> Unit){
    if (boolean.value) {
        val cityName = remember {
            mutableStateOf("")
        }
        AlertDialog(onDismissRequest = {
            boolean.value = false
        }, confirmButton = {
            TextButton(onClick = {
                onClick(cityName.value)
                boolean.value = false
            }) {
                Text(text = "OK")
            }
        }, dismissButton = {
            TextButton(onClick = {
                boolean.value = false
            }) {
                Text(text = "Cancel")
            }
        },
            title = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Enter city name")
                    TextField(value = cityName.value, onValueChange = {
                        cityName.value = it
                    })
                }

            })
    }

}

