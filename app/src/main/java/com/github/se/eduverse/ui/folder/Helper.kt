package com.github.se.eduverse.ui.folder

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.se.project.model.folder.TimeTable

/*
This file is meant to put all useful methods used in different files in a centralized way
*/


/**
 * A method to create a composable to display a time table
 * @param timeTable the time table to display
 * @param startTime the hour at which the day begin on the time table
 * @param endTime the hour at which de day end on the time table
 */
@Composable
fun DisplayTimeTable(timeTable: TimeTable, startTime: Int, endTime: Int) {
    Row {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            for (hour in startTime..endTime) {
                Text("${hour}h")
            }
            ColumnTimeTable(timeTable, 1, "Mon", startTime, endTime)
            ColumnTimeTable(timeTable, 2, "Tue", startTime, endTime)
            ColumnTimeTable(timeTable, 3, "Wed", startTime, endTime)
            ColumnTimeTable(timeTable, 4, "Thu", startTime, endTime)
            ColumnTimeTable(timeTable, 5, "Fri", startTime, endTime)
            ColumnTimeTable(timeTable, 6, "Sat", startTime, endTime)
            ColumnTimeTable(timeTable, 7, "Sun", startTime, endTime)p
        }

    }
}

@Composable
private fun ColumnTimeTable(
    timeTable: TimeTable, dayN: Int, dayS: String, startTime: Int, endTime: Int
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(dayS)
        for (hour in startTime..<endTime) {
            Surface(
                color = if(timeTable.isSelected(dayN, hour)) Color.Green else Color.LightGray,
                border = BorderStroke(1.dp, Color.Black)
            ) {  }
        }
    }
}