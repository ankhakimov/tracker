package com.example.gpstracker.fragments

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceManager
import com.example.gpstracker.MainApp
import com.example.gpstracker.MainViewModel
import com.example.gpstracker.R
import com.example.gpstracker.databinding.ViewTrackBinding
import org.osmdroid.config.Configuration
import org.osmdroid.library.BuildConfig
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class ViewTrackFragment : Fragment() {
    private var startPoint: GeoPoint? = null
    private lateinit var binding: ViewTrackBinding
    private val model: MainViewModel by activityViewModels{
        MainViewModel.ViewModelFactory((requireContext().applicationContext as MainApp).database)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        settingsOsm()
        binding = ViewTrackBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getTrack()
        binding.fCentr.setOnClickListener{
            if(startPoint != null) binding.mapView.controller.animateTo(startPoint)
        }
    }

    private fun getTrack() = with(binding){
        model.currentTrack.observe(viewLifecycleOwner){
            val speed = "Average speed: ${it.speed} km/h"
            val distance = "Distance ${it.distance} km"
            val date = "Date: ${it.date}"
            tvData.text = date
            tvTime.text = it.time
            tvDistance.text = distance
            tvAverageVel.text = speed
            val polyline = getPolyline(it.geoPoints)
            mapView.overlays.add(polyline)
            setMarkers(polyline.actualPoints)
            goToStartPosition(polyline.actualPoints[0])
            startPoint = polyline.actualPoints[0]
        }
    }

    private fun goToStartPosition(startPos: GeoPoint){
        binding.mapView.controller.zoomTo(18.0)
        binding.mapView.controller.animateTo(startPos)
    }

    private fun setMarkers(list: List<GeoPoint>) = with(binding){
        val startMarker = Marker(mapView)
        val finishMarker = Marker(mapView)
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        finishMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        startMarker.icon = getDrawable(requireContext(), R.drawable.ic_start_pos)
        finishMarker.icon = getDrawable(requireContext(), R.drawable.ic_finsh_pos)
        startMarker.position = list[0]
        finishMarker.position = list[list.size - 1]
        mapView.overlays.add(startMarker)
        mapView.overlays.add(finishMarker)
    }

    private fun getPolyline(geoPoints: String): Polyline{
        val polyline = Polyline()
        polyline.outlinePaint.color = Color.parseColor(
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString("color_key", "#FF0E79E3")
        )
        val list = geoPoints.split("/")
        list.forEach{
            if (it.isEmpty()) return@forEach
            val points = it.split(",")
            polyline.addPoint(GeoPoint(points[0].toDouble(), points[1].toDouble()))
        }
        return polyline
    }

    private fun settingsOsm(){
        Configuration.getInstance().load(activity as AppCompatActivity,
            activity?.getSharedPreferences("osm_pref", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
    }

    companion object {
        @JvmStatic
        fun newInstance() = ViewTrackFragment()
    }
}