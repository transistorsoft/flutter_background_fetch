package com.transistorsoft.flutter.backgroundfetch.backgroundfetchexample;

import io.flutter.app.FlutterApplication;

public class Application  extends FlutterApplication {
    @Override
    public void onCreate() {

        /// Test Strict mode.
        /*
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectAll()
                .penaltyLog()
                .build());

        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .penaltyDeath()
                .build());
        */

        super.onCreate();

    }
}