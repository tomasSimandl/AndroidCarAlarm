package com.example.tomas.carsecurity.storage.service

import com.example.tomas.carsecurity.storage.AppDatabase
import com.example.tomas.carsecurity.storage.entity.Route

/**
 * Service which only call [database] dao objects associated with route table.
 * @param database is open Room database.
 */
class RouteService(private val database: AppDatabase) {

    /**
     * Method insert one route to database.
     * @param route which will be inserted to database.
     * @return database id of inserted route
     */
    fun saveRoute(route: Route): Long {
        return database.routeDao().insert(route)
    }

    /**
     * Method update input route in database. Route must already exits in database.
     * @param route which will be updated.
     */
    fun updateRoute(route: Route) {
        database.routeDao().update(route)
    }

    /**
     * Method returns all routes stored in database.
     * @return list of all routes.
     */
    fun getRoutes(): List<Route> {
        return database.routeDao().getAll()
    }

    /**
     * Method return specific route from database.
     * @param routeId database id of requested route
     * @return One [Route] from database
     */
    fun getRoute(routeId: Int): Route {
        return database.routeDao().get(routeId)
    }

    /**
     * Method return number of all routes in database.
     * @return number of routes in database
     */
    fun countRoutes(): Long {
        return database.routeDao().count()
    }

    /**
     * Method delete input route from database.
     * @param route which will be deleted.
     */
    fun deleteRoute(route: Route) {
        database.routeDao().delete(route)
    }
}