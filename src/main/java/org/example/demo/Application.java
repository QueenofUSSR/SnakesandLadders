package org.example.demo;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;


public class Application extends javafx.application.Application {
    static Label connLabel;
    static BufferedReader in;
    static PrintWriter out;
    static Socket socket;
    static String name = null;
    static Scene loginScene;
    static Scene lobbyScene;
    static Thread lobbyThread;
    static Scene recordScene;
    private final ListView<String> historyRecordList = new ListView<>();
    private final List<Alert> alerts = new ArrayList<>();
    boolean openRecord = false;
    Controller controller;

    public static void main(String[] args) {
        launch(args);
    }

    private static int[][] setupBoard() {
        int[][] matrix = new int[10][10];
        Random r = new Random();
        Set<String> uniquePoints = new HashSet<>();
        int num = r.nextInt(10, 17);
        int[] count = new int[]{0, 0};
        while (uniquePoints.size() < num) {
            int x = r.nextInt(10);
            int y = r.nextInt(10);
            if (!uniquePoints.contains(x + "," + y) && !(x == 0 && y == 0) && !(x == 9 && y == 9)) {
                uniquePoints.add(x + "," + y);
                int pos = x * 10 + y + 1;
                int bound = (pos > 30 && pos < 70) ? 30 : Math.min(pos - 1, 100 - pos);
                matrix[x][y] = r.nextInt(Math.min(count[1] - bound, -1), Math.max(bound - count[0], 1));
                if (matrix[x][y] > 0) {
                    count[0]++;
                } else if (matrix[x][y] < 0) {
                    count[1]++;
                }
            }
        }
        System.out.println("Create a board:");
        for (int[] ints : matrix) {
            for (int anInt : ints) {
                System.out.print(anInt + " ");
            }
            System.out.println();
        }
        return matrix;
    }

    public static int[][] parseBoard(String sb) {
        int[][] board = new int[10][10];
        String[] rowStrings = sb.split(";");
        for (int i = 0; i < board.length; i++) {
            String[] cellStrings = rowStrings[i].split(",");
            for (int j = 0; j < board[i].length; j++) {
                board[i][j] = Integer.parseInt(cellStrings[j]);
            }
        }
        return board;
    }

    private static StringBuilder getStringBoard(int[][] board) {
        StringBuilder sb = new StringBuilder();
        for (int[] r : board) {
            for (int cell : r) {
                sb.append(cell).append(",");
            }
            sb.append(";");
        }
        return sb;
    }

    @Override
    public void start(Stage stage) {
        double w = 800;
        double h = 450;
        Pane connPane = connectPane(stage, w, h);
        Scene connScene = new Scene(connPane, w, h);
        Platform.runLater(() -> {
            stage.setTitle("连接服务器");
            stage.setScene(connScene);
            stage.show();
            stage.setOnCloseRequest(_ -> exit(stage));
        });
        new Thread(() -> {
            int tryNum = 0;
            while (++tryNum < 10) {
                try {
                    Thread.sleep(2000);
                    socket = new Socket("localhost", 12345);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    out = new PrintWriter(socket.getOutputStream(), true);
                    break;
                } catch (IOException | InterruptedException e) {
                    System.out.println(e.getMessage());
                }
            }
            int finalTryNum = tryNum;
            Platform.runLater(() -> {
                if (finalTryNum == 10) {
                    connLabel.setText("   木有骰子了T^T，请稍后再尝试登录。\n\n\n\n\n\n\n\n\n\n\n如需查询服务器重启时间，请访问游戏官网：\nhttps://github.com/solovesonxi/SnakeLadderGame");
                    connLabel.setLayoutX(w / 2 - 180);
                    connLabel.setLayoutY(h / 2 - 100);
                } else {
                    Pane loginPane = loginPane(stage, w, h);
                    loginScene = new Scene(loginPane, w, h);
                    stage.setTitle("用户登录");
                    stage.setScene(loginScene);
                    stage.show();
                }
            });
        }).start();
    }

