package com.example.tomas.carsecurity.preferenceFragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v14.preference.PreferenceFragment
import android.view.MenuItem
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.SettingsActivity

open class MyPreferenceFragment : PreferenceFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = getString(R.string.preference_file_key)
        preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE

        setHasOptionsMenu(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            startActivity(Intent(activity, SettingsActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}