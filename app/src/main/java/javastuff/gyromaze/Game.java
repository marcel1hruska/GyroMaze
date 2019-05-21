package javastuff.gyromaze;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

/** 
 * Main Game Activity
 * Holds Engine and necessary managers
 */
public class Game extends AppCompatActivity {
    enum Side {top, bottom, right, left}

    //game engine
    private Engine engine;
    private SensorManager sensorMngr;
    private Display display;

    /** 
     * Activity created
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // make the app fullscreen
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        
        sensorMngr = (SensorManager) getSystemService(SENSOR_SERVICE);
        display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        // initialize engine
        engine = new Engine(this);
        engine.setBackgroundResource(R.drawable.steel);
        setContentView(engine);
    }

    /**
     * Activity resumed
     * Keep paused on outside resume (display wake up)
     */
    @Override
    protected void onResume() {
        super.onResume();
        // refresh fullscreen
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    /**
    * Activity paused
     */
    @Override
    protected void onPause() {
        super.onPause();
        // stop the simulation
        engine.stop();
        engine.Paused = true;
    }

    class Engine extends FrameLayout implements SensorEventListener {
        // number of cells
        private final int cellsX;
        private  final int cellsY;
        // game state
        public boolean Paused = false;
        // acceleration sensor
        private Sensor accel;
        // captured sensors values
        private Vector sensor;
        // screen resolution
        private float screenWidth;
        private float screenHeight;
        // ball in maze
        private final Ball ball;
        // random maze generator
        private MazeGenerator gen;
        // existing walls
        private Wall verticalWalls[][];
        private Wall horizontalWalls[][];
        // previous time
        private long lastTime = 0;

        public Engine(Context context) {
            super(context);

            this.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    Paused = !Paused;
                    if (Paused) stop();
                    else start();
                    return true;
                }
            });

            // initialize final variables
            cellsX = 10;
            cellsY = 8;
            verticalWalls = new Wall[cellsX][cellsY-1];
            horizontalWalls= new Wall[cellsX-1][cellsY];
            accel = sensorMngr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensor = new Vector(0,0);

            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getRealMetrics(metrics);

            // initialize ball
            ball = new Ball(getContext(),Math.min(metrics.heightPixels / 25, metrics.widthPixels / 25) / 2);
            ball.setBackgroundResource(R.drawable.ball);
            ball.setLayerType(LAYER_TYPE_HARDWARE, null);
            addView(ball, new ViewGroup.LayoutParams(ball.diameter*2,ball.diameter*2));

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.RGB_565;

            // needed minimum size for cell so the ball can fit in
            int cellSize = Math.min(metrics.widthPixels / 12, metrics.heightPixels / 12);

            //possible width of vertical wall and height of horizontal wall
            int verticalWallWidth = (metrics.widthPixels - cellsY * cellSize) / (cellsY - 1);
            int horizontalWallHeight = (metrics.heightPixels - cellsX * cellSize) / (cellsX - 1);
            // find min from them as unified "thickness" of the wall
            int wallSize = Math.min(verticalWallWidth, horizontalWallHeight);

            // recompute height and width
            int verticalWallHeight = (metrics.heightPixels - (cellsX - 1)*wallSize)/cellsX;
            int horizontalWallHWidth = (metrics.widthPixels - (cellsY - 1)*wallSize)/cellsY;

            // generate maze
            gen = new MazeGenerator(cellsX,cellsY);
            gen.Generate();

