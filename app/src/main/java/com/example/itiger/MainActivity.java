package com.example.itiger;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private TetrisView tetrisView;
    private ArrayList<Tetromino> tetrominos = new ArrayList<>(); // Список всех фигур
    private Tetromino currentTetromino; // Текущая выбранная фигура
    private final int WIDTH = 8; // Ширина поля
    private final int HEIGHT = 8; // Высота поля
    private boolean isFalling = true; // Флаг для автоматического падения
    private final Handler fallHandler = new Handler(Looper.getMainLooper());
    private final long FALL_INTERVAL = 500; // Интервал падения (500 мс)

    // Кнопки управления
    private AppCompatImageButton buttonRotate;
    private AppCompatButton buttonUp;
    private AppCompatButton buttonDown;
    private AppCompatButton buttonLeft;
    private AppCompatButton buttonRight;

    // Описание всех тетромино (L, I, O, T, S, Z, J) с 4 поворотами
    private final int[][][] allTetrominos = {
            // L-образная
            {{0, WIDTH, WIDTH * 2, 1}, {WIDTH, WIDTH + 1, WIDTH + 2, WIDTH * 2 + 2}, {1, WIDTH + 1, WIDTH * 2 + 1, WIDTH * 2}, {WIDTH, WIDTH * 2, WIDTH * 2 + 1, WIDTH * 2 + 2}},
            // I-образная
            {{0, 1, 2, 3}, {0, WIDTH, WIDTH * 2, WIDTH * 3}, {0, 1, 2, 3}, {0, WIDTH, WIDTH * 2, WIDTH * 3}},
            // O-образная
            {{0, 1, WIDTH, WIDTH + 1}, {0, 1, WIDTH, WIDTH + 1}, {0, 1, WIDTH, WIDTH + 1}, {0, 1, WIDTH, WIDTH + 1}},
            // T-образная
            {{0, 1, 2, WIDTH + 1}, {1, WIDTH + 1, WIDTH * 2 + 1, WIDTH + 2}, {1, WIDTH, WIDTH + 1, WIDTH + 2}, {1, WIDTH + 1, WIDTH * 2 + 1, WIDTH}},
            // S-образная
            {{1, 2, WIDTH, WIDTH + 1}, {0, WIDTH, WIDTH + 1, WIDTH * 2 + 1}, {1, 2, WIDTH, WIDTH + 1}, {0, WIDTH, WIDTH + 1, WIDTH * 2 + 1}},
            // Z-образная
            {{0, 1, WIDTH + 1, WIDTH + 2}, {1, WIDTH + 1, WIDTH, WIDTH * 2}, {0, 1, WIDTH + 1, WIDTH + 2}, {1, WIDTH + 1, WIDTH, WIDTH * 2}},
            // J-образная
            {{0, WIDTH, WIDTH * 2, WIDTH * 2 - 1}, {WIDTH, WIDTH + 1, WIDTH + 2, 2}, {0, 1, WIDTH + 1, WIDTH * 2 + 1}, {WIDTH, WIDTH * 2, WIDTH * 2 + 1, WIDTH * 2 + 2}}
    };

    // Цвета для фигур
    private final int[] colors = {
            android.R.color.holo_blue_light,
            android.R.color.holo_red_light,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_purple,
            android.R.color.holo_blue_dark,
            android.R.color.holo_red_dark
    };

    // Индексы типов тетромино для определения формы
    private final int[] tetrominoTypes = {0, 1, 2, 3, 4, 5, 6}; // Соответствует L, I, O, T, S, Z, J

    // SharedPreferences для сохранения состояния
    private static final String PREFS_NAME = "TetrisPrefs";
    private static final String KEY_TETROMINO_COUNT = "TetrominoCount";
    private static final String KEY_CURRENT_TETROMINO_INDEX = "CurrentTetrominoIndex";
    private static final String KEY_TETROMINO_POSITION = "TetrominoPosition_";
    private static final String KEY_TETROMINO_TYPE = "TetrominoType_";
    private static final String KEY_TETROMINO_ROTATION = "TetrominoRotation_";
    private static final String KEY_TETROMINO_COLOR = "TetrominoColor_";

    // Цвет для выделения выбранной фигуры (инициализируем в onCreate)
    private int SELECTED_COLOR;

    // Runnable для автоматического падения
    private final Runnable fallRunnable = new Runnable() {
        @Override
        public void run() {
            if (isFalling) {
                // Применяем падение ко всем тетромино, которые не выбраны
                for (Tetromino tetromino : tetrominos) {
                    if (tetromino != currentTetromino) { // Пропускаем выбранное тетромино
                        moveTetrominoDown(tetromino);
                    }
                }
            }
            Log.d("Tetris", "Fall tick, isFalling=" + isFalling + ", tetrominos count=" + tetrominos.size() + ", currentTetromino=" + (currentTetromino != null ? currentTetromino.position : "null"));
            fallHandler.postDelayed(this, FALL_INTERVAL);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Инициализируем SELECTED_COLOR здесь, когда контекст уже доступен
        SELECTED_COLOR = ContextCompat.getColor(this, android.R.color.holo_orange_dark);

        // Инициализируем кнопки
        buttonRotate = findViewById(R.id.btnRotate);
        buttonUp = findViewById(R.id.btnUp);
        buttonDown = findViewById(R.id.btnDown);
        buttonLeft = findViewById(R.id.btnLeft);
        buttonRight = findViewById(R.id.btnRight);

        // Устанавливаем начальную видимость кнопок (скрыты, так как currentTetromino == null)
        updateControlButtonsVisibility();

        tetrisView = findViewById(R.id.tetrisView);
        tetrisView.setTetrominos(tetrominos);
        tetrisView.setCurrentTetromino(currentTetromino);
        tetrisView.setOnTetrominoSelectedListener(tetromino -> {
            // Если нажали на уже выбранный тетромино, снимаем выделение и возобновляем падение
            if (currentTetromino == tetromino) {
                currentTetromino.color = currentTetromino.originalColor; // Возвращаем исходный цвет
                currentTetromino = null; // Снимаем выделение
                isFalling = true; // Возобновляем падение
                Log.d("Tetris", "Deselected tetromino, falling resumed");
            } else {
                // Возвращаем исходный цвет предыдущей фигуре
                if (currentTetromino != null) {
                    currentTetromino.color = currentTetromino.originalColor;
                }
                // Устанавливаем новый текущий тетромино
                currentTetromino = tetromino;
                // Меняем цвет на выделенный
                currentTetromino.color = SELECTED_COLOR;
                isFalling = false; // Останавливаем падение при выборе фигуры
                Log.d("Tetris", "Selected tetromino at position: " + tetromino.position + ", falling stopped");
            }
            tetrisView.setCurrentTetromino(currentTetromino);
            updateControlButtonsVisibility(); // Обновляем видимость кнопок
            tetrisView.invalidate(); // Перерисовываем поле
        });

        // Восстанавливаем состояние
        restoreGameState();

        // Если нет сохранённого состояния, создаём новую фигуру
        if (tetrominos.isEmpty()) {
            createNewTetromino(null);
        }

        // Запускаем таймер падения
        fallHandler.post(fallRunnable);
        Log.d("Tetris", "Fall handler started");
    }

    // Метод для обновления видимости кнопок управления
    private void updateControlButtonsVisibility() {
        if (currentTetromino != null) {
            // Показываем кнопки, если тетромино выбрано
            buttonRotate.setVisibility(View.VISIBLE);
            buttonUp.setVisibility(View.VISIBLE);
            buttonDown.setVisibility(View.VISIBLE);
            buttonLeft.setVisibility(View.VISIBLE);
            buttonRight.setVisibility(View.VISIBLE);
            Log.d("Tetris", "Control buttons shown");
        } else {
            // Скрываем кнопки, если тетромино не выбрано
            buttonRotate.setVisibility(View.GONE);
            buttonUp.setVisibility(View.GONE);
            buttonDown.setVisibility(View.GONE);
            buttonLeft.setVisibility(View.GONE);
            buttonRight.setVisibility(View.GONE);
            Log.d("Tetris", "Control buttons hidden");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Сохраняем состояние игры при закрытии приложения
        saveGameState();
        // Останавливаем таймер падения
        fallHandler.removeCallbacks(fallRunnable);
        Log.d("Tetris", "Fall handler stopped onPause");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Возобновляем таймер падения
        fallHandler.post(fallRunnable);
        Log.d("Tetris", "Fall handler resumed onResume");
    }

    // Сохранение состояния игры
    private void saveGameState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Сохраняем количество тетромино
        editor.putInt(KEY_TETROMINO_COUNT, tetrominos.size());

        // Сохраняем индекс текущей фигуры
        int currentTetrominoIndex = -1;
        if (currentTetromino != null) {
            currentTetrominoIndex = tetrominos.indexOf(currentTetromino);
        }
        editor.putInt(KEY_CURRENT_TETROMINO_INDEX, currentTetrominoIndex);

        // Сохраняем данные каждого тетромино
        for (int i = 0; i < tetrominos.size(); i++) {
            Tetromino tetromino = tetrominos.get(i);
            editor.putInt(KEY_TETROMINO_POSITION + i, tetromino.position);
            editor.putInt(KEY_TETROMINO_TYPE + i, tetromino.typeIndex);
            editor.putInt(KEY_TETROMINO_ROTATION + i, tetromino.rotation);
            editor.putInt(KEY_TETROMINO_COLOR + i, tetromino.originalColor); // Сохраняем исходный цвет
        }

        editor.apply();
        Log.d("Tetris", "Game state saved: " + tetrominos.size() + " tetrominos, current index: " + currentTetrominoIndex);
    }

    // Восстановление состояния игры
    private void restoreGameState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Получаем количество тетромино
        int tetrominoCount = prefs.getInt(KEY_TETROMINO_COUNT, 0);
        if (tetrominoCount == 0) {
            Log.d("Tetris", "No saved game state found");
            return;
        }

        // Восстанавливаем тетромино
        tetrominos.clear();
        for (int i = 0; i < tetrominoCount; i++) {
            int position = prefs.getInt(KEY_TETROMINO_POSITION + i, 0);
            int typeIndex = prefs.getInt(KEY_TETROMINO_TYPE + i, 0);
            int rotation = prefs.getInt(KEY_TETROMINO_ROTATION + i, 0);
            int color = prefs.getInt(KEY_TETROMINO_COLOR + i, ContextCompat.getColor(this, colors[0]));

            int[] shape = allTetrominos[typeIndex][rotation];
            Tetromino tetromino = new Tetromino(position, shape, color, typeIndex, rotation);
            tetrominos.add(tetromino);
        }

        // Восстанавливаем текущую фигуру
        int currentTetrominoIndex = prefs.getInt(KEY_CURRENT_TETROMINO_INDEX, -1);
        if (currentTetrominoIndex >= 0 && currentTetrominoIndex < tetrominos.size()) {
            currentTetromino = tetrominos.get(currentTetrominoIndex);
            currentTetromino.color = SELECTED_COLOR; // Выделяем текущую фигуру
        } else {
            currentTetromino = null;
        }

        // Обновляем TetrisView
        tetrisView.setTetrominos(tetrominos);
        tetrisView.setCurrentTetromino(currentTetromino);
        updateControlButtonsVisibility(); // Обновляем видимость кнопок после восстановления
        tetrisView.invalidate();

        Log.d("Tetris", "Game state restored: " + tetrominoCount + " tetrominos, current index: " + currentTetrominoIndex);
    }

    // Класс для хранения информации о фигуре
    static class Tetromino {
        int position; // Позиция фигуры (индекс в одномерном массиве)
        int[] shape;  // Форма фигуры (индексы относительно позиции)
        int color;    // Текущий цвет фигуры
        int originalColor; // Исходный цвет фигуры
        int typeIndex; // Индекс типа тетромино (L, I, O, T, S, Z, J)
        int rotation; // Текущий поворот (0-3)

        Tetromino(int position, int[] shape, int color, int typeIndex, int rotation) {
            this.position = position;
            this.shape = shape;
            this.color = color;
            this.originalColor = color; // Сохраняем исходный цвет
            this.typeIndex = typeIndex;
            this.rotation = rotation;
        }
    }

    // Метод для корректировки позиции, чтобы тетромино не выходило за границы
    private int adjustPositionToBounds(int newPosition, int[] shape) {
        int minCol = WIDTH, maxCol = -1;
        int minRow = HEIGHT, maxRow = -1;

        // Находим минимальные и максимальные row и col для текущей формы
        for (int index : shape) {
            int pos = newPosition + index;
            int row = pos / WIDTH;
            int col = pos % WIDTH;
            minCol = Math.min(minCol, col);
            maxCol = Math.max(maxCol, col);
            minRow = Math.min(minRow, row);
            maxRow = Math.max(maxRow, row);
        }

        // Корректируем позицию, если фигура выходит за границы
        if (minCol < 0) {
            newPosition += (0 - minCol); // Сдвигаем вправо
        } else if (maxCol >= WIDTH) {
            newPosition -= (maxCol - (WIDTH - 1)); // Сдвигаем влево
        }

        if (minRow < 0) {
            newPosition += (0 - minRow) * WIDTH; // Сдвигаем вниз
        } else if (maxRow >= HEIGHT) {
            newPosition -= (maxRow - (HEIGHT - 1)) * WIDTH; // Сдвигаем вверх
        }

        return newPosition;
    }

    // Создание новой фигуры
    public void createNewTetromino(View view) {
        // Выбираем случайную фигуру
        Random random = new Random();
        int tetrominoIndex = random.nextInt(tetrominoTypes.length);
        int typeIndex = tetrominoTypes[tetrominoIndex];
        int rotation = random.nextInt(4);
        int[] shape = allTetrominos[typeIndex][rotation];
        int color = ContextCompat.getColor(this, colors[typeIndex]);

        // Начальная позиция в середине первой строки
        int startPosition = WIDTH / 2; // Середина первой строки (для 8x8 это 4)

        // Корректируем начальную позицию, чтобы фигура не выходила за границы
        startPosition = adjustPositionToBounds(startPosition, shape);

        // Проверяем, можно ли разместить фигуру
        for (int index : shape) {
            int pos = startPosition + index;
            if (isPositionOccupied(pos)) {
                Log.d("Tetris", "Cannot place tetromino: position occupied at " + pos);
                return; // Нельзя разместить фигуру, место занято
            }
        }

        // Создаем новую фигуру
        Tetromino newTetromino = new Tetromino(startPosition, shape, color, typeIndex, rotation);
        tetrominos.add(newTetromino);
        // Возвращаем исходный цвет предыдущей фигуре
        if (currentTetromino != null) {
            currentTetromino.color = currentTetromino.originalColor;
        }
        currentTetromino = null; // Новая фигура не выбрана по умолчанию
        isFalling = true; // Возобновляем падение для всех фигур
        tetrisView.setTetrominos(tetrominos);
        tetrisView.setCurrentTetromino(currentTetromino);
        updateControlButtonsVisibility(); // Обновляем видимость кнопок
        tetrisView.invalidate(); // Перерисовываем поле
        Log.d("Tetris", "New tetromino created at position: " + startPosition + ", isFalling=" + isFalling);
    }

    // Поворот фигуры
    public void rotateTetromino(View view) {
        if (currentTetromino == null) return;

        // Вычисляем новый поворот (против часовой стрелки)
        int newRotation = (currentTetromino.rotation - 1 + 4) % 4;
        int[] newShape = allTetrominos[currentTetromino.typeIndex][newRotation];
        int newPosition = currentTetromino.position;

        // Корректируем позицию, чтобы фигура не выходила за границы
        newPosition = adjustPositionToBounds(newPosition, newShape);

        // Проверяем, можно ли повернуть фигуру с новой позицией
        for (int index : newShape) {
            int pos = newPosition + index;
            if (isPositionOccupied(pos) && !isPartOfTetromino(currentTetromino, pos)) {
                Log.d("Tetris", "Cannot rotate tetromino: position occupied at " + pos);
                return; // Поворот приведёт к пересечению с другой фигурой
            }
        }

        // Применяем поворот
        currentTetromino.rotation = newRotation;
        currentTetromino.shape = newShape;
        currentTetromino.position = newPosition;
        tetrisView.invalidate(); // Перерисовываем поле
        Log.d("Tetris", "Tetromino rotated to rotation: " + newRotation + ", new position: " + newPosition);
    }

    // Проверка, занята ли позиция
    private boolean isPositionOccupied(int position) {
        if (position < 0 || position >= WIDTH * HEIGHT) {
            Log.d("Tetris", "Position out of bounds in isPositionOccupied: " + position);
            return true; // Считаем позицию занятой, если она вне поля
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

    // Движение вниз для конкретного тетромино
    private void moveTetrominoDown(Tetromino tetromino) {
        if (canMoveDown(tetromino)) {
            tetromino.position += WIDTH;
            tetrisView.invalidate(); // Перерисовываем поле
            Log.d("Tetris", "Tetromino moved down to position: " + tetromino.position);
        } else {
            Log.d("Tetris", "Tetromino cannot move down: blocked at position " + tetromino.position);
        }
    }

    // Движение вниз (для кнопки)
    public void moveDown(View view) {
        if (currentTetromino == null) return;
        moveTetrominoDown(currentTetromino);
    }

    // Движение вверх
    public void moveUp(View view) {
        if (currentTetromino == null) return;
        if (canMoveUp(currentTetromino)) {
            currentTetromino.position -= WIDTH;
            tetrisView.invalidate(); // Перерисовываем поле
            Log.d("Tetris", "Moved up to position: " + currentTetromino.position);
        } else {
            Log.d("Tetris", "Cannot move up: blocked");
        }
    }

    // Движение влево
    public void moveLeft(View view) {
        if (currentTetromino == null) return;
        if (canMoveLeft(currentTetromino)) {
            currentTetromino.position--;
            // Корректируем позицию, чтобы не выйти за границы
            currentTetromino.position = adjustPositionToBounds(currentTetromino.position, currentTetromino.shape);
            tetrisView.invalidate(); // Перерисовываем поле
            Log.d("Tetris", "Moved left to position: " + currentTetromino.position);
        } else {
            Log.d("Tetris", "Cannot move left: blocked");
        }
    }

    // Движение вправо
    public void moveRight(View view) {
        if (currentTetromino == null) return;
        if (canMoveRight(currentTetromino)) {
            currentTetromino.position++;
            // Корректируем позицию, чтобы не выйти за границы
            currentTetromino.position = adjustPositionToBounds(currentTetromino.position, currentTetromino.shape);
            tetrisView.invalidate(); // Перерисовываем поле
            Log.d("Tetris", "Moved right to position: " + currentTetromino.position);
        } else {
            Log.d("Tetris", "Cannot move right: blocked");
        }
    }

    // Проверка возможности движения вверх
    private boolean canMoveUp(Tetromino tetromino) {
        for (int index : tetromino.shape) {
            int newPos = tetromino.position + index - WIDTH;
            if (newPos < 0) {
                Log.d("Tetris", "Cannot move up: position out of bounds at newPos=" + newPos);
                return false; // Позиция выходит за верхнюю границу
            }
            if (isPositionOccupied(newPos) && !isPartOfTetromino(tetromino, newPos)) {
                return false;
            }
        }
        return true;
    }

    // Проверка возможности движения вниз
    private boolean canMoveDown(Tetromino tetromino) {
        for (int index : tetromino.shape) {
            int newPos = tetromino.position + index + WIDTH;
            if (newPos >= WIDTH * HEIGHT) {
                Log.d("Tetris", "Cannot move down: position out of bounds at newPos=" + newPos);
                return false; // Позиция выходит за нижнюю границу
            }
            if (isPositionOccupied(newPos) && !isPartOfTetromino(tetromino, newPos)) {
                return false;
            }
        }
        return true;
    }

    // Проверка возможности движения влево
    private boolean canMoveLeft(Tetromino tetromino) {
        for (int index : tetromino.shape) {
            int newPos = tetromino.position + index - 1;
            int row = newPos / WIDTH;
            int col = newPos % WIDTH;
            if (col < 0) {
                Log.d("Tetris", "Cannot move left: out of bounds at col=" + col);
                return false; // Выход за левую границу
            }
            if (isPositionOccupied(newPos) && !isPartOfTetromino(tetromino, newPos)) {
                return false;
            }
        }
        return true;
    }

    // Проверка возможности движения вправо
    private boolean canMoveRight(Tetromino tetromino) {
        for (int index : tetromino.shape) {
            int newPos = tetromino.position + index + 1;
            int row = newPos / WIDTH;
            int col = newPos % WIDTH;
            if (col >= WIDTH) {
                Log.d("Tetris", "Cannot move right: out of bounds at col=" + col);
                return false; // Выход за правую границу
            }
            if (isPositionOccupied(newPos) && !isPartOfTetromino(tetromino, newPos)) {
                return false;
            }
        }
        return true;
    }

    // Проверка, является ли позиция частью текущей фигуры
    private boolean isPartOfTetromino(Tetromino tetromino, int position) {
        for (int index : tetromino.shape) {
            if (tetromino.position + index == position) {
                return true;
            }
        }
        return false;
    }
}