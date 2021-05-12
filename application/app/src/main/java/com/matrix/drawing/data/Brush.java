package com.matrix.drawing.data;


import android.graphics.Path;

public class Brush {
    public int color;
    public int brushWidth;
    public Path path;

    public Brush(int color, int brushWidth, Path path) {
        this.color = color;
        this.brushWidth = brushWidth;
        this.path = path;
    }
}