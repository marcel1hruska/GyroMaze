package javastuff.gyromaze;

import java.util.ArrayList;
import java.util.Random;
import java.util.Stack;

/**
 * Generates random rectangular maze
 */
public class MazeGenerator {

    private Random random;
    private int width;
    private int height;

    //visited cells are marked true
    private boolean[][] cells;

    //removed walls are marked true
    public boolean[][] horizontalWalls;
    public boolean[][] verticalWalls;

    public MazeGenerator(int w, int h) {
        random = new Random();
        width = w;
        height = h;
        cells = new boolean[w][h];
        horizontalWalls = new boolean[w][h];
        verticalWalls = new boolean[w][h];
    }

    /**
     * Generates random maze
     */
    public void Generate() {
        Cell current = new Cell(random.nextInt(width), random.nextInt(height));
        cells[current.X][current.Y] = true;
        Stack<Cell> stack = new Stack<>();
        for (int visited = 1; visited < width * height; ++visited) {
            ArrayList<Cell> adj = current.AdjacentCells();
            if (adj.size() > 0) {
                Cell n = adj.get(random.nextInt(adj.size()));
                stack.push(current);
                removeWall(n, current);
                current = n;
                cells[current.X][current.Y] = true;
            } else {
                current = stack.pop();
                --visited;
            }
        }
    }

    /**
     * removes wall between given cells
     *
     * @param c
     * @param c2
     */
    private void removeWall(Cell c, Cell c2) {
        if (c.Y == c2.Y)
            horizontalWalls[Math.min(c.X, c2.X)][c.Y] = true;
        if (c.X == c2.X)
            verticalWalls[c.X][Math.min(c.Y, c2.Y)] = true;
    }

    /**
     * @param c
     * @param c2
     * @return True if wall is not present between given cells
     */
    public boolean IsWallRemoved(Cell c, Cell c2) {
        if (c.Y == c2.Y)
            return horizontalWalls[Math.min(c.X, c2.X)][c.Y];
        if (c.X == c2.X)
            return verticalWalls[c.X][Math.min(c.Y, c2.Y)];
        return false;
    }

    /**
     * Prints maze to the console
     */
    public void Print() {
        for (int x = 0; x <= 2 * width; x++)
            System.out.print("_");
        System.out.println();
        for (int y = 0; y < height; y++) {
            System.out.print("|");
            for (int x = 0; x < width; x++) {
                if (verticalWalls[x][y])
                    System.out.print(" ");
                else
                    System.out.print("_");
                if (horizontalWalls[x][y])
                    System.out.print("_");
                else
                    System.out.print("|");
            }
            System.out.println();
        }
    }

    public class Cell {
        public int X, Y;

        public Cell(int x, int y) {
            X = x;
            Y = y;
        }

        /**
         * @return list of adjacent cells
         */
        public ArrayList<Cell> AdjacentCells() {
            ArrayList<Cell> c = new ArrayList<>();
            if (X > 0 && !cells[X - 1][Y])
                c.add(new Cell(X - 1, Y));
            if (X < width - 1 && !cells[X + 1][Y])
                c.add(new Cell(X + 1, Y));
            if (Y > 0 && !cells[X][Y - 1])
                c.add(new Cell(X, Y - 1));
            if (Y < height - 1 && !cells[X][Y + 1])
                c.add(new Cell(X, Y + 1));
            return c;
        }
    }
}
