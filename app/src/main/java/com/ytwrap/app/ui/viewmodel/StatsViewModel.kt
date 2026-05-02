package com.ytwrap.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ytwrap.app.data.dao.HourlyListening
import com.ytwrap.app.data.dao.ListeningEventDao
import com.ytwrap.app.data.dao.TrackPlayCount
import com.ytwrap.app.data.entity.ListeningEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val listeningEventDao: ListeningEventDao
) : ViewModel() {

    private val todayStart: Long
    private val todayEnd: Long

    init {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        todayStart = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        todayEnd = cal.timeInMillis
    }

    val todayListeningMs: StateFlow<Long?> = listeningEventDao
        .observeTotalListeningMs(todayStart, todayEnd)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val hourlyListening: StateFlow<List<HourlyListening>> = listeningEventDao
        .observeHourlyListening(todayStart, todayEnd)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mostPlayedTrack: StateFlow<TrackPlayCount?> = listeningEventDao
        .observeMostPlayedTrack()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val topTracks: StateFlow<List<TrackPlayCount>> = listeningEventDao
        .observeTopTracks(5)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Home screen data
    private val sevenDaysAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
    private val sixMonthsAgo = System.currentTimeMillis() - 180L * 24 * 60 * 60 * 1000

    val heavyRotation: StateFlow<List<TrackPlayCount>> = listeningEventDao
        .observeHeavyRotation(sevenDaysAgo, 8)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val forgottenGems: StateFlow<List<TrackPlayCount>> = listeningEventDao
        .observeForgottenGems(sixMonthsAgo, 15)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyAdded: StateFlow<List<TrackPlayCount>> = listeningEventDao
        .observeRecentlyAdded(15)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun recordPlay(videoId: String, title: String, artist: String, thumbnailUrl: String, durationMs: Long) {
        viewModelScope.launch {
            listeningEventDao.insert(
                ListeningEvent(
                    videoId = videoId,
                    title = title,
                    artist = artist,
                    thumbnailUrl = thumbnailUrl,
                    durationMs = durationMs
                )
            )
        }
    }
}
