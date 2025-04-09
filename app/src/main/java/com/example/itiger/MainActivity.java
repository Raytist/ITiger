package com.example.itiger;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.Manifest;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.app.NotificationCompat;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private TetrisView tetrisView;
    private ArrayList<Tetromino> tetrominos = new ArrayList<>();
    private ArrayList<Tetromino> completedTetrominos = new ArrayList<>();
    private Tetromino currentTetromino;
    private final int WIDTH = 8;
    private final int HEIGHT = 8;
    private boolean isFalling = true;
    private final Handler fallHandler = new Handler(Looper.getMainLooper());
    private final long FALL_INTERVAL = 500;

    private AppCompatImageButton buttonRotate;
    private AppCompatButton buttonUp;
    private AppCompatButton buttonDown;
    private AppCompatButton buttonLeft;
    private AppCompatButton buttonRight;
    private AppCompatButton buttonViewInfo;

    private boolean isPomodoroRunning = false;
    private boolean isWorkPeriod = true;
    private long timeLeftInMillis;
    private long workDuration;
    private long breakDuration;
    private static final String CHANNEL_ID = "PomodoroChannel";
    private static final float BREAK_DURATION_FACTOR = 0.2f;
    private static final int NOTIFICATION_ID = 1;

    private static final int ARCHIVE_REQUEST_CODE = 1;

    private PomodoroService pomodoroService;
    private boolean isServiceBound = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PomodoroService.PomodoroBinder binder = (PomodoroService.PomodoroBinder) service;
            pomodoroService = binder.getService();
            isServiceBound = true;
            Log.d("MainActivity", "Service connected");
            syncWithService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;
            pomodoroService = null;
            Log.d("MainActivity", "Service disconnected");
        }
    };

    private final int[] colors = {
            android.R.color.holo_blue_light,
            android.R.color.holo_red_light,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_purple,
            android.R.color.holo_blue_dark,
            android.R.color.holo_red_dark
    };

    private final int[] tetrominoTypes = {0, 1, 2, 3, 4, 5, 6};

    private static final String PREFS_NAME = "TetrisPrefs";
    private static final String KEY_TETROMINO_COUNT = "TetrominoCount";
    private static final String KEY_CURRENT_TETROMINO_INDEX = "CurrentTetrominoIndex";
    private static final String KEY_TETROMINO_POSITION = "TetrominoPosition_";
    private static final String KEY_TETROMINO_TYPE = "TetrominoType_";
    private static final String KEY_TETROMINO_ROTATION = "TetrominoRotation_";
    private static final String KEY_TETROMINO_COLOR = "TetrominoColor_";
    private static final String KEY_TETROMINO_TITLE = "TetrominoTitle_";
    private static final String KEY_TETROMINO_DESCRIPTION = "TetrominoDescription_";
    private static final String KEY_TETROMINO_CATEGORY = "TetrominoCategory_";
    private static final String KEY_TETROMINO_DIFFICULTY = "TetrominoDifficulty_";
    private static final String KEY_TETROMINO_TIME = "TetrominoTime_";

    private static final String KEY_COMPLETED_TETROMINO_COUNT = "CompletedTetrominoCount";
    private static final String KEY_COMPLETED_TETROMINO_POSITION = "CompletedTetrominoPosition_";
    private static final String KEY_COMPLETED_TETROMINO_TYPE = "CompletedTetrominoType_";
    private static final String KEY_COMPLETED_TETROMINO_ROTATION = "CompletedTetrominoRotation_";
    private static final String KEY_COMPLETED_TETROMINO_COLOR = "CompletedTetrominoColor_";
    private static final String KEY_COMPLETED_TETROMINO_TITLE = "CompletedTetrominoTitle_";
    private static final String KEY_COMPLETED_TETROMINO_DESCRIPTION = "CompletedTetrominoDescription_";
    private static final String KEY_COMPLETED_TETROMINO_CATEGORY = "CompletedTetrominoCategory_";
    private static final String KEY_COMPLETED_TETROMINO_DIFFICULTY = "CompletedTetrominoDifficulty_";
    private static final String KEY_COMPLETED_TETROMINO_TIME = "CompletedTetrominoTime_";

    private int SELECTED_COLOR;

    private final Runnable fallRunnable = new Runnable() {
        @Override
        public void run() {
            if (isFalling) {
                for (Tetromino tetromino : tetrominos) {
                    if (tetromino != currentTetromino) {
                        moveTetrominoDown(tetromino);
                    }
                }
            }
            fallHandler.postDelayed(this, FALL_INTERVAL);
        }
    };

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }
        SELECTED_COLOR = ContextCompat.getColor(this, android.R.color.holo_orange_dark);

        createNotificationChannel();

        buttonRotate = findViewById(R.id.btnRotate);
        buttonUp = findViewById(R.id.btnUp);
        buttonDown = findViewById(R.id.btnDown);
        buttonLeft = findViewById(R.id.btnLeft);
        buttonRight = findViewById(R.id.btnRight);
        buttonViewInfo = findViewById(R.id.btnInfo);

        updateControlButtonsVisibility();

        tetrisView = findViewById(R.id.tetrisView);
        tetrisView.setTetrominos(tetrominos);
        tetrisView.setCurrentTetromino(currentTetromino);
        tetrisView.setOnTetrominoSelectedListener(tetromino -> {
            if (currentTetromino == tetromino) {
                currentTetromino.color = currentTetromino.originalColor;
                currentTetromino = null;
                isFalling = true;
            } else {
                if (currentTetromino != null) {
                    currentTetromino.color = currentTetromino.originalColor;
                }
                currentTetromino = tetromino;
                currentTetromino.color = SELECTED_COLOR;
                isFalling = false;
            }
            tetrisView.setCurrentTetromino(currentTetromino);
            updateControlButtonsVisibility();
            tetrisView.invalidate();
        });

        restoreGameState();

        if (tetrominos.isEmpty()) {
            createNewTetromino(null);
        }

        fallHandler.post(fallRunnable);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, PomodoroService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Pomodoro Notifications";
            String description = "Уведомления о завершении периодов Pomodoro";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void showNotification(String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Pomodoro")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void cancelNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
        Log.d("MainActivity", "Notification cancelled");
    }

    public void openArchive(View view) {
        Intent intent = new Intent(this, ArchiveActivity.class);
        intent.putExtra("completedTetrominos", completedTetrominos);
        startActivityForResult(intent, ARCHIVE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ARCHIVE_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                Object serializableExtra = data.getSerializableExtra("updatedCompletedTetrominos");
                if (serializableExtra instanceof ArrayList) {
                    completedTetrominos = (ArrayList<Tetromino>) serializableExtra;
                }
            }
        }
    }

    private void updateControlButtonsVisibility() {
        int visibility = (currentTetromino != null) ? View.VISIBLE : View.GONE;
        buttonRotate.setVisibility(visibility);
        buttonUp.setVisibility(visibility);
        buttonDown.setVisibility(visibility);
        buttonLeft.setVisibility(visibility);
        buttonRight.setVisibility(visibility);
        buttonViewInfo.setVisibility(visibility);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveGameState();
        fallHandler.removeCallbacks(fallRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        fallHandler.post(fallRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        fallHandler.removeCallbacksAndMessages(null);
    }

    private void saveGameState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putInt(KEY_TETROMINO_COUNT, tetrominos.size());

        int currentTetrominoIndex = -1;
        if (currentTetromino != null) {
            currentTetrominoIndex = tetrominos.indexOf(currentTetromino);
        }
        editor.putInt(KEY_CURRENT_TETROMINO_INDEX, currentTetrominoIndex);

        for (int i = 0; i < tetrominos.size(); i++) {
            Tetromino tetromino = tetrominos.get(i);
            editor.putInt(KEY_TETROMINO_POSITION + i, tetromino.position);
            editor.putInt(KEY_TETROMINO_TYPE + i, tetromino.typeIndex);
            editor.putInt(KEY_TETROMINO_ROTATION + i, tetromino.rotation);
            editor.putInt(KEY_TETROMINO_COLOR + i, tetromino.originalColor);
            editor.putString(KEY_TETROMINO_TITLE + i, tetromino.title);
            editor.putString(KEY_TETROMINO_DESCRIPTION + i, tetromino.description);
            editor.putString(KEY_TETROMINO_CATEGORY + i, tetromino.category);
            editor.putInt(KEY_TETROMINO_DIFFICULTY + i, tetromino.difficulty);
            editor.putInt(KEY_TETROMINO_TIME + i, tetromino.timeToComplete);
        }

        editor.putInt(KEY_COMPLETED_TETROMINO_COUNT, completedTetrominos.size());

        for (int i = 0; i < completedTetrominos.size(); i++) {
            Tetromino tetromino = completedTetrominos.get(i);
            editor.putInt(KEY_COMPLETED_TETROMINO_POSITION + i, tetromino.position);
            editor.putInt(KEY_COMPLETED_TETROMINO_TYPE + i, tetromino.typeIndex);
            editor.putInt(KEY_COMPLETED_TETROMINO_ROTATION + i, tetromino.rotation);
            editor.putInt(KEY_COMPLETED_TETROMINO_COLOR + i, tetromino.originalColor);
            editor.putString(KEY_COMPLETED_TETROMINO_TITLE + i, tetromino.title);
            editor.putString(KEY_COMPLETED_TETROMINO_DESCRIPTION + i, tetromino.description);
            editor.putString(KEY_COMPLETED_TETROMINO_CATEGORY + i, tetromino.category);
            editor.putInt(KEY_COMPLETED_TETROMINO_DIFFICULTY + i, tetromino.difficulty);
            editor.putInt(KEY_COMPLETED_TETROMINO_TIME + i, tetromino.timeToComplete);
        }

        editor.apply();
    }

    private void restoreGameState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        int tetrominoCount = prefs.getInt(KEY_TETROMINO_COUNT, 0);
        if (tetrominoCount > 0) {
            tetrominos.clear();
            for (int i = 0; i < tetrominoCount; i++) {
                int position = prefs.getInt(KEY_TETROMINO_POSITION + i, 0);
                int typeIndex = prefs.getInt(KEY_TETROMINO_TYPE + i, 0);
                int rotation = prefs.getInt(KEY_TETROMINO_ROTATION + i, 0);
                int color = prefs.getInt(KEY_TETROMINO_COLOR + i, ContextCompat.getColor(this, colors[0]));
                String title = prefs.getString(KEY_TETROMINO_TITLE + i, "");
                String description = prefs.getString(KEY_TETROMINO_DESCRIPTION + i, "");
                String category = prefs.getString(KEY_TETROMINO_CATEGORY + i, "");
                int difficulty = prefs.getInt(KEY_TETROMINO_DIFFICULTY + i, 1);
                int timeToComplete = prefs.getInt(KEY_TETROMINO_TIME + i, 0);

                int[] shape = generateShapeFromTimeAndDifficulty(timeToComplete, difficulty, rotation);
                Tetromino tetromino = new Tetromino(position, shape, color, typeIndex, rotation,
                        title, description, category, difficulty, timeToComplete);
                tetrominos.add(tetromino);
            }

            int currentTetrominoIndex = prefs.getInt(KEY_CURRENT_TETROMINO_INDEX, -1);
            if (currentTetrominoIndex >= 0 && currentTetrominoIndex < tetrominos.size()) {
                currentTetromino = tetrominos.get(currentTetrominoIndex);
                currentTetromino.color = SELECTED_COLOR;
            } else {
                currentTetromino = null;
            }

            tetrisView.setTetrominos(tetrominos);
            tetrisView.setCurrentTetromino(currentTetromino);
            updateControlButtonsVisibility();
            tetrisView.invalidate();
        }

        int completedTetrominoCount = prefs.getInt(KEY_COMPLETED_TETROMINO_COUNT, 0);
        if (completedTetrominoCount > 0) {
            completedTetrominos.clear();
            for (int i = 0; i < completedTetrominoCount; i++) {
                int position = prefs.getInt(KEY_COMPLETED_TETROMINO_POSITION + i, 0);
                int typeIndex = prefs.getInt(KEY_COMPLETED_TETROMINO_TYPE + i, 0);
                int rotation = prefs.getInt(KEY_COMPLETED_TETROMINO_ROTATION + i, 0);
                int color = prefs.getInt(KEY_COMPLETED_TETROMINO_COLOR + i, ContextCompat.getColor(this, colors[0]));
                String title = prefs.getString(KEY_COMPLETED_TETROMINO_TITLE + i, "");
                String description = prefs.getString(KEY_COMPLETED_TETROMINO_DESCRIPTION + i, "");
                String category = prefs.getString(KEY_COMPLETED_TETROMINO_CATEGORY + i, "");
                int difficulty = prefs.getInt(KEY_COMPLETED_TETROMINO_DIFFICULTY + i, 1);
                int timeToComplete = prefs.getInt(KEY_COMPLETED_TETROMINO_TIME + i, 0);

                int[] shape = generateShapeFromTimeAndDifficulty(timeToComplete, difficulty, rotation);
                Tetromino tetromino = new Tetromino(position, shape, color, typeIndex, rotation,
                        title, description, category, difficulty, timeToComplete);
                completedTetrominos.add(tetromino);
            }
        }
    }

    static class Tetromino implements java.io.Serializable {
        int position;
        int[] shape;
        int color;
        int originalColor;
        int typeIndex;
        int rotation;
        String title;
        String description;
        String category;
        int difficulty;
        int timeToComplete;

        Tetromino(int position, int[] shape, int color, int typeIndex, int rotation,
                  String title, String description, String category, int difficulty, int timeToComplete) {
            this.position = position;
            this.shape = shape;
            this.color = color;
            this.originalColor = color;
            this.typeIndex = typeIndex;
            this.rotation = rotation;
            this.title = title;
            this.description = description;
            this.category = category;
            this.difficulty = difficulty;
            this.timeToComplete = timeToComplete;
        }
    }

    private int adjustPositionToBounds(int newPosition, int[] shape) {
        int minCol = WIDTH, maxCol = -1;
        int minRow = HEIGHT, maxRow = -1;

        for (int index : shape) {
            int pos = newPosition + index;
            int row = pos / WIDTH;
            int col = pos % WIDTH;
            minCol = Math.min(minCol, col);
            maxCol = Math.max(maxCol, col);
            minRow = Math.min(minRow, row);
            maxRow = Math.max(maxRow, row);
        }

        if (minCol < 0) {
            newPosition += (0 - minCol);
        } else if (maxCol >= WIDTH) {
            newPosition -= (maxCol - (WIDTH - 1));
        }

        if (minRow < 0) {
            newPosition += (0 - minRow) * WIDTH;
        } else if (maxRow >= HEIGHT) {
            newPosition -= (maxRow - (HEIGHT - 1)) * WIDTH;
        }

        return newPosition;
    }

    private int[] generateShapeFromTimeAndDifficulty(int timeInSeconds, int difficulty, int rotation) {
        final int SECONDS_PER_COLUMN = 2 * 60 * 60;
        int columns = (int) Math.ceil((double) timeInSeconds / SECONDS_PER_COLUMN);
        columns = Math.min(columns, WIDTH);
        int rows = Math.min(difficulty, HEIGHT);

        if (rotation % 2 == 1) {
            int temp = rows;
            rows = columns;
            columns = temp;
        }

        rows = Math.min(rows, HEIGHT);
        columns = Math.min(columns, WIDTH);

        if (rows == 0) rows = 1;
        if (columns == 0) columns = 1;

        int[] shape = new int[rows * columns];
        int index = 0;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                shape[index++] = row * WIDTH + col;
            }
        }
        return shape;
    }

    public void createNewTetromino(View view) {
        if (isFinishing()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_new_tetromino, null);
        builder.setView(dialogView);

        EditText editTitle = dialogView.findViewById(R.id.edit_title);
        EditText editDescription = dialogView.findViewById(R.id.edit_description);
        Spinner spinnerCategory = dialogView.findViewById(R.id.spinner_category);
        EditText editDifficulty = dialogView.findViewById(R.id.edit_difficulty);
        EditText editTime = dialogView.findViewById(R.id.edit_time);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.categories,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        builder.setPositiveButton("Создать", (dialog, which) -> {
            if (isFinishing()) return;

            String title = editTitle.getText().toString().trim();
            String description = editDescription.getText().toString().trim();
            String category = spinnerCategory.getSelectedItem().toString();
            String difficultyStr = editDifficulty.getText().toString().trim();
            String timeStr = editTime.getText().toString().trim();

            if (title.isEmpty() || description.isEmpty() || difficultyStr.isEmpty() || timeStr.isEmpty()) {
                Toast.makeText(this, "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show();
                return;
            }

            int difficulty;
            try {
                difficulty = Integer.parseInt(difficultyStr);
                if (difficulty < 1 || difficulty > WIDTH) {
                    Toast.makeText(this, "Сложность должна быть от 1 до " + WIDTH, Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Некорректное значение сложности", Toast.LENGTH_SHORT).show();
                return;
            }

            int timeInMinutes;
            try {
                timeInMinutes = Integer.parseInt(timeStr);
                if (timeInMinutes <= 0) {
                    Toast.makeText(this, "Время (в минутах) должно быть больше 0", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Некорректное значение времени (должно быть в минутах)", Toast.LENGTH_SHORT).show();
                return;
            }

            int timeInSeconds = timeInMinutes * 60;

            int colorIndex;
            switch (category) {
                case "Образование":
                    colorIndex = 2;
                    break;
                case "Личное":
                    colorIndex = 1;
                    break;
                case "Работа":
                    colorIndex = 3;
                    break;
                case "Другое":
                default:
                    colorIndex = 0;
                    break;
            }
            int color = ContextCompat.getColor(this, colors[colorIndex]);

            int[] shape = generateShapeFromTimeAndDifficulty(timeInSeconds, difficulty, 0);
            int startPosition = WIDTH / 2;
            startPosition = adjustPositionToBounds(startPosition, shape);

            for (int index : shape) {
                int pos = startPosition + index;
                if (isPositionOccupied(pos)) {
                    Toast.makeText(this, "Невозможно разместить тетромино: позиция занята", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            Tetromino newTetromino = new Tetromino(startPosition, shape, color, colorIndex, 0,
                    title, description, category, difficulty, timeInSeconds);
            tetrominos.add(newTetromino);
            if (currentTetromino != null) {
                currentTetromino.color = currentTetromino.originalColor;
            }
            currentTetromino = null;
            isFalling = true;
            tetrisView.setTetrominos(tetrominos);
            tetrisView.setCurrentTetromino(currentTetromino);
            updateControlButtonsVisibility();
            tetrisView.invalidate();
        });

        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void syncWithService() {
        if (isServiceBound && pomodoroService != null) {
            isPomodoroRunning = pomodoroService.isRunning();
            timeLeftInMillis = pomodoroService.getTimeLeftInMillis();
            isWorkPeriod = pomodoroService.isWorkPeriod();
            Log.d("MainActivity", "Synced with service: running=" + isPomodoroRunning + ", timeLeft=" + timeLeftInMillis + ", isWork=" + isWorkPeriod);
        }
    }

    public void viewTetrominoInfo(View view) {
        if (currentTetromino == null) {
            Toast.makeText(this, "Тетромино не выбрано", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_view_tetromino, null);
        builder.setView(dialogView);

        TextView textTitle = dialogView.findViewById(R.id.text_title);
        TextView textDescription = dialogView.findViewById(R.id.text_description);
        TextView textCategory = dialogView.findViewById(R.id.text_category);
        TextView textDifficulty = dialogView.findViewById(R.id.text_difficulty);
        TextView textTime = dialogView.findViewById(R.id.text_time);
        TextView pomodoroStatus = dialogView.findViewById(R.id.pomodoro_status);
        TextView pomodoroTimer = dialogView.findViewById(R.id.pomodoro_timer);
        Button btnStartPause = dialogView.findViewById(R.id.btn_start_pause);
        Button btnReset = dialogView.findViewById(R.id.btn_reset);

        textDescription.setMovementMethod(new android.text.method.ScrollingMovementMethod());

        textTitle.setText(currentTetromino.title);
        textDescription.setText(currentTetromino.description);
        textCategory.setText("Категория: " + currentTetromino.category);
        textDifficulty.setText("Сложность: " + currentTetromino.difficulty);
        int timeInMinutes = currentTetromino.timeToComplete / 60;
        textTime.setText("Время: " + timeInMinutes + " минут");

        workDuration = currentTetromino.timeToComplete * 1000L;
        breakDuration = (long) (workDuration * BREAK_DURATION_FACTOR);

        final Handler timerHandler = new Handler(Looper.getMainLooper());
        final Runnable timerRunnable = new Runnable() {
            @Override
            public void run() {
                syncWithService();
                if (isPomodoroRunning) {
                    pomodoroStatus.setText(isWorkPeriod ? "Работа" : "Отдых");
                    btnStartPause.setText("Пауза");
                    btnReset.setEnabled(true);
                } else {
                    if (timeLeftInMillis <= 0) {
                        timeLeftInMillis = workDuration;
                        isWorkPeriod = true;
                    }
                    pomodoroStatus.setText("Работа (остановлено)");
                    btnStartPause.setText("Старт");
                    btnReset.setEnabled(timeLeftInMillis != workDuration);
                }
                updatePomodoroTimerText(pomodoroTimer);
                timerHandler.postDelayed(this, 1000);
            }
        };

        // Начальная синхронизация
        syncWithService();
        if (!isPomodoroRunning) {
            timeLeftInMillis = workDuration;
            isWorkPeriod = true;
            pomodoroStatus.setText("Работа (остановлено)");
            btnStartPause.setText("Старт");
            btnReset.setEnabled(false);
        } else {
            pomodoroStatus.setText(isWorkPeriod ? "Работа" : "Отдых");
            btnStartPause.setText("Пауза");
            btnReset.setEnabled(true);
        }
        updatePomodoroTimerText(pomodoroTimer);
        timerHandler.post(timerRunnable);

        btnStartPause.setOnClickListener(v -> {
            if (!isPomodoroRunning) {
                Log.d("MainActivity", "Starting Pomodoro service");
                Intent serviceIntent = new Intent(this, PomodoroService.class);
                serviceIntent.putExtra("tetromino", currentTetromino);
                startForegroundService(serviceIntent);
                isPomodoroRunning = true;
                btnStartPause.setText("Пауза");
                btnReset.setEnabled(true);
                pomodoroStatus.setText("Работа");
            } else if (isServiceBound && pomodoroService != null) {
                Log.d("MainActivity", "Pausing Pomodoro service");
                pomodoroService.pauseTimer();
                isPomodoroRunning = false;
                btnStartPause.setText("Старт");
                btnReset.setEnabled(true);
                pomodoroStatus.setText("Работа (остановлено)");
            }
            updatePomodoroTimerText(pomodoroTimer);
        });

        btnReset.setOnClickListener(v -> {
            if (isServiceBound && pomodoroService != null) {
                Log.d("MainActivity", "Resetting Pomodoro");
                pomodoroService.resetTimer();
                cancelNotification();
                isPomodoroRunning = false;
                timeLeftInMillis = workDuration;
                isWorkPeriod = true;
                pomodoroStatus.setText("Работа (остановлено)");
                updatePomodoroTimerText(pomodoroTimer);
                btnStartPause.setText("Старт");
                btnReset.setEnabled(false);
            }
        });

        builder.setNegativeButton("Удалить", (dialog, which) -> {
            Log.d("MainActivity", "Deleting Tetromino");
            tetrominos.remove(currentTetromino);
            stopService(new Intent(this, PomodoroService.class));
            cancelNotification();
            currentTetromino = null;
            isFalling = true;
            isPomodoroRunning = false;
            tetrisView.setTetrominos(tetrominos);
            tetrisView.setCurrentTetromino(currentTetromino);
            updateControlButtonsVisibility();
            tetrisView.invalidate();
            dialog.dismiss();
        });

        builder.setNeutralButton("Завершить", (dialog, which) -> {
            Log.d("MainActivity", "Completing Tetromino");
            completedTetrominos.add(currentTetromino);
            tetrominos.remove(currentTetromino);
            stopService(new Intent(this, PomodoroService.class));
            cancelNotification();
            currentTetromino = null;
            isFalling = true;
            isPomodoroRunning = false;
            tetrisView.setTetrominos(tetrominos);
            tetrisView.setCurrentTetromino(currentTetromino);
            updateControlButtonsVisibility();
            tetrisView.invalidate();
            dialog.dismiss();
        });

        builder.setPositiveButton("ОК", (dialog, which) -> {
            timerHandler.removeCallbacks(timerRunnable);
            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(d -> timerHandler.removeCallbacks(timerRunnable));
        dialog.show();
    }

    private void updatePomodoroTimerText(TextView timer) {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        String timeFormatted = String.format("%02d:%02d", minutes, seconds);
        timer.setText(timeFormatted);
    }
    public void rotateTetromino(View view) {
        if (currentTetromino == null) {
            Log.d("MainActivity", "Нет выбранного тетромино для поворота");
            return;
        }

        // Поворот против часовой стрелки: уменьшаем rotation (0 → 3 → 2 → 1)
        int newRotation = (currentTetromino.rotation - 1 + 4) % 4;
        int[] currentShape = currentTetromino.shape;
        int[] newShape;

        // Определяем текущие размеры формы
        int minRow = HEIGHT, maxRow = -1;
        int minCol = WIDTH, maxCol = -1;
        for (int index : currentShape) {
            int row = index / WIDTH;
            int col = index % WIDTH;
            minRow = Math.min(minRow, row);
            maxRow = Math.max(maxRow, row);
            minCol = Math.min(minCol, col);
            maxCol = Math.max(maxCol, col);
        }
        int currentHeight = maxRow - minRow + 1;
        int currentWidth = maxCol - minCol + 1;

        // Новая форма после поворота (меняем ширину и высоту местами для нечётных rotation)
        int newHeight = (newRotation % 2 == 0) ? currentHeight : currentWidth;
        int newWidth = (newRotation % 2 == 0) ? currentWidth : currentHeight;
        newShape = new int[newHeight * newWidth];
        int index = 0;

        Log.d("MainActivity", "Текущие размеры: width=" + currentWidth + ", height=" + currentHeight +
                ", Новые размеры: width=" + newWidth + ", height=" + newHeight);

        // Заполняем новую форму с учётом поворота против часовой стрелки
        for (int row = 0; row < newHeight; row++) {
            for (int col = 0; col < newWidth; col++) {
                int newRow, newCol;
                switch (newRotation) {
                    case 0: // Исходная ориентация
                        newRow = row;
                        newCol = col;
                        break;
                    case 1: // 270° против часовой (90° по часовой)
                        newRow = col;
                        newCol = newHeight - 1 - row;
                        break;
                    case 2: // 180°
                        newRow = newHeight - 1 - row;
                        newCol = newWidth - 1 - col;
                        break;
                    case 3: // 90° против часовой (270° по часовой)
                        newRow = newWidth - 1 - col;
                        newCol = row;
                        break;
                    default:
                        throw new IllegalStateException("Недопустимое значение rotation: " + newRotation);
                }
                newShape[index++] = newRow * WIDTH + newCol;
            }
        }

        // Корректируем позицию с учётом новой формы
        int newPosition = adjustPositionToBounds(currentTetromino.position, newShape);

        // Проверка на столкновение
        StringBuilder shapeLog = new StringBuilder("Новая форма: ");
        for (int posIndex : newShape) {
            int pos = newPosition + posIndex;
            shapeLog.append(pos).append(" ");
            if (pos < 0 || pos >= WIDTH * HEIGHT || (isPositionOccupied(pos) && !isPartOfTetromino(currentTetromino, pos))) {
                Log.d("MainActivity", "Поворот невозможен: позиция " + pos + " занята или вне поля");
                return; // Отмена поворота
            }
        }
        Log.d("MainActivity", shapeLog.toString());

        // Применяем поворот
        currentTetromino.rotation = newRotation;
        currentTetromino.shape = newShape;
        currentTetromino.position = newPosition;

        Log.d("MainActivity", "Поворот успешен: newRotation=" + newRotation + ", newPosition=" + newPosition);

        // Обновляем отображение
        tetrisView.setCurrentTetromino(currentTetromino);
        tetrisView.invalidate();
    }

    private boolean isPositionOccupied(int position) {
        if (position < 0 || position >= WIDTH * HEIGHT) {
            return true;
        }
        for (Tetromino tetromino : tetrominos) {
            for (int index : tetromino.shape) {
                if (tetromino.position + index == position) {
                    return true;
                }
            }
        }
        return false;
    }

    private void moveTetrominoDown(Tetromino tetromino) {
        if (canMoveDown(tetromino)) {
            tetromino.position += WIDTH;
            tetrisView.invalidate();
        }
    }

    public void moveDown(View view) {
        if (currentTetromino == null) return;
        moveTetrominoDown(currentTetromino);
    }

    public void moveUp(View view) {
        if (currentTetromino == null) return;
        if (canMoveUp(currentTetromino)) {
            currentTetromino.position -= WIDTH;
            tetrisView.invalidate();
        }
    }

    public void moveLeft(View view) {
        if (currentTetromino == null) return;
        if (canMoveLeft(currentTetromino)) {
            currentTetromino.position--;
            currentTetromino.position = adjustPositionToBounds(currentTetromino.position, currentTetromino.shape);
            tetrisView.invalidate();
        }
    }

    public void moveRight(View view) {
        if (currentTetromino == null) return;
        if (canMoveRight(currentTetromino)) {
            currentTetromino.position++;
            currentTetromino.position = adjustPositionToBounds(currentTetromino.position, currentTetromino.shape);
            tetrisView.invalidate();
        }
    }

    private boolean canMoveUp(Tetromino tetromino) {
        for (int index : tetromino.shape) {
            int newPos = tetromino.position + index - WIDTH;
            if (newPos < 0) {
                return false;
            }
            if (isPositionOccupied(newPos) && !isPartOfTetromino(tetromino, newPos)) {
                return false;
            }
        }
        return true;
    }

    private boolean canMoveDown(Tetromino tetromino) {
        for (int index : tetromino.shape) {
            int newPos = tetromino.position + index + WIDTH;
            if (newPos >= WIDTH * HEIGHT) {
                return false;
            }
            if (isPositionOccupied(newPos) && !isPartOfTetromino(tetromino, newPos)) {
                return false;
            }
        }
        return true;
    }

    private boolean canMoveLeft(Tetromino tetromino) {
        for (int index : tetromino.shape) {
            int newPos = tetromino.position + index - 1;
            int col = newPos % WIDTH;
            if (col < 0) {
                return false;
            }
            if (isPositionOccupied(newPos) && !isPartOfTetromino(tetromino, newPos)) {
                return false;
            }
        }
        return true;
    }

    private boolean canMoveRight(Tetromino tetromino) {
        for (int index : tetromino.shape) {
            int newPos = tetromino.position + index + 1;
            int col = newPos % WIDTH;
            if (col >= WIDTH) {
                return false;
            }
            if (isPositionOccupied(newPos) && !isPartOfTetromino(tetromino, newPos)) {
                return false;
            }
        }
        return true;
    }

    private boolean isPartOfTetromino(Tetromino tetromino, int position) {
        for (int index : tetromino.shape) {
            if (tetromino.position + index == position) {
                return true;
            }
        }
        return false;
    }
}