package net.assemble.timetone;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * サービス
 */
public class TimetoneService extends Service {
    private static final String TAG = "TimetoneService";

    private static ComponentName mService;
    private TimetonePlay mPlay;

    @Override
    public void onCreate() {
        super.onCreate();
        mPlay = new TimetonePlay(this);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        mPlay.setAlarm();
    }

    public void onDestroy() {
        mPlay.resetAlarm();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }


    /**
     * サービス開始
     */
    public static void startService(Context ctx) {
        mService = ctx.startService(new Intent(ctx, TimetoneService.class));
        if (mService == null) {
            Log.e(TAG, "TimetoneService could not start!");
        } else {
            Log.d(TAG, "TimetoneService started: " + mService);
        }
    }

    /**
     * サービス停止
     */
    public static void stopService(Context ctx) {
        if (mService != null) {
            Intent i = new Intent();
            i.setComponent(mService);
            boolean res = ctx.stopService(i);
            if (res == false) {
                Log.e(TAG, "TimetoneService could not stop!");
            } else {
                Log.d(TAG, "TimetoneService stopped: " + mService);
                mService = null;
            }
        }
    }
}
