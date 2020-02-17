package com.kmeans.graphics;

import org.joml.Vector2i;
import org.joml.Vector4f;

public class Point {
    public Vector4f position;
    public Centroid centroid;
    public Vector2i screen;

    public Point(Vector4f position, Centroid centroid, Vector2i screen) {
        this.position = position;
        this.centroid = centroid;
        this.screen = screen;
    }
}
