package com.example.gpstracker.fragments

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.example.gpstracker.MainApp
import com.example.gpstracker.MainViewModel
import com.example.gpstracker.R
import com.example.gpstracker.databinding.FragmentMainBinding
import com.example.gpstracker.db.MainDB
import com.example.gpstracker.db.TrackItem
import com.example.gpstracker.location.LocationModel
import com.example.gpstracker.location.LocationService
import com.example.gpstracker.utils.DialogManager
import com.example.gpstracker.utils.TimeUtils
import com.example.gpstracker.utils.checkPermission
import org.osmdroid.config.Configuration
import org.osmdroid.library.BuildConfig
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.*

class MainFragment : Fragment() {
    private var locationModel: LocationModel? = null
    private var pl: Polyline? = null
    private var isServiceRunning = false
    private var timer: Timer? = null
    private var startTime = 0L
    private var firstStart = true
    private lateinit var myLocOverlay: MyLocationNewOverlay
    private lateinit var binding: FragmentMainBinding
    private lateinit var pLauncher: ActivityResultLauncher<Array<String>>
    private val model: MainViewModel by activityViewModels{
        MainViewModel.ViewModelFactory((requireContext().applicationContext as MainApp).database)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        settingsOsm()
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        registerPermission()
        setOnClicks()
        checkServiceState()
        updateTime()
        registerLocReceiver()
        locationUpdates()
    }

    private fun setOnClicks() = with(binding){
        val listener = onClicks()
        fStartStop.setOnClickListener(listener)
        fCentr.setOnClickListener(listener)
    }

    private fun onClicks(): View.OnClickListener{
        return View.OnClickListener {
            when(it.id){
                R.id.fStartStop -> startStopService()
                R.id.fCentr -> centerLocation()
            }
        }
    }

    private fun centerLocation(){
        binding.mapView.controller.animateTo(myLocOverlay.myLocation)
        myLocOverlay.enableFollowLocation()
    }

    private fun locationUpdates() = with(binding){
        model.locationUpdates.observe(viewLifecycleOwner){
            val distance = "Distance: ${String.format("%.1f", it.distance)} m"
            val velocity = "Velocity: ${String.format("%.1f", 3.6f * it.velocity)} km/h"
            val aVelocity = "Average Velocity: ${getAverageSpeed(it.distance)} km/h"
            tvDistance.text = distance
            tvVelocity.text = velocity
            tvAverageVel.text = aVelocity
            locationModel = it
            updatePolyline(it.geoPointList)
        }
    }

    private fun updateTime(){
        model.timeData.observe(viewLifecycleOwner){
            binding.tvTime.text = it
        }
    }

    private fun startTimer(){
        timer?.cancel()
        timer = Timer()
        startTime = LocationService.startTime
        timer?.schedule(object : TimerTask(){
            override fun run() {
                activity?.runOnUiThread{
                    model.timeData.value = getCurrentTime()
                }
            }
        }, 1000, 1000)
    }

    private fun getAverageSpeed(distance: Float): String{
        return String.format("%.1f", 3.6f * (distance / ((System.currentTimeMillis() - startTime) / 1000.0f)))
    }

    private fun getCurrentTime(): String{
        return "Time: ${TimeUtils.getTime(System.currentTimeMillis() - startTime)}"
    }

    private fun geoPointToString(list: List<GeoPoint>): String{
        val sb = StringBuilder()
        list.forEach{
            sb.append("${it.latitude}, ${it.longitude}/")
        }
        Log.d("MyLog", "Points: $sb")
        return sb.toString()
    }

    private fun startStopService(){
        if (!isServiceRunning){
            startLocService()
        }else {
            activity?.stopService(Intent(activity, LocationService::class.java))
            binding.fStartStop.setImageResource(R.drawable.ic_play)
            timer?.cancel()
            val track = getTrackItem()
            DialogManager.showSaveDialog(requireContext(), track, object : DialogManager.Listener{
                override fun onClick() {
                    Toast.makeText(activity, "Track saved", Toast.LENGTH_SHORT).show()
                    model.insertTrack(track)
                }

            })
        }
        isServiceRunning = !isServiceRunning
    }

