package javastuff.gyromaze;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
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
 * Main game activity.
 * Holds Engine and necessary managers.
 */
public class Game extends AppCompatActivity {
    enum Side {top, bottom, right, left}

    //game engine
    private Engine engine;
    private SensorManager sensorMngr;
    private Display display;
    private AlertDialog pauseMenu;
    private AlertDialog gameOver;
    private MainMenu.Difficulty difficulty;
    private int highscore;
    /** 
     * Activity created
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sensorMngr = (SensorManager) getSystemService(SENSOR_SERVICE);
        display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        difficulty = MainMenu.Difficulty.values()[getIntent().getExtras().getInt("difficulty")];

        // initialize engine
        engine = new Engine(this);
        engine.setBackgroundResource(R.drawable.texture);
        setContentView(engine);

        // build pause menu
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(engine.getContext());
        // set dialog message
        alertDialogBuilder
                .setTitle("Paused")
                .setCancelable(false)
                .setPositiveButton("Continue",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                engine.unpause();
                            }
                        })
                .setNegativeButton("Back to Menu",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                end();
                            }
                        });

        // create alert dialog
        pauseMenu = alertDialogBuilder.create();

        alertDialogBuilder = new AlertDialog.Builder(engine.getContext());
        alertDialogBuilder
                .setTitle("Game Over")
                .setCancelable(false)
                .setNegativeButton("Back to Menu",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                end();
                            }
                        });
        gameOver = alertDialogBuilder.create();

        SharedPreferences prefs = getSharedPreferences("score_value_unique_key", Context.MODE_PRIVATE);
        highscore = prefs.getInt("score", 0);
    }

    /**
     * Activity resumed.
     * Keep paused on outside resume (display wake up).
     */
    @Override
    protected void onResume() {
        super.onResume();
    }

