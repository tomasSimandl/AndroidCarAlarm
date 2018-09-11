package com.example.tomas.carsecurity

import android.content.Context
import android.content.SharedPreferences

interface CheckObjByte {
     fun check(context: Context, sharedPreferences: SharedPreferences): Byte
}