package com.zhupeng.location;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 定时重复执行任务服务
 * 如果需要定制定时时间，请在调用startService/bindService时传入参数interval（单位毫秒），否则将会使用默认事件3秒
 */
@SuppressLint("InvalidWakeLockTag")
public class ScheduledExecuteService extends Service implements Runnable {

    private static final int NOTIFICATION_ID = 100;
    private static final String CHANNEL_ID = "ServiceNotificationChannel";
    public static final String EXTRA_INTERVAL = "interval";
    private static final long INTERVAL = 3 * 1000;
    private static final String WAKE_LOCK_TAG = "ServiceWakeLockTag";

    //执行器
    private ScheduledExecutorService mExecutor;
    //系统监听
    private SysChangeListener mSysChangeListener;
    //执行策略
    private Strategy mStrategy;
    //重复执行时间间隔
    private long mInterval = INTERVAL;
    //防止手机进入休眠状态而导致程序不能正常运行
    private PowerManager.WakeLock mWakeLock;

    @Override
    public void run() {
        if (mStrategy != null) {
            mStrategy.execute(getApplicationContext());
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (null == mStrategy) {
            mStrategy = StrategyFactory.create();
        }

        if (null == mSysChangeListener) {
            mSysChangeListener = new SysChangeListener();
            mSysChangeListener.listen(this);
        }

        if (null == mWakeLock) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG);
        }

        if (!mWakeLock.isHeld()) {
            mWakeLock.acquire();
        }

        startAlarm();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            mInterval = intent.getLongExtra(EXTRA_INTERVAL, INTERVAL);
        }

        if (null == mExecutor || mExecutor.isShutdown()) {
            mExecutor = Executors.newSingleThreadScheduledExecutor();
            mExecutor.scheduleAtFixedRate(this, 0, mInterval, TimeUnit.MILLISECONDS);
        }

        startForegroundCompat();
        return START_STICKY;
    }

    private void startAlarm() {
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        final long triggerAtTime = SystemClock.elapsedRealtime() + INTERVAL;
        Intent intent = new Intent(this, ServiceAlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(this, 1394, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            manager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            manager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);
        } else {
            manager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, INTERVAL, pi);
        }
    }

    private void startForegroundCompat() {
        createNotificationChannel("定时任务");

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 1394, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        final Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("定时任务执行中...")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    private void createNotificationChannel(String name) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(CHANNEL_ID,
                    name,
                    NotificationManager.IMPORTANCE_DEFAULT);

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new IServiceInterface.Stub() {
            @Override
            public void addResultListener(IResultListener listener) throws RemoteException {
                if (mStrategy != null && listener != null) {
                    mStrategy.addResultListener(listener);
                }
            }

            @Override
            public void removeResultListener(IResultListener listener) throws RemoteException {
                if (mStrategy != null) {
                    mStrategy.removeResultListener(listener);
                }
            }

            @Override
            public void removeAllResultListeners() throws RemoteException {
                if (mStrategy != null) {
                    mStrategy.removeAllResultListeners();
                }
            }
        };
    }

    @Override
    public void onDestroy() {
        if (mExecutor != null) {
            mExecutor.shutdownNow();
            mExecutor = null;
        }

        if (mSysChangeListener != null) {
            mSysChangeListener.disregard(getApplicationContext());
        }

        if (mStrategy != null) {
            mStrategy.finish(getApplicationContext());
        }

        if (mWakeLock != null) {
            mWakeLock.release();
        }
        super.onDestroy();
    }

    public static class ServiceAlarmReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!isServiceRunning(context, ScheduledExecuteService.class.getName())) {
                Intent i = new Intent(context, ScheduledExecuteService.class);
                ContextCompat.startForegroundService(context, i);
            }
        }

        /**
         * 判断服务是否正在运行
         *
         * @param context     上下文
         * @param serviceName 服务全路径名
         * @return
         */
        public boolean isServiceRunning(Context context, String serviceName) {
            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningServiceInfo> services = manager.getRunningServices(40);
            if (services.size() <= 0) {
                return false;
            }

            for (int i = 0; i < services.size(); i++) {
                String className = services.get(i).service.getClassName();
                if (className.equals(serviceName)) {
                    return true;
                }
            }
            return false;
        }
    }
}