    /**
    * Activity paused
     */
    @Override
    protected void onPause() {
        super.onPause();
        // stop the simulation
        engine.pause();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus)
            fullscreen();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        pauseMenu.dismiss();
        gameOver.dismiss();
    }

    /**
     * Save highscore and finish activity
     */
    public void end()
    {
        if (highscore < engine.score)
        {
            // save new highscore
            SharedPreferences.Editor editor = getSharedPreferences("score_value_unique_key", Context.MODE_PRIVATE).edit();
            editor.putInt("score", engine.score);
            editor.commit();
        }
        finish();
    }

    private void fullscreen()
    {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    /**
     * Game engine.
     * Captures sensor's values, on each frame updates ball's position and resolves collisions with coins, holes, walls and boundaries.
     */
    class Engine extends FrameLayout implements SensorEventListener {
        /**
         * Number of cells
         */
        private final int cellsX, cellsY;
        /**
         * Game state
         */
        public boolean Paused = false;
        /**
         * Acceleration sensor
         */
        private final Sensor accel;
        /**
         * Captured acceleration values
         */
        private Vector sensor;
        /**
         * Screen resolution
         */
        private float screenWidth,screenHeight;
        /**
         * THE ball
         */
        private Ball ball;
        /**
         * Random maze generator
         */
        private MazeGenerator gen;
        /**
         * Existing walls
         */
        private Wall verticalWalls[][], horizontalWalls[][];
        /**
         * Game info deduced from current resolution
         */
        private final int horizontalWallHWidth,verticalWallHeight,wallSize,cellSize;
        private final float circleDiameter;
        /**
         * Collectible coins
         */
        private Coin coins[][];
        /**
         * Holes, game's over if hit
         */
        private Hole holes[][];
        /**
         * Last step time
         */
        private long lastTime = 0;
        /**
         * Current score
         */
        int score = 0;
        /**
         * Number of coins left
         */
        int coinCount = 0;
        /**
         * Difficulty multipliers
         */
        float holeSize,holeChance;
        int scoreMultiplier;

        @SuppressLint("ClickableViewAccessibility")
        /**
         * Initializes engine variables
         */
        public Engine(Context context) {
            super(context);
            this.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN && !Paused) {
                        pause();
                    }
                    return true;
                }
            });

            // initialize variables
            cellsX = 10;
            cellsY = 8;
            verticalWalls = new Wall[cellsX][cellsY-1];
            horizontalWalls= new Wall[cellsX-1][cellsY];
            coins = new Coin[cellsX][cellsY];
            holes = new Hole[cellsX][cellsY];
            accel = sensorMngr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensor = new Vector(0,0);

            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getRealMetrics(metrics);

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.RGB_565;

            // needed minimum size for cell so the ball can fit in
            cellSize = Math.min(metrics.widthPixels / 12, metrics.heightPixels / 12);

            //possible width of vertical wall and height of horizontal wall
            int verticalWallWidth = (metrics.widthPixels - cellsY * cellSize) / (cellsY - 1);
            int horizontalWallHeight = (metrics.heightPixels - cellsX * cellSize) / (cellsX - 1);
            // find min from them as unified "thickness" of the wall
            wallSize = Math.min(verticalWallWidth, horizontalWallHeight);

            // recompute height and width
            verticalWallHeight = (metrics.heightPixels - (cellsX - 1)*wallSize)/cellsX;
            horizontalWallHWidth = (metrics.widthPixels - (cellsY - 1)*wallSize)/cellsY;

            circleDiameter = Math.min(metrics.heightPixels / 25, metrics.widthPixels / 25) / 2;

            // set difficulty
            switch (difficulty)
            {
                case Easy:
                    holeSize = 1.0f;
                    holeChance = 0.1f;
                    scoreMultiplier = 1;
                    break;
                case Medium:
                    holeSize = 1.1f;
                    holeChance = 0.2f;
                    scoreMultiplier = 2;
                    break;
                case Hard:
                    holeSize = 1.2f;
                    holeChance = 0.3f;
                    scoreMultiplier = 3;
                    break;
            }

            //initialize walls, holes and coins
            initialize();
        }

        /**
         * (Re)Initializes walls, holes and coins
         */
        private void initialize()
        {
            // clear objects
            removeAllViews();
            // intialize holes
            for (int i = 0; i < holes.length; i++)
            {
                for (int j = 0; j < holes[i].length; j++)
                {
                    if (Math.random() >= holeChance || (i == 0 && j == 0))
                    {
                        holes[i][j] = null;
                        continue;
                    }

                    float dispX = (float)Math.random() * cellSize + 1 - cellSize/2;
                    float dispY = (float)Math.random() * cellSize + 1 - cellSize/2;

                    holes[i][j] = new Hole(getContext(),circleDiameter*holeSize,
                            j * (horizontalWallHWidth + wallSize) + horizontalWallHWidth/2f + dispX,
                            i * (verticalWallHeight + wallSize) + verticalWallHeight/2f + dispY);
                    Hole h = holes[i][j];
                    h.setBackgroundResource(R.drawable.hole);
                    h.setLayerType(LAYER_TYPE_HARDWARE, null);
                    addView(h, new ViewGroup.LayoutParams((int)h.diameter*2, (int)h.diameter*2));

                    h.setTranslationX(h.X - h.diameter);
                    h.setTranslationY(h.Y - h.diameter);
                }
            }

            // initialize coins
            for (int i = 0; i < coins.length; i++) {
                for (int j = 0; j < coins[i].length; j++) {
                    if (Math.random() < 0.5) {
                        coins[i][j] = null;
                        continue;
                    }

                    float dispX = (float) Math.random() * cellSize / 5 + 1 - cellSize / 10;
                    float dispY = (float) Math.random() * cellSize / 5 + 1 - cellSize / 10;

                    coins[i][j] = new Coin(getContext(),(int)circleDiameter,
                            j * (horizontalWallHWidth + wallSize) + horizontalWallHWidth / 2f + dispX,
                            i * (verticalWallHeight + wallSize) + verticalWallHeight / 2f + dispY);
                    Coin c = coins[i][j];
                    c.setBackgroundResource(R.drawable.coin);
                    c.setLayerType(LAYER_TYPE_HARDWARE, null);
                    addView(c, new ViewGroup.LayoutParams(c.diameter * 2, c.diameter * 2));

                    c.setTranslationX(c.X - c.diameter);
                    c.setTranslationY(c.Y - c.diameter);

                    coinCount++;
                }
            }

            // generate maze
            gen = new MazeGenerator(cellsX,cellsY);
            gen.Generate(difficulty);

            // initialize walls
            for (int i = 0; i < cellsX; i++) {
                for (int j = 0; j < cellsY; j++) {
                    // create wall according to the generator
                    if (j < cellsY - 1) {
                        if (!gen.verticalWalls[i][j]){
                            // addition so that the maze looks smooth
                            int add = (i > 0 && (verticalWalls[i - 1][j] != null || horizontalWalls[i - 1][j] != null)) ? 0 : wallSize;
                            // initialize
                            verticalWalls[i][j] = new Wall(getContext(),
                                    j * (horizontalWallHWidth + wallSize) + horizontalWallHWidth,
                                    i * (verticalWallHeight + wallSize) - add,
                                    wallSize,
                                    verticalWallHeight + wallSize + add);
                            verticalWalls[i][j].setBackgroundResource(R.drawable.wall);
                            verticalWalls[i][j].setLayerType(LAYER_TYPE_HARDWARE, null);
                            addView(verticalWalls[i][j], new ViewGroup.LayoutParams(wallSize, verticalWallHeight + wallSize + add));
                            // move the wall to its position
                            verticalWalls[i][j].setTranslationX(verticalWalls[i][j].x);
                            verticalWalls[i][j].setTranslationY(verticalWalls[i][j].y);
                        }
                        else
                            verticalWalls[i][j] = null;
                    }
                    if (i < cellsX - 1) {
                        if (!gen.horizontalWalls[i][j]) {
                            int add = (j < cellsY - 1 && verticalWalls[i][j] != null) ? 0 : wallSize;
                            horizontalWalls[i][j] = new Wall(getContext(),
                                    j * (horizontalWallHWidth + wallSize),
                                    i * (verticalWallHeight + wallSize) + verticalWallHeight,
                                    horizontalWallHWidth + add,
                                    wallSize);

                            horizontalWalls[i][j].setBackgroundResource(R.drawable.wall);
                            horizontalWalls[i][j].setLayerType(LAYER_TYPE_HARDWARE, null);
                            addView(horizontalWalls[i][j], new ViewGroup.LayoutParams(horizontalWallHWidth + add, wallSize));

                            horizontalWalls[i][j].setTranslationX(horizontalWalls[i][j].x);
                            horizontalWalls[i][j].setTranslationY(horizontalWalls[i][j].y);
                        }
                        else
                            horizontalWalls[i][j] = null;
                    }
                }
            }

            // initialize ball
            ball = new Ball(getContext(),(int)circleDiameter);
            ball.setBackgroundResource(R.drawable.ball);
            ball.setLayerType(LAYER_TYPE_HARDWARE, null);
            addView(ball, new ViewGroup.LayoutParams(ball.diameter*2,ball.diameter*2));
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
         * @return True if the wall is inner, false otherwise
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
         * @return True if they intersect, false otherwise
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
            // coins
            for (int i = 0; i < coins.length; i++) {
                for (int j = 0; j < coins[i].length; j++) {
                    if (coins[i][j] != null && coins[i][j].getVisibility() != View.GONE && coins[i][j].collides(ball)) {
                        // coin is spawned, still there and collides with ball, hide it and increase score/decrease coin count
                        coins[i][j].setVisibility(View.GONE);
                        score+=scoreMultiplier;
                        coinCount--;
                        if (coinCount == 0) {
                            initialize();
                            ball.X = ball.Y = 0;
                        }
                    }
                }
            }
            // holes
            for (int i = 0; i < holes.length; i++) {
                for (int j = 0; j < holes[i].length; j++) {
                    if (holes[i][j] != null && holes[i][j].collides(ball)) {
                        // collided with ball, hide the ball and say that game's over
                        ball.setVisibility(View.GONE);
                        gameOver.setMessage("Your score: " + score + "\n" +
                                            "Highscore: " + highscore);
                        gameOver.show();
                    }
                }
            }
            // resolve boundary collisions
            ball.boundaryCollisions(screenWidth, screenHeight);
        }

        /**
         * Start the game by registering sensor listener
         */
        public void unpause() {
            Paused = false;
            fullscreen();
            sensorMngr.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME);
        }

        /**
         * Stop the game
         */
        public void pause() {
            Paused = true;
            pauseMenu.setMessage("Current score: " + engine.score + "\n" +
                    "Highscore: " + highscore + "\n" +
                    "Difficulty: " + ((difficulty == MainMenu.Difficulty.Easy) ? "Easy" : (difficulty == MainMenu.Difficulty.Medium) ? "Medium" : "Hard"));
            pauseMenu.show();
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

        /**
         * Used for updating ball's position
         * @param canvas
         */
        @Override
        protected void onDraw(Canvas canvas) {
            // start the game
            if (lastTime == 0)
                engine.unpause();

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
