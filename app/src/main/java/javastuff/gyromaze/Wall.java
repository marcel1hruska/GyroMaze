package javastuff.gyromaze;

import android.content.Context;
import android.view.View;

/**
 * Wall obstacle
 * Forms Maze
 */
class Wall extends View {
    /**
     * Wall attributes
     */
    public int x, y, width, height;
    public Wall(Context context) { super(context); }
}