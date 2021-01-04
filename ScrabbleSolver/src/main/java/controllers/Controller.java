package controllers;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.Property;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import models.Board;

import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    @FXML
    private AnchorPane mainPane, gamePane;

    @FXML
    private Label errLabel;

    @FXML
    private Button findWrdBtn;

    @FXML
    private TextField rack;

    @FXML
    private Label score;

    private Board board;
    private Button[][] controls = new Button[Board.SIZE][Board.SIZE];
    private Button targetBtn = null;
    private int targetX, targetY = -1;
    private boolean capsLocked = false;

    // Pixels offsets of the game board (array of control buttons)
    final static int offsetX = 30;
    final static int offsetY = 30;
    final static int btnSize = 35;

    public void initialize(URL location, ResourceBundle resources) {
        gamePane = new AnchorPane();
        board = new Board();

        for(int i = 0; i < Board.SIZE; ++i) {
            for(int j = 0; j < Board.SIZE; ++j) {
                Button btn = new Button();

                // checkbox styles and controls
                btn.setTranslateX(offsetX + i * (btnSize-1));
                btn.setTranslateY(offsetY + j * (btnSize-1));
                btn.setMinWidth(btnSize); btn.setMaxWidth(btnSize);
                btn.setMinHeight(btnSize); btn.setMaxHeight(btnSize);
                btn.setStyle("-fx-background-color: " + board.getBoxColor(i, j)
                        + "; -fx-border-radius: 0; -fx-border-color: black; -fx-background-radius: 0");

                btn.setOnMouseClicked(event -> {
                    error("");
                    targetX = ((int)event.getSceneX() - offsetX) / btnSize;
                    targetY = ((int)event.getSceneY() - offsetY) / btnSize;

                    refreshTargetBtn(targetX, targetY);
                });

                gamePane.getChildren().add(btn);
                controls[i][j] = btn;
            }
        }
        gamePane.setFocusTraversable(true);
        gamePane.requestFocus();
        mainPane.getChildren().add(gamePane);
        targetBtn = controls[0][0]; targetX = targetY = 0;

        // keyboards events
        gamePane.setOnKeyPressed(event -> {

            KeyCode keycode = event.getCode();
            error("");
            if(keycode.isLetterKey()) {
                if(targetBtn != null) {
                    if(!capsLocked)
                        putLetter(keycode.getName().toLowerCase(), targetX, targetY); // joker
                    else
                        putLetter(keycode.getName(), targetX, targetY);
                }
            } else if(keycode == KeyCode.BACK_SPACE) {
                if(targetBtn != null) {
                    removeLetter(targetX, targetY);
                }
            } else if(keycode == KeyCode.SHIFT) {
                capsLocked = true;
            } else if(keycode.isArrowKey()) {
                switch(keycode) {
                    case UP : targetY = (targetY - 1) % board.SIZE; if(targetY < 0) targetY += board.SIZE; break;
                    case DOWN : targetY = (targetY + 1) % board.SIZE; break;
                    case LEFT : targetX = (targetX - 1) % board.SIZE; if(targetX < 0) targetX += board.SIZE; break;
                    case RIGHT : targetX = (targetX + 1) % board.SIZE; break;
                }
                refreshTargetBtn(targetX, targetY);
            } else if(keycode.getName() != "?") {
                error("You have to enter valid letters or joker (?)");
            }

            // stop propagation (avoid default behaviour of arrow
            // keys that navigate through nodes)
            event.consume();
        });

        gamePane.setOnKeyReleased(event -> {
            if(event.getCode() == KeyCode.SHIFT)
                capsLocked = false;
        });

        // other controls
        findWrdBtn.setOnMouseClicked(event -> {
            error("");
            if(rack.getText().matches("[a-z/\\/" + board.JOKER + "?]*")) {
                if(rack.getText().length() > 7)
                    error("Rack size must be 7 maximum");
                else {
                    int res = board.computeBestWrd(rack.getText());
                    score.setText("score : " + board.getMaxScore());
                    if(res == -1)
                        error("No word found..");
                    paintBoard();
                }
            } else {
                error("You have to enter valid letters or joker (?)");
            }
        });

        score.setText("score : ");
    }

    private void refreshTargetBtn(int targetX, int targetY) {
        if(targetBtn != null)
            targetBtn.setStyle(targetBtn.getStyle() + "; -fx-border-width: 1");
        targetBtn = controls[targetX][targetY];
        targetBtn.setStyle(targetBtn.getStyle() + "; -fx-border-width: 2");
    }

    private void putLetter(String ltr, int i, int j) {
        // (i, j) and (x, y) on screen are inverted : i are rows and j columns
        // but x are columns and y rows on screen
        board.put(ltr.charAt(0), j, i);
        controls[i][j].setText(ltr);
        controls[i][j].setStyle(controls[i][j].getStyle() + "; -fx-background-color: white");

        if(ltr.charAt(0) >= 'A' && ltr.charAt(0) <= 'Z')
            controls[i][j].setTextFill(Color.RED);
        else
            controls[i][j].setTextFill(Color.BLACK);
    }

    private void removeLetter(int i, int j) {
        // (i, j) and (x, y) on screen are inverted : i are rows and j columns
        // but x are columns and y rows on screen
        board.remove(j, i);
        controls[i][j].setText("");
        controls[i][j].setStyle(controls[i][j].getStyle() + "; -fx-background-color: "
            + board.getBoxColor(i, j));
    }

    public void paintBoard() {
        for(int i = 0; i < board.SIZE; ++i) {
            for(int j = 0; j < board.SIZE; ++j) {
                char ltr;
                if((ltr = board.getBoard()[i][j]) != board.EMPTY) {
                    // (i, j) and (x, y) on screen are inverted : i are rows and j columns
                    // but x are columns and y rows on screen
                    putLetter(String.valueOf(ltr), j, i);
                }
            }
        }
    }

    public void error(String message) {
        errLabel.setText(message);
    }
}
