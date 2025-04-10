package com.example.itiger;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class WeekTetrisActivity extends AppCompatActivity {
    private AppCompatImageButton btnPreviousWeek;
    private AppCompatImageButton btnNextWeek;
    private TextView tvWeekPeriod;
    private TetrisView weekTetrisView;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private SimpleDateFormat displayDateFormat = new SimpleDateFormat("d MMM", Locale.getDefault());
    private String startOfWeekDate;
    private static final String PREFS_NAME = "TetrisPrefs";
    private static final int WIDTH = 7; // 7 дней в неделе
    private static final int HEIGHT = 8; // 8 временных слотов (с 8:00 до 22:00, по 2 часа)

    private ArrayList<MainActivity.Tetromino> weekTetrominos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_week_tetris);

        // Инициализация UI-компонентов
        btnPreviousWeek = findViewById(R.id.btnPreviousWeek);
        btnNextWeek = findViewById(R.id.btnNextWeek);
        tvWeekPeriod = findViewById(R.id.tvWeekPeriod);
        weekTetrisView = findViewById(R.id.weekTetrisView);

        if (weekTetrisView == null) {
            Log.e("WeekTetrisActivity", "WeekTetrisView not found in layout");
            Toast.makeText(this, "Ошибка: WeekTetrisView не найден", Toast.LENGTH_LONG).show();
            return;
        }

        // Устанавливаем размеры сетки для недельного Тетриса
        weekTetrisView.setWidth(7); // 7 столбцов
        weekTetrisView.setHeight(8); // 8 строк

        // Устанавливаем текущую неделю (на основе сегодняшней даты)
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        startOfWeekDate = dateFormat.format(calendar.getTime());

        // Устанавливаем слушатели для кнопок
        btnPreviousWeek.setOnClickListener(v -> navigateToAdjacentWeek(-1));
        btnNextWeek.setOnClickListener(v -> navigateToAdjacentWeek(1));

        // Отключаем взаимодействие с TetrisView
        weekTetrisView.setInteractionEnabled(false);

        // Добавляем поддержку долгого нажатия для отображения информации
// Line 69
        weekTetrisView.setOnTetrominoLongPressListener(tetromino -> {
            // Line 70
            Toast.makeText(this, tetromino.title + "\n" + tetromino.description, Toast.LENGTH_LONG).show();
        });

        // Загружаем данные для текущей недели
        loadWeekData();
        updateWeekDisplay();
    }

    private void navigateToAdjacentWeek(int weeksToAdd) {
        try {
            Date startDate = dateFormat.parse(startOfWeekDate);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(startDate);
            calendar.add(Calendar.WEEK_OF_YEAR, weeksToAdd);
            startOfWeekDate = dateFormat.format(calendar.getTime());
            loadWeekData();
            updateWeekDisplay();
        } catch (ParseException e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка при изменении недели", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateWeekDisplay() {
        try {
            Date startDate = dateFormat.parse(startOfWeekDate);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(startDate);
            String startFormatted = displayDateFormat.format(startDate);
            calendar.add(Calendar.DAY_OF_YEAR, 6); // Отображаем период для 7 дней
            String endFormatted = displayDateFormat.format(calendar.getTime());
            tvWeekPeriod.setText(String.format("%s - %s", startFormatted, endFormatted));
        } catch (ParseException e) {
            e.printStackTrace();
            tvWeekPeriod.setText("Ошибка отображения периода");
        }
    }

    private void loadWeekData() {
        weekTetrominos.clear();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        try {
            Date startDate = dateFormat.parse(startOfWeekDate);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(startDate);

            // Проходим по каждому дню недели (7 дней)
            for (int dayOfWeek = 0; dayOfWeek < 7; dayOfWeek++) {
                String currentDate = dateFormat.format(calendar.getTime());
                String datePrefix = currentDate + "_";

                // Загружаем тетромино для текущего дня
                int tetrominoCount = prefs.getInt(datePrefix + MainActivity.KEY_TETROMINO_COUNT, 0);
                for (int i = 0; i < tetrominoCount; i++) {
                    int position = prefs.getInt(datePrefix + MainActivity.KEY_TETROMINO_POSITION + i, 0);
                    int typeIndex = prefs.getInt(datePrefix + MainActivity.KEY_TETROMINO_TYPE + i, 0);
                    int rotation = prefs.getInt(datePrefix + MainActivity.KEY_TETROMINO_ROTATION + i, 0);
                    int color = prefs.getInt(datePrefix + MainActivity.KEY_TETROMINO_COLOR + i, 0);
                    String title = prefs.getString(datePrefix + MainActivity.KEY_TETROMINO_TITLE + i, "");
                    String description = prefs.getString(datePrefix + MainActivity.KEY_TETROMINO_DESCRIPTION + i, "");
                    String category = prefs.getString(datePrefix + MainActivity.KEY_TETROMINO_CATEGORY + i, "");
                    int difficulty = prefs.getInt(datePrefix + MainActivity.KEY_TETROMINO_DIFFICULTY + i, 1);
                    int timeToComplete = prefs.getInt(datePrefix + MainActivity.KEY_TETROMINO_TIME + i, 0);

                    int[] shape = MainActivity.generateShapeFromTimeAndDifficulty(timeToComplete, difficulty, rotation);
                    MainActivity.Tetromino tetromino = new MainActivity.Tetromino(position, shape, color, typeIndex, rotation,
                            title, description, category, difficulty, timeToComplete);

                    // Проверяем, есть ли блоки в последней строке (row = 7)
                    ArrayList<Integer> lastRowIndices = new ArrayList<>();
                    for (int index : tetromino.shape) {
                        int pos = tetromino.position + index;
                        int row = pos / 8; // WIDTH в MainActivity = 8
                        if (row == 7) { // Последняя строка
                            lastRowIndices.add(pos % 8); // Сохраняем колонку
                        }
                    }

                    if (!lastRowIndices.isEmpty()) {
                        // Поворачиваем на 90 градусов против часовой стрелки
                        int[] newShape = new int[lastRowIndices.size()];
                        int index = 0;
                        for (int col : lastRowIndices) {
                            int newRow = 7 - col; // Поворот: колонка 0 -> строка 7, колонка 7 -> строка 0
                            // Используем WIDTH = 7 для недельного Тетриса
                            newShape[index++] = newRow * 7 + dayOfWeek;
                        }

                        // Создаём новое тетромино для недельного Тетриса
                        MainActivity.Tetromino weekTetromino = new MainActivity.Tetromino(0, newShape, tetromino.color, tetromino.typeIndex, 0,
                                tetromino.title, tetromino.description, tetromino.category, tetromino.difficulty, tetromino.timeToComplete);
                        weekTetrominos.add(weekTetromino);
                    }
                }

                // Переходим к следующему дню
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }
        } catch (ParseException e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка загрузки данных недели", Toast.LENGTH_SHORT).show();
        }

        // Обновляем TetrisView
        weekTetrisView.setTetrominos(weekTetrominos);
        weekTetrisView.setCurrentTetromino(null);
        weekTetrisView.invalidate();
    }
}