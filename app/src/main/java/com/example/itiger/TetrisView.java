package com.example.itiger;

import android.annotation.SuppressLint;
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

    private ArrayList<MainActivity.Tetromino> tetrominos;
    private MainActivity.Tetromino currentTetromino;
    private OnTetrominoSelectedListener listener;
    private final int GRID_WIDTH = 8;
    private final int GRID_HEIGHT = 8;
    private float cellSize;
    private Paint cellPaint;
    private Paint gridPaint;

    public TetrisView(Context context) {
        super(context);
        init();
    }

    public TetrisView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @SuppressLint("ResourceAsColor")
    private void init() {
        cellPaint = new Paint();
        gridPaint = new Paint();
        gridPaint.setColor(R.color.ic_launcher_background);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(2f);
    }

    public void setTetrominos(ArrayList<MainActivity.Tetromino> tetrominos) {
        this.tetrominos = tetrominos;
        invalidate();
    }

    public void setCurrentTetromino(MainActivity.Tetromino currentTetromino) {
        this.currentTetromino = currentTetromino;
        invalidate();
    }

    public void setOnTetrominoSelectedListener(OnTetrominoSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        cellSize = Math.min(w / (float) GRID_WIDTH, h / (float) GRID_HEIGHT);
        Log.d("TetrisView", "Размер ячейки изменён: " + cellSize);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Рисуем сетку
        for (int i = 0; i <= GRID_WIDTH; i++) {
            float x = i * cellSize;
            canvas.drawLine(x, 0, x, GRID_HEIGHT * cellSize, gridPaint);
        }
        for (int i = 0; i <= GRID_HEIGHT; i++) {
            float y = i * cellSize;
            canvas.drawLine(0, y, GRID_WIDTH * cellSize, y, gridPaint);
        }

        // Рисуем тетромино
        if (tetrominos != null) {
            for (MainActivity.Tetromino tetromino : tetrominos) {
                cellPaint.setColor(tetromino.color);
                for (int index : tetromino.shape) {
                    int pos = tetromino.position + index;
                    int row = pos / GRID_WIDTH;
                    int col = pos % GRID_WIDTH;
                    float left = col * cellSize;
                    float top = row * cellSize;
                    canvas.drawRect(left, top, left + cellSize, top + cellSize, cellPaint);
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();

            int col = (int) (x / cellSize);
            int row = (int) (y / cellSize);
            int position = row * GRID_WIDTH + col;

            // Проверяем, попал ли клик в один из тетромино
            if (tetrominos != null) {
                for (MainActivity.Tetromino tetromino : tetrominos) {
                    for (int index : tetromino.shape) {
                        if (tetromino.position + index == position) {
                            if (listener != null) {
                                listener.onTetrominoSelected(tetromino);
                                Log.d("TetrisView", "Клик по тетромино на позиции: " + position);
                            }
                            return true;
                        }
                    }
                }
            }
            Log.d("TetrisView", "Клик вне тетромино на позиции: " + position);
        }
        return super.onTouchEvent(event);
    }

    public interface OnTetrominoSelectedListener {
        void onTetrominoSelected(MainActivity.Tetromino tetromino);
    }
}