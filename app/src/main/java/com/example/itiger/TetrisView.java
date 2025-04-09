package com.example.itiger;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

public class TetrisView extends View {

    private ArrayList<MainActivity.Tetromino> tetrominos;
    private MainActivity.Tetromino currentTetromino;
    private OnTetrominoSelectedListener listener;
    private final int WIDTH = 8;
    private final int HEIGHT = 8;
    private float cellSize;
    private Paint gridPaint;
    private Paint tetrominoPaint;
    private Paint textPaint;

    public TetrisView(Context context) {
        super(context);
        init();
    }

    public TetrisView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TetrisView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        gridPaint = new Paint();
        gridPaint.setColor(Color.GRAY);
        gridPaint.setStyle(Paint.Style.STROKE);

        tetrominoPaint = new Paint();
        tetrominoPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(30f);
        textPaint.setTextAlign(Paint.Align.CENTER);
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
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        cellSize = Math.min(w / (float) WIDTH, h / (float) HEIGHT);
        textPaint.setTextSize(cellSize * 0.5f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (int i = 0; i <= WIDTH; i++) {
            canvas.drawLine(i * cellSize, 0, i * cellSize, HEIGHT * cellSize, gridPaint);
        }
        for (int i = 0; i <= HEIGHT; i++) {
            canvas.drawLine(0, i * cellSize, WIDTH * cellSize, i * cellSize, gridPaint);
        }

        if (tetrominos != null) {
            for (MainActivity.Tetromino tetromino : tetrominos) {
                tetrominoPaint.setColor(tetromino.color);
                for (int index : tetromino.shape) {
                    int pos = tetromino.position + index;
                    int row = pos / WIDTH;
                    int col = pos % WIDTH;
                    if (row >= 0 && row < HEIGHT && col >= 0 && col < WIDTH) {
                        canvas.drawRect(
                                col * cellSize,
                                row * cellSize,
                                (col + 1) * cellSize,
                                (row + 1) * cellSize,
                                tetrominoPaint
                        );
                    }
                }
            }
        }

        drawHours(canvas);
    }

    private void drawHours(Canvas canvas) {
        int totalHours = 15; // 22 - 8 + 1
        int hoursPerColumn = totalHours / WIDTH;
        if (hoursPerColumn == 0) hoursPerColumn = 1;

        for (int col = 0; col < WIDTH; col++) {
            int hour = 8 + col * hoursPerColumn;
            if (hour > 22) hour = 22;
            float x = col * cellSize + cellSize / 2;
            float y = HEIGHT * cellSize + cellSize * 0.8f;
            canvas.drawText(String.valueOf(hour), x, y, textPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();
            int col = (int) (x / cellSize);
            int row = (int) (y / cellSize);
            int position = row * WIDTH + col;

            if (row >= 0 && row < HEIGHT && col >= 0 && col < WIDTH) {
                for (MainActivity.Tetromino tetromino : tetrominos) {
                    for (int index : tetromino.shape) {
                        if (tetromino.position + index == position) {
                            if (listener != null) {
                                listener.onTetrominoSelected(tetromino);
                            }
                            return true;
                        }
                    }
                }
            }
        }
        return super.onTouchEvent(event);
    }

    // Обновленный метод для проверки заполненного столбца
    public boolean checkFullColumn() {
        if (tetrominos == null) return false;

        boolean[] occupied = new boolean[WIDTH * HEIGHT];
        for (MainActivity.Tetromino tetromino : tetrominos) {
            for (int index : tetromino.shape) {
                int pos = tetromino.position + index;
                if (pos >= 0 && pos < WIDTH * HEIGHT) {
                    occupied[pos] = true;
                }
            }
        }

        // Проверяем каждый столбец
        for (int col = 0; col < WIDTH; col++) {
            boolean isFull = true;
            for (int row = 0; row < HEIGHT; row++) {
                if (!occupied[row * WIDTH + col]) {
                    isFull = false;
                    break;
                }
            }
            if (isFull) {
                return true;
            }
        }
        return false;
    }

    public interface OnTetrominoSelectedListener {
        void onTetrominoSelected(MainActivity.Tetromino tetromino);
    }
}