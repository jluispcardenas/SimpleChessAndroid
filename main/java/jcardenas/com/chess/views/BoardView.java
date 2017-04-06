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
package jcardenas.com.chess.views;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import jcardenas.com.chess.ia.SimpleIA;
import jcardenas.com.chess.activities.GameActivity;
import jcardenas.com.chess.callbacks.BoardCallback;
import jcardenas.com.chess.models.Move;
import jcardenas.com.chess.models.Piece;
import jcardenas.com.chess.models.PieceColor;
import jcardenas.com.chess.models.PieceType;
import jcardenas.com.chess.services.PubnubService;
import jcardenas.com.chess.utils.BoardEvaluator;
import jcardenas.com.chess.utils.Constants;
import jcardenas.com.chess.models.Mode;

public class BoardView extends View {
    //
    public static char[][] _board = new char[8][8];
    static char ids[] = new char[] {'k', 'q', 'b', 'n', 'r', 'p' };

    private GameActivity game;
    private HashMap<Integer, Piece> _pieces = new HashMap<Integer, Piece>();
    private Mode mode = Mode.HUMAN_COMPUTER;
    private boolean whites;
    private boolean gameOver = false;

    private Bitmap lightTile, darkTile;
    private int newWidth, newHeight;
    private BoardCallback callback = new BoardCallback();
    private boolean showLastMove = true;

    static private PieceColor mainColor = PieceColor.WHITE;
    static private ArrayList<Character> captures = new ArrayList<Character>();
    static private int lastmove = -1;

    public Canvas canvas;
    public Piece focus = null;


    public BoardView(Context context, GameActivity game) {
        this(context, game, null);
    }

    public BoardView(Context context, GameActivity game, AttributeSet atts) {
        super(context, atts);
        setFocusable(true);

        this.game = game;
        lastmove = -1;

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        showLastMove = sharedPrefs.getBoolean("lastmove", true);

        initComponents();
    }

    void initComponents() {
        DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();

        int boardWidth;
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            boardWidth = metrics.heightPixels;
            boardWidth -= (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 69, metrics);
        } else {
            boardWidth = metrics.widthPixels;
        }

       // setLayoutParams(new ViewGroup.LayoutParams(boardWidth, boardWidth));

        newWidth = (int)Math.ceil(boardWidth/8);
        newHeight = (int)Math.ceil(boardWidth/8);

        darkTile = getBitmapFromAsset(getContext(), "images/darkTile.png");
        lightTile = getBitmapFromAsset(getContext(), "images/lightTile.png");

