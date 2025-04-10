package com.example.itiger;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.view.GestureDetectorCompat;

import java.util.ArrayList;

public class TetrisView extends View {
    private int width = 8; // По умолчанию 8 столбцов
    private int height = 8; // По умолчанию 8 строк
    private float cellWidth;
    private float cellHeight;
    private Paint paint;
    private ArrayList<MainActivity.Tetromino> tetrominos = new ArrayList<>();
    private MainActivity.Tetromino currentTetromino;
    private boolean interactionEnabled = true;

    // Интерфейс для слушателя выбора тетромино
    public interface OnTetrominoSelectedListener {
        void onTetrominoSelected(MainActivity.Tetromino tetromino);
    }

    // Новый интерфейс для слушателя долгого нажатия
    public interface OnTetrominoLongPressListener {
        void onTetrominoLongPressed(MainActivity.Tetromino tetromino);
    }

    private OnTetrominoSelectedListener onTetrominoSelectedListener;
    private OnTetrominoLongPressListener onTetrominoLongPressListener;
    private GestureDetectorCompat gestureDetector;

    public TetrisView(Context context) {
        super(context);
        init(context);
    }

    public TetrisView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        paint = new Paint();
        updateCellDimensions();
        // Инициализация GestureDetector для обработки долгого нажатия
        gestureDetector = new GestureDetectorCompat(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent e) {
                handleLongPress(e);
            }
        });
    }

    public void setWidth(int newWidth) {
        this.width = newWidth;
        updateCellDimensions();
        invalidate();
    }

    public void setHeight(int newHeight) {
        this.height = newHeight;
        updateCellDimensions();
        invalidate();
    }

    public void setTetrominos(ArrayList<MainActivity.Tetromino> tetrominos) {
        this.tetrominos = tetrominos;
        invalidate();
    }

    public void setCurrentTetromino(MainActivity.Tetromino tetromino) {
        this.currentTetromino = tetromino;
        invalidate();
    }

    public void setInteractionEnabled(boolean enabled) {
        this.interactionEnabled = enabled;
    }

    public void setOnTetrominoSelectedListener(OnTetrominoSelectedListener listener) {
        this.onTetrominoSelectedListener = listener;
    }

    // Новый метод для установки слушателя долгого нажатия
    public void setOnTetrominoLongPressListener(OnTetrominoLongPressListener listener) {
        this.onTetrominoLongPressListener = listener;
    }

    private void updateCellDimensions() {
        if (getWidth() > 0 && getHeight() > 0) {
            cellWidth = (float) getWidth() / width;
            cellHeight = (float) getHeight() / height;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateCellDimensions();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Рисуем сетку
        paint.setColor(Color.LTGRAY);
        paint.setStyle(Paint.Style.STROKE);
        for (int i = 0; i <= width; i++) {
            canvas.drawLine(i * cellWidth, 0, i * cellWidth, getHeight(), paint);
        }
        for (int i = 0; i <= height; i++) {
            canvas.drawLine(0, i * cellHeight, getWidth(), i * cellHeight, paint);
        }

        // Рисуем все тетромино
        for (MainActivity.Tetromino tetromino : tetrominos) {
            paint.setColor(tetromino.color);
            paint.setStyle(Paint.Style.FILL);
            Log.d("TetrisView", "Рисуем тетромино: position=" + tetromino.position + ", shape=" + java.util.Arrays.toString(tetromino.shape));
            for (int index : tetromino.shape) {
                int pos = tetromino.position + index;
                int row = pos / width;
                int col = pos % width;
                Log.d("TetrisView", "  Ячейка: pos=" + pos + ", row=" + row + ", col=" + col);
                if (row >= 0 && row < height && col >= 0 && col < width) {
                    canvas.drawRect(col * cellWidth, row * cellHeight,
                            (col + 1) * cellWidth, (row + 1) * cellHeight, paint);
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!interactionEnabled) return super.onTouchEvent(event);

        // Передаём событие в GestureDetector для обработки долгого нажатия
        gestureDetector.onTouchEvent(event);

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();
            int col = (int) (x / cellWidth);
            int row = (int) (y / cellHeight);
            int position = row * width + col;
            Log.d("TetrisView", "Касание: x=" + x + ", y=" + y + ", row=" + row + ", col=" + col + ", position=" + position);

            for (MainActivity.Tetromino tetromino : tetrominos) {
                for (int index : tetromino.shape) {
                    int pos = tetromino.position + index;
                    Log.d("TetrisView", "Проверка тетромино: position=" + tetromino.position + ", index=" + index + ", pos=" + pos);
                    if (pos == position) {
                        Log.d("TetrisView", "Тетромино найдено: position=" + tetromino.position);
                        if (onTetrominoSelectedListener != null) {
                            onTetrominoSelectedListener.onTetrominoSelected(tetromino);
                            Log.d("TetrisView", "Слушатель вызван для тетромино");
                        }
                        return true;
                    }
                }
            }
            Log.d("TetrisView", "Тетромино не найдено на позиции " + position);
        }
        return super.onTouchEvent(event);
    }

    // Метод для обработки долгого нажатия
    private void handleLongPress(MotionEvent event) {
        if (!interactionEnabled) return;

        float x = event.getX();
        float y = event.getY();
        int col = (int) (x / cellWidth);
        int row = (int) (y / cellHeight);
        int position = row * width + col;
        Log.d("TetrisView", "Долгое нажатие: x=" + x + ", y=" + y + ", row=" + row + ", col=" + col + ", position=" + position);

        for (MainActivity.Tetromino tetromino : tetrominos) {
            for (int index : tetromino.shape) {
                int pos = tetromino.position + index;
                if (pos == position) {
                    Log.d("TetrisView", "Тетромино найдено для долгого нажатия: position=" + tetromino.position);
                    if (onTetrominoLongPressListener != null) {
                        onTetrominoLongPressListener.onTetrominoLongPressed(tetromino);
                        Log.d("TetrisView", "Слушатель долгого нажатия вызван для тетромино");
                    }
                    return;
                }
            }
        }
        Log.d("TetrisView", "Тетромино не найдено для долгого нажатия на позиции " + position);
    }

    // Метод из предыдущего исправления
    public boolean checkFullColumn() {
        for (int col = 0; col < width; col++) {
            boolean isColumnFull = true;
            for (int row = 0; row < height; row++) {
                int position = row * width + col;
                boolean isOccupied = false;
                for (MainActivity.Tetromino tetromino : tetrominos) {
                    for (int index : tetromino.shape) {
                        if (tetromino.position + index == position) {
                            isOccupied = true;
                            break;
                        }
                    }
                    if (isOccupied) break;
                }
                if (!isOccupied) {
                    isColumnFull = false;
                    break;
                }
            }
            if (isColumnFull) {
                Log.d("TetrisView", "Full column found at col=" + col);
                return true;
            }
        }
        Log.d("TetrisView", "No full columns found");
        return false;
    }
}