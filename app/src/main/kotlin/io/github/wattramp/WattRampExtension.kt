package io.github.wattramp

import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.KarooExtension
import io.github.wattramp.datatypes.*
import io.github.wattramp.engine.TestEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class WattRampExtension : KarooExtension("wattramp", "1.0.0") {

    companion object {
        @Volatile
        var instance: WattRampExtension? = null
            private set
    }

    lateinit var karooSystem: KarooSystemService
        private set

    val testEngine: TestEngine by lazy {
        TestEngine(this)
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        instance = this
        karooSystem = KarooSystemService(this)
        karooSystem.connect { }
    }

    override fun onDestroy() {
        instance = null
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
