package com.example.itiger;

import static androidx.core.app.ServiceCompat.startForeground;

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
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.app.NotificationCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private AppCompatImageButton btnPreviousDay;
    private AppCompatImageButton btnNextDay;
    private TextView tvCurrentDate;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", new Locale("ru"));
    private SimpleDateFormat displayDateFormat = new SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault());
    private String formatDateRussian(String dateStr) {
        try {
            Date date = dateFormat.parse(dateStr);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);

            String[] days = {"Вс", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб"};
            String[] months = {"января", "февраля", "марта", "апреля", "мая", "июня",
                    "июля", "августа", "сентября", "октября", "ноября", "декабря"};

            String dayOfWeek = days[cal.get(Calendar.DAY_OF_WEEK) - 1];
            int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
            String month = months[cal.get(Calendar.MONTH)];
            int year = cal.get(Calendar.YEAR);

            return String.format("%s, %d %s %d", dayOfWeek, dayOfMonth, month, year);
        } catch (ParseException e) {
            e.printStackTrace();
            return dateStr;
        }
    }
    private TetrisView tetrisView;
    private static final String KEY_MODIFIED_DATES = "ModifiedDates";
    private ArrayList<Tetromino> tetrominos = new ArrayList<>();
    private ArrayList<Tetromino> completedTetrominos = new ArrayList<>();
    private Tetromino currentTetromino;
    private static final int WIDTH = 8;
    private static final int HEIGHT = 8;
    private boolean isFalling = true;
    private final Handler fallHandler = new Handler(Looper.getMainLooper());
    private final long FALL_INTERVAL = 500;
    private String selectedDate;
    private static final String PREFS_NAME = "TetrisPrefs";
    private static final String KEY_POINTS = "Points";
    private int points = 0;
    private AppCompatImageButton buttonRotate;
    private AppCompatButton buttonUp;
    private AppCompatButton buttonDown;
    private AppCompatButton buttonLeft;
    private AppCompatButton buttonRight;
    private AppCompatImageButton buttonViewInfo;

    private boolean isPomodoroRunning = false;
    private boolean isWorkPeriod = true;
    private long timeLeftInMillis;
    private long workDuration;
    private long breakDuration;
    private static final String CHANNEL_ID = "PomodoroChannel";
    private static final float BREAK_DURATION_FACTOR = 0.2f;
    private static final int NOTIFICATION_ID = 1;

    private static final int ARCHIVE_REQUEST_CODE = 1;
    private static final int POMODORO_REQUEST_CODE = 1;

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
    static final String KEY_TETROMINO_COUNT = "TetrominoCount";
    private static final String KEY_CURRENT_TETROMINO_INDEX = "CurrentTetrominoIndex";
    static final String KEY_TETROMINO_POSITION = "TetrominoPosition_";
    static final String KEY_TETROMINO_TYPE = "TetrominoType_";
    static final String KEY_TETROMINO_ROTATION = "TetrominoRotation_";
    static final String KEY_TETROMINO_COLOR = "TetrominoColor_";
    static final String KEY_TETROMINO_TITLE = "TetrominoTitle_";
    static final String KEY_TETROMINO_DESCRIPTION = "TetrominoDescription_";
    static final String KEY_TETROMINO_CATEGORY = "TetrominoCategory_";
    static final String KEY_TETROMINO_DIFFICULTY = "TetrominoDifficulty_";
    static final String KEY_TETROMINO_TIME = "TetrominoTime_";

    static final String KEY_COMPLETED_TETROMINO_COUNT = "CompletedTetrominoCount";
    static final String KEY_COMPLETED_TETROMINO_POSITION = "CompletedTetrominoPosition_";
    static final String KEY_COMPLETED_TETROMINO_TYPE = "CompletedTetrominoType_";
    static final String KEY_COMPLETED_TETROMINO_ROTATION = "CompletedTetrominoRotation_";
    static final String KEY_COMPLETED_TETROMINO_COLOR = "CompletedTetrominoColor_";
    static final String KEY_COMPLETED_TETROMINO_TITLE = "CompletedTetrominoTitle_";
    static final String KEY_COMPLETED_TETROMINO_DESCRIPTION = "CompletedTetrominoDescription_";
    static final String KEY_COMPLETED_TETROMINO_CATEGORY = "CompletedTetrominoCategory_";
    static final String KEY_COMPLETED_TETROMINO_DIFFICULTY = "CompletedTetrominoDifficulty_";
    static final String KEY_COMPLETED_TETROMINO_TIME = "CompletedTetrominoTime_";

    private int SELECTED_COLOR;
    private boolean isTipShownInLifecycle = false;

    private final String[] timeManagementTips = {
            "Планируйте свой день заранее.",
            "Ставьте приоритеты задач.",
            "Используйте технику Pomodoro для повышения продуктивности.",
            "Делайте короткие перерывы каждые 25 минут работы."
    };

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

        createNotificationChannel();

        Intent intent = getIntent();
        selectedDate = intent.getStringExtra("selectedDate");
        if (selectedDate == null) {
            selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        }

        tetrisView = findViewById(R.id.tetrisView);
        if (tetrisView == null) {
            Log.e("MainActivity", "TetrisView not found in layout");
            Toast.makeText(this, "Ошибка: TetrisView не найден", Toast.LENGTH_LONG).show();
            return;
        }

        buttonRotate = findViewById(R.id.btnRotate);
        buttonUp = findViewById(R.id.btnUp);
        buttonDown = findViewById(R.id.btnDown);
        buttonLeft = findViewById(R.id.btnLeft);
        buttonRight = findViewById(R.id.btnRight);
        buttonViewInfo = findViewById(R.id.btnInfo);
        btnPreviousDay = findViewById(R.id.btnPreviousDay);
        btnNextDay = findViewById(R.id.btnNextDay);
        tvCurrentDate = findViewById(R.id.tvCurrentDate);
        btnPreviousDay.setOnClickListener(v -> navigateToAdjacentDay(-1));
        btnNextDay.setOnClickListener(v -> navigateToAdjacentDay(1));

        updateDateDisplay();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }

        isTipShownInLifecycle = false;
        SELECTED_COLOR = ContextCompat.getColor(this, android.R.color.holo_orange_dark);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        points = prefs.getInt(KEY_POINTS, 0); // Load initial points

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

    private void navigateToAdjacentDay(int daysToAdd) {
        try {
            saveGameState();
            Date currentDate = dateFormat.parse(selectedDate);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(currentDate);
            calendar.add(Calendar.DAY_OF_YEAR, daysToAdd);
            String newDate = dateFormat.format(calendar.getTime());
            selectedDate = newDate;
            restoreGameState();
            updateDateDisplay();
        } catch (ParseException e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка при изменении даты", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateDateDisplay() {
        tvCurrentDate.setText(formatDateRussian(selectedDate));
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Pomodoro Notifications";
            String description = "Уведомления о ходе Pomodoro";
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
        startActivityForResult(intent, ARCHIVE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ARCHIVE_REQUEST_CODE && resultCode == RESULT_OK) {
            NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_bottom_nuv);
            navController.navigate(R.id.navigation_dashboard);
        } else if (requestCode == POMODORO_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            if (data.getBooleanExtra("task_completed", false)) {
                if (currentTetromino != null) {
                    completedTetrominos.add(currentTetromino);
                    tetrominos.remove(currentTetromino);
                    currentTetromino = null;
                    isFalling = true;
                    if (tetrisView != null) {
                        tetrisView.setTetrominos(tetrominos);
                        tetrisView.setCurrentTetromino(currentTetromino);
                        updateControlButtonsVisibility();
                        tetrisView.invalidate();
                    }
                    awardPoints();
                    saveGameState();
                }
            }
        }
    }

    static class TetrominoWithDate implements java.io.Serializable {
        Tetromino tetromino;
        String date;

        TetrominoWithDate(Tetromino tetromino, String date) {
            this.tetromino = tetromino;
            this.date = date;
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
        Log.d("MainActivity", "Saving game state for date: " + selectedDate);
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        String datePrefix = selectedDate + "_";
        editor.putInt(datePrefix + KEY_TETROMINO_COUNT, tetrominos.size());

        int currentTetrominoIndex = -1;
        if (currentTetromino != null) {
            currentTetrominoIndex = tetrominos.indexOf(currentTetromino);
        }
        editor.putInt(datePrefix + KEY_CURRENT_TETROMINO_INDEX, currentTetrominoIndex);

        for (int i = 0; i < tetrominos.size(); i++) {
            Tetromino tetromino = tetrominos.get(i);
            editor.putInt(datePrefix + KEY_TETROMINO_POSITION + i, tetromino.position);
            editor.putInt(datePrefix + KEY_TETROMINO_TYPE + i, tetromino.typeIndex);
            editor.putInt(datePrefix + KEY_TETROMINO_ROTATION + i, tetromino.rotation);
            editor.putInt(datePrefix + KEY_TETROMINO_COLOR + i, tetromino.originalColor);
            editor.putString(datePrefix + KEY_TETROMINO_TITLE + i, tetromino.title);
            editor.putString(datePrefix + KEY_TETROMINO_DESCRIPTION + i, tetromino.description);
            editor.putString(datePrefix + KEY_TETROMINO_CATEGORY + i, tetromino.category);
            editor.putInt(datePrefix + KEY_TETROMINO_DIFFICULTY + i, tetromino.difficulty);
            editor.putInt(datePrefix + KEY_TETROMINO_TIME + i, tetromino.timeToComplete);
        }

        editor.putInt(datePrefix + KEY_COMPLETED_TETROMINO_COUNT, completedTetrominos.size());
        for (int i = 0; i < completedTetrominos.size(); i++) {
            Tetromino tetromino = completedTetrominos.get(i);
            editor.putInt(datePrefix + KEY_COMPLETED_TETROMINO_POSITION + i, tetromino.position);
            editor.putInt(datePrefix + KEY_COMPLETED_TETROMINO_TYPE + i, tetromino.typeIndex);
            editor.putInt(datePrefix + KEY_COMPLETED_TETROMINO_ROTATION + i, tetromino.rotation);
            editor.putInt(datePrefix + KEY_COMPLETED_TETROMINO_COLOR + i, tetromino.originalColor);
            editor.putString(datePrefix + KEY_COMPLETED_TETROMINO_TITLE + i, tetromino.title);
            editor.putString(datePrefix + KEY_COMPLETED_TETROMINO_DESCRIPTION + i, tetromino.description);
            editor.putString(datePrefix + KEY_COMPLETED_TETROMINO_CATEGORY + i, tetromino.category);
            editor.putInt(datePrefix + KEY_COMPLETED_TETROMINO_DIFFICULTY + i, tetromino.difficulty);
            editor.putInt(datePrefix + KEY_COMPLETED_TETROMINO_TIME + i, tetromino.timeToComplete);
        }

        Set<String> datesWithData = new HashSet<>(prefs.getStringSet("DatesWithData", new HashSet<>()));
        Set<String> modifiedDates = new HashSet<>(prefs.getStringSet(KEY_MODIFIED_DATES, new HashSet<>()));

        int prevTetrominoCount = prefs.getInt(datePrefix + KEY_TETROMINO_COUNT, 0);
        int prevCompletedCount = prefs.getInt(datePrefix + KEY_COMPLETED_TETROMINO_COUNT, 0);
        if (prevTetrominoCount != tetrominos.size() || prevCompletedCount != completedTetrominos.size()) {
            modifiedDates.add(selectedDate);
        }

        if (!tetrominos.isEmpty() || !completedTetrominos.isEmpty()) {
            datesWithData.add(selectedDate);
        } else {
            datesWithData.remove(selectedDate);
            modifiedDates.remove(selectedDate);
        }
        editor.putStringSet("DatesWithData", datesWithData);
        editor.putStringSet(KEY_MODIFIED_DATES, modifiedDates);
        editor.putInt(KEY_POINTS, points); // Save points

        editor.apply();
    }

    private void restoreGameState() {
        Log.d("MainActivity", "Restoring game state for date: " + selectedDate);
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String datePrefix = selectedDate + "_";

        int tetrominoCount = prefs.getInt(datePrefix + KEY_TETROMINO_COUNT, 0);
        tetrominos.clear();
        for (int i = 0; i < tetrominoCount; i++) {
            int position = prefs.getInt(datePrefix + KEY_TETROMINO_POSITION + i, 0);
            int typeIndex = prefs.getInt(datePrefix + KEY_TETROMINO_TYPE + i, 0);
            int rotation = prefs.getInt(datePrefix + KEY_TETROMINO_ROTATION + i, 0);
            int color = prefs.getInt(datePrefix + KEY_TETROMINO_COLOR + i, ContextCompat.getColor(this, colors[0]));
            String title = prefs.getString(datePrefix + KEY_TETROMINO_TITLE + i, "");
            String description = prefs.getString(datePrefix + KEY_TETROMINO_DESCRIPTION + i, "");
            String category = prefs.getString(datePrefix + KEY_TETROMINO_CATEGORY + i, "");
            int difficulty = prefs.getInt(datePrefix + KEY_TETROMINO_DIFFICULTY + i, 1);
            int timeToComplete = prefs.getInt(datePrefix + KEY_TETROMINO_TIME + i, 0);

            int[] shape = generateShapeFromTimeAndDifficulty(timeToComplete, difficulty, rotation);
            Tetromino tetromino = new Tetromino(position, shape, color, typeIndex, rotation,
                    title, description, category, difficulty, timeToComplete);
            tetrominos.add(tetromino);
        }

        int currentTetrominoIndex = prefs.getInt(datePrefix + KEY_CURRENT_TETROMINO_INDEX, -1);
        if (currentTetrominoIndex >= 0 && currentTetrominoIndex < tetrominos.size()) {
            currentTetromino = tetrominos.get(currentTetrominoIndex);
            currentTetromino.color = SELECTED_COLOR;
        } else {
            currentTetromino = null;
        }

        int completedTetrominoCount = prefs.getInt(datePrefix + KEY_COMPLETED_TETROMINO_COUNT, 0);
        completedTetrominos.clear();
        for (int i = 0; i < completedTetrominoCount; i++) {
            int position = prefs.getInt(datePrefix + KEY_COMPLETED_TETROMINO_POSITION + i, 0);
            int typeIndex = prefs.getInt(datePrefix + KEY_COMPLETED_TETROMINO_TYPE + i, 0);
            int rotation = prefs.getInt(datePrefix + KEY_COMPLETED_TETROMINO_ROTATION + i, 0);
            int color = prefs.getInt(datePrefix + KEY_COMPLETED_TETROMINO_COLOR + i, ContextCompat.getColor(this, colors[0]));
            String title = prefs.getString(datePrefix + KEY_COMPLETED_TETROMINO_TITLE + i, "");
            String description = prefs.getString(datePrefix + KEY_COMPLETED_TETROMINO_DESCRIPTION + i, "");
            String category = prefs.getString(datePrefix + KEY_COMPLETED_TETROMINO_CATEGORY + i, "");
            int difficulty = prefs.getInt(datePrefix + KEY_COMPLETED_TETROMINO_DIFFICULTY + i, 1);
            int timeToComplete = prefs.getInt(datePrefix + KEY_COMPLETED_TETROMINO_TIME + i, 0);

            int[] shape = generateShapeFromTimeAndDifficulty(timeToComplete, difficulty, rotation);
            Tetromino tetromino = new Tetromino(position, shape, color, typeIndex, rotation,
                    title, description, category, difficulty, timeToComplete);
            completedTetrominos.add(tetromino);
        }

        points = prefs.getInt(KEY_POINTS, 0); // Restore points

        if (tetrisView != null) {
            tetrisView.setTetrominos(tetrominos);
            tetrisView.setCurrentTetromino(currentTetromino);
            updateControlButtonsVisibility();
            tetrisView.invalidate();
        } else {
            Log.e("MainActivity", "TetrisView is null in restoreGameState");
        }
        updateDateDisplay();
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

    public void viewWeekTetris(View view) {
        Intent intent = new Intent(this, WeekTetrisActivity.class);
        startActivity(intent);
    }

    public static int[] generateShapeFromTimeAndDifficulty(int timeInSeconds, int difficulty, int rotation) {
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
            if (tetrisView != null) {
                tetrisView.setTetrominos(tetrominos);
                tetrisView.setCurrentTetromino(currentTetromino);
                updateControlButtonsVisibility();
                tetrisView.invalidate();
            }

            if (tetrisView.checkFullColumn()) {
                showTimeManagementTip();
            }
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
            if (!isPomodoroRunning && timeLeftInMillis <= 0 && !isWorkPeriod) {
                timeLeftInMillis = workDuration;
                isWorkPeriod = true;
            }
            Log.d("MainActivity", "Synced with service: running=" + isPomodoroRunning + ", timeLeft=" + timeLeftInMillis + ", isWork=" + isWorkPeriod);
        }
    }

    @SuppressLint({"MissingInflatedId", "LocalSuppress"})
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
        Button btnTogglePomodoro = dialogView.findViewById(R.id.btn_toggle_pomodoro);

        textDescription.setMovementMethod(new android.text.method.ScrollingMovementMethod());
        textTitle.setText(currentTetromino.title);
        textDescription.setText(currentTetromino.description);
        textCategory.setText("Категория: " + currentTetromino.category);
        textDifficulty.setText("Сложность: " + currentTetromino.difficulty);
        int timeInMinutes = currentTetromino.timeToComplete / 60;
        textTime.setText("Время: " + timeInMinutes + " минут");

        btnTogglePomodoro.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PomodoroActivity.class);
            intent.putExtra("pomodoro_duration", (long) currentTetromino.timeToComplete * 1000);
            intent.putExtra("tetromino_title", currentTetromino.title);
            startActivityForResult(intent, POMODORO_REQUEST_CODE);
        });

        builder.setNegativeButton("Удалить", (dialog, which) -> {
            tetrominos.remove(currentTetromino);
            stopService(new Intent(this, PomodoroService.class));
            cancelNotification();
            currentTetromino = null;
            isFalling = true;
            if (tetrisView != null) {
                tetrisView.setTetrominos(tetrominos);
                tetrisView.setCurrentTetromino(currentTetromino);
                updateControlButtonsVisibility();
                tetrisView.invalidate();
            }
            dialog.dismiss();
        });

        builder.setNeutralButton("Завершить", (dialog, which) -> {
            completedTetrominos.add(currentTetromino);
            tetrominos.remove(currentTetromino);
            currentTetromino = null;
            isFalling = true;
            if (tetrisView != null) {
                tetrisView.setTetrominos(tetrominos);
                tetrisView.setCurrentTetromino(currentTetromino);
                updateControlButtonsVisibility();
                tetrisView.invalidate();
            }
            awardPoints();
            saveGameState();
            dialog.dismiss();
        });

        builder.setPositiveButton("ОК", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private boolean checkFullColumn() {
        return tetrisView != null && tetrisView.checkFullColumn();
    }

    private void awardPoints() {
        if (checkFullColumn()) {
            points += 1;
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(KEY_POINTS, points);
            editor.apply();
            showTimeManagementTip(); // Show tip with updated points
        }
    }

    public void rotateTetromino(View view) {
        if (currentTetromino == null) {
            Log.d("MainActivity", "Нет выбранного тетромино для поворота");
            return;
        }

        int newRotation = (currentTetromino.rotation - 1 + 4) % 4;
        int[] currentShape = currentTetromino.shape;

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
        int height = maxRow - minRow + 1;
        int width = maxCol - minCol + 1;

        boolean[][] matrix = new boolean[height][width];
        for (int index : currentShape) {
            int row = (index / WIDTH) - minRow;
            int col = (index % WIDTH) - minCol;
            matrix[row][col] = true;
        }

        int newHeight = width;
        int newWidth = height;
        boolean[][] rotatedMatrix = new boolean[newHeight][newWidth];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                rotatedMatrix[j][height - 1 - i] = matrix[i][j];
            }
        }

        int[] newShape = new int[height * width];
        int index = 0;
        for (int row = 0; row < newHeight; row++) {
            for (int col = 0; col < newWidth; col++) {
                if (rotatedMatrix[row][col]) {
                    newShape[index++] = row * WIDTH + col;
                }
            }
        }

        int newPosition = currentTetromino.position;

        int minNewRow = HEIGHT, maxNewRow = -1;
        for (int posIndex : newShape) {
            int pos = newPosition + posIndex;
            int row = pos / WIDTH;
            minNewRow = Math.min(minNewRow, row);
            maxNewRow = Math.max(maxNewRow, row);
        }

        if (minNewRow < 0) {
            newPosition += (-minNewRow) * WIDTH;
        } else if (maxNewRow >= HEIGHT) {
            newPosition -= (maxNewRow - (HEIGHT - 1)) * WIDTH;
        }

        newPosition = wrapPosition(newPosition);

        for (int posIndex : newShape) {
            int pos = wrapPosition(newPosition + posIndex);
            int row = pos / WIDTH;
            if (row < 0 || row >= HEIGHT) {
                Log.d("MainActivity", "Поворот невозможен: выходит за вертикальные границы на позиции " + pos);
                return;
            }
            if (isPositionOccupied(pos) && !isPartOfTetromino(currentTetromino, pos)) {
                Log.d("MainActivity", "Поворот невозможен: позиция " + pos + " занята");
                return;
            }
        }

        currentTetromino.rotation = newRotation;
        currentTetromino.shape = newShape;
        currentTetromino.position = newPosition;

        Log.d("MainActivity", "Поворот успешен: newRotation=" + newRotation + ", newPosition=" + newPosition);
        if (tetrisView != null) {
            tetrisView.invalidate();
        }

        if (tetrisView.checkFullColumn()) {
            showTimeManagementTip();
        }
    }

    private int wrapPosition(int position) {
        int row = position / WIDTH;
        int col = position % WIDTH;

        col = (col + WIDTH) % WIDTH;

        if (row < 0) row = 0;
        if (row >= HEIGHT) row = HEIGHT - 1;

        return row * WIDTH + col;
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

    public void moveLeft(View view) {
        if (currentTetromino == null) {
            Log.d("MainActivity", "Тетромино не выбрано для движения влево");
            Toast.makeText(this, "Сначала выберите тетромино", Toast.LENGTH_SHORT).show();
            return;
        }

        int newPosition = currentTetromino.position - 1;
        for (int index : currentTetromino.shape) {
            int pos = newPosition + index;
            int row = pos / WIDTH;
            int col = pos % WIDTH;
            if (col < 0) {
                newPosition = currentTetromino.position + (WIDTH - 1);
                pos = newPosition + index;
                col = pos % WIDTH;
            }
            if (row < 0 || row >= HEIGHT || col >= WIDTH) {
                Log.d("MainActivity", "Нельзя двигаться влево: выходит за границы на позиции " + pos);
                return;
            }
            if (isPositionOccupied(pos) && !isPartOfTetromino(currentTetromino, pos)) {
                Log.d("MainActivity", "Нельзя двигаться влево: позиция " + pos + " занята");
                return;
            }
        }

        currentTetromino.position = newPosition;
        if (tetrisView != null) {
            tetrisView.invalidate();
        }
        Log.d("MainActivity", "Движение влево: newPosition=" + newPosition);
    }

    public void moveRight(View view) {
        if (currentTetromino == null) {
            Log.d("MainActivity", "Тетромино не выбрано для движения вправо");
            Toast.makeText(this, "Сначала выберите тетромино", Toast.LENGTH_SHORT).show();
            return;
        }

        int newPosition = currentTetromino.position + 1;
        for (int index : currentTetromino.shape) {
            int pos = newPosition + index;
            int row = pos / WIDTH;
            int col = pos % WIDTH;
            if (col >= WIDTH) {
                newPosition = currentTetromino.position - (WIDTH - 1);
                pos = newPosition + index;
                col = pos % WIDTH;
            }
            if (row < 0 || row >= HEIGHT || col < 0) {
                Log.d("MainActivity", "Нельзя двигаться вправо: выходит за границы на позиции " + pos);
                return;
            }
            if (isPositionOccupied(pos) && !isPartOfTetromino(currentTetromino, pos)) {
                Log.d("MainActivity", "Нельзя двигаться вправо: позиция " + pos + " занята");
                return;
            }
        }

        currentTetromino.position = newPosition;
        if (tetrisView != null) {
            tetrisView.invalidate();
        }
        Log.d("MainActivity", "Движение вправо: newPosition=" + newPosition);
    }

    public void moveUp(View view) {
        if (currentTetromino == null) {
            Log.d("MainActivity", "Тетромино не выбрано для движения вверх");
            Toast.makeText(this, "Сначала выберите тетромино", Toast.LENGTH_SHORT).show();
            return;
        }

        int newPosition = currentTetromino.position - WIDTH;
        for (int index : currentTetromino.shape) {
            int pos = newPosition + index;
            int row = pos / WIDTH;
            int col = pos % WIDTH;
            if (row < 0) {
                Log.d("MainActivity", "Нельзя двигаться вверх: выходит за границы на позиции " + pos);
                return;
            }
            if (isPositionOccupied(pos) && !isPartOfTetromino(currentTetromino, pos)) {
                Log.d("MainActivity", "Нельзя двигаться вверх: позиция " + pos + " занята");
                return;
            }
        }

        currentTetromino.position = newPosition;
        if (tetrisView != null) {
            tetrisView.invalidate();
        }
        Log.d("MainActivity", "Движение вверх: newPosition=" + newPosition);
    }

    public void moveDown(View view) {
        if (currentTetromino == null) {
            Log.d("MainActivity", "Тетромино не выбрано для движения вниз");
            Toast.makeText(this, "Сначала выберите тетромино", Toast.LENGTH_SHORT).show();
            return;
        }

        int newPosition = currentTetromino.position + WIDTH;
        for (int index : currentTetromino.shape) {
            int pos = newPosition + index;
            int row = pos / WIDTH;
            int col = pos % WIDTH;
            if (row >= HEIGHT) {
                Log.d("MainActivity", "Нельзя двигаться вниз: выходит за границы на позиции " + pos);
                return;
            }
            if (isPositionOccupied(pos) && !isPartOfTetromino(currentTetromino, pos)) {
                Log.d("MainActivity", "Нельзя двигаться вниз: позиция " + pos + " занята");
                return;
            }
        }

        currentTetromino.position = newPosition;
        if (tetrisView != null) {
            tetrisView.invalidate();
        }
        Log.d("MainActivity", "Движение вниз: newPosition=" + newPosition);
    }

    private void moveTetrominoDown(Tetromino tetromino) {
        int newPosition = tetromino.position + WIDTH;
        for (int index : tetromino.shape) {
            int pos = newPosition + index;
            int row = pos / WIDTH;
            if (row >= HEIGHT) {
                Log.d("MainActivity", "Падение остановлено: нижняя граница");
                return;
            }
            if (isPositionOccupied(pos) && !isPartOfTetromino(tetromino, pos)) {
                Log.d("MainActivity", "Падение остановлено: позиция " + pos + " занята");
                return;
            }
        }
        tetromino.position = newPosition;
        if (tetrisView != null) {
            tetrisView.invalidate();
        }
        Log.d("MainActivity", "Тетромино упало: newPosition=" + newPosition);

        if (tetrisView.checkFullColumn()) {
            showTimeManagementTip();
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

    private void showTimeManagementTip() {
        if (isTipShownInLifecycle) {
            Log.d("MainActivity", "Совет уже показан в этом жизненном цикле, пропускаем");
            return;
        }

        Random random = new Random();
        int tipIndex = random.nextInt(timeManagementTips.length);
        String tip = timeManagementTips[tipIndex];

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        points = prefs.getInt(KEY_POINTS, 0);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Совет по тайм-менеджменту");
        builder.setPositiveButton("ОК", (dialog, which) -> dialog.dismiss());
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();

        isTipShownInLifecycle = true;
        Log.d("MainActivity", "Совет показан, флаг установлен: " + isTipShownInLifecycle);
    }
}