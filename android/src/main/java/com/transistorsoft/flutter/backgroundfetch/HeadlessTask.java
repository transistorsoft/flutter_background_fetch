package com.transistorsoft.flutter.backgroundfetch;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.transistorsoft.tsbackgroundfetch.BackgroundFetch;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.embedding.engine.dart.DartExecutor;
import io.flutter.embedding.engine.plugins.shim.ShimPluginRegistry;
import io.flutter.plugin.common.JSONMethodCodec;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.view.FlutterCallbackInformation;
import io.flutter.view.FlutterMain;

public class HeadlessTask implements MethodChannel.MethodCallHandler, Runnable {
    private static final String KEY_REGISTRATION_CALLBACK_ID    = "registrationCallbackId";
    private static final String KEY_CLIENT_CALLBACK_ID          = "clientCallbackId";
    private static final String METHOD_CHANNEL_NAME             = BackgroundFetchModule.PLUGIN_ID + "/headless";
    private static final String ACTION_INITIALIZED              = "initialized";

    private Context mContext;
    // Deprecated 1.12.0
    private static PluginRegistry.PluginRegistrantCallback sPluginRegistrantCallback;
    private static FlutterEngine sBackgroundFlutterEngine;

    private static final AtomicBoolean sHeadlessTaskRegistered = new AtomicBoolean(false);

    private static MethodChannel sDispatchChannel;

    private long mRegistrationCallbackId;
    private long mClientCallbackId;
    private String mTaskId;

    private static final List<OnInitializedCallback> sOnInitializedListeners = new ArrayList<>();

    // Called by Application#onCreate
    static void setPluginRegistrant(PluginRegistry.PluginRegistrantCallback callback) {
        sPluginRegistrantCallback = callback;
    }

    // Called by BackgroundFetchModule
    static boolean register(final Context context, final List<Object> callbacks) {
        BackgroundFetch.getThreadPool().execute(new RegistrationTask(context, callbacks));
        return true;
    }

    public HeadlessTask(Context context, String taskId) {
        mContext = context;
        mTaskId = taskId;
        Log.d(BackgroundFetch.TAG, "\uD83D\uDC80 [HeadlessTask " + mTaskId + "]");
        BackgroundFetch.getThreadPool().execute(new TaskRunner());
    }

    @Override
    public void onMethodCall(MethodCall call, @NonNull MethodChannel.Result result) {
        Log.i(BackgroundFetch.TAG,"$ " + call.method);
        if (call.method.equalsIgnoreCase(ACTION_INITIALIZED)) {
            initialize();
        } else {
            result.notImplemented();
        }
    }

    private void initialize() {
        synchronized (sOnInitializedListeners) {
            if (!sOnInitializedListeners.isEmpty()) {
                for (OnInitializedCallback callback : sOnInitializedListeners) {
                    callback.onInitialized(sBackgroundFlutterEngine);
                }
                sOnInitializedListeners.clear();
            }
        }
        sHeadlessTaskRegistered.set(true);
        dispatch();
    }

    @Override
    public void run() {
        dispatch();
    }

    // Send event to Client.
    private void dispatch() {
        if (sBackgroundFlutterEngine == null) {
            startBackgroundIsolate();
        }

        if (!sHeadlessTaskRegistered.get()) {
            // Queue up events while background isolate is starting
            Log.d(BackgroundFetch.TAG, "[HeadlessTask] waiting for client to initialize");
            return;
        }

        JSONObject response = new JSONObject();
        try {
            response.put("callbackId", mClientCallbackId);
            response.put("taskId", mTaskId);
            sDispatchChannel.invokeMethod("", response);
        } catch (JSONException e) {
            BackgroundFetch.getInstance(mContext).finish(mTaskId);
            Log.e(BackgroundFetch.TAG, e.getMessage());
            e.printStackTrace();
        }
    }

    private void startBackgroundIsolate() {
        if (sBackgroundFlutterEngine != null) {
            Log.w(BackgroundFetch.TAG, "Background isolate already started");
            return;
        }

        String appBundlePath = FlutterMain.findAppBundlePath();
        AssetManager assets = mContext.getAssets();
        if (!sHeadlessTaskRegistered.get()) {
            sBackgroundFlutterEngine = new FlutterEngine(mContext);
            DartExecutor executor = sBackgroundFlutterEngine.getDartExecutor();
            // Create the Transmitter channel
            sDispatchChannel = new MethodChannel(executor, METHOD_CHANNEL_NAME, JSONMethodCodec.INSTANCE);
            sDispatchChannel.setMethodCallHandler(this);

            FlutterCallbackInformation callbackInfo = FlutterCallbackInformation.lookupCallbackInformation(mRegistrationCallbackId);

            if (callbackInfo == null) {
                Log.e(BackgroundFetch.TAG, "Fatal: failed to find callback: " + mRegistrationCallbackId);
                BackgroundFetch.getInstance(mContext).finish(mTaskId);
                return;
            }
            DartExecutor.DartCallback dartCallback = new DartExecutor.DartCallback(assets, appBundlePath, callbackInfo);
            executor.executeDartCallback(dartCallback);

            // The pluginRegistrantCallback should only be set in the V1 embedding as
            // plugin registration is done via reflection in the V2 embedding.
            if (sPluginRegistrantCallback != null) {
                sPluginRegistrantCallback.registerWith(new ShimPluginRegistry(sBackgroundFlutterEngine));
            }
        }
    }

    /**
     * Persist callbacks in Background-thread.
     */
    static class RegistrationTask implements Runnable {
        private Context mContext;
        private List<Object> mCallbacks;

        RegistrationTask(Context context, List<Object>callbacks) {
            mContext = context;
            mCallbacks = callbacks;
        }

        @Override
        public void run() {
            SharedPreferences prefs = mContext.getSharedPreferences(BackgroundFetch.TAG, Context.MODE_PRIVATE);

            // There is weirdness with the class of these callbacks (Integer vs Long) between assembleDebug vs assembleRelease.
            Object cb1 = mCallbacks.get(0);
            Object cb2 = mCallbacks.get(1);

            SharedPreferences.Editor editor = prefs.edit();
            if (cb1.getClass() == Long.class) {
                editor.putLong(KEY_REGISTRATION_CALLBACK_ID, (Long) cb1);
            } else if (cb1.getClass() == Integer.class) {
                editor.putLong(KEY_REGISTRATION_CALLBACK_ID, ((Integer) cb1).longValue());
            }

            if (cb2.getClass() == Long.class) {
                editor.putLong(KEY_CLIENT_CALLBACK_ID, (Long) cb2);
            } else if (cb2.getClass() == Integer.class) {
                editor.putLong(KEY_CLIENT_CALLBACK_ID, ((Integer) cb2).longValue());
            }
            editor.apply();
        }
    }

    /**
     * Load from SharedPreferences in a background-thread then dispatch on the main-thread.
     */
    class TaskRunner implements Runnable {
        @Override
        public void run() {
            SharedPreferences prefs = mContext.getSharedPreferences(BackgroundFetch.TAG, Context.MODE_PRIVATE);
            mRegistrationCallbackId = prefs.getLong(KEY_REGISTRATION_CALLBACK_ID, -1);
            mClientCallbackId = prefs.getLong(KEY_CLIENT_CALLBACK_ID, -1);

            BackgroundFetch.getUiHandler().post(HeadlessTask.this);
        }
    }

    public static void onInitialized(OnInitializedCallback callback) {
        synchronized (sOnInitializedListeners) {
            sOnInitializedListeners.add(callback);
        }
    }

    public interface OnInitializedCallback {
        void onInitialized(FlutterEngine engine);
    }
}
