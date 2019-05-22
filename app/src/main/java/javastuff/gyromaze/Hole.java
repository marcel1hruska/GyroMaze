package javastuff.gyromaze;

import android.content.Context;
import android.view.View;

/**
 * Hole in the floor, game ends upon collision
 */
class Hole extends View {
    public float diameter;
    public float X;
    public float Y;

    public Hole(Context context, float diameter, float X, float Y) {
        super(context);
        this.diameter = diameter;
        this.X = X;
        this.Y = Y;
    }

    /**
     * Ball-Hole collision check.
     * Ball must be closer than the edge of the hole to fall in.
     * @param b Ball to check
     * @return True if collision happened, false otherwise
     */
    public boolean collides(Ball b)
    {
        return Math.sqrt(Math.pow(X - b.X,2) + Math.pow(Y - b.Y,2)) < diameter;
    }
}
