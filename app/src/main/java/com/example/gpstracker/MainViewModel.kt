package com.example.gpstracker

import androidx.lifecycle.*
import com.example.gpstracker.db.MainDB
import com.example.gpstracker.db.TrackItem
import com.example.gpstracker.location.LocationModel
import kotlinx.coroutines.launch

@Suppress("UNCHECKED_CAST")
class MainViewModel(db: MainDB): ViewModel() {
    val dao = db.getDao()
    val locationUpdates = MutableLiveData<LocationModel>()
    val timeData = MutableLiveData<String>()
    val currentTrack = MutableLiveData<TrackItem>()
    val tracks = dao.getAllTracks().asLiveData()

    fun insertTrack(trackItem: TrackItem) = viewModelScope.launch {
        dao.insertTrack(trackItem)
    }

    fun deleteTrack(trackItem: TrackItem) = viewModelScope.launch {
        dao.deleteTrack(trackItem)
    }

    class ViewModelFactory(private val db: MainDB): ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(db) as T
            }
            throw java.lang.IllegalArgumentException("Unknown ViewModel class")
        }
    }
}