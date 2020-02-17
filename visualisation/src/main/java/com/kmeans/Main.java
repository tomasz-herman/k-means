package com.kmeans;

import com.kmeans.graphics.*;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector4f;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Random;

public class Main {

    public static int DEFAULT_HEIGHT = 720;
    public static int DEFAULT_WIDTH = 1280;
    private static JFileChooser chooser = new JFileChooser();
    private static File points;
    private static File centroids;

    public static void main(String[] args) {
        Scene scene = new Scene();
        readData(scene);
        Window window = new Window("K-means visualisation " + scene.points.size() + " points, " + scene.centroids.size() + " centroids, ", DEFAULT_WIDTH, DEFAULT_HEIGHT);
        window.pack();
        scene.camera = new Camera(new Vector3f(0.5f, 0.5f, 2), new Vector3f(0, 0f, 0));
        window.setScene(scene);
        window.start();
    }

    public static void readData(Scene scene){
        File workingDirectory = new File(System.getProperty("user.dir"));
        chooser.setCurrentDirectory(workingDirectory);
        chooser.setDialogTitle("Select points data");
        chooser.setCurrentDirectory(workingDirectory);
        int returnVal = chooser.showOpenDialog(null);
        if(returnVal == JFileChooser.APPROVE_OPTION){
            points = chooser.getSelectedFile();
        } else System.exit(0);
        chooser.setDialogTitle("Select centroids data");
        returnVal = chooser.showOpenDialog(null);
        if(returnVal == JFileChooser.APPROVE_OPTION){
            centroids = chooser.getSelectedFile();
        } else System.exit(0);
        Random random = new Random();
        try {
            var stream = Files.lines(centroids.toPath());
            stream.forEach(s -> {
                if(s.isEmpty())return;
                var tokens = s.split(" ");
                float x = Float.parseFloat(tokens[0]);
                float y = Float.parseFloat(tokens[1]);
                float z = Float.parseFloat(tokens[2]);
                scene.centroids.add(new Centroid(random.nextInt(), new Vector4f(x, y, z, 1), new Vector2i()));
            });
            stream = Files.lines(points.toPath());
            stream.forEach(s -> {
                if(s.isEmpty())return;
                var tokens = s.split(" ");
                float x = Float.parseFloat(tokens[0]);
                float y = Float.parseFloat(tokens[1]);
                float z = Float.parseFloat(tokens[2]);
                float minDist = Float.POSITIVE_INFINITY;
                Centroid closest = null;
                for (Centroid centroid : scene.centroids) {
                    float dist = centroid.position.distanceSquared(x, y, z, 0);
                    if(dist < minDist){
                        minDist = dist;
                        closest = centroid;
                    }
                }
                scene.points.add(new Point(new Vector4f(x, y, z, 1), closest, new Vector2i()));
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
