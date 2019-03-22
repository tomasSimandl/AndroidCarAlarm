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

/**
 * Class which is used for creating calls to phone number stored in SharedPreferences.
 *
 * @param context is my context used for access values in shared preferences.
 */
class CallProvider(private val context: MyContext) {

    /** Logger tag */
    private val tag = "CallProvider"

    /**
     * Object used for static access to [check] method.
     */
    companion object Check : CheckObjByte {
        /**
         * Method checks if service for making calls can be used.
         */
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

    /**
     * Method create call on phone number which is stored in SharedPreferences and set by user.
     */
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