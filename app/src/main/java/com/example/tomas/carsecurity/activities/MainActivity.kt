package com.example.tomas.carsecurity.activities

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.View
import com.example.tomas.carsecurity.MainService
import com.example.tomas.carsecurity.R
import com.example.tomas.carsecurity.fragments.LoginFragment
import com.example.tomas.carsecurity.fragments.MainFragment
import com.example.tomas.carsecurity.storage.Storage
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.nav_header_main.view.*


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

        drawer_layout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: View) {
                // display user name
                Thread (Runnable {
                    val user = Storage.getInstance(this@MainActivity).userService.getUser()

                    nav_view.post {
                        val header = nav_view.getHeaderView(0)
                        if (user == null){
                            header.usernameTextView.visibility = View.GONE
                            header.carnameTextView.visibility = View.GONE
                        } else {
                            header.usernameTextView.text = user.username
                            header.carnameTextView.text = user.carName
                            header.usernameTextView.visibility = View.VISIBLE
                            header.carnameTextView.visibility = View.VISIBLE
                        }
                    }
                }).start()
            }
        })
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