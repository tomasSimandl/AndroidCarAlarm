package com.example.tomas.carsecurity

import android.content.Context

/**
 * Interface used in Sensors for checking if sensor can be enabled.
 */
interface CheckObjByte {
     /**
      * Method checks if sensor can be enabled.
      * @param context is application context.
      * @return [CheckCodes] value which indicates result of check.
      */
     fun check(context: Context): Byte
}