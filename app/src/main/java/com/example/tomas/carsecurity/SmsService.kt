package com.example.tomas.carsecurity

import android.telephony.SmsManager

class SmsService {

    private val smsManager = SmsManager.getDefault()

    fun sendTextMessage(phoneNumber: String, text: String) {
        val textParts = smsManager.divideMessage(text)

        if (textParts.size > 1) {
            smsManager.sendMultipartTextMessage(phoneNumber, null, textParts, null, null)
        } else {
            smsManager.sendTextMessage(phoneNumber, null, text, null, null)
        }
    }
}