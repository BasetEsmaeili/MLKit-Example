package com.baset.googlelens.util

import android.content.Context
import android.util.Log
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.tflite.java.TfLite

class ModuleInstaller(private val context: Context) {
    companion object {
        private const val TAG = "ModuleInstaller"
    }

    private val moduleInstallClient by lazy(LazyThreadSafetyMode.NONE) {
        ModuleInstall.getClient(
            context
        )
    }
    private val tfLiteModule by lazy(LazyThreadSafetyMode.NONE) {
        TfLite.getClient(
            context
        )
    }

    fun installTfLiteModule() {
        moduleInstallClient
            .installModules(
                ModuleInstallRequest.newBuilder()
                    .addApi(tfLiteModule)
                    .build()
            ).addOnSuccessListener {
                if (it.areModulesAlreadyInstalled()) {
                    Log.d(TAG, "Modules are already installed when the request is sent.")
                    return@addOnSuccessListener
                }
                Log.d(TAG, "Modules will be installed.")
            }.addOnFailureListener {
                it.printStackTrace()
            }
    }
}