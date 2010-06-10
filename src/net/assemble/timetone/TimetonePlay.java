package net.assemble.timetone;

import java.util.Calendar;
import java.text.DateFormat;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Vibrator;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import net.assemble.timetone.R;

/**
 * 時刻読み上げ処理
 */
public class TimetonePlay {
    private static final String TAG = "Timetone";
    private static final int NOTIFICATIONID_ICON = 1;

    public static MediaPlayer g_Mp; // 再生中のMediaPlayer
    public static boolean g_Icon;       // 通知バーアイコン状態

    private AlarmManager mAlarmManager;
    private Context mCtx;
    private Calendar mCal;

    /**
     * Constructor
     *
     * @param context
     */
    public TimetonePlay(Context context) {
        mCtx = context;
        mAlarmManager = (AlarmManager) mCtx
                .getSystemService(Context.ALARM_SERVICE);
    }

    /**
     * MediaPlayer生成
     * 着信音量をMediaPlayerに設定する。
     *
     * @param mp 設定するMediaPlayer
     */
    private MediaPlayer createMediaPlayer(int resid) {
        // 再生中の音声があれば停止する。
        if (g_Mp != null) {
            g_Mp.stop();
            g_Mp.release();
            g_Mp = null;
        }

        // 生成
        MediaPlayer mp = MediaPlayer.create(mCtx, resid);
        if (mp == null) {
            Log.e(TAG, "Failed to create MediaPlayer!");
            return null;
        }

        // 音量設定
        AudioManager audio = (AudioManager) mCtx.getSystemService(Context.AUDIO_SERVICE);
        int vol;
        if (TimetonePreferences.getUseRingVolume(mCtx) != false) {
            // 着信音量を使用
            vol = audio.getStreamVolume(AudioManager.STREAM_RING);
        } else {
            // 設定値を使用
            vol = TimetonePreferences.getVolume(mCtx);
        }
        mp.setVolume(vol, vol);
        g_Mp = mp;
        return mp;
    }

    /**
     * 時報再生
     *
     * @param cal
     *            再生日時
     */
    public void play(Calendar cal) {
        mCal = cal;

        // バイブレーション
        if (TimetonePreferences.getVibrate(mCtx)) {
            Vibrator vibrator = (Vibrator) mCtx.getSystemService(Context.VIBRATOR_SERVICE);
            long[] pattern;
            if (cal.get(Calendar.MINUTE) < 30) {
                pattern = new long[] {500, 200, 100, 200, 500, 200, 100, 200};
            } else {
                pattern = new long[] {500, 200, 100, 200};
            }
            vibrator.vibrate(pattern, -1);

        //  // Receiverからは直接振動させられないため、Notificationを経由する
        //  // ->そんなことはなかった
        //  NotificationManager notificationManager = (NotificationManager) mCtx.getSystemService(Context.NOTIFICATION_SERVICE);
        //  Notification notification = new Notification();
        //  notification.vibrate = pattern;
        //  notificationManager.notify(R.string.app_name, notification);
        }

        MediaPlayer mp = createMediaPlayer(getSound(mCal));
        if (mp == null) {
            return;
        }
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.release();
                g_Mp = null;
            }
        });
        mp.start();
    }

    /**
     * 現在日時の時報再生
     */
    public void play() {
        Calendar cal = Calendar.getInstance();
        if (TimetonePreferences.issetHour(mCtx, cal)) {
            play(cal);
        }
    }

    /**
     * 時報テスト再生
     */
    public void playTest() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, 15);
        play(cal);
    }

    /**
     * 現在時刻から音声リソース取得
     *
     * @param cal
     *            日時
     * @return 音声リソースID
     */
    private static int getSound(Calendar cal) {
        return R.raw.tone;
    }

    /**
     * ノーティフィケーションバーにアイコンを表示
     */
    private void showNotification() {
        if (g_Icon != false) {
            return;
        }
        NotificationManager notificationManager = (NotificationManager)mCtx.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification(R.drawable.icon, mCtx.getResources().getString(R.string.app_name), System.currentTimeMillis());
        Intent intent = new Intent(mCtx, TimetonePreferencesActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(mCtx, 0, intent, 0);
        notification.setLatestEventInfo(mCtx, mCtx.getResources().getString(R.string.app_name), mCtx.getResources().getString(R.string.app_description), contentIntent);
        notification.flags |= Notification.FLAG_NO_CLEAR;
        notificationManager.notify(NOTIFICATIONID_ICON, notification);
        g_Icon = true;
    }

    /**
     * ノーティフィケーションバーのアイコンを消去
     */
    private void clearNotification() {
        if (g_Icon == false) {
            return;
        }
        NotificationManager notificationManager = (NotificationManager)mCtx.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATIONID_ICON);
        g_Icon = false;
    }

    /**
     * タイマ設定
     *
     * @param cal
     *            設定日時
     */
    public void setAlarm(Calendar cal, long interval) {
        mAlarmManager.cancel(pendingIntent());
        long next = cal.getTimeInMillis();
        next -= (next % 1000);
        mAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP, next, interval,
                pendingIntent());
        Log.d(TAG, "set alarm: "
                + DateFormat.getDateTimeInstance().format(cal.getTime())
                + " (msec=" + next + ", interval=" + interval + ")");
    }

    /**
     * 設定に従ってタイマを設定
     */
    public void setAlarm() {
        long interval;
        Calendar cal = Calendar.getInstance();
        if (TimetonePreferences.getPeriod(mCtx).equals(TimetonePreferences.PREF_PERIOD_EACHHOUR)) {
            // each hour
            cal.set(Calendar.MINUTE, 0);
            cal.add(Calendar.HOUR, 1);
            interval = 60 * 60 * 1000/*AlarmManager.INTERVAL_HOUR*/;
        } else {
            // each 30min.
            if (cal.get(Calendar.MINUTE) >= 30) {
                cal.set(Calendar.MINUTE, 0);
                cal.add(Calendar.HOUR, 1);
            } else {
                cal.set(Calendar.MINUTE, 30);
            }
            interval = 30 * 60 * 1000/*AlarmManager.INTERVAL_HALF_HOUR*/;
        }
        cal.set(Calendar.SECOND, 0);
        setAlarm(cal, interval);

        if (TimetonePreferences.getNotificationIcon(mCtx)) {
            showNotification();
        } else {
            clearNotification();
        }
    }

    /**
     * タイマ解除
     */
    public void resetAlarm() {
        mAlarmManager.cancel(pendingIntent());
        Log.d(TAG, "alarm canceled.");
        clearNotification();
    }

    /**
     * PendingIntent取得
     */
    public PendingIntent pendingIntent() {
        Intent intent = new Intent(mCtx, TimetoneAlarmReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(mCtx, 0, intent, 0);
        return sender;
    }
}
