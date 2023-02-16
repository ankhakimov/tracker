package com.example.gpstracker.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gpstracker.MainApp
import com.example.gpstracker.MainViewModel
import com.example.gpstracker.databinding.TracksBinding
import com.example.gpstracker.db.TrackAdapter
import com.example.gpstracker.db.TrackItem
import com.example.gpstracker.utils.openFragment

class TracksFragment : Fragment(), TrackAdapter.Listener {
    private lateinit var binding: TracksBinding
    private lateinit var trackAdapter: TrackAdapter
    private val model: MainViewModel by activityViewModels{
        MainViewModel.ViewModelFactory((requireContext().applicationContext as MainApp).database)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = TracksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRcView()
        getTracks()
    }

    private fun initRcView() = with(binding){
        trackAdapter = TrackAdapter(this@TracksFragment)
        rcView.layoutManager = LinearLayoutManager(requireContext())
        rcView.adapter = trackAdapter
    }

    private fun getTracks(){
        model.tracks.observe(viewLifecycleOwner){
            trackAdapter.submitList(it)
            binding.tvEmpty.visibility = if(it.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = TracksFragment()
    }

    override fun onClick(trackItem: TrackItem, type: TrackAdapter.ClickType) {
        when(type){
            TrackAdapter.ClickType.DELETE ->  model.deleteTrack(trackItem)
            TrackAdapter.ClickType.OPEN ->  {
                model.currentTrack.value = trackItem
                openFragment(ViewTrackFragment.newInstance())
            }
        }

    }
}