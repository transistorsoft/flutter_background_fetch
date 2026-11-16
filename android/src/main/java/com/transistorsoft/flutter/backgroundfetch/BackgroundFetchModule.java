package com.transistorsoft.flutter.backgroundfetch;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.transistorsoft.tsbackgroundfetch.BackgroundFetch;
import com.transistorsoft.tsbackgroundfetch.BackgroundFetchConfig;
import com.transistorsoft.tsbackgroundfetch.LifecycleManager;

import java.util.ArrayList;
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
    static final String EVENT_CHANNEL_NAME                  = PLUGIN_ID + "/events";

    private static final String ACTION_REGISTER_HEADLESS_TASK = "registerHeadlessTask";
    private static final String ACTION_SCHEDULE_TASK          = "scheduleTask";

    private static final String HEADLESS_JOB_SERVICE_CLASS = HeadlessTask.class.getName();

    private final FetchCallback mFetchCallback;

    private Context mContext;

    private final AtomicBoolean mIsAttachedToEngine = new AtomicBoolean(false);
    // Keyed by BinaryMessenger so tearing down one engine (e.g. a sibling
    // plugin's headless FlutterEngine) doesn't strip the handler from
    // another engine's channel.  The previous single-field design was
    // "last-attached wins" and lost the main-engine handler whenever any
    // other engine detached.
    private final Map<BinaryMessenger, MethodChannel> mMethodChannels = new HashMap<>();

    // (WO-012) Every plugin instance whose engine is alive (one per FlutterEngine, HeadlessTask's included), and the
    // order of attaches.  Guarded by mInstances.
    private final List<BackgroundFetchPlugin> mInstances = new ArrayList<>();
    private long mAttachCounter = 0;

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
        mFetchCallback = new FetchCallback();
    }

    void onAttachedToEngine(Context context, BinaryMessenger messenger, BackgroundFetchPlugin plugin) {
        // Poke BackgroundFetch alive once Context is received
        BackgroundFetch.getInstance(context);
        mIsAttachedToEngine.set(true);
        mContext = context;

        MethodChannel methodChannel = new MethodChannel(messenger, METHOD_CHANNEL_NAME);
        methodChannel.setMethodCallHandler(this);
        mMethodChannels.put(messenger, methodChannel);
        // (WO-012) The instance's engine is alive: a delivery may go to it once it has attached to an Activity.
        synchronized (mInstances) {
            mInstances.add(plugin);
        }
    }

    void onDetachedFromEngine(BinaryMessenger messenger, BackgroundFetchPlugin plugin) {
        // (WO-012) The engine is gone: drop its instance, leaving the flag alone.  An Activity detach, if any, came
        // first and decided the flag (onDetachedFromActivity).
        synchronized (mInstances) {
            mInstances.remove(plugin);
        }
        MethodChannel methodChannel = mMethodChannels.remove(messenger);
        if (methodChannel != null) {
            methodChannel.setMethodCallHandler(null);
        }
        if (mMethodChannels.isEmpty()) {
            mIsAttachedToEngine.set(false);
        }
    }

    // (WO-012) An Activity is attached to this instance's engine.
    void onAttachedToActivity(BackgroundFetchPlugin plugin) {
        // Tear down any lingering headless FlutterEngine spawned by
        // HeadlessTask while the main Activity was gone.  Keeping it
        // alive alongside the main engine risks plugin-channel conflicts
        // that surface as MissingPluginException on the /methods channel
        // (or a stuck splash / logo) when an FGS kept the process alive
        // post-termination.
        HeadlessTask.destroyBackgroundIsolate();

        // Inform BackgroundFetch LifecycleManager that we're not Headless (an Activity exists).
        LifecycleManager.getInstance().setHeadless(false);
        synchronized (mInstances) {
            plugin.mAttached = true;
            plugin.mAttachSeq = ++mAttachCounter;
        }
    }

    // (WO-012) The Activity is gone from this instance's engine.  A real detach (`terminal`) makes the SDK headless
    // only when no other instance is still attached to an Activity: when an app restarts itself, the new Activity's
    // engine attaches BEFORE the old one detaches, and the new screen is up.  A recreation's detach
    // (onDetachedFromActivityForConfigChanges) never does (WO-003).
    void onDetachedFromActivity(BackgroundFetchPlugin plugin, boolean terminal) {
        boolean othersAttached = false;
        synchronized (mInstances) {
            plugin.mAttached = false;
            for (BackgroundFetchPlugin p : mInstances) {
                if (p != plugin && p.mAttached) {
                    othersAttached = true;
                    break;
                }
            }
        }
        if (!terminal) return;
        if (othersAttached) {
            Log.d(BackgroundFetch.TAG, "[BackgroundFetchModule] An Activity detached while another is attached: not headless");
        } else {
            LifecycleManager.getInstance().setHeadless(true);
        }
    }

    // (WO-012) Where a fetch or a timeout goes: the sink of the instance attached most recently whose Dart side is
    // listening.  With no attached instance listening, the instance attached most recently whose engine is still alive
    // and listening (an engine that outlived its Activity, e.g. a FlutterEngineCache engine).  Never an instance that has
    // not attached to an Activity at least once (HeadlessTask's engine, another plugin's background engine).
    @Nullable
    private EventChannel.EventSink deliveryTarget() {
        synchronized (mInstances) {
            BackgroundFetchPlugin attached = null;
            BackgroundFetchPlugin alive = null;
            for (BackgroundFetchPlugin p : mInstances) {
                if (p.mAttachSeq == 0 || p.getEventSink() == null) continue;
                if (p.mAttached && (attached == null || p.mAttachSeq > attached.mAttachSeq)) attached = p;
                if (alive == null || p.mAttachSeq > alive.mAttachSeq) alive = p;
            }
            BackgroundFetchPlugin target = (attached != null) ? attached : alive;
            return (target != null) ? target.getEventSink() : null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
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

    private void configure(Map<String, Object> params, @NonNull Result result) {
        BackgroundFetch adapter = BackgroundFetch.getInstance(mContext);
        adapter.configure(buildConfig(params)
                .setTaskId(FETCH_TASK_ID)
                .setIsFetchTask(true)
                .build(), mFetchCallback);

        result.success(adapter.status());
    }

    private void start(@NonNull Result result) {
        BackgroundFetch adapter = BackgroundFetch.getInstance(mContext);
        adapter.start(FETCH_TASK_ID);
        result.success(adapter.status());
    }

    private void stop(@Nullable String taskId, @NonNull Result result) {
        BackgroundFetch adapter = BackgroundFetch.getInstance(mContext);
        adapter.stop(taskId);
        // (WO-011) true, as the types declare and as scheduleTask answers: the SDK's stop is void, and the status
        // this used to answer (always STATUS_AVAILABLE on Android) said nothing about the stop.
        result.success(true);
    }

    private void status(@NonNull Result result) {
        BackgroundFetch adapter = BackgroundFetch.getInstance(mContext);
        result.success(adapter.status());
    }

    private void finish(String taskId, Result result) {
        if (taskId == null) taskId = FETCH_TASK_ID;
        BackgroundFetch adapter = BackgroundFetch.getInstance(mContext);

        adapter.finish(taskId);
        result.success(true);
    }

    private void scheduleTask(Map<String, Object> params, @NonNull Result result) {
        BackgroundFetch adapter = BackgroundFetch.getInstance(mContext);
        adapter.scheduleTask(buildConfig(params).build());
        result.success(true);
    }

    @NonNull
    private BackgroundFetchConfig.Builder buildConfig(@NonNull Map<String, Object>params) {
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

    // (WO-012) The one callback the SDK holds (configure()); it delivers to deliveryTarget().
    class FetchCallback implements BackgroundFetch.Callback {
        @Override
        public void onFetch(String taskId) {
            Map<String, Object> event = new HashMap<>();
            event.put("timeout", false);
            event.put("taskId", taskId);
            EventChannel.EventSink sink = deliveryTarget();
            if (sink == null) {
                Log.e(BackgroundFetch.TAG, "FetchStreamHandler.onFetch mEventSink is null.  Cannot fire Dart callback");
                return;
            }
            sink.success(event);
        }
        @Override
        public void onTimeout(String taskId) {
            Map<String, Object> event = new HashMap<>();
            event.put("timeout", true);
            event.put("taskId", taskId);
            EventChannel.EventSink sink = deliveryTarget();
            if (sink == null) {
                Log.e(BackgroundFetch.TAG, "FetchStreamHandler.onTimeout mEventSink is null.  Cannot fire Dart callback");
                return;
            }
            sink.success(event);
        }
    }
}
