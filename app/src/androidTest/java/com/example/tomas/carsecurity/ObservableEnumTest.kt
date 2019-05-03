package com.example.tomas.carsecurity

import android.os.Looper
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import com.example.tomas.carsecurity.context.MyContext
import com.example.tomas.carsecurity.sensors.BatteryDetector
import com.example.tomas.carsecurity.sensors.LocationProvider
import com.example.tomas.carsecurity.sensors.MoveDetector
import com.example.tomas.carsecurity.sensors.SoundDetector
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class ObservableEnumTest {

    private lateinit var context: MyContext

    @Before
    fun init() {
        val appContext = InstrumentationRegistry.getTargetContext()
        context = MyContext(appContext, Looper.getMainLooper())
    }

    @Test
    fun getInstanceMoveDetector() {
        val detector = ObservableEnum.MoveDetector.getInstance(context)
        assertTrue(detector is MoveDetector)
    }

    @Test
    fun getInstanceBatteryDetector() {
        val detector = ObservableEnum.BatteryDetector.getInstance(context)
        assertTrue(detector is BatteryDetector)
    }

    @Test
    fun getInstanceSoundDetector() {
        val detector = ObservableEnum.SoundDetector.getInstance(context)
        assertTrue(detector is SoundDetector)
    }

    @Test
    fun getInstanceLocationProvider() {
        val detector = ObservableEnum.LocationProvider.getInstance(context)
        assertTrue(detector is LocationProvider)
    }
}