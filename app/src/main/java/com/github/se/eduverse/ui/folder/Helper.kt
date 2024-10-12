package com.github.se.eduverse.ui.folder

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.github.se.eduverse.model.folder.TimeTable

/*
This file is meant to put all useful methods used in different files in a centralized way
*/

const val daysInWeek = 7
const val firstHour = 7
const val lastHour = 21
const val hoursInDay = lastHour - firstHour

/**
 * A method to create a composable to display a time table
 *
 * @param timeTable the time table to display
 */
@Composable
fun DisplayTimeTable(timeTable: TimeTable) {
    val scrollState = rememberScrollState()
    Box(
        modifier =
        Modifier.fillMaxHeight(0.32.toFloat()).verticalScroll(scrollState).testTag("timeTable")) {
        Row {
            Column(
                modifier = Modifier.padding(top = 10.dp, start = 4.dp, end = 7.dp),
                horizontalAlignment = Alignment.End) {
                for (hour in firstHour..lastHour) {
                    Text("${hour}h")
                }
            }
            ColumnTimeTable(timeTable, 1, "Mon")
            ColumnTimeTable(timeTable, 2, "Tue")
            ColumnTimeTable(timeTable, 3, "Wed")
            ColumnTimeTable(timeTable, 4, "Thu")
            ColumnTimeTable(timeTable, 5, "Fri")
            ColumnTimeTable(timeTable, 6, "Sat")
            ColumnTimeTable(timeTable, 7, "Sun")
        }
    }
}

@Composable
private fun ColumnTimeTable(timeTable: TimeTable, dayN: Int, dayS: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(dayS)
        for (hour in firstHour ..< lastHour) {
            Surface(
                color = if (timeTable.isSelected(dayN, hour)) Color.Green else Color.White,
                border = BorderStroke(1.dp, Color.Black)) {
                Text("            ")
            }
        }
    }
}
