package com.example.tomas.carsecurity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v4.view.MenuItemCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.View
import com.example.tomas.carsecurity.fragments.LoginFragment
import com.example.tomas.carsecurity.fragments.MainFragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener  {



    private val tag = "MainActivity"
    private var isHomePage = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        // floating button
        button_float_setting.setOnClickListener {
            openSettings()
        }


        // side panel initialization
        val toggle = ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.navigation_panel_open, R.string.navigation_panel_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        onNavigationItemSelected(nav_view.menu.findItem(R.id.menu_home))
    }

    override fun onDestroy() {
        super.onDestroy()

        // stop service if it is not foreground service
        val intent = Intent(applicationContext, MainService::class.java)
        intent.action = MainService.Actions.ActionTryStop.name
        applicationContext.startService(intent)
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        }
        else if(isHomePage) {
            finish()
        } else {
            onNavigationItemSelected(nav_view.menu.findItem(R.id.menu_home))
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        isHomePage = false

        when (item.itemId) {
            R.id.menu_home -> { isHomePage = true; showFragment(MainFragment(), item) }
            R.id.menu_login -> showFragment(LoginFragment(), item)
            R.id.menu_settings -> openSettings()
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }


    private fun showFragment(fragment: Fragment, item: MenuItem){

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.view_content, fragment)
                .addToBackStack(item.title as String)
                .commit()

        title = item.title
        drawer_layout.closeDrawers()
    }

    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }






}
