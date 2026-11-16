package com.transistorsoft.flutter.backgroundfetch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;

/** BackgroundFetchPlugin: one instance per FlutterEngine (every engine runs the generated registrant). */
public class BackgroundFetchPlugin implements FlutterPlugin, ActivityAware {
    public static final String TAG                          = "TSBackgroundFetch";

    private BinaryMessenger mMessenger;

    // (WO-012) This engine's own fetch event channel and its Dart side's sink, for the engine's lifetime, as 1.6.2 made
    // the method channel.  The one process-wide channel it replaces was re-pointed by every attach and removed by every
    // detach, so an old Activity's detach could strip the handler of the engine that had just attached.
    private EventChannel mEventChannel;
    @Nullable private volatile EventChannel.EventSink mEventSink;

    // (WO-012) Read and written by BackgroundFetchModule, under its lock: whether this instance is attached to an
    // Activity now, and the order of its last attach (0: never attached, e.g. HeadlessTask's engine).
    boolean mAttached = false;
    long mAttachSeq = 0;

    public BackgroundFetchPlugin() { }

    @Override
    public void onAttachedToEngine(@NonNull FlutterPlugin.FlutterPluginBinding binding) {
        mMessenger = binding.getBinaryMessenger();
        // (WO-012)
        mEventChannel = new EventChannel(mMessenger, BackgroundFetchModule.EVENT_CHANNEL_NAME);
        mEventChannel.setStreamHandler(new EventChannel.StreamHandler() {
            @Override
            public void onListen(Object args, EventChannel.EventSink eventSink) {
                mEventSink = eventSink;
            }
            @Override
            public void onCancel(Object args) {
                mEventSink = null;
            }
        });
        BackgroundFetchModule.getInstance().onAttachedToEngine(binding.getApplicationContext(), mMessenger, this);
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        BackgroundFetchModule.getInstance().onDetachedFromEngine(binding.getBinaryMessenger(), this);
        // (WO-012) Only this engine's own channel.
        if (mEventChannel != null) {
            mEventChannel.setStreamHandler(null);
        }
        mEventChannel = null;
        mEventSink = null;
        mMessenger = null;
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding activityPluginBinding) {
        // (WO-012) Recorded by the module: the flag rule and the delivery target read it.
        BackgroundFetchModule.getInstance().onAttachedToActivity(this);
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        // (WO-003, WO-012) A recreation is not a termination: the flag is left alone.  The instance stays recorded,
        // marked detached, until onDetachedFromEngine (a cached engine survives the recreation and is re-attached).
        BackgroundFetchModule.getInstance().onDetachedFromActivity(this, false);
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding activityPluginBinding) {
        // (WO-012) A cached engine's instance survives the recreation and is attached again here.
        BackgroundFetchModule.getInstance().onAttachedToActivity(this);
    }

    @Override
    public void onDetachedFromActivity() {
        // (WO-012) A real detach: headless only if no other instance is attached (the module decides).
        BackgroundFetchModule.getInstance().onDetachedFromActivity(this, true);
    }

    @Nullable
    EventChannel.EventSink getEventSink() {
        return mEventSink;
    }
}
