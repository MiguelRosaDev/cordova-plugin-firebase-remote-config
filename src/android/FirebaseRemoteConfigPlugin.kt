package com.outsystems.experts.firebase.remote.config.plugin

import com.google.firebase.remoteconfig.*
import org.apache.cordova.*
import org.json.JSONArray
import org.json.JSONObject


private const val ACTION_GET_ALL_KEYS = "getAllKeys"
private const val ACTION_SET_CONFIG_SETTINGS = "setConfigSettings"

class FirebaseRemoteConfigPlugin : CordovaPlugin(), ConfigUpdateListener {

    private lateinit var firebaseRemoteConfig: FirebaseRemoteConfig
    private var callbackContext: CallbackContext? = null

    override fun initialize(cordova: CordovaInterface?, webView: CordovaWebView?) {
        super.initialize(cordova, webView)

        firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        firebaseRemoteConfig.addOnConfigUpdateListener(this)
    }

    override fun execute(
        action: String,
        args: JSONArray,
        callbackContext: CallbackContext
    ): Boolean {
        this.callbackContext = callbackContext
        return when (action) {
            ACTION_GET_ALL_KEYS -> {
                fetchAndActivateToGetAllKeys()
                true
            }
            ACTION_SET_CONFIG_SETTINGS -> {
                setConfigSettings(args, callbackContext)
                true
            }
            else -> false
        }
    }

    private fun fetchAndActivateToGetAllKeys() {
        cordova.threadPool.execute {
            try {
                // Fetch and activate the latest configurations
                firebaseRemoteConfig.fetchAndActivate()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            getAllKeys()
                        } else {
                            this.callbackContext?.error(task.exception?.message.toString())
                        }
                    }
            } catch (ex: Exception) {
                this.callbackContext?.error(ex.message)
            }
        }
    }

    private fun getAllKeys() {
        cordova.threadPool.execute {
            try {
                val keys = firebaseRemoteConfig.all
                val jsonArray = JSONArray()
                for (entry in keys.entries) {
                    val key = entry.key
                    val value = entry.value.asString()
                    val jsonObject = JSONObject()
                    jsonObject.put("key", key)
                    jsonObject.put("value", value)
                    jsonArray.put(jsonObject)
                }
                val pluginResult = PluginResult(PluginResult.Status.OK, jsonArray)
                pluginResult.keepCallback = true
                this.callbackContext?.sendPluginResult(pluginResult)
            } catch (e: Exception) {
                this.callbackContext?.error(e.message.toString())
            }
        }
    }

    private fun setConfigSettings(args: JSONArray, callbackContext: CallbackContext) {
        cordova.threadPool.execute {
            try {
                val settingsJson = args.getJSONObject(0)
                val minimumFetchInterval = settingsJson.optLong("minimumFetchInterval", 3600L)
                val configSettings = FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(minimumFetchInterval)
                    .build()
                firebaseRemoteConfig.setConfigSettingsAsync(configSettings)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            callbackContext.success("Config settings updated")
                        } else {
                            callbackContext.error("Failed to update config settings " + task.exception?.message)
                        }
                    }
            } catch (e: Exception) {
                callbackContext.error("Invalid config settings: ${e.message}")
            }
        }
    }

    override fun onUpdate(configUpdate: ConfigUpdate) {
        if (configUpdate.updatedKeys.isNotEmpty()) {
            fetchAndActivateToGetAllKeys()
        }
    }

    override fun onError(error: FirebaseRemoteConfigException) {
        callbackContext?.error(error.message.toString())
    }
}