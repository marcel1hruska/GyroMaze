package javastuff.gyromaze;

import android.content.Context;
import android.view.View;

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

    public boolean collides(Ball b)
    {
        return Math.sqrt(Math.pow(X - b.X,2) + Math.pow(Y - b.Y,2)) < diameter;
    }
}
