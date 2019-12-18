package com.transistorsoft.flutter.backgroundfetch;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/** BackgroundFetchPlugin */
public class BackgroundFetchPlugin implements FlutterPlugin, ActivityAware {
    public static final String TAG                          = "TSBackgroundFetch";

    // @deprecated Not used by v2.
    public static void registerWith(Registrar registrar) {
        BackgroundFetchModule module = BackgroundFetchModule.getInstance();
        module.onAttachedToEngine(registrar.context(), registrar.messenger());
        if (registrar.activity() != null) {
            module.setActivity(registrar.activity());
        }
    }

    // @deprecated Called by Application#onCreate
    public static void setPluginRegistrant(PluginRegistry.PluginRegistrantCallback callback) {
        HeadlessTask.setPluginRegistrant(callback);
    }

    public BackgroundFetchPlugin() { }

    @Override
    public void onAttachedToEngine(FlutterPlugin.FlutterPluginBinding binding) {
        BackgroundFetchModule.getInstance().onAttachedToEngine(binding.getApplicationContext(), binding.getBinaryMessenger());
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        BackgroundFetchModule.getInstance().onDetachedFromEngine();
    }

    @Override
    public void onAttachedToActivity(ActivityPluginBinding activityPluginBinding) {
        BackgroundFetchModule.getInstance().setActivity(activityPluginBinding.getActivity());
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        // TODO: the Activity your plugin was attached to was
        // destroyed to change configuration.
        // This call will be followed by onReattachedToActivityForConfigChanges().
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding activityPluginBinding) {
        // TODO: your plugin is now attached to a new Activity
        // after a configuration change.
    }

    @Override
    public void onDetachedFromActivity() {
        BackgroundFetchModule.getInstance().setActivity(null);
    }
}
