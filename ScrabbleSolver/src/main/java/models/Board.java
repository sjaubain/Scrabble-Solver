package models;

import jdk.nashorn.internal.scripts.JO;

import static models.Board.bonus.*;

public class Board {

    private Dictionary dico;

    // current score used as a temporary variable
    private int score;

    public final static int SIZE = 15;
    public final static char JOKER = '?';
    public final static char EMPTY = '-';
    enum bonus {L2, L3, M2, M3, __}
    private int letterWeights[] = {1,3,3,2,1,4,2,4,1,8,10,1,2,1,1,3,8,1,1,1,1,4,10,10,10,10};
    private bonus bonusMask[][] = {
        {M3,__,__,L2,__,__,__,M3,__,__,__,L2,__,__,M3},
        {__,M2,__,__,__,L3,__,__,__,L3,__,__,__,M2,__},
        {__,__,M2,__,__,__,L2,__,L2,__,__,__,M2,__,__},
        {L2,__,__,M2,__,__,__,L2,__,__,__,M2,__,__,L2},
        {__,__,__,__,M2,__,__,__,__,__,M2,__,__,__,__},
        {__,L3,__,__,__,L3,__,__,__,L3,__,__,__,L3,__},
        {__,__,L2,__,__,__,L2,__,L2,__,__,__,L2,__,__},
        {M3,__,__,L2,__,__,__,M2,__,__,__,L2,__,__,M3},
        {__,__,L2,__,__,__,L2,__,L2,__,__,__,L2,__,__},
        {__,L3,__,__,__,L3,__,__,__,L3,__,__,__,L3,__},
        {__,__,__,__,M2,__,__,__,__,__,M2,__,__,__,__},
        {L2,__,__,M2,__,__,__,L2,__,__,__,M2,__,__,L2},
        {__,__,M2,__,__,__,L2,__,L2,__,__,__,M2,__,__},
        {__,M2,__,__,__,L3,__,__,__,L3,__,__,__,M2,__},
        {M3,__,__,L2,__,__,__,M3,__,__,__,L2,__,__,M3}
    };

    private char board[][];

    // best score parameters :
    private int rowMaxScore, colMaxScore, dirXMaxScore, dirYMaxScore, maxScore;
    private String wrdMaxScore;

    public Board() {
        board = new char[SIZE][SIZE];
        for(int i = 0; i < SIZE; ++i)
            for(int j = 0; j < SIZE; ++j)
                board[i][j] = EMPTY;
        dico = new Dictionary();
        dico.load("input/ods8.txt");
        score = 0;
        rowMaxScore = colMaxScore = dirXMaxScore = dirYMaxScore = maxScore = 0;
    }

    public void put(char c, int row, int col) {
        board[row][col] = c;
    }

    public void remove(int row, int col) {
        board[row][col] = EMPTY;
    }

    public String getBoxColor(int row, int col) {
        switch(bonusMask[row][col]) {
            case M2:
                return "#ffccff";
            case M3:
                return "#ff4d4d";
            case L2:
                return "#99ffff";
            case L3:
                return "#4d94ff";
            case __:
                return "#b3ffb3";
        }
        return "#ffffff";
    }

    public char[][] getBoard() {
        return board;
    }

    private boolean isAnchored(int row, int col) {

        if(row < 0 || row >= SIZE || col < 0 || col >= SIZE)
            return false;

        return (row - 1 >= 0 && board[row - 1][col] != EMPTY)
                || (row + 1 < SIZE && board[row + 1][col] != EMPTY)
                || (col - 1 >= 0 && board[row][col - 1] != EMPTY)
                || (col + 1 < SIZE && board[row][col + 1] != EMPTY)
                || (row == SIZE / 2 && col == SIZE / 2); // for the first turn when board is empty
    }

    private boolean crossCheck(int row, int col, int dirX, int dirY) {

        int rowLtr = row, colLtr = col; // row and col of just put letter

        String crossWrd = "";
        do {
            row -= dirX; col -= dirY;
        } while(row >= 0 && col >= 0 && board[row][col] != EMPTY);

        row += dirX; col += dirY;
        int startRow = row, startCol = col;
        do {
            crossWrd += board[row][col];
            row += dirX; col += dirY;
        } while(row < SIZE && col < SIZE && board[row][col] != EMPTY);

        boolean validCrossWrd = crossWrd.length() > 1 && dico.isValidWorld(crossWrd, false);

        if(validCrossWrd) {
            remove(rowLtr, colLtr);
            computeScore(startRow, startCol, dirX, dirY, crossWrd);
        }

        return crossWrd.length() == 1 || validCrossWrd;
    }

