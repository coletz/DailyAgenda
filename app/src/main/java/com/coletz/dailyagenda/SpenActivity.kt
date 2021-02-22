package com.coletz.dailyagenda

import android.widget.Toast
import com.samsung.android.sdk.penremote.SpenRemote
import com.samsung.android.sdk.penremote.SpenRemote.FEATURE_TYPE_BUTTON
import com.samsung.android.sdk.penremote.SpenUnitManager
import kotlinx.coroutines.*

abstract class SpenActivity : CoroutineActivity() {
    protected var spenUnitManager: SpenUnitManager? = null
    protected val spenRemote: SpenRemote = SpenRemote.getInstance()

    override fun onStart() {
        super.onStart()
        if (spenRemote.isFeatureEnabled(FEATURE_TYPE_BUTTON)) {
            if (!spenRemote.isConnected) {
                spenRemote.connect(this,
                    object: SpenRemote.ConnectionResultCallback {
                        override fun onSuccess(p0: SpenUnitManager?) {
                            spenUnitManager = p0
                            p0 ?: return
                            onSpenConnected()
                        }

                        override fun onFailure(p0: Int) {
                            Toast.makeText(this@SpenActivity, "Spen not working", Toast.LENGTH_LONG).show()
                        }
                    });
            }
        }
    }

    override fun onStop() {
        spenRemote.disconnect(this)
        super.onStop()
    }

    open fun onSpenConnected() {}
}