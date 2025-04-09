package com.example.itiger;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
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
    private int workCyclesCompleted = 0;
    private int totalCycles;
    private long totalWorkTime;
    private static final long DEFAULT_WORK_DURATION = 25 * 60 * 1000L;
    private static final long DEFAULT_BREAK_DURATION = 5 * 60 * 1000L;
    private static final String CHANNEL_ID = "PomodoroChannel";
    private static final int NOTIFICATION_ID = 1;
    private MainActivity.Tetromino tetromino;
    private MediaPlayer mediaPlayer;

    // Параметр ускорения времени (для тестирования)
    private static final int TIME_ACCELERATION_FACTOR = 1; // Ускорение в 10 раз
    private static final long UPDATE_INTERVAL = 1000 / TIME_ACCELERATION_FACTOR; // Интервал обновления (например, 100 мс)
    private static final long TIME_DECREMENT = 1000; // Уменьшение времени на 1 секунду за интервал

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning && timeLeftInMillis > 0) {
                timeLeftInMillis -= TIME_DECREMENT;
                updateNotification();
                handler.postDelayed(this, UPDATE_INTERVAL);
            } else if (isRunning && timeLeftInMillis <= 0) {
                if (isWorkPeriod) {
                    workCyclesCompleted++;
                    long elapsedWorkTime = workCyclesCompleted * workDuration;
                    if (breakDuration > 0 && elapsedWorkTime < totalWorkTime) {
                        sendPeriodEndNotification("Работа завершена! Время отдыха.", R.raw.relaxing);
                        isWorkPeriod = false;
                        timeLeftInMillis = breakDuration;
                        handler.postDelayed(this, UPDATE_INTERVAL);
                    } else {
                        sendPeriodEndNotification("Задача завершена!", R.raw.finish);
                        isRunning = false;
                        stopForeground(true);
                        stopSelf();
                    }
                } else {
                    if (workCyclesCompleted < totalCycles && (workCyclesCompleted * workDuration) < totalWorkTime) {
                        sendPeriodEndNotification("Отдых завершён! Время работы.", R.raw.work);
                        isWorkPeriod = true;
                        timeLeftInMillis = workDuration;
                        handler.postDelayed(this, UPDATE_INTERVAL);
                    } else {
                        sendPeriodEndNotification("Задача завершена!", R.raw.finish);
                        isRunning = false;
                        stopForeground(true);
                        stopSelf();
                    }
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
            if (tetromino != null) {
                totalWorkTime = tetromino.timeToComplete * 1000L;
                if (totalWorkTime < DEFAULT_WORK_DURATION) {
                    workDuration = totalWorkTime;
                    breakDuration = 0;
                    totalCycles = 1;
                } else {
                    workDuration = DEFAULT_WORK_DURATION;
                    breakDuration = DEFAULT_BREAK_DURATION;
                    long cycleDuration = workDuration + breakDuration;
                    totalCycles = (int) (totalWorkTime / cycleDuration);
                    if (totalWorkTime % cycleDuration > 0) {
                        totalCycles++;
                    }
                }
                timeLeftInMillis = intent.getLongExtra("timeLeft", workDuration);
                isWorkPeriod = intent.getBooleanExtra("isWorkPeriod", true);
                workCyclesCompleted = 0;
                isRunning = true;
                startForeground(NOTIFICATION_ID, createNotification());
                handler.removeCallbacks(timerRunnable);
                handler.post(timerRunnable);
            }
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        handler.removeCallbacks(timerRunnable);
        stopForeground(true);
        stopSound();
    }

    public void pauseTimer() {
        isRunning = false;
        handler.removeCallbacks(timerRunnable);
        stopForeground(true);
    }

    public void resumeTimer(long timeLeft, boolean isWork) {
        if (!isRunning) {
            timeLeftInMillis = timeLeft;
            isWorkPeriod = isWork;
            isRunning = true;
            startForeground(NOTIFICATION_ID, createNotification());
            handler.removeCallbacks(timerRunnable);
            handler.post(timerRunnable);
        }
    }

    public void resetTimer() {
        isRunning = false;
        handler.removeCallbacks(timerRunnable);
        timeLeftInMillis = workDuration;
        isWorkPeriod = true;
        workCyclesCompleted = 0;
        stopForeground(true);
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        String timeFormatted = String.format("%02d:%02d", minutes, seconds);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Pomodoro")
                .setContentText((isWorkPeriod ? "Работа: " : "Отдых: ") + timeFormatted + " (" + workCyclesCompleted + "/" + totalCycles + ")")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setSound(null);

        return builder.build();
    }

    private void updateNotification() {
        Notification notification = createNotification();
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private void sendPeriodEndNotification(String message, int soundResId) {
        Intent stopSoundIntent = new Intent(this, PomodoroService.class);
        stopSoundIntent.setAction("STOP_SOUND");
        PendingIntent stopSoundPendingIntent = PendingIntent.getService(this, 0, stopSoundIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Pomodoro")
                .setContentText(message + " (" + workCyclesCompleted + "/" + totalCycles + ")")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .addAction(R.drawable.ic_launcher_foreground, "Остановить звук", stopSoundPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, builder.build());

        playCustomSound(soundResId);
    }

    private void playCustomSound(int soundResId) {
        stopSound();
        try {
            mediaPlayer = MediaPlayer.create(this, soundResId);
            mediaPlayer.setOnCompletionListener(mp -> stopSound());
            mediaPlayer.start();
        } catch (Exception e) {
            Log.e("PomodoroService", "Error playing sound: " + e.getMessage());
        }
    }

    private void stopSound() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        if (intent != null && "STOP_SOUND".equals(intent.getAction())) {
            stopSound();
        }
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

    public MainActivity.Tetromino getCurrentTetromino() {
        return tetromino;
    }

    public int getWorkCyclesCompleted() {
        return workCyclesCompleted;
    }
}