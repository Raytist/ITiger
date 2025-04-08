package com.example.itiger;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;

public class TetrisView extends View {

    private final int WIDTH = 8; // Ширина поля
    private final int HEIGHT = 8; // Высота поля
    private float cellSize; // Размер ячейки в пикселях
    private final Paint paint = new Paint();
    private ArrayList<MainActivity.Tetromino> tetrominos;
    private MainActivity.Tetromino currentTetromino;
    private OnTetrominoSelectedListener listener;

    public TetrisView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setFocusable(true);
    }

    public void setTetrominos(ArrayList<MainActivity.Tetromino> tetrominos) {
        this.tetrominos = tetrominos;
    }

    public void setCurrentTetromino(MainActivity.Tetromino currentTetromino) {
        this.currentTetromino = currentTetromino;
    }

    public void setOnTetrominoSelectedListener(OnTetrominoSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        int size = Math.min(width, height); // Делаем поле квадратным
        setMeasuredDimension(size, size);

        // Рассчитываем размер ячейки
        cellSize = (float) size / WIDTH;
        Log.d("TetrisView", "Field size: " + size + "x" + size + ", cellSize: " + cellSize);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Фон уже установлен в XML как белый (@color/white), поэтому не переопределяем его здесь

        // Рисуем сетку (для наглядности, более тёмный цвет на белом фоне)
        paint.setColor(Color.DKGRAY); // Тёмно-серый для контраста
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        for (int i = 0; i <= WIDTH; i++) {
            float x = i * cellSize;
            canvas.drawLine(x, 0, x, getHeight(), paint);
        }
        for (int i = 0; i <= HEIGHT; i++) {
            float y = i * cellSize;
            canvas.drawLine(0, y, getWidth(), y, paint);
        }

        // Рисуем все тетромино с небольшим отступом (padding) внутри ячеек
        if (tetrominos != null) {
            for (MainActivity.Tetromino tetromino : tetrominos) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(tetromino.color);
                for (int index : tetromino.shape) {
                    int pos = tetromino.position + index;
                    if (pos < 0 || pos >= WIDTH * HEIGHT) {
                        Log.d("TetrisView", "Cannot draw tetromino: position out of bounds at pos=" + pos);
                        continue; // Пропускаем отрисовку, если позиция вне поля
                    }
                    int row = pos / WIDTH;
                    int col = pos % WIDTH;
                    if (row >= 0 && row < HEIGHT && col >= 0 && col < WIDTH) {
                        float padding = 2; // Небольшой отступ внутри ячейки
                        float left = col * cellSize + padding;
                        float top = row * cellSize + padding;
                        float right = (col + 1) * cellSize - padding;
                        float bottom = (row + 1) * cellSize - padding;
                        canvas.drawRect(left, top, right, bottom, paint);
                    } else {
                        Log.d("TetrisView", "Cannot draw tetromino: out of bounds at row=" + row + ", col=" + col);
                    }
                }
            }
            Log.d("TetrisView", "Drawing " + tetrominos.size() + " tetrominos");
        } else {
            Log.d("TetrisView", "No tetrominos to draw");
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();
            int col = (int) (x / cellSize);
            int row = (int) (y / cellSize);
            if (row >= 0 && row < HEIGHT && col >= 0 && col < WIDTH) {
                int clickedPosition = row * WIDTH + col;
                for (MainActivity.Tetromino tetromino : tetrominos) {
                    for (int index : tetromino.shape) {
                        int tetrominoPos = tetromino.position + index;
                        if (tetrominoPos == clickedPosition) {
                            if (listener != null) {
                                listener.onTetrominoSelected(tetromino);
                            }
                            return true;
                        }
                    }
                }
            } else {
                Log.d("TetrisView", "Touch out of bounds: row=" + row + ", col=" + col);
            }
        }
        return super.onTouchEvent(event);
    }

    public interface OnTetrominoSelectedListener {
        void onTetrominoSelected(MainActivity.Tetromino tetromino);
    }
}