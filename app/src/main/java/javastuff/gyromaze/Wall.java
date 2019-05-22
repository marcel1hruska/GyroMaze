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
    public Wall(Context context, int x, int y, int width, int height) {
        super(context);
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
}