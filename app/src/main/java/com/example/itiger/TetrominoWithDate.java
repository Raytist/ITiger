package com.example.itiger;

import java.io.Serializable;

public class TetrominoWithDate implements Serializable {
    public Tetromino tetromino;
    public String date;

    public TetrominoWithDate(Tetromino tetromino, String date) {
        this.tetromino = tetromino;
        this.date = date;
    }
}
