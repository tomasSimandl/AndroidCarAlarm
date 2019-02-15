package com.example.tomas.carsecurity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem


class LoginActivity : AppCompatActivity() {

    private val clientId = "your-client-id"
    private val clientSecret = "your-client-secret"
    private val redirectUri = "your://redirecturi"

    /**
     * Set up the action bar if action bar is used.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.login)

        actionBar?.setDisplayShowHomeEnabled(true)
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    /**
     * Handle back arrow click in action bar
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
