package ir.tapsell.tapsell_plus

import android.app.Activity
import android.util.Log
import android.view.ViewGroup
import androidx.annotation.NonNull
import com.google.gson.Gson
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import ir.tapsell.plus.*

class TapsellPlusPlugin: FlutterPlugin, MethodCallHandler, ActivityAware {

    private lateinit var channel : MethodChannel
    private lateinit var activity: Activity

    private var showAdOpenedResult: Result? = null
    private var showAdClosedResult: Result? = null
    private var showAdRewardedResult: Result? = null
    private val TAG = "TapsellPlusFlutter"

    /** backward compatibility with embedding v1  */
    fun registerWith(registrar: PluginRegistry.Registrar) {
        val plugin = TapsellPlusPlugin()
        plugin.activity = registrar.activity()
        plugin.setupMethodChannel(registrar.messenger())
    }

    override fun onAttachedToEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        setupMethodChannel(binding.binaryMessenger)
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity;
    }

    override fun onDetachedFromActivity() {
        // Not yet implemented
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        // Not yet implemented
    }
    
    override fun onDetachedFromActivityForConfigChanges() {
        // Not yet implemented
    }

    private fun setupMethodChannel(messenger: BinaryMessenger) {
        channel = MethodChannel(messenger, "tapsell_plus")
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        Log.d(TAG, call.method)
        when (call.method) {
            "initialize" -> {
                val appId: String = call.argument("appId")!!
                TapsellPlus.initialize(activity, appId)
            }
            "showAdClosedListener" -> {
                showAdClosedResult = result
            }
            "showAdRewardedListener" -> {
                showAdRewardedResult = result
            }
            "setDebugMode" -> {
                val logLevel: Int = call.argument("logLevel") ?: Log.DEBUG
                TapsellPlus.setDebugMode(logLevel)
            }
            "addFacebookTestDevice" -> {
                val hash: String = call.argument("hash")!!
                TapsellPlus.addFacebookTestDevice(hash)
            }
            "requestRewardedVideo" -> {
                val zoneId: String = call.argument("zoneId")!!
                TapsellPlus.requestRewardedVideo(activity, zoneId, object : AdRequestCallback() {
                    override fun response() {
                        Log.d(TAG, "${call.method}Response")
                        result.success(zoneId)
                    }

                    override fun error(message: String?) {
                        Log.e(TAG, "${call.method}Error")
                        result.error(zoneId, message, null)
                    }
                })
            }
            "requestInterstitial" -> {
                val zoneId: String = call.argument("zoneId")!!
                TapsellPlus.requestInterstitial(activity, zoneId, object : AdRequestCallback() {
                    override fun response() {
                        Log.d(TAG, "${call.method}Response")
                        result.success(zoneId)
                    }

                    override fun error(message: String?) {
                        Log.e(TAG, "${call.method}Error")
                        result.error(zoneId, message, null)
                    }
                })
            }
            "requestNativeBanner" -> {
                val zoneId: String = call.argument("zoneId")!!
                TapsellPlus.requestNativeBanner(activity, zoneId, object : AdRequestCallback() {
                    override fun response() {
                        Log.d(TAG, "${call.method}Response")
                        requestNativeBannerResponse(zoneId, result)
                    }

                    override fun error(message: String?) {
                        Log.e(TAG, "${call.method}Error: $message")
                        result.error(zoneId, message, null)
                    }
                })
            }
            "showAd" -> {
                val zoneId: String = call.argument("zoneId")!!
                TapsellPlus.showAd(activity, null, false, zoneId, object : AdShowListener() {
                    override fun onOpened() {
                        Log.d(TAG, "${call.method}AdOpened")
                        try {
                            val response = TapsellPlusResponseModel(zoneId, "showAdOpened")
                            result.success(Gson().toJson(response))
                        } catch (e: Exception) {
                            Log.e(TAG, e.message)
                            result.error(zoneId, e.message, null)
                        }
                    }

                    override fun onClosed() {
                        Log.d(TAG, "${call.method}AdClosed")
                        try {
                            val response = TapsellPlusResponseModel(zoneId, "showAdClosed")
                            showAdClosedResult?.success(Gson().toJson(response))
                        } catch (e: Exception) {
                            Log.e(TAG, e.message)
                            result.error(zoneId, e.message, null)
                        }
                    }

                    override fun onRewarded() {
                        Log.d(TAG, "${call.method}AdRewarded")
                        try {
                            val response = TapsellPlusResponseModel(zoneId, "showAdRewarded")
                            showAdRewardedResult?.success(Gson().toJson(response))
                        } catch (e: Exception) {
                            Log.e(TAG, e.message)
                            result.error(zoneId, e.message, null)
                        }
                    }

                    override fun onError(message: String?) {
                        Log.e(TAG, "${call.method}AdError")
                        result.error(zoneId, message, null)
                    }
                })
            }
            "showBannerAd" -> {
                val zoneId: String = call.argument("zoneId")!!
                val bannerType: TapsellPlusBannerType = call.argument("bannerType")!!
                val adContainer: ViewGroup = call.argument("adContainer")!!

                TapsellPlus.showBannerAd(
                        activity,
                        adContainer,
                        zoneId,
                        bannerType, object : AdRequestCallback() {
                    override fun response() {
                        Log.d(TAG, "${call.method}Response")
                        requestNativeBannerResponse(zoneId, result)
                    }

                    override fun error(message: String?) {
                        Log.e(TAG, "${call.method}Error")
                        result.error(zoneId, message, null)
                    }
                })
            }
            "nativeBannerAdClicked" -> {
                val zoneId: String = call.argument("zoneId")!!
                val adId: String = call.argument("adId")!!

                TapsellPlus.nativeBannerAdClicked(activity, zoneId, adId)
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    private fun requestNativeBannerResponse(zoneId: String, result: Result) {
        try {
            val tapsellPlusNativeBanner: TapsellPlusNativeBanner =
                    TapsellPlus.getNativeBannerObject(activity, zoneId)

            if (tapsellPlusNativeBanner.error) {
                result.error(zoneId, tapsellPlusNativeBanner.errorMessage, null)
                return
            }

            val json = Gson().toJson(tapsellPlusNativeBanner)
            Log.e(TAG, json)
            result.success(json)
        } catch (e: Exception) {
            Log.e(TAG, e.message)
        }
    }
}
