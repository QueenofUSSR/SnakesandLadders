package org.example.demo;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.io.PrintWriter;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class Controller {

    private final Circle[] playerCircles = new Circle[2];
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    public GridPane gridBoard; // 棋盘面板
    public Button rollButt1;
    public Button rollButt2;
    public Button loadBtn;
    @FXML
    public Label userLabel1;
    @FXML
    public Label userLabel2;
    public int mode;
    @FXML
    private Label posLabel1;
    @FXML
    private Label posLabel2;
    @FXML
    private Pane gameBoard; // 游戏面板
    private PrintWriter out;
    private String name;
    private String opponent;
    private String current;
    private int[][] board;

    @FXML
    public void initialize(PrintWriter out, String name, String oppo, String curr, int p1, int p2, int[][] board, int mode) {
        this.out = out;
        this.name = name;
        this.opponent = oppo;
        this.current = curr;
        this.board = board;
        this.mode = mode;
        posLabel1.setText(String.valueOf(p1));
        posLabel2.setText(String.valueOf(p2));
        gridBoard = new GridPane();
        gridBoard.setHgap(10);
        gridBoard.setVgap(10);
        gridBoard.setAlignment(Pos.CENTER);
        gameBoard.getChildren().add(gridBoard);
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                // 创建一个 VBox 布局容器，以便垂直排列标签
                StackPane cell = new StackPane();
                VBox vbox = new VBox();
                vbox.setPrefSize(50, 50);
                vbox.setAlignment(Pos.TOP_LEFT); // 设置对齐方式为左上角
                Label positionLabel = new Label(String.valueOf(i * 10 + j + 1));
                Label effectLabel = new Label(String.valueOf(board[i][j]));
                positionLabel.setTextFill(Color.BLACK);
                effectLabel.setTextFill(Color.BLACK);
                Color cellColor;
                if (board[i][j] > 0) {
                    cellColor = Color.WHITE;
                } else if (board[i][j] < 0) {
                    cellColor = Color.BLACK;
                    positionLabel.setTextFill(Color.WHITE);
                    effectLabel.setTextFill(Color.WHITE);
                } else {
                    effectLabel.setVisible(false);
                    cellColor = switch ((i * 10 + j) % 4) {
                        case 0 -> Color.GREEN;
                        case 1 -> Color.ORANGE;
                        case 2 -> Color.LIGHTBLUE;
                        case 3 -> Color.PINK;
                        default -> Color.TRANSPARENT;
                    };
                }
                cell.setStyle("-fx-background-color: " + toHexString(cellColor) + ";");
                vbox.getChildren().add(positionLabel);
                cell.getChildren().add(vbox);
                cell.getChildren().add(effectLabel);
                int[] tansPos = transformPosition(i * 10 + j);
                gridBoard.add(cell, tansPos[1], tansPos[0]);
            }
        }
        int[] pos1 = transformPosition(p1 - 1);
        int[] pos2 = transformPosition(p2 - 1);
        addPlayerCircle(pos1[0], pos1[1], true);
        addPlayerCircle(pos2[0], pos2[1], false);
        rollButt1.setDisable(opponent.equals(current));
        rollButt2.setDisable(name.equals(current));
        if (mode != 0) { // 匹配或排位
            userLabel1.setText("玩家位置");
            userLabel2.setText("对手位置");
            rollButt1.setText("投掷骰子");
            rollButt2.setText("查看敌情");
            rollButt2.setOnAction(_ -> handleCheck());
            loadBtn.setVisible(false);
        }
    }

    private String toHexString(Color color) {
        return String.format("#%02X%02X%02X", (int) (color.getRed() * 255), (int) (color.getGreen() * 255), (int) (color.getBlue() * 255));
    }

    // 坐标变换，从左下角开始到左上角结束
    private int[] transformPosition(int num) {
        int row = num / 10;
        int col = num % 10;
        int adjustedCol = row % 2 == 0 ? col : 9 - col;
        int adjustedRow = 9 - row;
        return new int[]{adjustedRow, adjustedCol};
    }

    // 添加玩家棋子
    private void addPlayerCircle(int x, int y, boolean isP1) {
        Color color = isP1 ? Color.BLUE : Color.RED;
        int offset = isP1 ? 15 : 35;
        Circle playerCircle = new Circle(10);
        playerCircles[isP1 ? 0 : 1] = playerCircle;
        playerCircle.setFill(color);
        playerCircle.setCenterX(y * 60 + offset);
        playerCircle.setCenterY(x * 60 + offset);
        gameBoard.getChildren().add(playerCircle);
    }

    // 生成一个点数
    @FXML
    public void handleRoll1(ActionEvent actionEvent) {
        Random r = new Random(); // 可在此添加道具
        int num = r.nextInt(6) + 1;
//        int num = 2; // 贝利亚测试连续跳跃
        System.out.println("自己点数为：" + num);
        int oldPos = Integer.parseInt(posLabel1.getText());
        int newPos = oldPos + num;
        newPos = multiMovePiece(playerCircles[0], posLabel1, oldPos, newPos, true);
        if (newPos == 100) {
            out.println("game:over:win:" + opponent + ":1");
        } else {
            out.println("game:" + opponent + ":" + num + ":" + newPos);
        }
        rollButt1.setDisable(true);
        if (mode == 0) { // 单机
            rollButt2.setDisable(false);
        }
    }

    // 单机时生成另一个点数
    @FXML
    public void handleRoll2(ActionEvent actionEvent) {
        Random r = new Random();
        int num = r.nextInt(6) + 1;
        System.out.println(opponent + "点数为：" + num);
        int oldPos = Integer.parseInt(posLabel2.getText());
        int newPos = oldPos + num;
        newPos = multiMovePiece(playerCircles[1], posLabel2, oldPos, newPos, false);
        if (newPos == 100) {
            out.println("game:over:win:" + name + ":2");
        } else {
            out.println("game:" + name + ":" + num + ":" + newPos);
        }
        rollButt1.setDisable(false);
        rollButt2.setDisable(true);
    }

    // 服务器传回对方点数
    public void oppoRoll(int num) {
        System.out.println(opponent + "点数为：" + num);
        int oldPos = Integer.parseInt(posLabel2.getText());
        int newPos = oldPos + num;
        multiMovePiece(playerCircles[1], posLabel2, oldPos, newPos, false);
        rollButt1.setDisable(false);
    }

    // 连续跳跃
    private int multiMovePiece(Circle circle, Label label, int oldPos, int newPos, boolean isP1) {
        if (newPos > 100) {
            newPos = 200 - newPos;
        }
        movePiece(circle, label, oldPos, newPos, isP1);
        int effect = board[(newPos - 1) / 10][(newPos - 1) % 10];
        int time = 1;
        while (effect != 0) {
            oldPos = newPos;
            newPos += effect;
            int finalOldPos = oldPos;
            int finalNewPos = newPos;
            System.out.println("Player " + current + " get effect1 " + effect + " and move to " + newPos);
            scheduler.schedule(() -> movePiece(circle, label, finalOldPos, finalNewPos, isP1), time* 800L, TimeUnit.MILLISECONDS);
            time++;
            effect = board[(newPos - 1) / 10][(newPos - 1) % 10];
        }
        return newPos;
    }


    private void movePiece(Circle circle, Label label, int oldPos, int newPos, boolean isP1) {
        Platform.runLater(() -> label.setText(String.valueOf(newPos)));
        int[] oldTrans = transformPosition(oldPos - 1);
        int[] newTrans = transformPosition(newPos - 1);
        int offset = isP1 ? 15 : 35;
        double startX = oldTrans[1] * 60 + offset;
        double startY = oldTrans[0] * 60 + offset;
        double endX = newTrans[1] * 60 + offset;
        double endY = newTrans[0] * 60 + offset;

        Timeline timeline = new Timeline();
        int frames = 60;
        for (int i = 0; i <= frames; i++) {
            final int frame = i;
            KeyFrame keyFrame = new KeyFrame(Duration.millis(frame * 800.0 / frames), _ -> {
                double t = (double) frame / frames;
                t = t * t * (3 - 2 * t); // 使用缓动公式
                double currentX = startX + t * (endX - startX);
                double currentY = startY + t * (endY - startY);
                double currentRadius = 20 - 20 * Math.abs(t - 0.5); // 从15到25再到15
                circle.setCenterX(currentX);
                circle.setCenterY(currentY);
                circle.setRadius(currentRadius);
            });
            timeline.getKeyFrames().add(keyFrame);
        }
        timeline.play();
    }


    // 检测对方是否挂机
    @FXML
    public void handleCheck() {
        out.println("game:over:check:" + opponent);
    }

    // 申请重新游戏
    @FXML
    private void handleReset() {
        out.println("game:reset:" + opponent);
    }

    // 主动结束游戏
    @FXML
    private void handleEscape() {
        out.println("game:over:escape:" + opponent);
    }

    // 下载单机存档
    @FXML
    public void handleLoad() {
        out.println("game:load");
    }
}
