package com.example.tomas.carsecurity

import android.content.Context
import android.content.SharedPreferences

interface CheckObjString {
     fun check(context: Context, sharedPreferences: SharedPreferences): String
}