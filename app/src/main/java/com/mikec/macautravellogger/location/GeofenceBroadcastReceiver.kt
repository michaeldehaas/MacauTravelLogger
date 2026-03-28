package com.mikec.macautravellogger.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.mikec.macautravellogger.data.local.DetectionMethod
import com.mikec.macautravellogger.data.repository.TripRepository
import com.mikec.macautravellogger.util.DateUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: TripRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return

        val transition = event.geofenceTransition
        val pendingResult = goAsync()

        scope.launch {
            try {
                when (transition) {
                    Geofence.GEOFENCE_TRANSITION_ENTER -> {
                        if (repository.getActive() == null) {
                            repository.checkIn(
                                date = DateUtils.getCurrentDate(),
                                time = DateUtils.getCurrentTime(),
                                method = DetectionMethod.AUTO
                            )
                        }
                    }
                    Geofence.GEOFENCE_TRANSITION_EXIT -> {
                        val active = repository.getActive()
                        if (active != null) {
                            repository.checkOut(active, DateUtils.getCurrentTime())
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
