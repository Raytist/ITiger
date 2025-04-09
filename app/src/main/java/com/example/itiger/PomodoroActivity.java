package com.example.itiger;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class PomodoroActivity extends AppCompatActivity {

    private TextView textTitle, textDescription, textCategory, textDifficulty, textTime;
    private TextView pomodoroStatus, pomodoroTimer, pomodoroCycles;
    private Button btnStartPause, btnReset;
    private MainActivity.Tetromino currentTetromino;
    private PomodoroService pomodoroService;
    private boolean isServiceBound = false;
    private boolean isPomodoroRunning = false;
    private long timeLeftInMillis;
    private boolean isWorkPeriod = true;
    private Handler timerHandler = new Handler(Looper.getMainLooper());

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PomodoroService.PomodoroBinder binder = (PomodoroService.PomodoroBinder) service;
            pomodoroService = binder.getService();
            isServiceBound = true;
            syncWithService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pomodoro);

        // Инициализация UI элементов
        textTitle = findViewById(R.id.text_title);
        textDescription = findViewById(R.id.text_description);
        textCategory = findViewById(R.id.text_category);
        textDifficulty = findViewById(R.id.text_difficulty);
        textTime = findViewById(R.id.text_time);
        pomodoroStatus = findViewById(R.id.pomodoro_status);
        pomodoroTimer = findViewById(R.id.pomodoro_timer);
        pomodoroCycles = findViewById(R.id.pomodoro_cycles);
        btnStartPause = findViewById(R.id.btn_start_pause);
        btnReset = findViewById(R.id.btn_reset);

        textDescription.setMovementMethod(new ScrollingMovementMethod());

        // Получение текущего тетромино из Intent
        currentTetromino = (MainActivity.Tetromino) getIntent().getSerializableExtra("tetromino");
        if (currentTetromino == null) {
            finish(); // Закрываем активность, если тетромино не передано
            return;
        }

        // Отображение информации о тетромино
        textTitle.setText(currentTetromino.title);
        textDescription.setText(currentTetromino.description);
        textCategory.setText("Категория: " + currentTetromino.category);
        textDifficulty.setText("Сложность: " + currentTetromino.difficulty);
        int timeInMinutes = currentTetromino.timeToComplete / 60;
        textTime.setText("Время: " + timeInMinutes + " минут");

        // Расчёт параметров Pomodoro
        long totalWorkTime = currentTetromino.timeToComplete * 1000L;
        final long DEFAULT_WORK_DURATION = 25 * 60 * 1000L;
        final long DEFAULT_BREAK_DURATION = 5 * 60 * 1000L;
        final int[] totalCycles = new int[1];
        final long[] workDurationForTimer = new long[1];
        final long[] remainingWorkTime = new long[1];
        final long totalTime = currentTetromino.timeToComplete * 1000L;

        // Параметр ускорения времени
        final int TIME_ACCELERATION_FACTOR = 10;
        final long UPDATE_INTERVAL = 1000 / TIME_ACCELERATION_FACTOR;

        if (totalWorkTime <= DEFAULT_WORK_DURATION) {
            totalCycles[0] = 1;
            workDurationForTimer[0] = totalWorkTime;
            remainingWorkTime[0] = 0;
        } else {
            long fullCycleDuration = DEFAULT_WORK_DURATION + DEFAULT_BREAK_DURATION;
            totalCycles[0] = (int) (totalWorkTime / fullCycleDuration);
            long remainingTimeAfterFullCycles = totalWorkTime % fullCycleDuration;
            if (remainingTimeAfterFullCycles > 0) {
                remainingWorkTime[0] = remainingTimeAfterFullCycles;
                totalCycles[0]++;
            } else {
                remainingWorkTime[0] = 0;
            }
            workDurationForTimer[0] = DEFAULT_WORK_DURATION;
        }

        // Инициализация начального состояния
        final int[] workCyclesCompleted = new int[1];
        workCyclesCompleted[0] = 0;
        if (isServiceBound && pomodoroService != null && pomodoroService.isRunning()) {
            workCyclesCompleted[0] = pomodoroService.getWorkCyclesCompleted();
        }
        pomodoroCycles.setText(workCyclesCompleted[0] + "/" + totalCycles[0]);

        if (!isPomodoroRunning) {
            timeLeftInMillis = workDurationForTimer[0];
            isWorkPeriod = true;
            int totalMinutes = (int) (totalTime / 1000) / 60;
            int totalSeconds = (int) (totalTime / 1000) % 60;
            pomodoroTimer.setText(String.format("%02d:%02d", totalMinutes, totalSeconds));
        } else {
            int minutes = (int) (timeLeftInMillis / 1000) / 60;
            int seconds = (int) (timeLeftInMillis / 1000) % 60;
            pomodoroTimer.setText(String.format("%02d:%02d", minutes, seconds));
        }

        final Runnable timerRunnable = new Runnable() {
            @Override
            public void run() {
                syncWithService();
                if (isPomodoroRunning && isServiceBound && pomodoroService != null) {
                    workCyclesCompleted[0] = pomodoroService.getWorkCyclesCompleted();
                    pomodoroCycles.setText(workCyclesCompleted[0] + "/" + totalCycles[0]);
                    pomodoroStatus.setText(isWorkPeriod ? "Работа" : "Отдых");
                    btnStartPause.setText("Пауза");
                    btnReset.setEnabled(true);
                    updatePomodoroTimerText(pomodoroTimer);
                } else {
                    if (timeLeftInMillis <= 0 || timeLeftInMillis == workDurationForTimer[0]) {
                        pomodoroStatus.setText("Работа");
                        btnStartPause.setText("Старт");
                        btnReset.setEnabled(false);
                        int totalMinutes = (int) (totalTime / 1000) / 60;
                        int totalSeconds = (int) (totalTime / 1000) % 60;
                        pomodoroTimer.setText(String.format("%02d:%02d", totalMinutes, totalSeconds));
                    } else {
                        pomodoroStatus.setText(isWorkPeriod ? "Работа (остановлено)" : "Отдых (остановлено)");
                        btnStartPause.setText("Продолжить");
                        btnReset.setEnabled(true);
                        updatePomodoroTimerText(pomodoroTimer);
                    }
                    pomodoroCycles.setText(workCyclesCompleted[0] + "/" + totalCycles[0]);
                }
                timerHandler.postDelayed(this, UPDATE_INTERVAL);
            }
        };

        if (!isPomodoroRunning) {
            pomodoroStatus.setText("Работа");
            btnStartPause.setText("Старт");
            btnReset.setEnabled(false);
        } else {
            pomodoroStatus.setText(isWorkPeriod ? "Работа" : "Отдых");
            btnStartPause.setText("Пауза");
            btnReset.setEnabled(true);
        }
        timerHandler.post(timerRunnable);

        btnStartPause.setOnClickListener(v -> {
            if (!isPomodoroRunning) {
                Intent serviceIntent = new Intent(this, PomodoroService.class);
                serviceIntent.putExtra("tetromino", currentTetromino);
                if (timeLeftInMillis == workDurationForTimer[0] || timeLeftInMillis <= 0) {
                    timeLeftInMillis = workDurationForTimer[0];
                    isWorkPeriod = true;
                }
                serviceIntent.putExtra("timeLeft", timeLeftInMillis);
                serviceIntent.putExtra("isWorkPeriod", isWorkPeriod);
                startForegroundService(serviceIntent);
                bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
                isPomodoroRunning = true;
                pomodoroStatus.setText(isWorkPeriod ? "Работа" : "Отдых");
                btnStartPause.setText("Пауза");
                btnReset.setEnabled(true);
                int minutes = (int) (timeLeftInMillis / 1000) / 60;
                int seconds = (int) (timeLeftInMillis / 1000) % 60;
                pomodoroTimer.setText(String.format("%02d:%02d", minutes, seconds));
            } else if (isServiceBound && pomodoroService != null) {
                if (isPomodoroRunning) {
                    // Пауза
                    timeLeftInMillis = pomodoroService.getTimeLeftInMillis();
                    isWorkPeriod = pomodoroService.isWorkPeriod();
                    pomodoroService.pauseTimer();
                    isPomodoroRunning = false;
                    pomodoroStatus.setText(isWorkPeriod ? "Работа (остановлено)" : "Отдых (остановлено)");
                    btnStartPause.setText("Продолжить");
                    btnReset.setEnabled(true);
                    updatePomodoroTimerText(pomodoroTimer);
                } else {
                    // Продолжить
                    pomodoroService.resumeTimer(timeLeftInMillis, isWorkPeriod);
                    isPomodoroRunning = true;
                    pomodoroStatus.setText(isWorkPeriod ? "Работа" : "Отдых");
                    btnStartPause.setText("Пауза");
                    btnReset.setEnabled(true);
                    updatePomodoroTimerText(pomodoroTimer);
                }
            }
        });

        btnReset.setOnClickListener(v -> {
            if (isServiceBound && pomodoroService != null) {
                pomodoroService.resetTimer();
                unbindService(serviceConnection);
                isServiceBound = false;
            }
            stopService(new Intent(this, PomodoroService.class));
            isPomodoroRunning = false;
            timeLeftInMillis = workDurationForTimer[0];
            isWorkPeriod = true;
            workCyclesCompleted[0] = 0;
            pomodoroCycles.setText(workCyclesCompleted[0] + "/" + totalCycles[0]);
            pomodoroStatus.setText("Работа");
            btnStartPause.setText("Старт");
            btnReset.setEnabled(false);
            int totalMinutes = (int) (totalTime / 1000) / 60;
            int totalSeconds = (int) (totalTime / 1000) % 60;
            pomodoroTimer.setText(String.format("%02d:%02d", totalMinutes, totalSeconds));
        });
    }

    private void syncWithService() {
        if (isServiceBound && pomodoroService != null) {
            if (pomodoroService.isRunning()) {
                timeLeftInMillis = pomodoroService.getTimeLeftInMillis();
                isWorkPeriod = pomodoroService.isWorkPeriod();
                isPomodoroRunning = true;
            }
        }
    }

    private void updatePomodoroTimerText(TextView timerTextView) {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        timerTextView.setText(String.format("%02d:%02d", minutes, seconds));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacksAndMessages(null);
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
    }
}