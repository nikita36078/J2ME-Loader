package javax.microedition.lcdui.game;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/**
 *
 * @author Andres Navarro
 */


// Synchronization is important because of two things:
// first setStaticTileSet can change the whole Object
// so any function could be running at the same time as a setStaticTileSet
// and have inconsistent behaviour
// second is the animated tiles, their indexes should be consecutive and
// two simultaneus createAnimatedTile methods could blow that up.
// One would expect only one thread accessing this class at the same time
// but it becomes a little tricky when you consider that repaints access this
// class and you have no control or knowledge of the repaint thread on most
// systems

public class TiledLayer extends Layer {
    private final int rows, cols;
    // package access for collision detection
    Image img;

    private int tileHeight, tileWidth, numStaticTiles;


    // the matrix for storing the tiles
    private int [][]tiles;

    // the list of anmated tiles
    // NOTE the first animatedTile (index -1) goes
    // into the first position in the array (index 0)
    // so to access the correct tile use animatedTiles[-n-1]
    int []animatedTiles;
    // the ammount of animated tiles
    int numAnimatedTiles;

    public TiledLayer(int cols, int rows, Image img, int tileWidth, int tileHeight) {
        // the specification doesn't states if the TiledLayer is visible on creation
        // we assume it is
        super(0, 0, cols * tileWidth, rows * tileHeight, true);

        if (img == null)
            throw new NullPointerException();
        if (cols <= 0 || rows <= 0 || tileHeight <= 0 || tileWidth <= 0)
            throw new IllegalArgumentException();
        if (img.getWidth() % tileWidth != 0 || img.getHeight() % tileHeight != 0)
            throw new IllegalArgumentException();

        this.img = img;
        this.cols = cols;
        this.rows = rows;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.numStaticTiles = (img.getWidth() / tileWidth) * (img.getHeight() / tileHeight);
        this.tiles = new int[rows][cols];
        this.animatedTiles = new int[5];
        this.numAnimatedTiles = 0;
    }

    // it is synchronized to avoid problems with the animatedTiles array and count
    public int createAnimatedTile(int staticTileIndex) {
        synchronized (this) {
            if (staticTileIndex < 0 || staticTileIndex > numStaticTiles)
                throw new IndexOutOfBoundsException();

            if (numAnimatedTiles == animatedTiles.length) {
                int [] temp = new int [numAnimatedTiles + 6];
                System.arraycopy(animatedTiles, 0, temp, 0, numAnimatedTiles);
                animatedTiles = temp;
            }

            animatedTiles[numAnimatedTiles] = staticTileIndex;
            numAnimatedTiles++;
            return -numAnimatedTiles;
        }
    }

    public int getAnimatedTile(int index) {
        synchronized (this) {
            index = -index-1;
            if (index < 0 || index >= numAnimatedTiles)
                throw new IndexOutOfBoundsException();
            return animatedTiles[index];
        }
    }

    public void setAnimatedTile(int index, int staticTileIndex) {
        synchronized (this) {
            index = -index-1;
            if (index < 0 || index >= numAnimatedTiles)
                throw new IndexOutOfBoundsException();
            if (staticTileIndex < 0 || staticTileIndex > numStaticTiles)
                throw new IndexOutOfBoundsException();

            animatedTiles[index] = staticTileIndex;
        }
    }

    public int getCell(int col, int row) {
        return this.tiles[row][col];
    }

    public void setCell(int col, int row, int index) {
        synchronized (this) {
            if (-index-1 >= numAnimatedTiles || index > numStaticTiles)
                throw new IndexOutOfBoundsException();
            tiles[row][col] = index;
        }
    }

    public void setStaticTileSet(Image image, int tileWidth, int tileHeight) {
        synchronized (this) {
            if (img == null)
                throw new NullPointerException();
            if (tileHeight <= 0 || tileWidth <= 0)
                throw new IllegalArgumentException();
            if (img.getWidth() % tileWidth != 0 || img.getHeight() % tileHeight != 0)
                throw new IllegalArgumentException();

            int newNumStaticTiles = (img.getWidth() / getCellWidth()) *
                    (img.getHeight() / getCellHeight());


            // recalculate size
            int w = cols * tileWidth;
            int h = rows * tileHeight;

            setSize(w, h);

            this.img = img;
            this.tileWidth = tileWidth;
            this.tileHeight = tileHeight;

            if (newNumStaticTiles >= numStaticTiles) {
                this.numStaticTiles = newNumStaticTiles;
                return;
            }
            // if there are less static tiles
            // all animated tiles are discarded and
            // the tiledLayer is filled with tiles with index 0

            this.numStaticTiles = newNumStaticTiles;
            this.animatedTiles = new int[5];
            this.numAnimatedTiles = 0;
            this.fillCells(0, 0, getColumns(), getRows(), 0);
        }
    }

    public void fillCells(int col, int row, int numCols, int numRows, int index) {
        synchronized (this) {
            if (numCols < 0 || numRows < 0)
                throw new IllegalArgumentException();
            if (row < 0 || col < 0 || col + numCols > this.cols || row + numRows > this.rows)
                throw new IndexOutOfBoundsException();
            if (-index-1 >= numAnimatedTiles || index > numStaticTiles)
                throw new IndexOutOfBoundsException();

            int rMax = row + numRows;
            int cMax = col + numCols;
            for (int r = row; r < rMax; r++) {
                for (int c = col; c < cMax; c++) {
                    tiles[r][c] = index;
                }
            }
        }
    }

    // dont need for synch here as columns are a constant
    // after creation
    public final int getColumns() {
        return cols;
    }

    // dont need for synch here as rows are a constant
    // after creation
    public final int getRows() {
        return rows;
    }

    public final int getCellWidth() {
        return tileWidth;
    }

    public final int getCellHeight() {
        return tileHeight;
    }

    public final void paint(Graphics g) {
        synchronized (this) {
            if (!this.isVisible())
                return;

            int x = getX();
            int y = getY();

            int c0 = 0;
            int r0 = 0;
            int cMax = getColumns();
            int rMax = getRows();

            int tW = getCellWidth();
            int tH = getCellHeight();

            int cX = g.getClipX();
            int cY = g.getClipY();
            int cW = g.getClipWidth();
            int cH = g.getClipHeight();

            // take out the columns and rows that are outside of
            // the clip area, this should speed things up a bit
            int x0 = x;
            int anchor = Graphics.LEFT | Graphics.TOP;

            int imgCols = img.getWidth() / tW;
            int imgRows = img.getHeight() / tH;

            for (int r = r0; r < rMax; r++, y += tH) {
                x = x0;
                for (int c = c0; c < cMax; c++, x += tW) {
                    int tile = getCell(c, r);
                    if (tile < 0)
                        tile = getAnimatedTile(tile);
                    if (tile == 0)
                        continue;

                    tile--;

                    int xSrc = tW * (tile % imgCols);
                    int ySrc = (tile / imgCols) * tH;

                    g.drawRegion(img, xSrc, ySrc, tW, tH, Sprite.TRANS_NONE, x, y, anchor);
                }
            }
        }
    }
}
