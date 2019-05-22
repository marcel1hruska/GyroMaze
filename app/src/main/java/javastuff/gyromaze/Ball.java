package javastuff.gyromaze;

import android.content.Context;
import android.view.View;

/**
 * Moving ball in maze.
 * Recomputes its position according to the velocity taken from phone's accelerometer.
 */
class Ball extends View {
    /**
     * Diameter of ball
     */
    public int diameter;
    /**
     * Previous position
     */
    public float oldX, oldY;
    /**
     * Current position
     */
    public float X = 0, Y = 0;
    /**
     * Balls velocity
     */
    public float velX,velY;

    public Ball(Context context,int diameter) {
        super(context);
        this.diameter = diameter;
    }

    /**
     * Update ball's position and velocity
     * @param sensorX X value from accelerometer
     * @param sensorY Y value from accelerometer
     * @param delta Time delta
     */
    public void reposition(float sensorX, float sensorY, double delta) {
        //inverted
        float accelX = -sensorX*200;
        float accelY = sensorY*200;

        oldX = X;
        oldY = Y;

        X += velX * delta + accelX * delta * delta / 2;
        Y += velY * delta + accelY * delta * delta / 2;

        velX += accelX * delta;
        velY += accelY * delta;
    }

    /**
     * Resolves collisions with screen boundaries
     * @param screenWidth Width of screen
     * @param screenHeight Height of screen
     */
    public void boundaryCollisions(float screenWidth, float screenHeight) {
        if (X + diameter > screenWidth) {
            X = screenWidth - diameter;
            velX = 0;
        } else if (X - diameter < 0) {
            X = diameter;
            velX = 0;
        }
        if (Y + diameter > screenHeight) {
            Y = screenHeight - diameter;
            velY = 0;
        } else if (Y - diameter < 0) {
            Y = diameter;
            velY = 0;
        }
    }
}