package com.example.itiger;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

public class PomodoroActivity extends AppCompatActivity {
    private TextView pomodoroStatus, pomodoroCycles, pomodoroTimer;
    private AppCompatButton btnStartPause, btnReset;
    private AppCompatImageButton btnAccelerate;
    private LinearLayout pomodoroLayout;
    private long timeLeftInMillis; // Оставшееся время текущего цикла
    private long totalWorkTime; // Полное время задачи
    private long remainingTotalTime; // Оставшееся общее время
    private boolean isTimerRunning = false;
    private CountDownTimer countDownTimer;
    private static final long POMODORO_DURATION = 25 * 60 * 1000L; // 25 минут
    private static final long BREAK_DURATION = 5 * 60 * 1000L; // 5 минут
    private int cyclesCompleted = 0;
    private int totalCycles = 1;
    private boolean isWorkPeriod = true;
    private boolean isAccelerated = false;
    private static final int ACCELERATION_FACTOR = 100;
    private static final String CHANNEL_ID = "PomodoroChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 10;

    // MediaPlayer для звуков
    private MediaPlayer relaxSound;
    private MediaPlayer workSound;
    private MediaPlayer finishSound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pomodoro);

        pomodoroLayout = findViewById(R.id.pomodoro);
        pomodoroStatus = findViewById(R.id.pomodoro_status);
        pomodoroCycles = findViewById(R.id.pomodoro_cycles);
        pomodoroTimer = findViewById(R.id.pomodoro_timer);
        btnStartPause = findViewById(R.id.btn_start_pause);
        btnReset = findViewById(R.id.btn_reset);
        btnAccelerate = findViewById(R.id.btn_accelerate);

        // Инициализация звуков
        relaxSound = MediaPlayer.create(this, R.raw.relaxing);
        workSound = MediaPlayer.create(this, R.raw.work);
        finishSound = MediaPlayer.create(this, R.raw.finish);

        createNotificationChannel();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }

        totalWorkTime = getIntent().getLongExtra("pomodoro_duration", POMODORO_DURATION);
        String tetrominoTitle = getIntent().getStringExtra("tetromino_title");
        setTitle(tetrominoTitle != null ? "Pomodoro: " + tetrominoTitle : "Pomodoro");

        // Рассчитываем количество циклов
        long fullCycleDuration = POMODORO_DURATION + BREAK_DURATION;
        totalCycles = (int) (totalWorkTime / POMODORO_DURATION);
        if (totalWorkTime % POMODORO_DURATION > 0) totalCycles++;

        remainingTotalTime = totalWorkTime;
        timeLeftInMillis = Math.min(remainingTotalTime, POMODORO_DURATION); // Первый цикл

        updateTimerText();
        pomodoroStatus.setText("Работа");
        pomodoroCycles.setText("Циклов: " + cyclesCompleted + "/" + totalCycles);
        pomodoroLayout.setBackgroundColor(Color.parseColor("#e74c3c"));

        btnStartPause.setOnClickListener(v -> {
            if (isTimerRunning) {
                pauseTimer();
            } else {
                startTimer();
            }
        });

        btnReset.setOnClickListener(v -> resetTimer());

        btnAccelerate.setOnClickListener(v -> {
            isAccelerated = !isAccelerated;
            btnAccelerate.setImageResource(R.drawable.baseline_access_alarms_24);
            if (isTimerRunning) {
                pauseTimer();
                startTimer();
            }
        });
    }

    private void startTimer() {
        long interval = isAccelerated ? (1000 / ACCELERATION_FACTOR) : 1000;
        long totalTime = isAccelerated ? timeLeftInMillis / ACCELERATION_FACTOR : timeLeftInMillis;

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(totalTime, interval) {
            @Override
            public void onTick(long millisUntilFinished) {
                long displayMillis = isAccelerated ? millisUntilFinished * ACCELERATION_FACTOR : millisUntilFinished;
                timeLeftInMillis = displayMillis;
                updateTimerText();
            }

            @Override
            public void onFinish() {
                timeLeftInMillis = 0;
                updateTimerText();
                timerFinished();
            }
        }.start();

        isTimerRunning = true;
        btnStartPause.setText("Пауза");
        btnReset.setEnabled(true);
    }

    private void pauseTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isTimerRunning = false;
        btnStartPause.setText("Продолжить");
    }

    private void resetTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isTimerRunning = false;
        remainingTotalTime = totalWorkTime;
        timeLeftInMillis = Math.min(remainingTotalTime, POMODORO_DURATION);
        cyclesCompleted = 0;
        isWorkPeriod = true;
        pomodoroStatus.setText("Работа");
        pomodoroCycles.setText("Циклов: " + cyclesCompleted + "/" + totalCycles);
        pomodoroLayout.setBackgroundColor(Color.parseColor("#e74c3c"));
        btnStartPause.setText("Старт");
        btnReset.setEnabled(false);
        updateTimerText();
        cancelNotification();
    }

    private void timerFinished() {
        if (isWorkPeriod) {
            cyclesCompleted++;
            remainingTotalTime -= POMODORO_DURATION;
            if (remainingTotalTime <= 0) {
                completeTask();
            } else {
                isWorkPeriod = false;
                timeLeftInMillis = BREAK_DURATION;
                pomodoroStatus.setText("Перерыв");
                pomodoroLayout.setBackgroundColor(Color.parseColor("#2ecc71"));
                showNotification("Рабочий период завершён. Время для перерыва!");
                playSound(relaxSound); // Проигрываем relaxing.mp3
            }
        } else {
            isWorkPeriod = true;
            timeLeftInMillis = Math.min(remainingTotalTime, POMODORO_DURATION);
            pomodoroStatus.setText("Работа");
            pomodoroLayout.setBackgroundColor(Color.parseColor("#e74c3c"));
            showNotification("Перерыв окончен. Время работать!");
            playSound(workSound); // Проигрываем work.mp3
        }
        pomodoroCycles.setText("Циклов: " + cyclesCompleted + "/" + totalCycles);
        updateTimerText();
        if (remainingTotalTime > 0) {
            startTimer(); // Автоматический запуск следующего цикла
        }
    }

    private void completeTask() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("task_completed", true);
        setResult(RESULT_OK, resultIntent);
        showNotification("Задача завершена!");
        playSound(finishSound); // Проигрываем finish.mp3
        finish();
    }

    private void updateTimerText() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        pomodoroTimer.setText(String.format("%02d:%02d", minutes, seconds));
    }

    // Метод для воспроизведения звука
    private void playSound(MediaPlayer mediaPlayer) {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                    mediaPlayer.prepare();
                }
                mediaPlayer.start();
                Log.d("PomodoroActivity", "Playing sound");
            } catch (Exception e) {
                Log.e("PomodoroActivity", "Error playing sound", e);
            }
        } else {
            Log.e("PomodoroActivity", "MediaPlayer is null");
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Pomodoro Notifications";
            String description = "Уведомления о ходе Pomodoro";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            android.app.NotificationChannel channel = new android.app.NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void showNotification(String message) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("Pomodoro")
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true);

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(NOTIFICATION_ID, builder.build());
            Log.d("PomodoroActivity", "Notification shown: " + message);
        }
    }

    private void cancelNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("PomodoroActivity", "Notification permission granted");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        // Освобождаем ресурсы MediaPlayer
        if (relaxSound != null) {
            relaxSound.release();
            relaxSound = null;
        }
        if (workSound != null) {
            workSound.release();
            workSound = null;
        }
        if (finishSound != null) {
            finishSound.release();
            finishSound = null;
        }
        cancelNotification();
    }
}