package com.example.itiger;

import java.io.Serializable;

public class Tetromino implements Serializable {
    public int position;
    public int[] shape;
    public int originalColor;
    public int typeIndex;
    public int rotation;
    public String title;
    public String description;
    public String category;
    public int difficulty;
    public int timeToComplete;

    public Tetromino(int position, int[] shape, int color, int typeIndex, int rotation,
                     String title, String description, String category, int difficulty, int timeToComplete) {
        this.position = position;
        this.shape = shape;
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