    public void spreadRight(int row, int col, int dirX, int dirY, String rack, String partialWrd, boolean validMove) {

        if(row == SIZE || col == SIZE || board[row][col] == EMPTY) {

            // first check if the current word is valid
            if(validMove && dico.isValidWorld(partialWrd, false)) {
                int startWrdRow = row - dirX * (partialWrd.length() );
                int startWrdCol = col - dirY * (partialWrd.length() );

                int prevScore = this.score;
                computeScore(startWrdRow, startWrdCol, dirX, dirY, partialWrd);
                checkScore(startWrdRow, startWrdCol, dirX, dirY, partialWrd, this.score);
                this.score = prevScore;
            }

            if(row < SIZE && col < SIZE) {

                validMove = validMove || isAnchored(row, col);

                // then look for new words recursively
                for (int i = 0; i < rack.length(); ++i) {

                    char c = rack.charAt(i) == JOKER ? 'A' : rack.charAt(i);

                    // if the current char is a joker, we try all possibilities
                    // in [A, Z], otherwise we just try with current char and leave the loop
                    for(; c <= 'Z' || rack.charAt(i) != JOKER; ++c) {

                        // save params before recursion
                        partialWrd += c;
                        put(c, row, col);
                        int prevScore = this.score;
                        boolean crossCheck = crossCheck(row, col, dirY, dirX);
                        remove(row, col);

                        // we don't keep on trying if the current word is not a valid prefix
                        if (crossCheck && dico.isValidWorld(partialWrd, true)) {
                            spreadRight(row + dirX, col + dirY, dirX, dirY,
                                    rack.substring(0, i) + rack.substring(i + 1, rack.length()),
                                    partialWrd, validMove);
                        }

                        // restore params after recursion
                        partialWrd = partialWrd.substring(0, partialWrd.length() - 1);
                        this.score = prevScore;

                        if (rack.charAt(i) != JOKER)
                            break;
                    }
                }
            }
        } else {
            partialWrd += board[row][col];
            // validMove is true if we use a letter already on board
            spreadRight(row + dirX, col + dirY, dirX, dirY,
                    rack, partialWrd, true);
        }
    }

    private void computeScore(int row, int col, int dirX, int dirY, String wrd) {
        int score = 0, nbM2 = 0, nbM3 = 0;
        int nbLettersFromRack = 0;
        for(int i = 0; i < wrd.length(); ++i) {
            char c = board[row][col];
            // joker if uppercase letter
            int weight = (wrd.charAt(i) >= 'A' && wrd.charAt(i) <= 'Z') ?  0 : letterWeights[wrd.charAt(i) - 97];
            if(c == EMPTY) {
                if(bonusMask[row][col] == L3)
                    score += weight * 3;
                else if(bonusMask[row][col] == L2)
                    score += weight * 2;
                else {
                    score += weight;
                    if(bonusMask[row][col] == M3)
                        nbM3++;
                    else if(bonusMask[row][col] == M2)
                        nbM2++;
                }
                nbLettersFromRack++;
            } else {
                score += weight;
            }
            row += dirX; col += dirY;
        }
        score *= Math.pow(2, nbM2) * Math.pow(3, nbM3);
        if(nbLettersFromRack == 7)
            score += 50;
        this.score += score;
    }

    private void checkScore(int row, int col, int dirX, int dirY, String wrd, int currScore) {

        if(currScore > this.maxScore) {
            maxScore = currScore;
            rowMaxScore = row;
            colMaxScore = col;
            dirXMaxScore = dirX;
            dirYMaxScore = dirY;
            wrdMaxScore = wrd;
        }
    }

    public void putWord(int row, int col, int dirX, int dirY, String wrd) {
        for(int i = 0; i < wrd.length(); ++i) {
            if(board[row][col] == EMPTY) {
                put(wrd.charAt(i), row, col);
            }
            row += dirX; col += dirY;
        }
    }

    public int computeBestWrd(String rack) {

        maxScore = 0;
        wrdMaxScore = "";
        for(int i = 0; i < SIZE; ++i) {
            for(int j = 0; j < SIZE; ++j) {
                if(board[i][j] == EMPTY) {

                    // horizontal
                    String partialWrd = "";
                    int col = j;
                    while(--col >= 0 && board[i][col] != EMPTY) {
                        partialWrd = board[i][col] + partialWrd;
                    }
                    spreadRight(i, j, 0, 1, rack, partialWrd, false);

                    // vertical
                    partialWrd = "";
                    int row = i;
                    while(--row >= 0 && board[row][j] != EMPTY) {
                        partialWrd = board[row][j] + partialWrd;
                    }
                    spreadRight(i, j, 1, 0, rack, partialWrd, false);
                }
            }
        }

        if(wrdMaxScore == null || wrdMaxScore.length() == 0)
            return -1;
        else {
            putWord(rowMaxScore, colMaxScore, dirXMaxScore, dirYMaxScore, wrdMaxScore);
            return 0;
        }
    }

    public int getMaxScore() {
        return this.maxScore;
    }
}
