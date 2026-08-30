package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var dbHelper: DatabaseHelper
    private val apiKey = "0590ef8fff22da8e34831b5f4da0902b"

    private lateinit var tvLocation: TextView
    private lateinit var tvTemp: TextView
    private lateinit var tvCondition: TextView
    private lateinit var tvHumidity: TextView
    private lateinit var tvWind: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnToggleUnit: Button

    private var currentCity = ""
    private var currentTemp = ""
    private var currentCondition = ""
    private var unitMode = "metric" // "metric" for °C, "imperial" for °F

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        dbHelper = DatabaseHelper(this)

        // SharedPreferences: Load user unit preference
        val sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        unitMode = sharedPref.getString("unit", "metric") ?: "metric"

        tvLocation = findViewById(R.id.tvLocation)
        tvTemp = findViewById(R.id.tvTemp)
        tvCondition = findViewById(R.id.tvCondition)
        tvHumidity = findViewById(R.id.tvHumidity)
        tvWind = findViewById(R.id.tvWind)
        progressBar = findViewById(R.id.progressBar)
        btnToggleUnit = findViewById(R.id.btnToggleUnit)

        updateUnitButtonText()
        checkLocationPermissionAndFetch()

        // SharedPreferences toggle handling
        btnToggleUnit.setOnClickListener {
            unitMode = if (unitMode == "metric") "imperial" else "metric"
            sharedPref.edit().putString("unit", unitMode).apply()
            updateUnitButtonText()
            checkLocationPermissionAndFetch()
        }

        // Refresh weather data
        findViewById<Button>(R.id.btnRefresh).setOnClickListener {
            checkLocationPermissionAndFetch()
        }

        // Logout handling
        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Save weather record to SQLite
        findViewById<Button>(R.id.btnSave).setOnClickListener {
            if (currentCity.isNotEmpty()) {
                val success = dbHelper.insertRecord(currentCity, currentTemp, currentCondition)
                if (success) Toast.makeText(this, "Record Saved!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No weather data available to save", Toast.LENGTH_SHORT).show()
            }
        }

        // Open Saved Records screen
        findViewById<Button>(R.id.btnViewSaved).setOnClickListener {
            startActivity(Intent(this, SavedRecordsActivity::class.java))
        }

        // Share weather via Implicit Intent
        findViewById<Button>(R.id.btnShare).setOnClickListener {
            if (currentCity.isNotEmpty()) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "Current weather in $currentCity: $currentTemp, $currentCondition.")
                }
                startActivity(Intent.createChooser(shareIntent, "Share Weather Via"))
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    private fun checkLocationPermissionAndFetch() {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No Internet Connection!", Toast.LENGTH_LONG).show()
            return
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 101)
        } else {
            progressBar.visibility = View.VISIBLE
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    fetchWeatherData(location.latitude, location.longitude)
                } else {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun fetchWeatherData(lat: Double, lon: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitInstance.api.getWeather(lat, lon, apiKey, unitMode)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    if (response.isSuccessful && response.body() != null) {
                        val data = response.body()!!
                        val symbol = if (unitMode == "metric") "°C" else "°F"

                        currentCity = data.name
                        currentTemp = "${data.main.temp}$symbol"
                        currentCondition = data.weather.firstOrNull()?.description ?: ""

                        tvLocation.text = currentCity
                        tvTemp.text = currentTemp
                        tvCondition.text = currentCondition.replaceFirstChar { it.uppercase() }
                        tvHumidity.text = "Humidity: ${data.main.humidity}%"
                        tvWind.text = "Wind: ${data.wind.speed} ${if (unitMode == "metric") "m/s" else "mph"}"
                    } else {
                        Toast.makeText(this@MainActivity, "API Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "Request Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateUnitButtonText() {
        btnToggleUnit.text = if (unitMode == "metric") "Unit: °C" else "Unit: °F"
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkLocationPermissionAndFetch()
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }
}