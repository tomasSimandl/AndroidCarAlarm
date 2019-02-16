package com.example.tomas.carsecurity.storage.service

import com.example.tomas.carsecurity.storage.AppDatabase
import com.example.tomas.carsecurity.storage.entity.Route

class RouteService (private val database: AppDatabase) {

    fun saveRoute(route: Route): Long{
        return database.routeDao().insert(route)
    }

    fun updateRoute(route: Route) {
        database.routeDao().update(route)
    }

    fun getRoutes(): List<Route> {
        return database.routeDao().getAll()
    }

    fun getRoute(routeId: Int): Route {
        return database.routeDao().get(routeId)
    }

    fun deleteRoute(route: Route) {
        database.routeDao().delete(route)
    }

    fun finishRoute(route: Route) {
        route.finished = true
        database.routeDao().update(route)
    }
}