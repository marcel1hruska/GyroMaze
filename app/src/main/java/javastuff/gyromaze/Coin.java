package javastuff.gyromaze;

import android.content.Context;
import android.view.View;

/**
 * Collectible coin, increases highscore
 */
class Coin extends View {
    public int diameter;
    public float X;
    public float Y;

    public Coin(Context context, int diameter, float X, float Y) {
        super(context);
        this.diameter = diameter;
        this.X = X;
        this.Y = Y;
    }

    /**
     * Coin-Ball collision check
     * @param b Ball to check
     * @return if collision happened
     */
    public boolean collides(Ball b)
    {
        return Math.sqrt(Math.pow(X - b.X,2) + Math.pow(Y - b.Y,2)) < diameter + b.diameter;
    }
}
