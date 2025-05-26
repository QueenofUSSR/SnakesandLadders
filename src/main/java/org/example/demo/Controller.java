package org.example.demo;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    public Button propStairway;
    public Button propDeadLock;
    public Button loadBtn;
    public Button saveBtn;
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

    private int STAIRWAY_TO_HEAVEN = 1;
    private int DEAD_LOCK = 2;

    public int dead = 0;
    public int stairway = 0;

    @FXML
    private Label diceLabel;
    @FXML
    private ImageView diceGIF;

    private static final Media ButtonEffect = new Media(Objects.requireNonNull(Controller.class.getResource("/org/example/demo/audio/ButtonPressed.mp3")).toString());
    private static final MediaPlayer ButtonAudio = new MediaPlayer(ButtonEffect);

    @FXML
    public void initialize(PrintWriter out, String name, String oppo, String curr, int p1, int p2, int[][] board, int mode) {
        this.out = out;
        this.name = name;
        this.opponent = oppo;
        this.current = curr;
        this.board = board;
        this.mode = mode;
        diceLabel.setText(curr);
        diceGIF.setImage(new Image(Objects.requireNonNull(Controller.class.getResource("/org/example/demo/GIF/1.gif")).toExternalForm()));
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
                int[] tansPos = transformPosition(i * 10 + j);
                StackPane cell = new StackPane();
                VBox vbox = new VBox();
                vbox.setPrefSize(50, 50);
                vbox.setAlignment(Pos.TOP_LEFT); // 设置对齐方式为左上角
                Label positionLabel = new Label(String.valueOf(i * 10 + j + 1));
                Label effectLabel = new Label(String.valueOf(board[i][j]));
                positionLabel.setTextFill(Color.BLACK);
                effectLabel.setTextFill(Color.BLACK);
                positionLabel.setTextFill(Color.BLACK);
                positionLabel.getStyleClass().add("cell-label");
                positionLabel.styleProperty().bind(Bindings.format("-fx-font-size: %.2fpx;", cell.widthProperty().divide(3)));
                Color cellColor;
                if (board[i][j] > 0) {
                    cellColor = Color.DEEPSKYBLUE;
                    addSnakeOrLadder(gameBoard, transformPosition(i * 10 + j), transformPosition(i * 10 + j + board[i][j]), true);
                } else if (board[i][j] < 0) {
                    cellColor = Color.ORANGERED;
                    positionLabel.setTextFill(Color.WHITE);
                    effectLabel.setTextFill(Color.WHITE);
                    addSnakeOrLadder(gameBoard, transformPosition(i * 10 + j), transformPosition(i * 10 + j + board[i][j]), false);
                } else {
                    effectLabel.setVisible(false);
                    cellColor = switch ((tansPos[0] * 10 + tansPos[1]) % 4) {
                        case 0 -> Color.LIGHTYELLOW;
                        case 1 -> Color.LIGHTPINK;
                        case 2 -> Color.YELLOW;
                        case 3 -> Color.WHITE;
                        default -> Color.TRANSPARENT;
                    };
                }
                cell.setStyle("-fx-background-color: " + toHexString(cellColor) + ";");
                vbox.getChildren().add(effectLabel);
                cell.getChildren().add(vbox);
                cell.getChildren().add(positionLabel);
                gridBoard.add(cell, tansPos[1], tansPos[0]);
            }
        }
        int[] pos1 = transformPosition(p1 - 1);
        int[] pos2 = transformPosition(p2 - 1);
        addPlayerCircle(pos1[0], pos1[1], true);
        addPlayerCircle(pos2[0], pos2[1], false);
        rollButt1.setDisable(opponent.equals(current));
        rollButt2.setDisable(mode == 0 && name.equals(current));
        rollButt2.setDisable(name.equals(current));
        propStairway.setVisible(false);
        propDeadLock.setVisible(false);
        if (mode != 0) { // 匹配或排位
            propStairway.setVisible(true);
            propDeadLock.setVisible(true);
            userLabel1.setText("玩家位置");
            userLabel2.setText("对手位置");
            rollButt1.setText("投掷骰子");
            rollButt2.setText("查看敌情");
            rollButt2.setOnAction(_ -> handleCheck());
            loadBtn.setVisible(false);
            saveBtn.setVisible(false);
        }
    }

    private void addSnakeOrLadder(Pane pane, int[] src, int[] dest, boolean isLadder) {
        Canvas canvas = new Canvas(600, 600); // 根据需要设置尺寸
        GraphicsContext gc = canvas.getGraphicsContext2D();
        Color color = isLadder ? Color.rgb(100, 100, 255, 0.7) : Color.rgb(255, 100, 100, 0.7);
        double startX = src[0] * 60 + (src[0] > dest[0] ? 25 : (isLadder ? 10 : 40));
        double startY = src[1] * 60 + (src[1] > dest[1] ? 10 : (src[1] == dest[1] ? 25 : 40));
        double endX = dest[0] * 60 + (src[0] > dest[0] ? 25 : (isLadder ? 40 : 10));
        double endY = dest[1] * 60 + (src[1] > dest[1] ? 40 : (dest[1] == src[1] ? 25 : 10));
        double width = 10;
        double angle = Math.atan2(endY - startY, endX - startX);
        double halfWidth = width / 2;
        double[] xPoints = new double[4];
        double[] yPoints = new double[4];
        xPoints[0] = startX - halfWidth * Math.sin(angle); // 左上角
        yPoints[0] = startY + halfWidth * Math.cos(angle);
        xPoints[1] = startX + halfWidth * Math.sin(angle); // 右上角
        yPoints[1] = startY - halfWidth * Math.cos(angle);
        xPoints[2] = endX + halfWidth * Math.sin(angle);   // 右下角
        yPoints[2] = endY - halfWidth * Math.cos(angle);
        xPoints[3] = endX - halfWidth * Math.sin(angle);   // 左下角
        yPoints[3] = endY + halfWidth * Math.cos(angle);
        gc.setFill(color);
        gc.fillPolygon(yPoints, xPoints, 4);
        pane.getChildren().add(canvas);
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
        Color color = isP1 ? new Color(0.15, 0.15, 1, 0.8) : new Color(1, 0.15, 0.15, 0.8);
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
    public void handleRoll1() {
        Platform.runLater(() -> {
            ButtonAudio.stop();
            ButtonAudio.play();
        });

        Random r = new Random();
        int num = r.nextInt(6) + 1;

        playDiceRoll(diceGIF,num,diceLabel,name,()->{
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
        });
    }

    // 单机时生成另一个点数
    @FXML
    public void handleRoll2() {
        Platform.runLater(() -> {
            ButtonAudio.stop();
            ButtonAudio.play();
        });

        Random r = new Random();
        int num = r.nextInt(6) + 1;
        playDiceRoll(diceGIF, num, diceLabel, opponent, () -> {
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
        });
    }

    //骰子动画
    public static void playDiceRoll(ImageView imageView, int diceNum, Label diceLabel, String player, Runnable onFinished) {
        List<Image> frames = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            frames.add(new Image(Objects.requireNonNull(Controller.class.getResource("/org/example/demo/GIF/0" + i + ".gif")).toExternalForm()));
        }
        frames.add(new Image(Objects.requireNonNull(Controller.class.getResource(
                "/org/example/demo/GIF/"+ (diceNum>6?20:diceNum) + ".gif")).toExternalForm()));

        Timeline timeline = new Timeline();

        // 播放前12帧动画（每帧 50ms）
        for (int i = 0; i < frames.size(); i++) {
            final int index = i;
            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(50 * i), _ -> {
                diceLabel.setText(player);
                imageView.setImage(frames.get(index));
            }));
        }

        // 在最后加一帧：显示最终结果 diceNum.gif（时间为第13帧）
        timeline.getKeyFrames().add(
                new KeyFrame(Duration.millis(50 * frames.size()), e -> {
                    diceLabel.setText(player);
                    Image finalFrame = new Image(Objects.requireNonNull(
                            Controller.class.getResource("/org/example/demo/GIF/" + (diceNum>6?20:diceNum) + ".gif")
                    ).toExternalForm());
                    imageView.setImage(finalFrame);
                })
        );

        timeline.setOnFinished(_ -> {
            if (onFinished != null) {
                onFinished.run();
            }
        });

        timeline.play();
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
        System.out.println("Player " + (isP1 ? name : opponent) + " move from " + oldPos + " to " + newPos);
        if (newPos > 100) {
            newPos = 200 - newPos;
        }
        int finalNewPos0 = newPos;
        if (oldPos + 1 == newPos || (oldPos - 1) / 10 == (newPos - 1) / 10) {
            movePiece(circle, label, oldPos, newPos, isP1);
        } else if (oldPos % 10 == 0 || oldPos % 10 == 1) {
            int tempPos = oldPos + 1;
            movePiece(circle, label, oldPos, tempPos, isP1);
            scheduler.schedule(() -> movePiece(circle, label, tempPos, finalNewPos0, isP1), 600L, TimeUnit.MILLISECONDS);
        } else if (newPos % 10 == 0 || newPos % 10 == 1) {
            int tempPos = oldPos / 10 * 10 + 10;
            movePiece(circle, label, oldPos, tempPos, isP1);
            scheduler.schedule(() -> movePiece(circle, label, tempPos, finalNewPos0, isP1), 600L, TimeUnit.MILLISECONDS);
        } else {
            int tempPos = oldPos / 10 * 10 + 10;
            movePiece(circle, label, oldPos, tempPos, isP1);
            scheduler.schedule(() -> movePiece(circle, label, tempPos, tempPos + 1, isP1), 600L, TimeUnit.MILLISECONDS);
            scheduler.schedule(() -> movePiece(circle, label, tempPos + 1, finalNewPos0, isP1), 1200L, TimeUnit.MILLISECONDS);
        }
        int effect = board[(newPos - 1) / 10][(newPos - 1) % 10];
        int time = 1;
        while (effect != 0) {
            oldPos = newPos;
            newPos += effect;
            int finalOldPos = oldPos;
            int finalNewPos = newPos;
            System.out.println("Player " + current + " get effect1 " + effect + " and move to " + newPos);
            scheduler.schedule(() -> movePiece(circle, label, finalOldPos, finalNewPos, isP1), time * 800L, TimeUnit.MILLISECONDS);
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
        double duration = 400 + Math.sqrt((oldTrans[0] - newTrans[0]) * (oldTrans[0] - newTrans[0]) + (oldTrans[1] - newTrans[1]) * (oldTrans[1] - newTrans[1])) * 20;
        int frames = 60;
        for (int i = 0; i <= frames; i++) {
            final int frame = i;
            KeyFrame keyFrame = new KeyFrame(Duration.millis(frame * duration / frames), _ -> {
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

    public void handleSave() { out.println("game:save"); }

    public void handleStairway() {
//        STAIRWAY_TO_HEAVEN--;
//        stairway = 4;
        out.println("STAIRWAY_TO_HEAVEN:" + opponent);
//        propStairway.setDisable(STAIRWAY_TO_HEAVEN==0);
    }
    public void handleDeadLock() {
//        DEAD_LOCK--;
//        dead = 3;
        out.println("DEAD_LOCK:" + opponent);
//        propDeadLock.setDisable(DEAD_LOCK==0);
    }
}
