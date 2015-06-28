/*
 * CalWatch
 * Copyright (C) 2014 by Dan Wallach
 * Home page: http://www.cs.rice.edu/~dwallach/calwatch/
 * Licensing: http://www.cs.rice.edu/~dwallach/calwatch/licensing.html
 */
package org.dwallach.calwatch;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

/**
 * Created by dwallach on 10/26/14.
 */
public class VersionWrapper {
    private static final String TAG = "VersionWrapper";

    private static String versionString;

    public static void logVersion(Context activity) {
        try {
            PackageInfo pinfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
            int versionNumber = pinfo.versionCode;
            String versionName = pinfo.versionName;

            versionString = "Version: " + versionName + " (" + versionNumber + ")";

            Log.i("VersionWrapper", versionString);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "failed to get package manager!");
        }
    }

    public static String getVersionString() {
        return versionString;
    }
}
