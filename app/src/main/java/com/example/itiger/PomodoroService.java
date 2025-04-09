package com.example.itiger;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.core.app.NotificationCompat;

public class PomodoroService extends Service {

    private final IBinder binder = new PomodoroBinder();
    private Handler handler = new Handler(Looper.getMainLooper());
    private long timeLeftInMillis;
    private boolean isWorkPeriod = true;
    private boolean isRunning = false;
    private long workDuration;
    private long breakDuration;
    private static final String CHANNEL_ID = "PomodoroChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final float BREAK_DURATION_FACTOR = 0.2f;

    private MainActivity.Tetromino tetromino;

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning && timeLeftInMillis > 0) {
                timeLeftInMillis -= 1000;
                updateNotification();
                Log.d("PomodoroService", "Tick: " + (timeLeftInMillis / 1000) + " seconds left");
                handler.postDelayed(this, 1000);
            } else if (timeLeftInMillis <= 0) {
                isWorkPeriod = !isWorkPeriod;
                timeLeftInMillis = isWorkPeriod ? workDuration : breakDuration;
                updateNotification();
                if (isRunning) {
                    handler.postDelayed(this, 1000);
                } else {
                    stopSelf();
                }
            }
        }
    };

    public class PomodoroBinder extends Binder {
        PomodoroService getService() {
            return PomodoroService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            tetromino = (MainActivity.Tetromino) intent.getSerializableExtra("tetromino");
            if (tetromino != null && !isRunning) {
                workDuration = tetromino.timeToComplete * 1000L;
                breakDuration = (long) (workDuration * BREAK_DURATION_FACTOR);
                timeLeftInMillis = workDuration;
                isWorkPeriod = true;
                isRunning = true;
                startForeground(NOTIFICATION_ID, createNotification());
                handler.post(timerRunnable);
                Log.d("PomodoroService", "Service started with workDuration=" + workDuration);
            }
        }
        return START_NOT_STICKY; // Сервис не перезапускается автоматически
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        handler.removeCallbacks(timerRunnable);
        stopForeground(true);
        Log.d("PomodoroService", "Service destroyed");
    }

    public void pauseTimer() {
        isRunning = false;
        handler.removeCallbacks(timerRunnable);
        stopForeground(true);
        Log.d("PomodoroService", "Timer paused, timeLeft=" + timeLeftInMillis);
    }

    public void resetTimer() {
        isRunning = false;
        handler.removeCallbacks(timerRunnable);
        timeLeftInMillis = workDuration;
        isWorkPeriod = true;
        stopForeground(true);
        stopSelf();
        Log.d("PomodoroService", "Timer reset, timeLeft=" + timeLeftInMillis);
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        String timeFormatted = String.format("%02d:%02d", minutes, seconds);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Pomodoro")
                .setContentText((isWorkPeriod ? "Работа: " : "Отдых: ") + timeFormatted)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void updateNotification() {
        Notification notification = createNotification();
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    public long getTimeLeftInMillis() {
        return timeLeftInMillis;
    }

    public boolean isWorkPeriod() {
        return isWorkPeriod;
    }

    public boolean isRunning() {
        return isRunning;
    }
}