    private fun getTrackItem(): TrackItem {
        return TrackItem(
            null,
            getCurrentTime(),
            TimeUtils.getDate(),
            String.format("%.1f", locationModel?.distance?.div(1000) ?: 0),
            getAverageSpeed(locationModel?.distance ?: 0.0f),
            geoPointToString(locationModel?.geoPointList ?: listOf())
        )
    }

    private fun checkServiceState(){
        isServiceRunning = LocationService.isRunning
        if (isServiceRunning){
            binding.fStartStop.setImageResource(R.drawable.ic_stop)
            startTimer()
        }
    }

    private fun startLocService(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity?.startForegroundService(Intent(activity, LocationService::class.java))
        } else{
            activity?.startService(Intent(activity, LocationService::class.java))
        }
        binding.fStartStop.setImageResource(R.drawable.ic_stop)
        LocationService.startTime = System.currentTimeMillis()
        startTimer()
    }

    override fun onResume() {
        super.onResume()
        checkLocPermission()
        firstStart = true
    }

    private fun settingsOsm(){
        Configuration.getInstance().load(activity as AppCompatActivity,
            activity?.getSharedPreferences("osm_pref", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
    }

    private fun initOsm() = with(binding){
        pl = Polyline()
        pl?.outlinePaint?.color = Color.parseColor(
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString("color_key", "#FF0E79E3")
        )
        mapView.controller.setZoom(20.0)
        val mLocProvider = GpsMyLocationProvider(activity)
        myLocOverlay = MyLocationNewOverlay(mLocProvider, mapView)
        myLocOverlay.enableMyLocation()
        myLocOverlay.enableFollowLocation()
        myLocOverlay.runOnFirstFix{
            mapView.overlays.clear()
            mapView.overlays.add(pl)
            mapView.overlays.add(myLocOverlay)
        }
    }

    private fun registerPermission(){
        pLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            if (it[Manifest.permission.ACCESS_FINE_LOCATION] == true){
                initOsm()
            }else {
                Toast.makeText(activity, "Вы не дали разрешение на определение местопопложения!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkLocPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            checkPermissionAfter10()
        }else{
            checkPermissionBefore10()
        }
    }

    private fun checkPermissionAfter10() {
        if (checkPermission(Manifest.permission.ACCESS_FINE_LOCATION) &&
            checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION) &&
            checkPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        ) {
            initOsm()
            checkLocationEnabled()
        } else {
            pLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION))
        }
    }

    private fun checkPermissionBefore10() {
        if (checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            initOsm()
            checkLocationEnabled()
        } else {
            pLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
        }
    }

    private fun checkLocationEnabled(){
        val lManager = activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isEnabled = lManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (!isEnabled){
            DialogManager.showLocEnableDialog(activity as AppCompatActivity,
            object : DialogManager.Listener{
                override fun onClick() {
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
            })
        }else{
            Toast.makeText(activity, "Location enabled", Toast.LENGTH_SHORT).show()
        }
    }

    private val receiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == LocationService.LOC_MODEL_INTENT){
                val localModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                    intent.getSerializableExtra(LocationService.LOC_MODEL_INTENT, LocationModel::class.java)
                } else{
                    intent.getSerializableExtra(LocationService.LOC_MODEL_INTENT) as LocationModel
                }
                model.locationUpdates.value = localModel
            }
        }
    }

    private fun registerLocReceiver(){
        val locFilter = IntentFilter(LocationService.LOC_MODEL_INTENT)
        LocalBroadcastManager.getInstance(activity as AppCompatActivity)
            .registerReceiver(receiver, locFilter)
    }

    private fun addPoint(list: List<GeoPoint>){
        if(list.isNotEmpty()) pl?.addPoint(list[list.size - 1])
    }

    private fun fillPolyline(list: List<GeoPoint>){
        list.forEach{
            pl?.addPoint(it)
        }
    }

    private fun updatePolyline(list: List<GeoPoint>){
        if(list.size > 1 && firstStart) {
            fillPolyline(list)
            firstStart = false
        }else{
            addPoint(list)
        }
    }

    override fun onDetach() {
        super.onDetach()
        LocalBroadcastManager.getInstance(activity as AppCompatActivity)
            .unregisterReceiver(receiver)
    }

    companion object {
        @JvmStatic
        fun newInstance() = MainFragment()
    }
}