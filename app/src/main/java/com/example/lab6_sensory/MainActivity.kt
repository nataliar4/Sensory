package com.example.lab6_sensory

import android.annotation.SuppressLint
import android.app.UiModeManager.MODE_NIGHT_YES
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.animation.Animation.RELATIVE_TO_SELF
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import org.w3c.dom.Text
import java.lang.Math.toDegrees

class MainActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var mSensorManager: SensorManager
    private lateinit var locationManager: LocationManager
    private var mLightSensor: Sensor? = null
    private var mTempSensor: Sensor? = null
    private var mAccSensor: Sensor? = null
    private var mGravSensor: Sensor? = null
    private lateinit var compass: ImageView
    private lateinit var tempVal: TextView
    private lateinit var coordVal: TextView
    private lateinit var azimuthVal: TextView
    private lateinit var lightVal: TextView
    private lateinit var clayout: ConstraintLayout
    private var tempMeas = ""
    private var tempAvailble: Boolean = false
    private var accVal = FloatArray(3)
    private var gravVal = FloatArray(3)
    private var accBool = false
    private var gravBool = false
    private  var azimuth = 0.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        compass = findViewById(R.id.compass)
        tempVal = findViewById(R.id.temperatureValue)
        coordVal = findViewById(R.id.coordValue)
        azimuthVal = findViewById(R.id.azimuthValue)
        lightVal = findViewById(R.id.lightValue)
        clayout = findViewById(R.id.constrLayout)
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

//        SENSORY-----------------------
        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null){
            mTempSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
            tempAvailble = true
        } else {
            tempVal.text = "No temperature sensor availble"
            tempAvailble = false
        }
        mAccSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mGravSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)

//        LOKALIZACJA-------------------
        if (ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10, 1F) {
            event ->
                val y = "%.3f".format(event.longitude)
                val x = "%.3f".format(event.latitude)
                coordVal.text = "$x, $y"
        }

//      TEMPERATURA---------------------
        CoroutineScope(Dispatchers.Main).launch {
            showTemperature()
            println("Coroutine scope")
        }

    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}


    override fun onSensorChanged(event: SensorEvent?) {
//  TEMPERATURA
        if (event?.sensor?.type == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            val temp = event.values[0]
            tempMeas = "$temp Celsius"
            println(tempMeas)
        }
//  KOMPAS
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            lowPass(event.values, accVal)
            accBool = true
        }
        if (event?.sensor?.type == Sensor.TYPE_GRAVITY) {
            lowPass(event.values, gravVal)
            gravBool = true
        }
        if (accBool && gravBool) {
            var r = FloatArray(9)
            if (SensorManager.getRotationMatrix(r, null, accVal, gravVal)) {
                var orienation = FloatArray(3)
                SensorManager.getOrientation(r, orienation)
                val newAzimuth = (toDegrees(orienation[0].toDouble()) + 360).toFloat() % 360

                val rotateAnimation = RotateAnimation(
                    azimuth,
                    -newAzimuth,
                    RELATIVE_TO_SELF, 0.5f,
                    RELATIVE_TO_SELF, 0.5f)
                rotateAnimation.duration = 1000
                rotateAnimation.fillAfter = true
                if (!this::compass.isInitialized) {compass = findViewById(R.id.compass)}
                compass.startAnimation(rotateAnimation)
                azimuth = -newAzimuth

                azimuthVal.text = azimuth.toString()
            }
        }
//  ŚWIATŁO
        if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
            val light = event.values[0]
//            if (previousLight < 5000.0f && light >= 5000.0f) {
////                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
////                recreate()
//                Toast.makeText(this, "Light", Toast.LENGTH_SHORT)
//            } else if (previousLight < 5000.0f && light >= 5000.0f){
////                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
////                recreate()
//                Toast.makeText(this, "Light", Toast.LENGTH_SHORT)
//            }
//            println("$previousLight, $light")
//            previousLight = light
            if (light > 5000) {
                lightVal.text = "Light"
                azimuthVal.setTextColor(Color.parseColor("#FF000000"))
                tempVal.setTextColor(Color.parseColor("#FF000000"))
                coordVal.setTextColor(Color.parseColor("#FF000000"))
                lightVal.setTextColor(Color.parseColor("#FF000000"))
                clayout.setBackgroundResource(R.color.white)
            } else {
                lightVal.text = "Dark"
                azimuthVal.setTextColor(Color.parseColor("#FFBBBBBB"))
                tempVal.setTextColor(Color.parseColor("#FFBBBBBB"))
                coordVal.setTextColor(Color.parseColor("#FFBBBBBB"))
                lightVal.setTextColor(Color.parseColor("#FFBBBBBB"))
                clayout.setBackgroundResource(R.color.black)
            }
        }
    }
    fun lowPass(input: FloatArray, output: FloatArray) {
        val alpha = 0.05f
        for (i in input.indices) {
            output[i] += alpha * (input[i] - output[i])
        }
    }
    suspend fun showTemperature() {
        while(true) {
            tempVal.text = tempMeas
            println("showTemperature: $tempMeas")
            delay(60000L)
        }
    }
    override fun onResume() {
        super.onResume()
        mSensorManager.registerListener(this, mLightSensor, SensorManager.SENSOR_DELAY_NORMAL)
        if (tempAvailble) mSensorManager.registerListener(this, mTempSensor, SensorManager.SENSOR_DELAY_NORMAL)
        mSensorManager.registerListener(this, mAccSensor, SensorManager.SENSOR_DELAY_NORMAL)
        mSensorManager.registerListener(this, mGravSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }
    override fun onPause() {
        super.onPause()
        mSensorManager.unregisterListener(this, mLightSensor)
        mSensorManager.unregisterListener(this, mTempSensor)
        mSensorManager.unregisterListener(this, mAccSensor)
        mSensorManager.unregisterListener(this, mGravSensor)
    }
}
