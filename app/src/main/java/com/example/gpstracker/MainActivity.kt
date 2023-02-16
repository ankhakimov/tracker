package com.example.gpstracker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.gpstracker.databinding.ActivityMainBinding
import com.example.gpstracker.fragments.MainFragment
import com.example.gpstracker.fragments.SettingsFragment
import com.example.gpstracker.fragments.TracksFragment
import com.example.gpstracker.utils.openFragment

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        onBottomNavClicks()
        openFragment(MainFragment.newInstance())
    }

    private fun onBottomNavClicks(){
        binding.bNavMenu.setOnItemSelectedListener {
            when(it.itemId){
                R.id.idHome -> openFragment(MainFragment.newInstance())
                R.id.idTracks -> openFragment(TracksFragment.newInstance())
                R.id.idSettings -> openFragment(SettingsFragment())
            }
            true
        }
    }
}