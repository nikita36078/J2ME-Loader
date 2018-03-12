/*
 * Copyright 2018 Nikita Shakarun
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package javax.microedition.lcdui.game;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class TiledLayer extends Layer {
	private int cellHeight;
	private int cellWidth;
	private int rows;
	private int columns;
	private int[][] cellMatrix;

	Image sourceImage;
	private int numberOfTiles;
	int[] tileSetX;
	int[] tileSetY;
	private int[] anim_to_static;
	private int numOfAnimTiles;

	public TiledLayer(int columns, int rows, Image image, int tileWidth,
					  int tileHeight) {
		super(columns < 1 || tileWidth < 1 ? -1 : columns * tileWidth,
				rows < 1 || tileHeight < 1 ? -1 : rows * tileHeight);

		if (((image.getWidth() % tileWidth) != 0) ||
				((image.getHeight() % tileHeight) != 0)) {
			throw new IllegalArgumentException();
		}
		this.columns = columns;
		this.rows = rows;

		cellMatrix = new int[rows][columns];

		int noOfFrames = (image.getWidth() / tileWidth) * (image.getHeight() / tileHeight);
		createStaticSet(image, noOfFrames + 1, tileWidth, tileHeight, true);
	}

	public int createAnimatedTile(int staticTileIndex) {
		// checks static tile
		if (staticTileIndex < 0 || staticTileIndex >= numberOfTiles) {
			throw new IndexOutOfBoundsException();
		}

		if (anim_to_static == null) {
			anim_to_static = new int[4];
			numOfAnimTiles = 1;
		} else if (numOfAnimTiles == anim_to_static.length) {
			// grow anim_to_static table if needed
			int new_anim_tbl[] = new int[anim_to_static.length * 2];
			System.arraycopy(anim_to_static, 0,
					new_anim_tbl, 0, anim_to_static.length);
			anim_to_static = new_anim_tbl;
		}
		anim_to_static[numOfAnimTiles] = staticTileIndex;
		numOfAnimTiles++;
		return (-(numOfAnimTiles - 1));
	}

	public void setAnimatedTile(int animatedTileIndex, int staticTileIndex) {
		// checks static tile
		if (staticTileIndex < 0 || staticTileIndex >= numberOfTiles) {
			throw new IndexOutOfBoundsException();
		}
		// do animated tile index check
		animatedTileIndex = -animatedTileIndex;
		if (anim_to_static == null || animatedTileIndex <= 0
				|| animatedTileIndex >= numOfAnimTiles) {
			throw new IndexOutOfBoundsException();
		}

		anim_to_static[animatedTileIndex] = staticTileIndex;

	}

	public int getAnimatedTile(int animatedTileIndex) {
		animatedTileIndex = -animatedTileIndex;
		if (anim_to_static == null || animatedTileIndex <= 0
				|| animatedTileIndex >= numOfAnimTiles) {
			throw new IndexOutOfBoundsException();
		}

		return anim_to_static[animatedTileIndex];
	}

	public void setCell(int col, int row, int tileIndex) {
		if (col < 0 || col >= this.columns || row < 0 || row >= this.rows) {
			throw new IndexOutOfBoundsException();
		}

		if (tileIndex > 0) {
			// do checks for static tile
			if (tileIndex >= numberOfTiles) {
				throw new IndexOutOfBoundsException();
			}
		} else if (tileIndex < 0) {
			// do animated tile index check
			if (anim_to_static == null ||
					(-tileIndex) >= numOfAnimTiles) {
				throw new IndexOutOfBoundsException();
			}
		}

		cellMatrix[row][col] = tileIndex;

	}

	public int getCell(int col, int row) {
		if (col < 0 || col >= this.columns || row < 0 || row >= this.rows) {
			throw new IndexOutOfBoundsException();
		}
		return cellMatrix[row][col];
	}

	public void fillCells(int col, int row, int numCols, int numRows, int tileIndex) {
		if (numCols < 0 || numRows < 0) {
			throw new IllegalArgumentException();
		}

		if (col < 0 || col >= this.columns || row < 0 || row >= this.rows ||
				col + numCols > this.columns || row + numRows > this.rows) {
			throw new IndexOutOfBoundsException();
		}

		if (tileIndex > 0) {
			// do checks for static tile
			if (tileIndex >= numberOfTiles) {
				throw new IndexOutOfBoundsException();
			}
		} else if (tileIndex < 0) {
			// do animated tile index check
			if (anim_to_static == null ||
					(-tileIndex) >= numOfAnimTiles) {
				throw new IndexOutOfBoundsException();
			}
		}

		for (int rowCount = row; rowCount < row + numRows; rowCount++) {
			for (int columnCount = col;
				 columnCount < col + numCols; columnCount++) {
				cellMatrix[rowCount][columnCount] = tileIndex;
			}
		}
	}

	public final int getCellWidth() {
		return cellWidth;
	}

	public final int getCellHeight() {
		return cellHeight;
	}

	public final int getColumns() {
		return columns;
	}

	public final int getRows() {
		return rows;
	}

	public void setStaticTileSet(Image image, int tileWidth, int tileHeight) {
		// if img is null img.getWidth() will throw NullPointerException
		if (tileWidth < 1 || tileHeight < 1 ||
				((image.getWidth() % tileWidth) != 0) ||
				((image.getHeight() % tileHeight) != 0)) {
			throw new IllegalArgumentException();
		}
		setWidthImpl(columns * tileWidth);
		setHeightImpl(rows * tileHeight);

		int noOfFrames =
				(image.getWidth() / tileWidth) * (image.getHeight() / tileHeight);

		// the zero th index is left empty for transparent tile
		// so it is passed in  createStaticSet as noOfFrames + 1

		if (noOfFrames >= (numberOfTiles - 1)) {
			// maintain static indices
			createStaticSet(image, noOfFrames + 1, tileWidth, tileHeight, true);
		} else {
			createStaticSet(image, noOfFrames + 1, tileWidth,
					tileHeight, false);
		}
	}

	@Override
	public final void paint(Graphics g) {
		if (g == null) {
			throw new NullPointerException();
		}

		if (visible) {
			int startColumn = 0;
			int endColumn = this.columns;
			int startRow = 0;
			int endRow = this.rows;

			// calculate the number of columns left of the clip
			int number = (g.getClipX() - this.x) / cellWidth;
			if (number > 0) {
				startColumn = number;
			}

			// calculate the number of columns right of the clip
			int endX = this.x + (this.columns * cellWidth);
			int endClipX = g.getClipX() + g.getClipWidth();
			number = (endX - endClipX) / cellWidth;
			if (number > 0) {
				endColumn -= number;
			}

			// calculate the number of rows above the clip
			number = (g.getClipY() - this.y) / cellHeight;
			if (number > 0) {
				startRow = number;
			}

			// calculate the number of rows below the clip
			int endY = this.y + (this.rows * cellHeight);
			int endClipY = g.getClipY() + g.getClipHeight();
			number = (endY - endClipY) / cellHeight;
			if (number > 0) {
				endRow -= number;
			}

			// paint all visible cells
			int tileIndex = 0;

			// y-coordinate
			int ty = this.y + (startRow * cellHeight);
			for (int row = startRow;
				 row < endRow; row++, ty += cellHeight) {

				// reset the x-coordinate at the beginning of every row
				// x-coordinate to draw tile into
				int tx = this.x + (startColumn * cellWidth);
				for (int column = startColumn; column < endColumn;
					 column++, tx += cellWidth) {

					tileIndex = cellMatrix[row][column];
					// check the indices
					// if animated get the corresponding
					// static index from anim_to_static table
					if (tileIndex == 0) { // transparent tile
						continue;
					} else if (tileIndex < 0) {
						tileIndex = getAnimatedTile(tileIndex);
					}

					g.drawRegion(sourceImage,
							tileSetX[tileIndex],
							tileSetY[tileIndex],
							cellWidth, cellHeight,
							Sprite.TRANS_NONE,
							tx, ty,
							Graphics.TOP | Graphics.LEFT);
				}
			}
		}
	}

	private void createStaticSet(Image image, int noOfFrames, int tileWidth,
								 int tileHeight, boolean maintainIndices) {
		cellWidth = tileWidth;
		cellHeight = tileHeight;

		int imageW = image.getWidth();
		int imageH = image.getHeight();

		sourceImage = image;

		numberOfTiles = noOfFrames;
		tileSetX = new int[numberOfTiles];
		tileSetY = new int[numberOfTiles];

		if (!maintainIndices) {
			// populate cell matrix, all the indices are 0 to begin with
			for (rows = 0; rows < cellMatrix.length; rows++) {
				int totalCols = cellMatrix[rows].length;
				for (columns = 0; columns < totalCols; columns++) {
					cellMatrix[rows][columns] = 0;
				}
			}
			// delete animated tiles
			anim_to_static = null;
		}

		int currentTile = 1;

		for (int locY = 0; locY < imageH; locY += tileHeight) {
			for (int locX = 0; locX < imageW; locX += tileWidth) {

				tileSetX[currentTile] = locX;
				tileSetY[currentTile] = locY;

				currentTile++;
			}
		}
	}
}
