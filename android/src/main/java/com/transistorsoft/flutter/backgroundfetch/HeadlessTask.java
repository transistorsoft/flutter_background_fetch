package com.transistorsoft.flutter.backgroundfetch;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.transistorsoft.tsbackgroundfetch.BackgroundFetch;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.flutter.plugin.common.JSONMethodCodec;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.view.FlutterCallbackInformation;
import io.flutter.view.FlutterMain;
import io.flutter.view.FlutterNativeView;
import io.flutter.view.FlutterRunArguments;

public class HeadlessTask implements MethodChannel.MethodCallHandler {
    static final String KEY_REGISTRATION_CALLBACK_ID    = "registrationCallbackId";
    static final String KEY_CLIENT_CALLBACK_ID          = "clientCallbackId";
    private static final String METHOD_CHANNEL_NAME = BackgroundFetchPlugin.PLUGIN_ID + "/headless";
    private static final String ACTION_INITIALIZED      = "initialized";

    private static PluginRegistry.PluginRegistrantCallback sPluginRegistrantCallback;
    private static final AtomicBoolean sHeadlessTaskRegistered = new AtomicBoolean(false);
    private static FlutterNativeView sBackgroundFlutterView;
    private static MethodChannel sDispatchChannel;

    private long mRegistrationCallbackId;
    private long mClientCallbackId;
    private Context mContext;

    HeadlessTask(Context context) {
        mContext = context;
        SharedPreferences prefs = context.getSharedPreferences(BackgroundFetch.TAG, Context.MODE_PRIVATE);
        mRegistrationCallbackId = prefs.getLong(KEY_REGISTRATION_CALLBACK_ID, -1);
        mClientCallbackId = prefs.getLong(KEY_CLIENT_CALLBACK_ID, -1);

        Log.d(BackgroundFetch.TAG, "\uD83D\uDC80 [HeadlessTask]");
        if (sBackgroundFlutterView == null) {
            initFlutterView();
        }
        synchronized(sHeadlessTaskRegistered) {
            if (!sHeadlessTaskRegistered.get()) {
                // Queue up events while background isolate is starting
                Log.d(BackgroundFetch.TAG, "[HeadlessTask] waiting for client to initialize");
                return;
            }
        }
        dispatch();
    }

    // Called by Application#onCreate
    static void setPluginRegistrant(PluginRegistry.PluginRegistrantCallback callback) {
        sPluginRegistrantCallback = callback;
    }

    // Called by FLTBackgroundGeolocationPlugin
    static boolean register(Context context, List<Long> callbacks) {
        SharedPreferences prefs = context.getSharedPreferences(BackgroundFetch.TAG, Context.MODE_PRIVATE);
        if (prefs.contains(KEY_REGISTRATION_CALLBACK_ID) && prefs.contains(KEY_CLIENT_CALLBACK_ID)) {
            return false;
        }
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(KEY_REGISTRATION_CALLBACK_ID, callbacks.get(0));
        editor.putLong(KEY_CLIENT_CALLBACK_ID, callbacks.get(1));
        editor.apply();
        return true;
    }

    @Override
    public void onMethodCall(MethodCall call, MethodChannel.Result result) {
        Log.i(BackgroundFetch.TAG,"$ " + call.method);
        if (call.method.equalsIgnoreCase(ACTION_INITIALIZED)) {
            synchronized(sHeadlessTaskRegistered) {
                sHeadlessTaskRegistered.set(true);
            }
            dispatch();
        } else {
            result.notImplemented();
        }
    }

    // Send event to Client.
    private void dispatch() {
        JSONObject response = new JSONObject();
        try {
            response.put("callbackId", mClientCallbackId);
            sDispatchChannel.invokeMethod("", response);
        } catch (JSONException e) {
            BackgroundFetch.getInstance(mContext).finish();
            Log.e(BackgroundFetch.TAG, e.getMessage());
            e.printStackTrace();
        }
    }

    private void initFlutterView() {
        FlutterMain.ensureInitializationComplete(mContext, null);

        FlutterCallbackInformation callbackInfo = FlutterCallbackInformation.lookupCallbackInformation(mRegistrationCallbackId);

        if (callbackInfo == null) {
            Log.e(BackgroundFetch.TAG,"Fatal: failed to find callback");
            BackgroundFetch.getInstance(mContext).finish();
            return;
        }

        sBackgroundFlutterView = new FlutterNativeView(mContext.getApplicationContext(), true);

        // Create the Transmitter channel
        sDispatchChannel = new MethodChannel(sBackgroundFlutterView, METHOD_CHANNEL_NAME, JSONMethodCodec.INSTANCE);
        sDispatchChannel.setMethodCallHandler(this);

        sPluginRegistrantCallback.registerWith(sBackgroundFlutterView.getPluginRegistry());

        // Dispatch back to client for initialization.
        FlutterRunArguments args = new FlutterRunArguments();
        args.bundlePath = FlutterMain.findAppBundlePath(mContext);
        args.entrypoint = callbackInfo.callbackName;
        args.libraryPath = callbackInfo.callbackLibraryPath;
        sBackgroundFlutterView.runFromBundle(args);
    }
}
