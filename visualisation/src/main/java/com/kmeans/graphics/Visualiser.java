package com.kmeans.graphics;

import javax.swing.*;
import java.awt.event.*;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;

public class Visualiser implements MouseMotionListener, KeyListener, MouseListener {

    private Renderer renderer;
    private Scene scene;
    private Canvas canvas;
    private Consumer<String> displayFPS = System.out::println;

    public Visualiser(int width, int height) {
        this.canvas = new Canvas(width, height);
        this.renderer = new Renderer(canvas);
    }

    public void setDisplayFPS(Consumer<String> displayFPS) {
        this.displayFPS = displayFPS;
    }

    public void setScene(Scene scene){
        this.scene = scene;
    }

    public void start(){
        int frames = 0;
        int updates = 0;
        long time = 0;
        long dt = 1_000_000_000 / 60;
        long currentTime = System.nanoTime();
        while(true){
            long newTime = System.nanoTime();
            long frameTime = newTime - currentTime;
            currentTime = newTime;
            while ( frameTime > 0 )
            {
                long deltaTime = Math.min(frameTime, dt);
                update(deltaTime / 1e9f);
                frameTime -= deltaTime;
                time += deltaTime;
                updates++;
            }
            try {
                SwingUtilities.invokeAndWait(this::render);
            } catch (InterruptedException | InvocationTargetException e) {
                e.printStackTrace();
            }
            frames++;
            if(time > 1e9){
                displayFPS.accept(frames + " fps, " + updates + " ups");
                time = 0;
                frames = 0;
                updates = 0;
            }
        }
    }

    private void render(){
        renderer.renderScene(scene);
    }

    private void update(float delta){
        if(up){
            scene.camera.move(0, 1 * delta, 0);
        }
        if(down){
            scene.camera.move(0, -1 * delta, 0);
        }
        if(left){
            scene.camera.move(-1 * delta, 0, 0);
        }
        if(right){
            scene.camera.move(1 * delta, 0, 0);
        }
        if(forward){
            scene.camera.move(0, 0, -1 * delta);
        }
        if(backward){
            scene.camera.move(0, 0, 1 * delta);
        }
        scene.camera.rotate(deltaY * delta * 20, 0, 0);
        scene.camera.rotate(0, deltaX * delta * 20, 0);
        deltaX = 0;
        deltaY = 0;
    }

    public JPanel getMainPanel() {
        return canvas;
    }

    private boolean up = false;
    private boolean down = false;
    private boolean left = false;
    private boolean right = false;
    private boolean forward = false;
    private boolean backward = false;

    private int mouseX;
    private int mouseY;
    private int deltaX;
    private int deltaY;

    @Override
    public void mouseDragged(MouseEvent mouseEvent) {
        int newMouseX = mouseEvent.getX();
        int newMouseY = mouseEvent.getY();
        deltaX = newMouseX - mouseX;
        deltaY = newMouseY - mouseY;
        mouseX = newMouseX;
        mouseY = newMouseY;
    }

    @Override
    public void mouseMoved(MouseEvent mouseEvent) {

    }

    @Override
    public void keyTyped(KeyEvent keyEvent) {

    }

    @Override
    public void keyPressed(KeyEvent keyEvent) {
        if(keyEvent.getKeyCode() == KeyEvent.VK_W){
            forward = true;
        }
        else if(keyEvent.getKeyCode() == KeyEvent.VK_S){
            backward = true;
        }
        else if(keyEvent.getKeyCode() == KeyEvent.VK_A){
            left = true;
        }
        else if(keyEvent.getKeyCode() == KeyEvent.VK_D){
            right = true;
        }
        else if(keyEvent.getKeyCode() == KeyEvent.VK_Q){
            down = true;
        }
        else if(keyEvent.getKeyCode() == KeyEvent.VK_E){
            up = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent keyEvent) {
        if(keyEvent.getKeyCode() == KeyEvent.VK_W){
            forward = false;
        }
        else if(keyEvent.getKeyCode() == KeyEvent.VK_S){
            backward = false;
        }
        else if(keyEvent.getKeyCode() == KeyEvent.VK_A){
            left = false;
        }
        else if(keyEvent.getKeyCode() == KeyEvent.VK_D){
            right = false;
        }
        else if(keyEvent.getKeyCode() == KeyEvent.VK_Q){
            down = false;
        }
        else if(keyEvent.getKeyCode() == KeyEvent.VK_E){
            up = false;
        }
    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent) {
        mouseX = mouseEvent.getX();
        mouseY = mouseEvent.getY();
    }

    @Override
    public void mousePressed(MouseEvent mouseEvent) {
        mouseX = mouseEvent.getX();
        mouseY = mouseEvent.getY();
    }

    @Override
    public void mouseReleased(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseEntered(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseExited(MouseEvent mouseEvent) {

    }
}