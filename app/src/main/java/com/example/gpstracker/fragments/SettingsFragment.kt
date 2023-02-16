package com.example.gpstracker.fragments

import android.graphics.Color
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener
import androidx.preference.PreferenceFragmentCompat
import com.example.gpstracker.R


class SettingsFragment: PreferenceFragmentCompat() {
    private lateinit var timePref: Preference
    private lateinit var colorPref: Preference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.main_preference, rootKey)
        init()

    }

    private fun init(){
        timePref = findPreference("time_update_key")!!
        colorPref = findPreference("color_key")!!
        val changeListener = onChangeListener()
        timePref.onPreferenceChangeListener = changeListener
        colorPref.onPreferenceChangeListener = changeListener
        initPref()
    }

    private fun onChangeListener(): OnPreferenceChangeListener{
        return OnPreferenceChangeListener{
            pref, value ->
                when(pref.key){
                    "time_update_key" -> onTimeChange(value.toString())
                    "color_key" -> onColorChange(value.toString())
                }
            true
        }
    }
    private fun onColorChange(value: String){
        colorPref.icon?.setTint(Color.parseColor(value))
    }

    private fun onTimeChange(value: String){
        val nameArray = resources.getStringArray(R.array.log_time_update_name)
        val valueArray = resources.getStringArray(R.array.log_time_update_value)
        val title = timePref.title.toString().substringBefore(":")
        timePref.title = "$title: ${nameArray[valueArray.indexOf(value)]}"
    }

    private fun initPref(){
        val pref = timePref.preferenceManager.sharedPreferences
        val nameArray = resources.getStringArray(R.array.log_time_update_name)
        val valueArray = resources.getStringArray(R.array.log_time_update_value)
        val title = timePref.title
        timePref.title = "$title: ${nameArray[valueArray
            .indexOf(pref?.getString("time_update_key", "3000"))]}"
        val trackColor = pref?.getString("color_key", "#FF0E79E3")
        colorPref.icon?.setTint(Color.parseColor(trackColor))
    }
}