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
    private ArrayList<MainActivity.Tetromino> tetrominos = new ArrayList<>();
    private MainActivity.Tetromino currentTetromino;
    private int width = 8; // По умолчанию 8, но для недельного Тетриса будет 7
    private int height = 8;
    private Paint paint;
    private OnTetrominoSelectedListener listener;
    private OnTetrominoLongPressListener longPressListener;
    private boolean interactionEnabled = true;
    private float cellWidth;
    private float cellHeight;

    public TetrisView(Context context) {
        super(context);
        init();
    }

    public TetrisView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
    }

    public void setInteractionEnabled(boolean enabled) {
        this.interactionEnabled = enabled;
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

    public void setTetrominos(ArrayList<MainActivity.Tetromino> tetrominos) {
        this.tetrominos = tetrominos;
    }

    public void setCurrentTetromino(MainActivity.Tetromino tetromino) {
        this.currentTetromino = tetromino;
    }

    public void setOnTetrominoSelectedListener(OnTetrominoSelectedListener listener) {
        this.listener = listener;
    }

    public void setOnTetrominoLongPressListener(OnTetrominoLongPressListener listener) {
        this.longPressListener = listener;
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

        // Рисуем тетромино
        for (MainActivity.Tetromino tetromino : tetrominos) {
            paint.setColor(tetromino.color);
            paint.setStyle(Paint.Style.FILL);
            for (int index : tetromino.shape) {
                int row = index / width;
                int col = index % width;
                canvas.drawRect(col * cellWidth, row * cellHeight,
                        (col + 1) * cellWidth, (row + 1) * cellHeight, paint);
            }
        }

        // Рисуем текущий (выбранный) тетромино, если он есть
        if (currentTetromino != null) {
            paint.setColor(currentTetromino.color);
            paint.setStyle(Paint.Style.FILL);
            for (int index : currentTetromino.shape) {
                int row = index / width;
                int col = index % width;
                canvas.drawRect(col * cellWidth, row * cellHeight,
                        (col + 1) * cellWidth, (row + 1) * cellHeight, paint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            int col = (int) (event.getX() / cellWidth);
            int row = (int) (event.getY() / cellHeight);
            int position = row * width + col;

            for (MainActivity.Tetromino tetromino : tetrominos) {
                for (int index : tetromino.shape) {
                    if (index == position) {
                        if (!interactionEnabled && longPressListener != null) {
                            longPressListener.onTetrominoLongPressed(tetromino);
                            return true;
                        }
                        if (interactionEnabled && listener != null) {
                            listener.onTetrominoSelected(tetromino);
                            return true;
                        }
                    }
                }
            }
        }
        return super.onTouchEvent(event);
    }

    public boolean checkFullColumn() {
        for (int col = 0; col < width; col++) {
            boolean isFull = true;
            for (int row = 0; row < height; row++) {
                int pos = row * width + col;
                boolean occupied = false;
                for (MainActivity.Tetromino tetromino : tetrominos) {
                    for (int index : tetromino.shape) {
                        if (index == pos) {
                            occupied = true;
                            break;
                        }
                    }
                    if (occupied) break;
                }
                if (!occupied) {
                    isFull = false;
                    break;
                }
            }
            if (isFull) return true;
        }
        return false;
    }

    public interface OnTetrominoSelectedListener {
        void onTetrominoSelected(MainActivity.Tetromino tetromino);
    }

    public interface OnTetrominoLongPressListener {
        void onTetrominoLongPressed(MainActivity.Tetromino tetromino);
    }
}