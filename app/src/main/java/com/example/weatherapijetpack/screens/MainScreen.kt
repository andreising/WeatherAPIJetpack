package com.example.weatherapijetpack.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.weatherapijetpack.R
import com.example.weatherapijetpack.data.WeatherModel
import com.example.weatherapijetpack.helpers.*
import com.example.weatherapijetpack.ui.theme.Blue_bg
import com.google.accompanist.pager.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject


@Composable
fun MainCard(weatherModel: MutableState<WeatherModel>, onClickSync: ()->Unit, onClickSearch: ()->Unit) {
    val currentDay = weatherModel.value
    val link = currentDay.imageUrl
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        backgroundColor = Blue_bg,
        shape = RoundedCornerShape(8.dp),
        elevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    modifier = Modifier.padding(top = 8.dp, start = 8.dp),
                    text = currentDay.time,
                    style = TextStyle(fontSize = 15.sp),
                    color = Color.White
                )
                AsyncImage(
                    model = link,
                    contentDescription = "img_weather",
                    modifier = Modifier
                        .size(35.dp)
                        .padding(top = 8.dp, end = 8.dp)
                )

            }
            Text(
                text = currentDay.city,
                modifier = Modifier.padding(top = 5.dp),
                style = TextStyle(fontSize = 20.sp),
                color = Color.White
            )
            Text(
                text = tempIndo(currentDay),
                modifier = Modifier.padding(top = 5.dp),
                style = TextStyle(fontSize = 65.sp),
                color = Color.White
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    onClickSearch.invoke()
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.search),
                        contentDescription = "img_search",
                        tint = Color.White
                    )
                }
                Text(
                    text = "${currentDay.maxTemp}°C/${currentDay.minTemp}°C",
                    color = Color.White
                )
                IconButton(onClick = {
                    onClickSync.invoke()
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.refresh),
                        contentDescription = "img_refresh",
                        tint = Color.White
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalPagerApi::class)
@Composable
fun TabLayout(daysList: MutableState<List<WeatherModel>>, day: MutableState<WeatherModel>){
    val tabList = listOf( "HOUR", "DAYS")
    val pagerState = rememberPagerState()
    val tabIndex = pagerState.currentPage
    val coroutineScope = rememberCoroutineScope()
    Column(
        modifier = Modifier.clip(RoundedCornerShape(8.dp))
    ) {
        TabRow(
            selectedTabIndex = tabIndex,
            indicator = {pos ->
                TabRowDefaults.Indicator(
                    Modifier.pagerTabIndicatorOffset(pagerState, pos)
                )
            },
            backgroundColor = Blue_bg

            ) {
            tabList.forEachIndexed { index, s ->
                Tab(selected = false, onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                },
                text = {
                    Text(text = s)
                })
            }
        }
        HorizontalPager(
            count = tabList.size,
            state = pagerState,
            modifier = Modifier.weight(1.0f)
        ) {index ->
            val list = when(index){
                0 -> getHoursList(day.value)
                1 -> daysList.value
                else -> daysList.value
            }
            MainList(list = list,  currentDay = day)

        }
    }   
}

private fun getHoursList(weatherModel: WeatherModel): List<WeatherModel>{
    if (weatherModel.hours.isEmpty()) return emptyList()
    Log.d("tagFirst", weatherModel.hours)
    val array = JSONArray(weatherModel.hours)
    val list = emptyList<WeatherModel>().toMutableList()
    for (i in 0 until array.length()) {
        val obj = array[i] as JSONObject
        val item = WeatherModel(
            weatherModel.city,
            obj.getString(TIME),
            obj.getJSONObject(CONDITION).getString(TEXT),
            obj.getString(TEMP_C),
            "",
            "",
            "https:"+obj.getJSONObject(CONDITION).getString(ICON),
            ""
        )
        list.add(item)
    }
    return list
}

fun tempIndo(currentDay: WeatherModel): String {
    val currentTemp = currentDay.currentTemp
    return if (currentTemp.isEmpty()) "${currentDay.maxTemp}°C/${currentDay.minTemp}°C"
    else "$currentTemp°C"
}