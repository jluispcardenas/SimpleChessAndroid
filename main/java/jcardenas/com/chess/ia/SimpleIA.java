package jcardenas.com.chess.ia;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import jcardenas.com.chess.models.Move;
import jcardenas.com.chess.models.PieceColor;
import jcardenas.com.chess.utils.BoardEvaluator;
import jcardenas.com.chess.views.BoardView;

public class SimpleIA {
    static int depth = 2;
    static Comparator comp = new Comparator<Move>() {
        @Override
        public int compare(Move a, Move b) {
            if (a.score < b.score)
                return 1;
            if (a.score > b.score)
                return -1;
            else
                return 0;
        }
    };

    public static Move computeBestMove() {
        int alpha = -99999;
        int beta = 99999;

        ArrayList<Move> moves = BoardView.generateValidMoves(PieceColor.BLACK, true);
        Collections.sort(moves, comp);
        Move bestMove = null;

        for (int i = 0; i < moves.size(); i++) {
            Move move = moves.get(i);

            char backBoard[][] = new char[8][8];
            BoardView.arrayCopy(BoardView._board, backBoard);
            int c = move.from / 8, r = move.from % 8, c2 = move.to / 8, r2 = move.to % 8;

            BoardView._board[c2][r2] = BoardView._board[c][r];
            BoardView._board[c][r] = '\0';

            // PVS enhancement
            int value = -alphaBeta(depth - 1, -alpha - 1, -alpha, PieceColor.WHITE);
            if (value > alpha && value < beta) {
                value = -alphaBeta(depth - 1, -beta, -alpha, PieceColor.WHITE);
            }

            if (value > alpha) {
                alpha = value;
                bestMove = move;
            }

            BoardView.arrayCopy (backBoard, BoardView._board);
        }

        return bestMove;
    }

    static int alphaBeta(int depth, int alpha, int beta, PieceColor color) {
        if (depth == 0)
            return BoardEvaluator.getBoardScore (color);

        int value = -99999;
        boolean pv = false;
        PieceColor enemyColor = color == PieceColor.WHITE ? PieceColor.BLACK : PieceColor.WHITE;

        if (depth >= 3) {
            value = -alphaBeta(depth - (depth > 6 ? 3 : 2) - 1, -beta, -beta + 1, enemyColor);
            if (value >= beta)
                return beta;
        }

        ArrayList<Move> moves = BoardView.generateValidMoves (color, true);
        Collections.sort(moves, comp);

        for (int i = 0; i < moves.size(); i++) {
            Move move = moves.get(i);
            char backBoard[][] = new char[8][8];
            BoardView.arrayCopy (BoardView._board, backBoard);
            int c = move.from / 8, r = move.from % 8, c2 = move.to / 8, r2 = move.to % 8;
            BoardView._board[c2][r2] = BoardView._board[c][r];
            BoardView._board[c][r]='\0';
            if (pv) {
                value = -alphaBeta(depth - 1, -alpha - 1, -alpha, enemyColor);
                if (value > alpha && value < beta) {
                    value = -alphaBeta(depth - 1, -beta, -alpha, enemyColor);
                }
            } else {
                value = -alphaBeta(depth - 1, -beta, -alpha, enemyColor);
            }
            BoardView.arrayCopy (backBoard, BoardView._board);

            if (value >= beta)
                return beta;

            if (value > alpha) {
                alpha = value;
                pv = true;
            }
        }

        return alpha;
    }
}