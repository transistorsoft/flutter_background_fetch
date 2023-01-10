package com.transistorsoft.flutter.backgroundfetch;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.transistorsoft.tsbackgroundfetch.BackgroundFetch;
import com.transistorsoft.tsbackgroundfetch.BackgroundFetchConfig;
import com.transistorsoft.tsbackgroundfetch.LifecycleManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/** BackgroundFetchPlugin */
public class BackgroundFetchModule implements MethodCallHandler {
    private static BackgroundFetchModule sInstance;

    public static final String TAG                          = "TSBackgroundFetch";
    static final String PLUGIN_ID                           = "com.transistorsoft/flutter_background_fetch";
    static final String FETCH_TASK_ID                       = "flutter_background_fetch";

    private static final String METHOD_CHANNEL_NAME         = PLUGIN_ID + "/methods";
    private static final String EVENT_CHANNEL_NAME = PLUGIN_ID + "/events";

    private static final String ACTION_REGISTER_HEADLESS_TASK = "registerHeadlessTask";
    private static final String ACTION_SCHEDULE_TASK          = "scheduleTask";

    private static final String HEADLESS_JOB_SERVICE_CLASS = HeadlessTask.class.getName();

    private FetchStreamHandler mFetchCallback;

    private Context mContext;

    private BinaryMessenger mMessenger;
    private AtomicBoolean mIsAttachedToEngine = new AtomicBoolean(false);
    private MethodChannel mMethodChannel;
    private EventChannel mEventChannelTask;

    public static BackgroundFetchModule getInstance() {
        if (sInstance == null) {
            sInstance = getInstanceSynchronized();
        }
        return sInstance;
    }

    private static synchronized BackgroundFetchModule getInstanceSynchronized() {
        if (sInstance == null) sInstance = new BackgroundFetchModule();
        return sInstance;
    }

    private BackgroundFetchModule() {
        mFetchCallback = new FetchStreamHandler();
    }

    void onAttachedToEngine(Context context, BinaryMessenger messenger) {
        mIsAttachedToEngine.set(true);
        mMessenger = messenger;
        mContext = context;

        mMethodChannel = new MethodChannel(messenger, METHOD_CHANNEL_NAME);
        mMethodChannel.setMethodCallHandler(this);
    }

    void onDetachedFromEngine() {
        mIsAttachedToEngine.set(false);
        mMethodChannel.setMethodCallHandler(null);
        mMethodChannel = null;
    }

    void setActivity(Activity activity) {
        if (activity != null) {
            LifecycleManager.getInstance().setHeadless(false);
            mEventChannelTask = new EventChannel(mMessenger, EVENT_CHANNEL_NAME);
            mEventChannelTask.setStreamHandler(mFetchCallback);
        } else {
            LifecycleManager.getInstance().setHeadless(true);
            mEventChannelTask.setStreamHandler(null);
            mEventChannelTask = null;
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
            stop((String) call.arguments, result);
        } else if (call.method.equals(BackgroundFetch.ACTION_STATUS)) {
            status(result);
        } else if (call.method.equals(BackgroundFetch.ACTION_FINISH)) {
            String taskId = (String) call.arguments;
            finish(taskId, result);
        } else if (call.method.equals(ACTION_REGISTER_HEADLESS_TASK)) {
            registerHeadlessTask((List<Object>) call.arguments, result);
        } else if (call.method.equals(ACTION_SCHEDULE_TASK)) {
            Map<String, Object> params = (Map<String, Object>) call.arguments;
            scheduleTask(params, result);
        } else {
            result.notImplemented();
        }
    }

    private void registerHeadlessTask(List<Object> callbacks, Result result) {
        if (HeadlessTask.register(mContext, callbacks)) {
            result.success(true);
        } else {
            result.error("HEADLESS_TASK_ALREADY_REGISTERED", "Only one HeadlessTask may be registered", null);
        }
    }

    private void configure(Map<String, Object> params, Result result) {
        BackgroundFetch adapter = BackgroundFetch.getInstance(mContext);
        adapter.configure(buildConfig(params)
                .setTaskId(FETCH_TASK_ID)
                .setIsFetchTask(true)
                .build(), mFetchCallback);

        result.success(adapter.status());
    }

    private void start(Result result) {
        BackgroundFetch adapter = BackgroundFetch.getInstance(mContext);
        adapter.start(FETCH_TASK_ID);
        result.success(adapter.status());
    }

    private void stop(@Nullable String taskId, Result result) {
        BackgroundFetch adapter = BackgroundFetch.getInstance(mContext);
        adapter.stop(taskId);
        result.success(adapter.status());
    }

    private void status(Result result) {
        BackgroundFetch adapter = BackgroundFetch.getInstance(mContext);
        result.success(adapter.status());
    }

    private void finish(String taskId, Result result) {
        if (taskId == null) taskId = FETCH_TASK_ID;
        BackgroundFetch adapter = BackgroundFetch.getInstance(mContext);

        adapter.finish(taskId);
        result.success(true);
    }

    private void scheduleTask(Map<String, Object> params, Result result) {
        BackgroundFetch adapter = BackgroundFetch.getInstance(mContext);
        adapter.scheduleTask(buildConfig(params).build());
        result.success(true);
    }

    private BackgroundFetchConfig.Builder buildConfig(Map<String, Object>params) {
        BackgroundFetchConfig.Builder config = new BackgroundFetchConfig.Builder();

        if (params.containsKey(BackgroundFetchConfig.FIELD_TASK_ID)) {
            config.setTaskId((String) params.get(BackgroundFetchConfig.FIELD_TASK_ID));
        }
        if (params.containsKey("minimumFetchInterval")) {
            config.setMinimumFetchInterval((int) params.get("minimumFetchInterval"));
        }
        if (params.containsKey("delay")) {
            Integer delay = (Integer) params.get("delay");
            if (delay != null) config.setDelay(delay.longValue());
        }
        if (params.containsKey("stopOnTerminate")) {
            config.setStopOnTerminate((boolean) params.get("stopOnTerminate"));
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
        if (params.containsKey("forceAlarmManager")) {
            config.setForceAlarmManager((boolean) params.get("forceAlarmManager"));
        }
        if (params.containsKey("periodic")) {
            config.setPeriodic((boolean) params.get("periodic"));
        }
        return config;
    }

    class FetchStreamHandler implements EventChannel.StreamHandler, BackgroundFetch.Callback {
        private EventChannel.EventSink mEventSink;

        @Override
        public void onFetch(String taskId) {
            Map<String, Object> event = new HashMap<>();
            event.put("timeout", false);
            event.put("taskId", taskId);
            if (mEventSink == null) {
                Log.e(BackgroundFetch.TAG, "FetchStreamHandler.onFetch mEventSink is null.  Cannot fire Dart callback");
                return;
            }
            mEventSink.success(event);
        }
        @Override
        public void onTimeout(String taskId) {
            Map<String, Object> event = new HashMap<>();
            event.put("timeout", true);
            event.put("taskId", taskId);
            if (mEventSink == null) {
                Log.e(BackgroundFetch.TAG, "FetchStreamHandler.onTimeout mEventSink is null.  Cannot fire Dart callback");
                return;
            }
            mEventSink.success(event);
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
