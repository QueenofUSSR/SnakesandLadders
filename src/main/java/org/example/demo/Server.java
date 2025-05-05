package org.example.demo;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class Server {
    private static final HashMap<String, PrintWriter> onlineOut = new HashMap<>(); // （在线用户，通信输出）
    private static final HashMap<String, String> matching = new HashMap<>(); // （匹配中的用户，游戏模式）
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1); // 定时器
    private static final HashMap<String, Game> games = new HashMap<>(); // （用户名，所在对局）
    private static final int port = 12345;
    private static final long sleepTime = 600 * 1000; // 瞌睡时间设置为10分钟
    private static Map<String, String> users; // （用户名，密码）
    private static Map<String, List<String>> records; // （用户名，战绩列表）

    public static void main(String[] args) {
        // 创建一个线程池
        try (ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newCachedThreadPool()) {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                loadUsers();
                loadRecords();
                System.out.println("服务器在" + port + "苏醒了！");
                scheduler.scheduleAtFixedRate(Server::broadcastOnlinePlayers, 0, 2, TimeUnit.SECONDS);
                AtomicLong lastConn = new AtomicLong(System.currentTimeMillis());
                Thread serverStopped = new Thread(() -> {
                    while (true) {
                        if (pool.getActiveCount() == 0 && System.currentTimeMillis() - lastConn.get() > sleepTime) {
                            System.out.println("由于无人在意，服务器睡着啦！");
                            System.exit(0);
                        }
                        try {
                            System.out.println("10秒睁一次眼");
                            Thread.sleep(10 * 1000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                });
                serverStopped.start();
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Client connected " + clientSocket.getInetAddress());
                    lastConn.set(System.currentTimeMillis());
                    pool.execute(new ClientHandler(clientSocket));
                }
            } catch (IOException e) {
                System.out.println("服务器粗错啦: " + e.getMessage());
            } finally {
                pool.shutdown();
            }
        }
    }

    // 遍历在线人员列表，给不在游戏的人发送所有玩家状态（默认都是好友）
    private static void broadcastOnlinePlayers() {
        for (Map.Entry<String, PrintWriter> entry : onlineOut.entrySet()) {
            if (!games.containsKey(entry.getKey())) {
                StringBuilder s = new StringBuilder();
                for (String u : users.keySet()) {
                    s.append(u).append(",");
                    if (!onlineOut.containsKey(u)) {
                        s.append("F;");
                    } else if (games.containsKey(u)) {
                        s.append("G;");
                    } else if (matching.containsKey(u)) {
                        s.append("M;");
                    } else {
                        s.append("O;");
                    }
                }
                entry.getValue().println("players:" + s);
            }
        }
    }

    public static void loadUsers() {
        users = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader("src/main/resources/org/example/demo/users.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    Server.users.put(parts[0], parts[1]);
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void loadRecords() {
        records = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader("src/main/resources/org/example/demo/records.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                records.computeIfAbsent(parts[0], _ -> new ArrayList<>()).add(line.substring(parts[0].length() + 1));
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void saveUsers() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("src/main/resources/org/example/demo/users.txt"))) {
            for (Map.Entry<String, String> entry : users.entrySet()) {
                writer.write(entry.getKey() + ":" + entry.getValue());
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void saveRecords() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("src/main/resources/org/example/demo/records.txt"))) {
            for (Map.Entry<String, List<String>> entry : records.entrySet()) {
                List<String> record = entry.getValue();
                for (String r : record) {
                    writer.write(entry.getKey() + ":" + r);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private static String loadGame(String name) {
        Path filePath = Paths.get("src/main/resources/org/example/demo/savedGame/" + name + ".txt");
        try {
            return Files.readString(filePath).trim();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private String name = null;
        private BufferedReader in;
        private PrintWriter out;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void checkGame() {
            if (games.containsKey(name)) {
                Game g = games.get(name);
                if (g.u1.equals(name)) {
                    out.println("reGame:" + g.u2 + ":" + g.curr + ":" + g.p1 + ":" + g.p2 + ":" + g.board + ":" + g.mode);
                } else {
                    out.println("reGame:" + g.u1 + ":" + g.curr + ":" + g.p2 + ":" + g.p1 + ":" + g.board + ":" + g.mode);
                }
            }
        }

        private void forward(String oppo, String msg) {
            if (onlineOut.containsKey(oppo)) {
                onlineOut.get(oppo).println(msg);
            }
        }

        private String getRecord(String name, String res, String oppo, Game game) {
            long diff = (System.currentTimeMillis() - Long.parseLong(game.time)) / 1000;
            return name + "  《" + res + "》  " + oppo + "   " + "   " + String.format("%d分%d秒", diff / 60, diff % 60) + "   " + game.start;
        }


        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("Client" + clientSocket.getInetAddress() + "/" + name + ": " + message);
                    if (message.contains(":")) {
                        String[] parts = message.split(":");
                        if (name == null) {
                            if (onlineOut.containsKey(parts[1])) {
                                out.println("Account is already logged in elsewhere");
                            } else {
                                if ("register".equals(parts[0])) {
                                    if (parts.length == 3 && !users.containsKey(parts[1])) {
                                        users.put(parts[1], parts[2]);
                                        saveUsers();
                                        name = parts[1];
                                        onlineOut.put(parts[1], out);
                                        out.println("login successfully");
                                        checkGame();
                                    } else {
                                        out.println("This name is already in use!");
                                    }
                                } else if ("login".equals(parts[0])) {
                                    if (parts.length == 3 && users.containsKey(parts[1]) && users.get(parts[1]).equals(parts[2])) {
                                        name = parts[1];
                                        onlineOut.put(parts[1], out);
                                        out.println("login successfully");
                                        checkGame();
                                    } else {
                                        out.println("The name or password is incorrect!");
                                    }
                                }
                            }
                        } else {
                            if ("record".equals(parts[0])) {
                                if (parts.length == 2 && users.containsKey(parts[1])) {
                                    List<String> record = records.get(parts[1]);
                                    out.println("record:" + (record == null ? "" : String.join(";", record)));
                                }
                            } else if ("logout".equals(parts[0])) {
                                if (parts.length == 2 && onlineOut.containsKey(parts[1])) {
                                    onlineOut.remove(name);
                                    name = null;
                                    out.println("logout successfully");
                                }
                            } else if ("invite".equals(parts[0])) {
                                if (parts.length == 2) {
                                    if ("cancel".equals(parts[1])) {
                                        matching.remove(name);
                                    } else if ("随机匹配".equals(parts[1]) || "高端排位".equals(parts[1])) {
                                        String waiting = null;
                                        String type = null;
                                        for (Map.Entry<String, String> entry : matching.entrySet()) {
                                            if (parts[1].equals(entry.getValue())) {
                                                waiting = entry.getKey();
                                                type = parts[1];
                                                break;
                                            }
                                        }
                                        if (waiting == null) {
                                            matching.put(name, parts[1]);
                                            out.println("invite Result:no body");
                                        } else {
                                            matching.remove(waiting);
                                            forward(waiting, "be matched:" + name + ":" + type);
                                        }
                                    } else { // 单机直接传送board
                                        games.put(name, new Game(name, name + "2", name, "1", "1", parts[1], "单机模式", ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), String.valueOf(System.currentTimeMillis())));
                                    }
                                } else if (parts.length == 3) {
                                    forward(parts[2], "be invited:" + name);
                                }
                            } else if ("invite Result".equals(parts[0])) {
                                if ("accept".equals(parts[2])) {
                                    matching.remove(name);
                                    matching.remove(parts[1]);
                                    String startTime = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                                    Game g = new Game(parts[1], name, parts[3], "1", "1", parts[4], parts[5], startTime, String.valueOf(System.currentTimeMillis()));
                                    games.put(name, g);
                                    games.put(parts[1], g);
                                    forward(parts[1], "invite Result:" + name + ":" + parts[2] + ":" + parts[3] + ":" + parts[4] + ":" + parts[5]);
                                } else {
                                    forward(parts[1], "invite Result:" + name + ":" + parts[2]);
                                }
                            } else if ("game".equals(parts[0])) {
                                if (games.containsKey(name)) {
                                    Game g = games.get(name);
                                    switch (parts[1]) {
                                        case "over" -> {
                                            boolean over = false;
                                            if ("escape".equals(parts[2])) {
                                                out.println("game:over:escape");
                                                if (!"单机模式".equals(g.mode)) {
                                                    forward(parts[3], "game:over:opponent escape");
                                                    records.computeIfAbsent(name, _ -> new ArrayList<>()).add(getRecord(name, "逃跑", parts[3], g));
                                                    records.computeIfAbsent(parts[3], _ -> new ArrayList<>()).add(getRecord(parts[3], "完胜", name, g));
                                                }
                                                over = true;
                                            } else if ("check".equals(parts[2]) && !onlineOut.containsKey(parts[3])) {
                                                out.println("game:over:opponent escape");
                                                records.computeIfAbsent(name, _ -> new ArrayList<>()).add(getRecord(name, "躺赢", parts[3], g));
                                                records.computeIfAbsent(parts[3], _ -> new ArrayList<>()).add(getRecord(parts[3], "挂机", name, g));
                                                over = true;
                                            } else if ("win".equals(parts[2])) {
                                                if (!"单机模式".equals(g.mode)) {
                                                    out.println("game:over:victory");
                                                    forward(parts[3], "game:over:defeat");
                                                    records.computeIfAbsent(name, _ -> new ArrayList<>()).add(getRecord(name, "胜利", parts[3], g));
                                                    records.computeIfAbsent(parts[3], _ -> new ArrayList<>()).add(getRecord(parts[3], "失败", name, g));
                                                } else {
                                                    out.println("1".equals(parts[4]) ? "game:over:victory" : "game:over:defeat");
                                                }
                                                over = true;
                                            }
                                            if (over) {
                                                games.remove(name);
                                                System.out.println("游戏结束，" + name + "的对局已被删除");
                                                if (!"单机模式".equals(g.mode)) {
                                                    games.remove(parts[3]);
                                                    System.out.println(parts[3] + "的对局已被删除");
                                                    saveRecords();
                                                }
                                            }
                                        }
                                        case "reset" -> {
                                            if ("单机模式".equals(g.mode)) {
                                                g.p1 = "1";
                                                g.p2 = "1";
                                                g.curr = name;
                                                forward(name, "game:reset accept:" + name + "2:" + g.u1 + ":" + g.board + ":" + g.mode);
                                            } else {
                                                forward(parts[2], "game:reset:" + name + ":" + g.u1 + ":" + g.board + ":" + g.mode);
                                            }
                                        }
                                        case "reset accept" -> {
                                            g.p1 = "1";
                                            g.p2 = "1";
                                            g.curr = g.u1;
                                            forward(parts[2], "game:reset accept:" + name + ":" + g.u1 + ":" + g.board + ":" + g.mode);
                                        }
                                        case "load" -> {
                                            String load = loadGame(name);
                                            if (load != null) {
                                                String[] loads = load.split(":");
                                                g.curr = loads[0];
                                                g.p1 = loads[1];
                                                g.p2 = loads[2];
                                                g.board = loads[3];
                                                out.println("game:load:" + g.curr + ":" + g.p1 + ":" + g.p2 + ":" + g.board);
                                            }
                                        }
                                        default -> {
                                            g.curr = parts[1];
                                            if (g.u1.equals(g.curr)) {
                                                g.p2 = parts[3];
                                            } else {
                                                g.p1 = parts[3];
                                            }
                                            if (!"单机模式".equals(g.mode)) {
                                                forward(parts[1], "game:" + parts[2]);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if ("exit".equals(message)) {
                        break;
                    } else {
                        out.println("invalid message: " + message);
                    }
                }
            } catch (IOException e) {
                System.out.println(name + " " + e.getMessage());
            } finally {
                try {
                    if (name != null) {
                        matching.remove(name);
                        onlineOut.remove(name);
                        // 联机模式下等待重联或对方结束比赛，单机自动保存
                        if (games.containsKey(name) && "单机模式".equals(games.get(name).mode)) {
                            Game g = games.remove(name);
                            Path filePath = Paths.get("src/main/resources/org/example/demo/savedGame/" + name + ".txt");
                            try {
                                if (Files.exists(filePath)) {
                                    Files.writeString(filePath, "", StandardOpenOption.WRITE);
                                } else {
                                    Files.createFile(filePath);
                                }
                                Files.writeString(filePath, g.curr + ":" + g.p1 + ":" + g.p2 + ":" + g.board);
                            } catch (IOException e) {
                                System.out.println(e.getMessage());
                            }
                        }
                    }
                    if (in != null) in.close();
                    if (out != null) out.close();
                    if (clientSocket != null) {
                        System.out.println(clientSocket.getInetAddress() + " disconnected");
                        clientSocket.close();
                    }
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }

    static class Game {
        String u1;
        String u2;
        String curr;
        String p1;
        String p2;
        String board;
        String mode;
        String start;
        String time;

        public Game(String u1, String u2, String curr, String p1, String p2, String board, String mode, String start, String time) {
            this.u1 = u1;
            this.u2 = u2;
            this.curr = curr;
            this.p1 = p1;
            this.p2 = p2;
            this.board = board;
            this.mode = mode;
            this.start = start;
            this.time = time;
        }
    }
}
