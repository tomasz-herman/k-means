package com.kmeans.graphics;

import javax.swing.*;
import java.awt.*;

public class Window extends JFrame {

    private static final int MINIMUM_WIDTH = 320;
    private static final int MINIMUM_HEIGHT = 240;

    private Visualiser visualiser;
    private String title;
    public Window(String title, int width, int height){
        this.title = title;
        visualiser = new Visualiser(width, height);
        setContentPane(visualiser.getMainPanel());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setTitle(title);
        setSize(new Dimension(width, height));
        setPreferredSize(new Dimension(width, height));
        setMinimumSize(new Dimension(MINIMUM_WIDTH, MINIMUM_HEIGHT));
        setResizable(true);
        setLocationRelativeTo(null);
        setVisible(true);
        getContentPane().setBackground(Color.BLACK);
        visualiser.setDisplayFPS(this::setText);
        addMouseListener(visualiser);
        addKeyListener(visualiser);
        addMouseMotionListener(visualiser);
    }

    public void setText(String text){
        setTitle(title + " " + text);
    }

    public void setScene(Scene scene){
        visualiser.setScene(scene);
    }

    public void start(){
        visualiser.start();
    }
}