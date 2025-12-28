package com.educaflow.common.pdf;

/**
 *
 * @author logongas
 */
public class Rectangulo {


    private final float x;
    private final float y;
    private final float width;
    private final float height;

    public Rectangulo(float x, float y, float width, float height) {
        this.x=x;
        this.y=y;
        this.width=width;
        this.height=height;
    }

    /**
     * @return the x
     */
    public float getX() {
        return x;
    }

    /**
     * @return the y
     */
    public float getY() {
        return y;
    }

    /**
     * @return the width
     */
    public float getWidth() {
        return width;
    }

    /**
     * @return the height
     */
    public float getHeight() {
        return height;
    }
    
    
}