        // scale
        darkTile = Bitmap.createScaledBitmap(darkTile, newWidth, newHeight, false);
        lightTile = Bitmap.createScaledBitmap(lightTile, newWidth, newHeight, false);
    }

    public void onDraw(Canvas canvas) {
        this.canvas = canvas;

        int w = lightTile.getWidth();

        for (int r = 0; r < 8; r++)
        {
            Bitmap bm = ((r%2) == 0) ? lightTile : darkTile;
            for (int c = 0; c < 8; c++) {
                canvas.drawBitmap(bm, c*w, r*w, null);

                bm = (bm == darkTile) ? lightTile : darkTile;
                // highlight last move
                if (showLastMove && (r*8)+c == lastmove) {
                    Paint paint = new Paint();
                    paint.setColor(Color.GREEN);
                    paint.setStyle(Paint.Style.FILL);
                    paint.setAlpha(100);

                    canvas.drawRect(new Rect((c*w), (r*w), (c*w)+w, (r*w)+w), paint);
                }
            }
        }
        if (_pieces.isEmpty())
            restart();
        else {
            for (Integer key : _pieces.keySet()) {
                Piece pc = (Piece)_pieces.get(key);
                pc.changeType(pc.getType());
            }
        }

    }

    public void restart() {
        whites = true;
        for (int i = 0; i < 64; i++)
            _board[(i/8)][(i%8)] = '\0';

        for (Integer key : _pieces.keySet()) {
            Piece pc = (Piece)_pieces.get(key);
            pc.remove();
        }
        _pieces.clear();

        setGameOver(false);

        boolean swhite = getMainColor() == PieceColor.BLACK;
        for (int r = 0; r < 8; r++)
        {
            if (r == 6) swhite = !swhite;
            for (int c = 0; c < 8; c++)
            {
                //
                PieceType type = PieceType.UNKNOWN;
                if (r == 0 || r == 7)
                {
                    type = PieceType.KING;
                    switch (c)
                    {
                        case 0:
                        case 7:
                            type = PieceType.ROOK;
                            break;
                        case 1:
                        case 6:
                            type = PieceType.KNIGHT;
                            break;
                        case 2:
                        case 5:
                            type = PieceType.BISHOP;
                            break;
                        default:
                            if (c == (getMainColor() == PieceColor.WHITE ? 3 : 4))
                                type = PieceType.QUEEN;
                            break;
                    }
                }
                else if (r == 1 || r == 6)
                {
                    type = PieceType.PAWN;
                }

                if (type != PieceType.UNKNOWN)
                {
                    Piece p = new Piece(type, (swhite ? PieceColor.WHITE : PieceColor.BLACK), c, r, this, showLastMove);
                    save(c, r, p);
                }
            }
        }

    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void setGameOver(boolean g) {
        gameOver = g;
    }

    public void setMode(Mode m) {
        mode = m;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMainColor(PieceColor c) {
        if (c != PieceColor.NONE)
            mainColor = c;
    }

    static public PieceColor getMainColor() {
        return mainColor;
    }

    public int getNewWidth() {
        return newWidth;
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public void setBoardCallback(BoardCallback callback) {
        this.callback = callback;
    }

    public void onMove(Move move) {
        callback.onMoveComplete(move);
    }

    public void onGameOver(PieceColor color) {
        callback.onGameOver(color);
    }

    static public int validMove(PieceType type, int c, int r, int dc, int dr, int capture[], boolean first)
    {
        capture[0] = -1;
        char piece = '\0', cpiece = '\0';
        PieceColor color;
        int ac = Math.abs(c - dc);
        int ar = Math.abs(r - dr);

        if (dc > 7 || dr > 7 || (piece = _board[c][r]) == '\0') {
            return 0;
        }

        color = getColor(piece);

        if ((cpiece = _board[dc][dr]) != '\0')
        {
            if (getColor(cpiece) != color)
                capture[0] = dc * 8 + dr;
            else
                return 0;
        }
        if (type == PieceType.QUEEN)
        {
            if (c != dc && r != dr)
                type = PieceType.BISHOP;
            else
                type = PieceType.ROOK;
        }

        if (type == PieceType.KNIGHT && !((ar + ac) == 3 && Math.max(ar, ac) == 2))
        {
            return 0;
        }
        else if (type == PieceType.BISHOP)
        {
            if (dc == c || dr == r || ac != ar)
                return 0;
            else
                return blocked(c, r, dc, dr) == -1 ? 1 : 0;
        }
        else if (type == PieceType.ROOK)
        {
            if (dr != r && dc != c)
                return 0;
            else
                return blocked(c, r, dc, dr) == -1 ? 1 : 0;
        }
        else if (type == PieceType.PAWN)
        {
            int ina = color == getMainColor() ? -1 : 1;
            int ipos = color == getMainColor() ? 6 : 1;
            int dif = r + ina;

            if (!((dr == dif && ((capture[0] == -1 && c == dc) || (capture[0] != -1 && ac == 1)))
            || ((r == ipos && capture[0] == -1 && dr == (dif + ina) && _board[dc][dif] == '\0' && c == dc))))
                return 0;
        }
        else if (type == PieceType.KING) {
            if (!(((ac + ar) == 1) || ((ac + ar) == 2 && Math.max(ac, ar) != 2))) {
                // revisar enroque
                int rd = color == getMainColor() ? 7 : 0;
                if (c == 4 && r == rd && dc == 6 && dr == rd
                        || c == 4 && r == rd && dc == 2 && dr == rd) {
                    char rook = _board[(dc == 6 ? 7 : 0)][dr];
                    if (getType(rook) == PieceType.ROOK && getColor(rook) == color
                            && blocked(c, r, dc, dr) == -1) {
                        return 2 + ((dc == 6 ? 7 : 0) * 8 + dr);
                    }
                }
                return 0;
            }
        }

        return 1;
    }

    static int blocked(int ac, int ar, int bc, int br)
    {
        int bl = -1;
        int ica = ac != bc ? (bc > ac ? 1 : -1) : 0;
        int icb = ar != br ? (br > ar ? 1 : -1) : 0;
        int tc = ac + ica;
        int tr = ar + icb;
        while (true) {
            if ((br > ar && tr >= br) || (br < ar && tr <= br)
                    || (bc > ac && tc >= bc) || (bc < ac && tc <= bc))
                break;
            if (_board[tc][tr] != '\0')
            {
                bl = (tc * 8) + tr;
                break;
            }
            tc += ica;
            tr += icb;
        }
        return bl;
    }

    void save(int c, int r, Piece pc)
    {
        _pieces.remove(pc.getRow()*8+pc.getCol());
        _pieces.remove(r*8+c);
        _pieces.put(r*8+c, pc);

        char p = ids[pc.getType().ordinal()];

        _board[c][r] = pc.getColor() == PieceColor.BLACK ? Character.toUpperCase(p) : p;
    }

    public void makeMove(int ac, int ar, int bc, int br, int enroque)
    {
        Piece pc = getPiece(ac, ar);
        whites = !whites;

        _board[ac][ar] = '\0';
        save(bc, br, pc);

        lastmove = (ar * 8) + ac;

        if (enroque > 1) {
            // hacer enroque
            enroque -= 2;
            int roc = (enroque/8), ror = (enroque%8), broc = (bc == 6 ? 5 : 3);

            Piece pcrook = getPiece(roc, ror);
            _board[roc][ror] = '\0';

            save(broc, ror, pcrook);
            pcrook.moveTo(broc, ror);
        }

        if (!isGameOver() && (br == 0 || br == 7) && pc.getType() == PieceType.PAWN) {
            // Promote pawn
            char convert = '0';
            int pos = 0;
            for (int i = 0; i < captures.size(); i++) {
                char c = captures.get(i);
                if ((c != 'P' && c != 'p') && getColor(c) == pc.getColor()
                        && (convert == 'Q' || convert == 'q' || c > convert)) {
                    convert = c;
                    pos = i;
                }
            }
            if (convert != '0') {
                pc.setType(getType(convert));
                pc.changeType(null);
                captures.set(pos, _board[bc][br]);
                _board[bc][br] = convert;
            }
        }

        // OnMoveComplete
        if (pc.getColor() == getMainColor() && getMode() == Mode.HUMAN_ONLINE)
        {
            // send message to channel
           /* ((MainBoard*)(this->parent()->parent()))->sendMessage(tr("MOV %1x%2:%3x%4\r\n").arg(ac).arg(ar)
                .arg(bc).arg(br));*/
            JSONObject json = new JSONObject();
            try {
                json.put(Constants.JSON_USER, game.getUsername());
                json.put(Constants.MOVE, ac + "x" + ar + ":" + bc + "x" + br);

                PubnubService.publish(game.getChannel(), json, Constants.MOVE_ACTION);

            } catch (JSONException e) { e.printStackTrace(); }

        } else if (pc.getColor() == getMainColor() && getMode() == Mode.HUMAN_COMPUTER) {
            final Handler handler = new Handler();
            Thread thread = new Thread() {
                @Override
                public void run() {
                    final Move move = SimpleIA.computeBestMove();

                    handler.post(new Runnable() {
                        public void run() {
                            int c = move.to / 8, r = move.to % 8;

                            Piece pm = BoardView.this.getPiece(move.from/8, move.from%8);
                            pm.move(c, r);

                            BoardView.this.invalidate();

                            BoardView.this.callback.onMoveComplete(move);

                            checkAlert();
                        }
                    });
                }
            };
            thread.setPriority(Thread.MAX_PRIORITY);
            thread.start();
        }

        checkAlert();
    }

    static public ArrayList generateValidMoves(PieceColor color, boolean calc)
    {
        ArrayList moves = new ArrayList(48);
        for (int i = 0; i < 64; i++)
        {
            int c = i / 8, r = i % 8;
            char p = _board[c][r];
            if (p != '\0' && getColor(p) == color)
            {
                for (int n = 0; n < 64; n++)
                {
                    int c2 = n / 8, r2 = n % 8;
                    int capture[] = {-1};
                    int ret = 0;
                    if ((ret = validMove(getType(p), c, r, c2, r2, capture, false)) > 0)
                    {
                        int score = 0;
                        if (calc)
                        {
                            char[][] backBoard = new char[8][8];
                            arrayCopy(_board, backBoard);
                            _board[c2][r2] = _board[c][r];
                            _board[c][r] = '\0';

                            // hacer enroque
                            if (ret > 1) {
                                ret -= 2;
                                int roc = (ret/8), ror = (ret%8), broc = (c2 == 6 ? 5 : 3);
                                _board[broc][ror] = _board[roc][ror];
                                _board[roc][ror] = '\0';
                            }

                            score = BoardEvaluator.getBoardScore(color);
                            if (capture[0] != -1) {
                                score += (BoardEvaluator.pieceValue(_board[c2][r2]) - BoardEvaluator.pieceValue(p) / 10);
                            }
                            arrayCopy(backBoard, _board);
                        }

                        Move m = new Move(i, n, score, capture[0]);
                        moves.add(m);
                    }
                }
            }
        }

        return moves;
    }

    public Piece getPiece(int c, int r) {
        return _pieces.get(r*8+c);
    }

    static public PieceColor getColor(char c) {
        return Character.isLowerCase(c) ? PieceColor.WHITE : PieceColor.BLACK;
    }

    boolean myTurn(PieceColor color)
    {
        int whites = this.whites ? 1 : 2;
        return whites == color.ordinal();
    }

    boolean myTurn()
    {
        return myTurn(getMainColor());
    }

    void checkAlert()
    {
        if (!isGameOver())
        {
            if (checkCheck(whites ? PieceColor.WHITE : PieceColor.BLACK))
            {
                callback.onCheck(whites ? PieceColor.WHITE : PieceColor.BLACK);
            }
        }
    }

    boolean checkCheck(PieceColor color)
    {
        char ck = color == PieceColor.WHITE ? 'k' : 'K';
        int pos = getPos(ck);
        int kc = pos / 8, kr = pos % 8;
        for (int i = 0; i < 64; i++)
        {
            int c = i / 8, r = i % 8;
            char p = _board[c][r];
            if (p != '\0' && getColor(p) != getColor(ck))
            {
                int capture[] = {-1};
                if (validMove(getType(p), c, r, kc, kr, capture, false) > 0)
                {
                    return true;
                }
            }
        }

        return false;
    }

    int getPos(char p, int distinct)
    {
        for (int i = 0; i < 64; i++)
            if (p == _board[(i / 8)][(i % 8)] && (distinct == -1 || i != distinct))
                return i;
        return -1;
    }

    int getPos(char p) {
        return getPos(p, -1);
    }

    static public PieceType getType(char c)
    {
        for (int i = 0; i < 6; i++)
            if (ids[i] == Character.toLowerCase(c))
                return PieceType.values()[i];

        return PieceType.UNKNOWN;
    }

    public void capture(Piece cpiece)
    {
        cpiece.setCapture(true);
        //cpiece.hide();
        captures.add(_board[cpiece.getCol()][cpiece.getRow()]);

        if (cpiece.getType() == PieceType.KING)
        {
            setGameOver(true);

            callback.onGameOver(cpiece.getColor());
        }
    }

    int fix(int n) {
        return Math.abs(n - 7);
    }

    public static Bitmap getBitmapFromAsset(Context context, String filePath) {
        AssetManager assetManager = context.getAssets();

        InputStream istr;
        Bitmap bitmap = null;
        try {
            istr = assetManager.open(filePath);
            bitmap = BitmapFactory.decodeStream(istr);
        } catch (IOException e) {
            // handle exception
        }

        return bitmap;
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect){
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        switch (event.getAction())
        {
            case MotionEvent.ACTION_UP:

                if (mode == Mode.UNSTARTED || !myTurn())
                    return false;

                double x = event.getX();
                double y = event.getY();
                int col = (int)Math.floor(x/newWidth);
                int row = (int)Math.floor(y/newWidth);

                Piece p;
                if ((p =_pieces.get(row*8+col)) != null) {
                    if (myTurn(p.getColor())) {
                        // es propia, poner focus
                        if (focus != null)
                           focus.setFocus(false);

                        p.setFocus(true);
                        focus = p;
                    } else {
                        // revisamos si es posible comer
                        if (focus != null) {
                            if (focus.move(col, row)) {
                                focus.setFocus(false);
                                focus = null;
                            } else {

                            }
                        }
                    }
                } else if (focus != null) {
                    // mover a una posicion vacia
                    if (focus.move(col, row)) {
                        focus.setFocus(false);
                        focus = null;
                    } else {

                    }
                } else {

                }

                invalidate();
                break;
            default:
                break;
        }

        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            initComponents();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            initComponents();
        }
    }

    public static char[][] arrayCopy(char[][] src, char[][] target) {
        int length = src.length;
        for (int i = 0; i < length; i++) {
            System.arraycopy(src[i], 0, target[i], 0, src[i].length);
        }
        return target;
    }
}
