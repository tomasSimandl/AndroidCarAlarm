package com.example.tomas.carsecurity.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.support.v4.content.ContextCompat
import android.util.Log
import com.example.tomas.carsecurity.CheckCodes
import com.example.tomas.carsecurity.CheckObjByte
import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.context.ToolsContext

class CallProvider (private val context: MyContext) {

    private val tag = "CallProvider"

    companion object Check: CheckObjByte {
        override fun check(context: Context): Byte {
            return if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
                CheckCodes.hardwareNotSupported
            } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                CheckCodes.permissionDenied
            } else if (!ToolsContext(context).isCallAllow) {
                CheckCodes.notAllowed
            } else {
                CheckCodes.success
            }
        }
    }


    fun createCall() {
        if (check(context.appContext) == CheckCodes.success) {
            val phoneNumber = context.communicationContext.phoneNumber

            if (phoneNumber.isNotBlank()) {
                Log.d(tag, "Calling to contact phone number.")
                try {
                    val intent = Intent(Intent.ACTION_CALL)
                    intent.data = Uri.fromParts("tel", phoneNumber, null)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.appContext.startActivity(intent)
                } catch (e: Exception) {
                    Log.d(tag, """Can not make call: $e""")
                } catch (e: SecurityException) { // used because compiler can not recognise that permission is checked by method check
                    Log.d(tag, """Can not make call because Permission denied: $e""")
                }
            } else {
                Log.d(tag, "Can not call because contact number is not set.")
            }
        } else {
            Log.d(tag, "Can not call because permissions denied.")
        }
    }
}