            // initialize walls
            for (int i = 0; i < cellsX; i++) {
                for (int j = 0; j < cellsY; j++) {
                    // create wall according to the generator
                    if (j < cellsY - 1 && !gen.verticalWalls[i][j]) {
                        // initialize
                        verticalWalls[i][j] = new Wall(getContext());
                        verticalWalls[i][j].setBackgroundResource(R.drawable.wall);
                        verticalWalls[i][j].setLayerType(LAYER_TYPE_HARDWARE, null);
                        // addition so that the maze looks smooth
                        int add = (i > 0 && (verticalWalls[i-1][j] != null || horizontalWalls[i-1][j] != null)) ? 0 : wallSize;
                        addView(verticalWalls[i][j], new ViewGroup.LayoutParams(wallSize, verticalWallHeight + wallSize + add));
                        // set attributes
                        verticalWalls[i][j].x = j * (horizontalWallHWidth + wallSize) + horizontalWallHWidth;
                        verticalWalls[i][j].y = i * (verticalWallHeight + wallSize) - add;
                        verticalWalls[i][j].width = wallSize;
                        verticalWalls[i][j].height = verticalWallHeight  + wallSize + add;
                        // move the wall to its position
                        verticalWalls[i][j].setTranslationX(verticalWalls[i][j].x);
                        verticalWalls[i][j].setTranslationY(verticalWalls[i][j].y);
                    }
                    if (i < cellsX - 1 && !gen.horizontalWalls[i][j]) {
                        horizontalWalls[i][j] = new Wall(getContext());
                        horizontalWalls[i][j].setBackgroundResource(R.drawable.wall);
                        horizontalWalls[i][j].setLayerType(LAYER_TYPE_HARDWARE, null);
                        int add = (j < cellsY - 1 && verticalWalls[i][j] != null) ? 0 : wallSize;
                        addView(horizontalWalls[i][j], new ViewGroup.LayoutParams(horizontalWallHWidth + add, wallSize));
                        horizontalWalls[i][j].x = j*(horizontalWallHWidth + wallSize);
                        horizontalWalls[i][j].y = i * (verticalWallHeight + wallSize) + verticalWallHeight;
                        horizontalWalls[i][j].width = horizontalWallHWidth + add;
                        horizontalWalls[i][j].height = wallSize;
                        horizontalWalls[i][j].setTranslationX(horizontalWalls[i][j].x);
                        horizontalWalls[i][j].setTranslationY(horizontalWalls[i][j].y);
                    }
                }
            }
        }

        private float clamp(float val, float min, float max) {
            return Math.max(min, Math.min(max, val));
        }

        private double distance(Vector v, Vector w) {
            return Math.pow(v.x - w.x,2) + Math.pow(v.y - w.y,2);
        }

        /**
         * Computes closest distance to segment from a point
         * @param P Point
         * @param A Segment start
         * @param B Segment end
         * @return Distance to the segment AB from point P
         */
        private double closestDistanceToSegment(Vector P, Vector A, Vector B) {
            double l2 = distance(A, B);
            if (l2 == 0) return distance(P, A);
            double t = ((P.x - A.x) * (B.x - A.x) + (P.y - A.y) * (B.y - A.y)) / l2;
            t = Math.max(0, Math.min(1, t));
            return Math.sqrt(distance(P, new Vector(A.x + (float)t * (B.x - A.x), A.y + (float)t * (B.y - A.y)) ));
        }

        /**
         * Decides whether the wall found is inner or outer wall
         * @param w Wall we want to check
         * @param s What side we want to check
         * @param second The second side that is to decide
         * @return is the wall inner
         */
        private boolean isWallCovered(Wall w, Side s, Side second)
        {
            for (int i = 0; i < verticalWalls.length; i++) {
                for (int j = 0; j < verticalWalls[i].length; j++) {
                    if (verticalWalls[i][j] != null && verticalWalls[i][j].x == w.x && verticalWalls[i][j].y == w.y)
                    {
                        if (i == 0 || (i == verticalWalls.length - 1)) return false;
                        // unique rules adjusted to the maze layout
                        switch (s) {
                            case top:
                                return (verticalWalls[i-1][j] != null || horizontalWalls[i-1][j] != null);
                            case bottom:
                                return verticalWalls[i+1][j] != null;
                            case left:
                                return horizontalWalls[i][j] != null && second == Side.bottom;
                            case right:
                                return horizontalWalls[i][j + 1] != null;
                        }
                    }
                }
            }
            for (int i = 0; i < horizontalWalls.length; i++) {
                for (int j = 0; j < horizontalWalls[i].length; j++) {
                    if (horizontalWalls[i][j] != null && horizontalWalls[i][j].x == w.x && horizontalWalls[i][j].y == w.y){
                        if (j == 0 || j == horizontalWalls[i].length - 1) return false;
                        switch (s) {
                            case top:
                                return false;
                            case bottom:
                                return verticalWalls[i+1][j] != null && second == Side.right;
                            case left:
                                return horizontalWalls[i][j-1] != null || verticalWalls[i][j-1] != null;
                            case right:
                                return horizontalWalls[i][j + 1] != null || verticalWalls[i][j] != null;
                        }
                    }
                }
            }
            return false;
        }

        /**
         * Returns side with which we collided
         * Computes shortest distance from the old ball's position, decides which side of the wall is the closest
         * Inner walls raise a problem as the should not be handled as real walls, but they exist and the shortest distance might point to them
         * In that case, we try to check whether the checked wall is hidden or not
         * @param w Wall that we intersected with
         * @return Side of intersection
         */
        private Side getCollisionSide(Wall w)
        {
            Vector ballPos = new Vector(ball.oldX,ball.oldY);

            // check one by one
            // left side
            Side closest = Side.left;
            // minimum distance to left side
            double min = closestDistanceToSegment(ballPos,new Vector(w.x,w.y), new Vector(w.x,w.y + w.height));
            // current positions of wall segment
            //right side
            Vector currentWallStart = new Vector(w.x + w.width,w.y);
            Vector currentWallEnd = new Vector(w.x + w.width,w.y + w.height);
            double dist = closestDistanceToSegment(ballPos, currentWallStart,currentWallEnd);
            if (dist == min)
                return (isWallCovered(w, Side.right, closest) ? closest : Side.right);
            if (dist < min)
            {
                min = dist;
                closest = Side.right;
            }
            // top side
            currentWallStart = new Vector(w.x,w.y);
            currentWallEnd = new Vector(w.x + w.width,w.y);
            dist =closestDistanceToSegment(ballPos, currentWallStart,currentWallEnd);
            if (dist == min)
                return (isWallCovered(w, Side.top, closest) ? closest : Side.top);
            if (dist < min)
            {
                min = dist;
                closest = Side.top;
            }
            // bottom side
            currentWallStart = new Vector(w.x,w.y + w.height);
            currentWallEnd = new Vector(w.x + w.width,w.y + w.height);
            dist = closestDistanceToSegment(ballPos, currentWallStart,currentWallEnd);
            if (dist == min)
                return (isWallCovered(w, Side.bottom, closest) ? closest : Side.bottom);
            if (dist < min)
                closest = Side.bottom;
            return closest;
        }

        /**
         * Checks whether the ball intersects with a wall
         * @param w Wall to check
         * @return Intersects or not
         */
        private boolean intersects(Wall w)
        {
            // closest point to the ball from the wall
            Vector closest = new Vector(clamp(ball.X, w.x, w.x + w.width),clamp(ball.Y, w.y, w.y + w.height));

            // distance from the circle to this point
            Vector dist = new Vector (ball.X - closest.x, ball.Y - closest.y);

            // check if the distance is smaller than ball's radius
            return Math.pow(dist.x,2) + Math.pow(dist.y,2) < (Math.pow(ball.diameter,2));
        }

        /**
         * Resolves ball collision with specific wall
         * @param w Wall to resolve
         */
        private void wallCollision(Wall w)
        {
            // no wall here
            if (w == null)
                return;
            // check for collision
            if (intersects(w)) {
                // there is a collision, find out from what side
                Side s = getCollisionSide(w);
                // update ball accordingly
                switch (s) {
                    case top:
                        ball.Y = w.y - ball.diameter - 1;
                        ball.velY = 0;
                        break;
                    case bottom:
                        ball.Y = w.y + w.height + ball.diameter + 1;
                        ball.velY = 0;
                        break;
                    case left:
                        ball.X = w.x - ball.diameter - 1;
                        ball.velX = 0;
                        break;
                    case right:
                        ball.X = w.x + w.width + ball.diameter + 1;
                        ball.velX = 0;
                        break;
                }
            }
        }

        /**
         * Updates ball and resolves collisions
         * @param sensorX X value from accelerometer
         * @param sensorY Y value from accelerometer
         * @param delta Time delta
         */
        private void update(float sensorX, float sensorY, double delta) {
            // update ball
            ball.reposition(sensorX, sensorY, delta);
            // check collisions with vertical walls after update
            for (int i = 0; i < verticalWalls.length; i++) {
                for (int j = 0; j < verticalWalls[i].length; j++) {
                    wallCollision(verticalWalls[i][j]);
                }
            }
            // horizontal walls
            for (int i = 0; i < horizontalWalls.length; i++) {
                for (int j = 0; j < horizontalWalls[i].length; j++) {
                    wallCollision(horizontalWalls[i][j]);
                }
            }
            // resolve boundary collisions
            ball.boundaryCollisions(screenWidth, screenHeight);
        }

        /**
         * Start the game by registering sensor listener
         */
        public void start() {
            sensorMngr.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME);
        }

        /**
         * Stop the game
         */
        public void stop() {
            sensorMngr.unregisterListener(this);
        }

        @Override
        protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
            screenWidth = width;
            screenHeight = height;
        }

        /**
         * Accelerometer changes
         * @param event change
         */
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
                return;
            switch (display.getRotation()) {
                case Surface.ROTATION_0:
                    sensor.x = event.values[0];
                    sensor.y = event.values[1];
                    break;
                case Surface.ROTATION_90:
                    sensor.x = -event.values[1];
                    sensor.y = event.values[0];
                    break;
                case Surface.ROTATION_180:
                    sensor.x = -event.values[0];
                    sensor.y = -event.values[1];
                    break;
                case Surface.ROTATION_270:
                    sensor.x = event.values[1];
                    sensor.y = -event.values[0];
                    break;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        protected void onDraw(Canvas canvas) {
            // update time delta
            long time = System.currentTimeMillis();
            float delta = (time - lastTime) / 1000.f;
            lastTime = time;

            // if game is running
            if (!Paused)
                update(sensor.x, sensor.y, delta);

            // move the ball
            ball.setTranslationX(ball.X - ball.diameter);
            ball.setTranslationY(ball.Y - ball.diameter);

            invalidate();
        }
    }
}
