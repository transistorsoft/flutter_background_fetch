package com.transistorsoft.flutter.backgroundfetch;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.transistorsoft.tsbackgroundfetch.BackgroundFetch;
import com.transistorsoft.tsbackgroundfetch.BackgroundFetchConfig;

import java.util.List;
import java.util.Map;

import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/** BackgroundFetchPlugin */
public class BackgroundFetchPlugin implements MethodCallHandler {
    public static final String TAG                          = "TSBackgroundFetch";
    static final String PLUGIN_ID                           = "com.transistorsoft/flutter_background_fetch";

    private static final String METHOD_CHANNEL_NAME         = PLUGIN_ID + "/methods";
    private static final String EVENT_CHANNEL_NAME          = PLUGIN_ID + "/events";

    private static final String ACTION_REGISTER_HEADLESS_TASK = "registerHeadlessTask";

    private static final String HEADLESS_JOB_SERVICE_CLASS = "com.transistorsoft.flutter.backgroundfetch.HeadlessJobService";

    private FetchStreamHandler mFetchCallback;
    private Context mContext;
    private boolean mForceReload = false;

    /** Plugin registration. */
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), METHOD_CHANNEL_NAME);
        channel.setMethodCallHandler(new BackgroundFetchPlugin(registrar));
    }

    private BackgroundFetchPlugin(Registrar registrar) {
        mFetchCallback = new FetchStreamHandler();
        mContext = registrar.context().getApplicationContext();

        if (registrar.activity() != null) {
            Intent intent = registrar.activity().getIntent();
            String action = intent.getAction();

            if (BackgroundFetch.ACTION_FORCE_RELOAD.equalsIgnoreCase(action)) {
                mForceReload = true;
                registrar.activity().moveTaskToBack(true);
            }
            new EventChannel(registrar.messenger(), EVENT_CHANNEL_NAME).setStreamHandler(mFetchCallback);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onMethodCall(MethodCall call, @NonNull Result result) {
        if (call.method.equals(BackgroundFetch.ACTION_CONFIGURE)) {
            Map<String, Object> params = (Map<String, Object>) call.arguments;
            configure(params, result);
        } else if (call.method.equals(BackgroundFetch.ACTION_START)) {
            start(result);
        } else if (call.method.equals(BackgroundFetch.ACTION_STOP)) {
            stop(result);
        } else if (call.method.equals(BackgroundFetch.ACTION_STATUS)) {
            status(result);
        } else if (call.method.equals(BackgroundFetch.ACTION_FINISH)) {
            finish(result);
        } else if (call.method.equals(ACTION_REGISTER_HEADLESS_TASK)) {
            registerHeadlessTask((List<Object>) call.arguments, result);
        } else {
            result.notImplemented();
        }
    }

    // Called by Application#onCreate
    public static void setPluginRegistrant(PluginRegistry.PluginRegistrantCallback callback) {
        HeadlessTask.setPluginRegistrant(callback);
    }
    // experimental Flutter Headless (NOT READY)
    private void registerHeadlessTask(List<Object> callbacks, Result result) {
        SharedPreferences prefs = mContext.getSharedPreferences(BackgroundFetch.TAG, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(HeadlessTask.KEY_REGISTRATION_CALLBACK_ID);
        editor.remove(HeadlessTask.KEY_CLIENT_CALLBACK_ID);
        editor.apply();

        if (HeadlessTask.register(mContext, callbacks)) {
            result.success(true);
        } else {
            result.error("HEADLESS_TASK_ALREADY_REGISTERED", "Only one HeadlessTask may be registered", null);
        }
    }

    private void configure(Map<String, Object> params, Result result) {
        BackgroundFetchConfig.Builder config = new BackgroundFetchConfig.Builder();
        if (params.containsKey("minimumFetchInterval")) {
            config.setMinimumFetchInterval((int) params.get("minimumFetchInterval"));
        }
        if (params.containsKey("stopOnTerminate")) {
            config.setStopOnTerminate((boolean) params.get("stopOnTerminate"));
        }
        if (params.containsKey("forceReload")) {
            config.setForceReload((boolean) params.get("forceReload"));
        }
        if (params.containsKey("startOnBoot")) {
            config.setStartOnBoot((boolean) params.get("startOnBoot"));
        }
        if (params.containsKey("enableHeadless")) {
            boolean enableHeadless = (boolean) params.get("enableHeadless");
            if (enableHeadless) {
                config.setJobService(HEADLESS_JOB_SERVICE_CLASS);
            }
        }
        if (params.containsKey("requiredNetworkType")) {
            config.setRequiredNetworkType((int) params.get("requiredNetworkType"));
        }
        if (params.containsKey("requiresBatteryNotLow")) {
            config.setRequiresBatteryNotLow((boolean) params.get("requiresBatteryNotLow"));
        }
        if (params.containsKey("requiresCharging")) {
            config.setRequiresCharging((boolean) params.get("requiresCharging"));
        }
        if (params.containsKey("requiresDeviceIdle")) {
            config.setRequiresDeviceIdle((boolean) params.get("requiresDeviceIdle"));
        }
        if (params.containsKey("requiresStorageNotLow")) {
            config.setRequiresStorageNotLow((boolean) params.get("requiresStorageNotLow"));
        }

        BackgroundFetch adapter = BackgroundFetch.getInstance(mContext);
        adapter.configure(config.build(), mFetchCallback);

        if (mForceReload) {
            mFetchCallback.onFetch();
        }
        mForceReload = false;
        result.success(adapter.status());
    }

    private void start(Result result) {
        BackgroundFetch adapter = BackgroundFetch.getInstance(mContext);
        adapter.start();
        result.success(adapter.status());
    }

    private void stop(Result result) {
        BackgroundFetch adapter = BackgroundFetch.getInstance(mContext);
        adapter.stop();
        result.success(adapter.status());
    }

    private void status(Result result) {
        BackgroundFetch adapter = BackgroundFetch.getInstance(mContext);
        result.success(adapter.status());
    }

    private void finish(Result result) {
        BackgroundFetch adapter = BackgroundFetch.getInstance(mContext);
        adapter.finish();
        result.success(true);
    }

    class FetchStreamHandler implements EventChannel.StreamHandler, BackgroundFetch.Callback {
        private EventChannel.EventSink mEventSink;

        @Override
        public void onFetch() {
            mEventSink.success(true);
        }
        @Override
        public void onListen(Object args, EventChannel.EventSink eventSink) {
            mEventSink = eventSink;
        }
        @Override
        public void onCancel(Object args) {

        }
    }
}
