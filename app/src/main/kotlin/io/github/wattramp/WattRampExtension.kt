package io.github.wattramp

import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.KarooExtension
import io.github.wattramp.datatypes.*
import io.github.wattramp.engine.TestEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WattRampExtension : KarooExtension("wattramp", BuildConfig.VERSION_NAME) {

    companion object {
        @Volatile
        var instance: WattRampExtension? = null
            private set
    }

    lateinit var karooSystem: KarooSystemService
        private set

    // Connection state flow for observing connection status
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    val testEngine: TestEngine by lazy {
        TestEngine(this)
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        instance = this
        karooSystem = KarooSystemService(this)

        // Connect to Karoo system and track connection state
        karooSystem.connect { connected ->
            _isConnected.value = connected
            if (connected) {
                android.util.Log.i("WattRampExtension", "KarooSystemService connected successfully")
            } else {
                android.util.Log.w("WattRampExtension", "KarooSystemService connection failed or disconnected")
            }
        }
    }

    override fun onDestroy() {
        instance = null
        _isConnected.value = false
        testEngine.destroy()
        serviceScope.cancel()
        karooSystem.disconnect()
        super.onDestroy()
    }

    override val types by lazy {
        listOf(
            // Graphical widget (2x1)
            CurrentIntervalDataType(this),
            // Numeric fields (1x1)
            TargetPowerDataType(this),
            TestProgressDataType(this),
            PowerZoneDataType(this),
            DeviationDataType(this),
            ElapsedTimeDataType(this),
            AveragePowerDataType(this),
            MaxPowerDataType(this),
            FtpPredictionDataType(this)
        )
    }
}
