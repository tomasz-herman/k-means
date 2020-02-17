package com.kmeans.graphics;

import org.joml.Vector2i;
import org.joml.Vector4f;

public class Centroid {

    public Centroid(int color, Vector4f position, Vector2i screen) {
        this.color = color;
        this.position = position;
        this.screen = screen;
    }

    public int color;
    public Vector4f position;
    public Vector2i screen;
}
