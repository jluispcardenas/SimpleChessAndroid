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
package jcardenas.com.chess.utils;

import jcardenas.com.chess.views.BoardView;
import jcardenas.com.chess.models.PieceColor;
import jcardenas.com.chess.models.PieceType;

public class BoardEvaluator {
     static short[] pknight =
                {0, 4, 8, 10, 10, 8, 4, 0,
                        4, 8, 16, 20, 20, 16, 8, 4,
                        8, 16, 24, 28, 28, 24, 16, 8,
                        10, 20, 28, 32, 32, 28, 20, 10,
                        10, 20, 28, 32, 32, 28, 20, 10,
                        8, 16, 24, 28, 28, 24, 16, 8,
                        4, 8, 16, 20, 20, 16, 8, 4,
                        0, 4, 8, 10, 10, 8, 4, 0};

        static short[] pbishop =
                {14, 14, 14, 14, 14, 14, 14, 14,
                        14, 22, 18, 18, 18, 18, 22, 14,
                        14, 18, 22, 22, 22, 22, 18, 14,
                        14, 18, 22, 22, 22, 22, 18, 14,
                        14, 18, 22, 22, 22, 22, 18, 14,
                        14, 18, 22, 22, 22, 22, 18, 14,
                        14, 22, 18, 18, 18, 18, 22, 14,
                        14, 14, 14, 14, 14, 14, 14, 14};

        static short[] ppawn =
                {0, 0, 0, 0, 0, 0, 0, 0,
                        4, 4, 4, 0, 0, 4, 4, 4,
                        6, 8, 2, 10, 10, 2, 8, 6,
                        6, 8, 12, 16, 16, 12, 8, 6,
                        8, 12, 16, 24, 24, 16, 12, 8,
                        12, 16, 24, 32, 32, 24, 16, 12,
                        12, 16, 24, 32, 32, 24, 16, 12,
                        0, 0, 0, 0, 0, 0, 0, 0};

        static public int getBoardScore(PieceColor color)
        {
            int total = 0;
            for (int i = 0; i < 64; i++)
            {
                int c = i / 8, r = i % 8;
                char p = BoardView._board[c][r];
                if (p != '\0')
                {
                    PieceType type = BoardView.getType(p);
                    PieceColor pcolor = BoardView.getColor(p);
                    int result = 0;
                    switch (type)
                    {
                        case KING:
                            result += 9000;
                            break;
                        case QUEEN:
                            result += 1100;
                            result += pknight[i];
                            break;
                        case BISHOP:
                            result += 315;
                            result += pbishop[i];
                            break;
                        case KNIGHT:
                            result += 330;
                            result += pknight[i];
                            break;
                        case ROOK:
                            result += 500;
                            break;
                        case PAWN:
                            result += 100;
                            result += ppawn[i];
                            break;
                    }

                    total += (color == pcolor) ? result : -result;
                }
            }

            return total;
        }

        static public int pieceValue(char c)
        {
            int value = 0;
            switch (Character.toLowerCase(c))
            {
                case 'k':
                    value = 9000;
                    break;
                case 'q':
                    value = 1100;
                    break;
                case 'b':
                    value = 315;
                    break;
                case 'n':
                    value = 330;
                    break;
                case 'r':
                    value = 500;
                    break;
                case 'p':
                    value = 100;
                    break;
            }

            return value;
        }
    }

