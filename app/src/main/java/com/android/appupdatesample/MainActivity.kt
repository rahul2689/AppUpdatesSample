package com.android.appupdatesample

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

private const val REQUEST_CODE_APP_UPDATE = 102

class MainActivity : AppCompatActivity(), InstallStateUpdatedListener {
    private lateinit var appUpdateManager: AppUpdateManager
    private var updateFlag: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkForInAppUpdate()
    }

    override fun onResume() {
        super.onResume()
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                showSnackbar(getString(R.string.app_update_msg), getString(R.string.restart), onClickListener)
            } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                requestUpdate(appUpdateInfo, AppUpdateType.IMMEDIATE)
            }
        }
    }

    /**
     * 1.Here value of updateFlag can be fetched from Remote Config by defining a JSON in remote config and fetch value
     * of FLEXIBLE or IMMEDIATE update flag from there.
     * 2.We are fetching the value of updateFlag from RemoteConfig because appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) or
     * appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) always returns true.
     */
    private fun checkForInAppUpdate() {
        //create an instance of app update manager using AppUpdateManagerFactory
        appUpdateManager = AppUpdateManagerFactory.create(this)

        //create object of appUpdateInfo which also return an Intent Object that will be used to request an Update.
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            //
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                if (updateFlag == 0 && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                    requestUpdate(appUpdateInfo)
                    appUpdateManager.registerListener(this@MainActivity)
                } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    requestUpdate(appUpdateInfo, AppUpdateType.IMMEDIATE)
                }
            }
        }
    }

    /**
     * Request for an update available and wait for the result code status in onActivityResult()
     * Here Update Type can be : (AppUpdateType.FLEXIBLE or AppUpdateType.IMMEDIATE)
     **/
    private fun requestUpdate(appUpdateInfo: AppUpdateInfo?, updateType: Int = AppUpdateType.FLEXIBLE) {
        appUpdateManager.startUpdateFlowForResult(
            appUpdateInfo,
            updateType,
            this@MainActivity,
            REQUEST_CODE_APP_UPDATE
        )
    }

    /**
     * If Result is not ok then request for update again.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_APP_UPDATE && resultCode != RESULT_OK) {
            checkForInAppUpdate() // if in any case update request is cancelled and failed
        }
    }

    override fun onStateUpdate(state: InstallState) {
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            showSnackbar(getString(R.string.app_update_msg), getString(R.string.restart), onClickListener)
            appUpdateManager.unregisterListener(this)
        }
    }

    private fun showSnackbar(message: String, actionText: String, clickListener: View.OnClickListener) {
        findViewById<View>(R.id.parentLayout)?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_INDEFINITE).setAction(actionText, clickListener).show()
        }
    }

    private val onClickListener = View.OnClickListener { view ->
        appUpdateManager.completeUpdate()
    }

}
