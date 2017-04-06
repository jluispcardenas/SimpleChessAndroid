/*
 * Copyright 2016 Jose Luis Cardenas - jluis.pcardenas@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package jcardenas.com.chess.models;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

import jcardenas.com.chess.views.BoardView;

public class Piece {
    static private Bitmap spriteWhite = null;
    static private Bitmap spriteBlack = null;
    private BoardView board;
    private PieceType type;
    private PieceColor color;
    private boolean capture = false;
    private int row;
    private int col;
    private boolean focus = false;
    private boolean firstmove = true;
    static private int lastmove = -1;
    static private boolean showLastMove = true;

    public Piece(PieceType t, PieceColor cl, int c, int r, BoardView biw, boolean showLastMove) {
        if (spriteWhite == null)
            spriteWhite = BoardView.getBitmapFromAsset(biw.getContext(), "images/whitepieces.png");
        if (spriteBlack == null)
            spriteBlack = BoardView.getBitmapFromAsset(biw.getContext(), "images/blackpieces.png");

        this.board = biw;
        Piece.showLastMove = showLastMove;
        row = r;
        col = c;

        setColor(cl);
        setCapture(false);
        setType(t);

        changeType(type);
    }

    public void changeType(PieceType t)
    {
        Bitmap sprite = getColor() == PieceColor.WHITE ? spriteWhite : spriteBlack;
        int ordinal = getType().ordinal()*75;
        int w = board.getNewWidth(), x = col * w, y = row * w;
        Rect src = new Rect(ordinal, 0, ordinal+sprite.getHeight(), sprite.getHeight());
        Rect dest = new Rect(x, y, x+w, y+w);

        board.getCanvas().drawBitmap(sprite, src, dest, null);
        if (focus == true) {
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5);
            paint.setColor(Color.BLUE);

            board.getCanvas().drawRect(new Rect(x, y, x+w, y+w), paint);
        } else if (showLastMove && lastmove == (row*8)+col) {
            Paint paint = new Paint();
            paint.setColor(Color.GREEN);
            paint.setStyle(Paint.Style.FILL);
            paint.setAlpha(50);

            board.getCanvas().drawRect(new Rect(x, y, x+w, y+w), paint);
        }
    }

    public PieceType setType(PieceType t) {
        if (t != PieceType.UNKNOWN)
            type = t;

        return type;
    }

    public PieceType getType() {
        return type;
    }

    public PieceColor setColor(PieceColor c) {
        if (c != PieceColor.NONE)
            color = c;
        return color;
    }

    public PieceColor getColor() {
        return color;
    }

    public boolean setCapture(boolean capture) {
        this.capture = capture;
        return this.capture;
    }

    public boolean isCaptured() {
        return capture;
    }

    public void moveTo(int c, int r) {
        row = r;
        col = c;

        changeType(getType());
    }

    public boolean move(int c, int r)
    {
        int capture[] = {-1};
        int ret = 0;
        if ((ret = BoardView.validMove(getType(), col, row, c, r, capture, false)) > 0) {
            // save move
            if (capture[0] != -1)
                board.capture(board.getPiece((capture[0]/8), (capture[0]%8)));

            board.makeMove(col, row, c, r, ret);

            moveTo(c, r);

            firstmove = false;
            lastmove = (r*8) + c;

            return true;
        } else {
            Log.i("ERROR", "Movimiento invalido! + " + col + "--" + row + " ,,, " + c + "--" + r);
        }

        return false;
    }

    public int getCol () {
        return col;
    }

    public int getRow() {
        return row;
    }

    public void setFocus(boolean f) {
        focus = f;
    }

    public void remove() {}
}
