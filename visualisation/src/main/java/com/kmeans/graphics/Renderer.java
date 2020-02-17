package com.kmeans.graphics;

import org.joml.*;

import java.lang.Math;

public class Renderer {
    private static final float FOV = (float) Math.toRadians(60.0f);
    private static final float Z_NEAR = 0.01f;
    private static final float Z_FAR = 10.f;

    private final Transformation transformation = new Transformation();

    private Canvas canvas;

    public Renderer(Canvas canvas) {
        this.canvas = canvas;
    }

    public void renderScene(Scene scene) {
        canvas.clear();
        Matrix4f projectionMatrix = transformation.getProjectionMatrix(FOV, canvas.getWidth(), canvas.getHeight(), Z_NEAR, Z_FAR);
        Matrix4f viewMatrix = transformation.getViewMatrix(scene.camera);
        Matrix4f viewProjectionMatrix = transformation.getViewProjectionMatrix(viewMatrix, projectionMatrix);

        for (Centroid centroid : scene.centroids) {
            Vector4f transformed = viewProjectionMatrix.transform(centroid.position, new Vector4f());
            transformed.set((((transformed.x / transformed.w + 1) / 2) * canvas.getWidth()), (((-transformed.y / transformed.w + 1) / 2) * canvas.getHeight()), transformed.z / transformed.w, 1.0f / transformed.w);
            centroid.screen = new Vector2i((int)transformed.x, (int)transformed.y);
            if(transformed.z > -1 && transformed.z < 1){
                for (int i = -4; i < 5; i++) {
                    for (int j = -4; j < 5; j++) {
                        canvas.setPixel(centroid.screen.x + i, centroid.screen.y + j, centroid.color, transformed.z);
                    }
                }
            }
        }
        for (Point point : scene.points) {
            Vector4f transformed = viewProjectionMatrix.transform(point.position, new Vector4f());
            transformed.set((((transformed.x / transformed.w + 1) / 2) * canvas.getWidth()), (((-transformed.y / transformed.w + 1) / 2) * canvas.getHeight()), transformed.z / transformed.w, 1.0f / transformed.w);
            point.screen = new Vector2i((int)transformed.x, (int)transformed.y);
            if(transformed.z > -1 && transformed.z < 1){
                for (int i = -1; i < 2; i++) {
                    for (int j = -1; j < 2; j++) {
                        canvas.setPixel(point.screen.x + i, point.screen.y + j, point.centroid.color, transformed.z);
                    }
                }            }
        }
        canvas.repaint();
    }
}