    private Pane connectPane(Stage stage, double w, double h) {
        Pane pane = new Pane();
        connLabel = new Label("死命加载中，请耐心等待喔~\n\n\n\n\n\n\n\n\n\n\n\n        制作人：陈世有，朱炫睿，李皓玥");
        connLabel.setFont(Font.font("楷体", FontWeight.BOLD, 18));
        connLabel.setTextFill(Color.BLACK);
        connLabel.setLayoutX(w / 2 - 140);
        connLabel.setLayoutY(h / 2 - 105);
        Image backgroundImage = new Image("file:src/main/resources/org/example/demo/background.png");
        BackgroundImage background = new BackgroundImage(backgroundImage, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, true, true));
        Button exitButton = new Button("退出游戏");
        exitButton.setLayoutX(w - 75);
        exitButton.setLayoutY(h - 30);
        exitButton.setOnAction(_ -> {
            stage.close();
            System.exit(0);
        });
        pane.setBackground(new Background(background));
        pane.getChildren().add(connLabel);
        pane.getChildren().add(exitButton);
        return pane;
    }

    private Pane loginPane(Stage stage, double w, double h) {
        Pane pane = new Pane();
        Label userLabel = new Label("用户名:");
        userLabel.setFont(Font.font("楷体", FontWeight.BOLD, 18));
        userLabel.setTextFill(Color.BLACK);
        userLabel.setLayoutX(w / 2 - 160);
        userLabel.setLayoutY(h / 2 - 60);

        TextField userTextField = new TextField();
        userTextField.setLayoutX(w / 2 - 50);
        userTextField.setLayoutY(h / 2 - 60);

        Label pwLabel = new Label("密码:");
        pwLabel.setFont(Font.font("楷体", FontWeight.BOLD, 18));
        pwLabel.setTextFill(Color.BLACK);
        pwLabel.setLayoutX(w / 2 - 160);
        pwLabel.setLayoutY(h / 2);

        PasswordField pwBox = new PasswordField();
        pwBox.setLayoutX(w / 2 - 50);
        pwBox.setLayoutY(h / 2);

        Image backgroundImage = new Image("file:src/main/resources/org/example/demo/background.png");
        BackgroundImage background = new BackgroundImage(backgroundImage, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, true, true));
        Button registerButton = new Button("注册");
        registerButton.setLayoutX(w / 2 - 70);
        registerButton.setLayoutY(h / 2 + 40);
        Button loginButton = new Button("登录");
        loginButton.setLayoutX(w / 2 + 30);
        loginButton.setLayoutY(h / 2 + 40);
        Button exitButton = new Button("退出游戏");
        exitButton.setLayoutX(w - 75);
        exitButton.setLayoutY(h - 30);
        registerButton.setOnAction(_ -> handleLogin(stage, "register", userTextField.getText(), pwBox.getText()));
        loginButton.setOnAction(_ -> handleLogin(stage, "login", userTextField.getText(), pwBox.getText()));
        exitButton.setOnAction(_ -> handleLogin(stage, "exit", userTextField.getText(), pwBox.getText()));
        pane.setBackground(new Background(background));
        pane.getChildren().add(userLabel);
        pane.getChildren().add(userTextField);
        pane.getChildren().add(pwLabel);
        pane.getChildren().add(pwBox);
        pane.getChildren().add(registerButton);
        pane.getChildren().add(loginButton);
        pane.getChildren().add(exitButton);
        return pane;
    }

    private void handleLogin(Stage stage, String choice, String username, String password) {
        System.out.println(choice + ": 用户名=" + username + ", 密码=" + password + "->" + hashPassword(password));
        if ("exit".equals(choice)) {
            exit(stage);
        } else {
            out.println(choice + ":" + username + ":" + hashPassword(password));
            String response = null;
            try {
                response = in.readLine();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
            System.out.println("Server1: " + response);
            if ("login successfully".equals(response)) {
                name = username;
                lobbyPane(stage);
            }
        }

    }

    private void exit(Stage stage) {
        try {
            if (out != null) {
                out.println("exit");
                out.close();
            }
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
            stage.close();
            System.exit(0);
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    private void lobbyPane(Stage stage) {
        VBox lobbyRoot = new VBox(10);  // 设置 VBox 间距为 10
        lobbyRoot.setPadding(new Insets(10)); // 设置 VBox 的内边距
        System.out.println("加载游戏大厅");
        lobbyThread = new Thread(() -> handleLobby(stage, lobbyRoot));
        lobbyThread.start();
        lobbyScene = new Scene(lobbyRoot, 440, 460);
        Platform.runLater(() -> {
            stage.setTitle("游戏大厅");
            stage.setScene(lobbyScene);
            stage.show();
        });
    }

    private void handleLobby(Stage stage, VBox lobbyRoot) {
        String response;
        while (name != null) {
            try {
                if ((response = in.readLine()) != null) {
                    System.out.println("Server2: " + response);
                    if ("logout successfully".equals(response)) {
                        Platform.runLater(() -> {
                            stage.setTitle("用户登录");
                            stage.setScene(loginScene);
                            stage.show();
                        });
                        name = null;
                        break;
                    }
                    if (response.startsWith("players:")) {
                        String[] players = response.substring(8).split(";");
                        Arrays.sort(players, (p1, p2) -> {
                            if (p1.startsWith(name)) return -1;
                            if (p2.startsWith(name)) return 1;
                            return Boolean.compare(p1.endsWith("F"), p2.endsWith("F"));
                        });
                        Platform.runLater(() -> {
                            List<HBox> HBoxes = new ArrayList<>();
                            for (String player : players) {
                                HBox box = new HBox(12); // 设置 HBox 间距为 12
                                box.setAlignment(Pos.CENTER_LEFT); // 左对齐
                                box.setPadding(new Insets(6)); // 设置 HBox 的内边距
                                String[] parts = player.split(",");
                                Label nameLabel = new Label(parts[0] + switch (parts[1]) {
                                    case "O" -> " （在线）";
                                    case "M" -> " （匹配中）";
                                    case "G" -> " （游戏中）";
                                    default -> " （离线）";
                                });
                                box.getChildren().add(nameLabel);
                                if (name.equals(parts[0])) {
                                    box.setStyle("-fx-background-color: #ADD8E6;");
                                } else if ("F".equals(parts[1])) {
                                    box.setStyle("-fx-background-color: #D3D3D3;");
                                } else {
                                    box.setStyle("-fx-background-color: #90EE90;");
                                    if ("O".equals(parts[1])) { // 只能邀请在线玩家打匹配
                                        Button butt = new Button("邀请游戏");
                                        butt.setOnAction(_ -> out.println("invite:随机匹配:" + parts[0]));
                                        box.getChildren().add(butt);
                                    }
                                }
                                box.setOnMouseClicked(e -> handleRecord(stage, e, parts[0]));
                                HBoxes.add(box);
                            }
                            HBox buttonBox = new HBox(20);
                            buttonBox.setAlignment(Pos.CENTER);
                            buttonBox.setPadding(new Insets(10));
                            Button modeButt = new Button("开始游戏");
                            Button rankButt = new Button("查看榜单");
                            Button backButt = new Button("退出登录");
                            modeButt.setOnAction(_ -> selectMode(stage));
                            rankButt.setOnAction(_ -> checkoutLeaderboard());
                            backButt.setOnAction(_ -> out.println("logout:" + name));
                            buttonBox.getChildren().addAll(modeButt, rankButt, backButt);
                            lobbyRoot.getChildren().setAll(HBoxes);
                            lobbyRoot.getChildren().add(buttonBox);
                        });
                    } else if (response.startsWith("record:")) {
                        String[] record = response.substring(7).split(";");
                        Platform.runLater(() -> historyRecordList.getItems().setAll(record));
                    } else if (response.startsWith("be invited:")) {
                        String[] parts = response.split(":");
                        Platform.runLater(() -> {
                            Alert alt = createAlert("邀请通知", "你收到了一条邀请", parts[1] + " 邀请你进行一场比赛。\n");
                            ButtonType accButt = new ButtonType("接受", ButtonBar.ButtonData.OK_DONE);
                            ButtonType refButt = new ButtonType("拒绝", ButtonBar.ButtonData.CANCEL_CLOSE);
                            alt.getButtonTypes().setAll(accButt, refButt);
                            Optional<ButtonType> result = alt.showAndWait();
                            if (result.isPresent() && result.get().equals(accButt)) {
                                int[][] board = setupBoard();
                                Random r = new Random();
                                String current = r.nextInt(2) == 1 ? name : parts[1];
                                out.println("invite Result:" + parts[1] + ":accept:" + current + ":" + getStringBoard(board) + ":" + "随机匹配");
                                handleGame(stage, parts[1], current, 1, 1, board, "随机匹配");
                            } else {
                                out.println("invite Result:" + parts[1] + ":refuse");
                            }
                        });
                    } else if (response.startsWith("be matched:")) {
                        String[] parts = response.split(":");
                        Platform.runLater(() -> {
                            System.out.println("Alert数量：" + alerts.size());
                            if (!alerts.isEmpty()) {
                                alerts.getLast().close();
                                alerts.clear();
                            }
                            int[][] board = setupBoard();
                            Random r = new Random();
                            String current = r.nextInt(2) == 1 ? name : parts[1];
                            out.println("invite Result:" + parts[1] + ":accept:" + current + ":" + getStringBoard(board) + ":" + parts[2]);
                            handleGame(stage, parts[1], current, 1, 1, board, parts[2]);
                        });
                    } else if (response.startsWith("invite Result:")) {
                        String[] parts = response.split(":");
                        Platform.runLater(() -> {
                            if ("no body".equals(parts[1])) {
                                Alert alt = createAlert("匹配通知", "你太强了，暂时没有找到合适的对手！", "请耐心等待喔~");
                                alerts.add(alt);
                                ButtonType cancelButt = new ButtonType("取消匹配", ButtonBar.ButtonData.CANCEL_CLOSE);
                                alt.getButtonTypes().setAll(cancelButt);
                                Optional<ButtonType> result = alt.showAndWait();
                                if (result.isPresent() && result.get().equals(cancelButt)) {
                                    out.println("invite:cancel");
                                    alerts.remove(alt);
                                }
                            } else if ("accept".equals(parts[2])) {
                                handleGame(stage, parts[1], parts[3], 1, 1, parseBoard(parts[4]), parts[5]);
                            } else {
                                createAlert("邀请结果通知", "你的邀请已被拒", parts[1] + "还没开打就已经害怕了").show();
                            }
                        });
                    } else if (response.startsWith("game:")) {
                        String[] parts = response.split(":");
                        switch (parts[1]) {
                            case "over" -> Platform.runLater(() -> {
                                if (controller != null) {
                                    Alert alt = switch (parts[2]) {
                                        case "opponent escape" ->
                                                createAlert("对局结束", "大获全胜", "666对手被你吓破跑了");
                                        case "victory" ->
                                                createAlert("对局结束", "胜利", controller.mode != 0 ? "恭喜你获得胜利(^o^)" : "恭喜玩家1获得胜利(^o^)");
                                        case "defeat" ->
                                                createAlert("对局结束", "失败", controller.mode != 0 ? "失败乃成功之母(T^T)" : "恭喜玩家2获得胜利(^o^)");
                                        default ->
                                                createAlert("对局结束", "逃跑", controller.mode != 0 ? "三十六计走为上计，你是真的6" : "你已退出单机模式对局");
                                    };
                                    alt.showAndWait();
                                    System.out.println("游戏结束了哈");
                                    stage.setTitle("游戏大厅");
                                    stage.setScene(lobbyScene);
                                    stage.show();
                                }
                            });
                            case "reset" -> {
                                Platform.runLater(() -> {
                                    Alert alt = createAlert("对局通知", "重新开始", parts[2] + "想要和你重新开始本场对局。\n");
                                    ButtonType accButt = new ButtonType("接受", ButtonBar.ButtonData.OK_DONE);
                                    ButtonType refButt = new ButtonType("拒绝", ButtonBar.ButtonData.CANCEL_CLOSE);
                                    alt.getButtonTypes().setAll(accButt, refButt);
                                    Optional<ButtonType> result = alt.showAndWait();
                                    if (result.isPresent() && result.get().equals(accButt)) {
                                        out.println("game:reset accept:" + parts[2]);
                                        handleGame(stage, parts[2], parts[3], 1, 1, parseBoard(parts[4]), parts[5]);
                                    }
                                });
                            }
                            case "reset accept" ->
                                    handleGame(stage, parts[2], parts[3], 1, 1, parseBoard(parts[4]), parts[5]);
                            case "load" ->
                                    handleGame(stage, name + "2", parts[2], Integer.parseInt(parts[3]), Integer.parseInt(parts[4]), parseBoard(parts[5]), "单机模式");
                            default -> {
                                if (controller != null) {
                                    controller.oppoRoll(Integer.parseInt(parts[1]));
                                }
                            }
                        }
                    } else if (response.startsWith("reGame:")) {
                        String[] parts = response.split(":");
                        Platform.runLater(() -> {
                            ButtonType accButt = new ButtonType("确定，王者归来", ButtonBar.ButtonData.OK_DONE);
                            ButtonType refButt = new ButtonType("算了，放他一马", ButtonBar.ButtonData.CANCEL_CLOSE);
                            Alert alt = createAlert("对局通知", "你还有暂未完成的对局", "是否要继续？");
                            alt.getButtonTypes().setAll(accButt, refButt);
                            Optional<ButtonType> result = alt.showAndWait();
                            if (result.isPresent() && result.get().equals(accButt)) {
                                handleGame(stage, parts[1], parts[2], Integer.parseInt(parts[3]), Integer.parseInt(parts[4]), parseBoard(parts[5]), parts[6]);
                            } else {
                                out.println("game:over:escape:" + parts[1]);
                            }
                        });
                    }
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    public Alert createAlert(String title, String headText, String content) {
        return new Alert(Alert.AlertType.INFORMATION, content) {{
            setTitle(title);
            setHeaderText(headText);
        }};
    }

    private void handleRecord(Stage stage, MouseEvent e, String player) {
        if (e.getClickCount() == 2 && !openRecord) {
            System.out.println("查看玩家" + player + "历史战绩");
            out.println("record:" + player);
            VBox recordRoot = new VBox();
            recordRoot.getChildren().add(historyRecordList);
            Button backButton = new Button("返回");
            backButton.setAlignment(Pos.CENTER);
            backButton.setOnAction(_ -> Platform.runLater(() -> {
                stage.setScene(lobbyScene);
                stage.show();
                openRecord = false;
            }));
            openRecord = true;
            recordRoot.getChildren().add(backButton);
            recordScene = new Scene(recordRoot, 440, 460);
            stage.setTitle("玩家" + player + "历史战绩");
            stage.setScene(recordScene);
            stage.show();
        }
    }

    // 选择游戏模式
    private void selectMode(Stage stage) {
        ChoiceDialog<String> dialog = new ChoiceDialog<>("单机模式", "单机模式", "随机匹配", "高端排位");
        dialog.setTitle("模式选择");
        dialog.setContentText("请选择游戏模式：");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(choice -> {
            System.out.println("游戏模式: " + choice);
            if (choice.equals("单机模式")) {
                int[][] board = setupBoard();
                handleGame(stage, name + "2", name, 1, 1, board, choice);
                out.println("invite:" + getStringBoard(board));
            } else {
                out.println("invite:" + choice);
            }
        });
    }

    // 查看排行榜：实时计算/对局结束更新
    private void checkoutLeaderboard() {

    }

    //最开始分发棋盘和重新载入时调用
    private void handleGame(Stage stage, String oppo, String curr, int p1, int p2, int[][] board, String mode) {
        FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("board.fxml"));
        try {
            VBox gameRoot = fxmlLoader.load();
            controller = fxmlLoader.getController();
            controller.initialize(out, name, oppo, curr, p1, p2, board, ("单机模式".equals(mode) ? 0 : "随机匹配".equals(mode) ? 1 : 2));
            Platform.runLater(() -> {
                stage.setTitle("与" + oppo + "的对战");
                stage.setScene(new Scene(gameRoot, 500 + 300, 500 + 160));
                stage.show();
            });
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }
}