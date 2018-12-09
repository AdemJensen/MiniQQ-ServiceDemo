package com.ChenHui;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

/*
 * 具体的实现方法如下：
 * 首先是主线程，负责接收客户端的连接请求。
 * 每当接收一个连接请求，我们都将它的输入流和输出流保存下来
 * 然后我们单纯为每一个客户端开一个接收用的Thread，一旦读取到信息，我们就获取信息，并遍历主存储给在线的client发送消息。
 * 输出信息完全不需要thread。
 */

public class Server {
    public static void main(String[] args) {
        ServerClientRequestReceiverThread clientRequestReceiver = new ServerClientRequestReceiverThread();

        clientRequestReceiver.start();
        System.out.println("[INSTANCE] The server has started.");
        Scanner sc = new Scanner(System.in);
        while (true) {
            if (sc.next().equals("EXIT")) {
                System.out.println("[INSTANCE] The server has stopped.");
                MasterDataStorage.masterSystemRunning = false;
                System.exit(0);
            }
        }
    }
}

class MasterDataStorage {
    public static boolean masterSystemRunning = true;
    static class StorageElement {
        public boolean enabled;
        public BufferedReader iStream;
        public PrintWriter oStream;
        public Socket socket;
        public StorageElement() {
            this.enabled = false;
        }
        public StorageElement(Socket socket, BufferedReader iStream, PrintWriter oStream) {
            this.enabled = true;
            //将stream转换成BufferedReader，能够使外部操作更加方便
            this.iStream = iStream;
            //将stream转换成PrintWriter，能够使外部操作更加方便
            this.oStream = oStream;
            this.socket = socket;
        }
    }
    public static int length = ClientDatabase.length;
    private static StorageElement[] contents = new StorageElement[length];
    public static void set(int serial, Socket socket, BufferedReader iStream, PrintWriter oStream) {
        contents[serial] = new StorageElement(socket, iStream, oStream);
    }
    public static BufferedReader getBufferedReader(int serial) {
        return contents[serial].iStream;
    }
    public static PrintWriter getPrintWriter(int serial) {
        return contents[serial].oStream;
    }
    public static Socket getSocket(int serial) {
        return contents[serial].socket;
    }
}

class ServerClientRequestReceiverThread extends Thread {    //副线程，用于接受客户端的连接请求
    @Override
    public void run() {
        ServerSocket serverSocket;// 创建服务器Socket对象
        try {
            serverSocket = new ServerSocket(9999);// 监听客户端的连接
            while (MasterDataStorage.masterSystemRunning) {
                Socket clientSocket = serverSocket.accept(); // 阻塞
                ClientValidationThread validator = new ClientValidationThread(clientSocket);
                validator.start();
                new ClientValidationTimerThread(validator).start();
            }
        } catch (IOException e) {
            System.out.println("[ERROR] The server has crashed.");
            System.exit(1);
        }

    }
}

class ClientValidationThread extends Thread {
    public String status;
    private Socket target;
    public ClientValidationThread(Socket target) {
        this.target = target;
    }
    @Override
    public void run() {
        try {
            System.out.println("[NOTICE] A client is attempting to validate.");
            BufferedReader checker = new BufferedReader(new InputStreamReader(target.getInputStream()));
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(target.getOutputStream()));
            this.status = "waiting";
            String username = checker.readLine();
            String password = checker.readLine();
            this.status = "validating";
            synchronized (ClientDatabase.class) {
                int gotUser = ClientDatabase.validateUser(username, password);
                if (gotUser > -1 && !ClientDatabase.isOnline(gotUser)) {
                    synchronized (MasterDataStorage.class) {
                        MasterDataStorage.set(gotUser, target, checker, writer);
                        writer.println("success");
                        writer.flush();
                        (new ClientSenderThread(gotUser)).start();
                        ClientDatabase.bringOnline(gotUser);
                        System.out.printf("[NOTICE] A client has logged online (%d)\n", gotUser);
                    }
                    int count = 0;
                    writer.print("[ROOM] Online users:");
                    for (int i = 0; i < ClientDatabase.length; i++) {
                        if (ClientDatabase.isOnline(i)) {
                            count++;
                            if (i == gotUser) continue;
                            writer.print(" {" + ClientDatabase.getName(i) + "}");
                            synchronized (MasterDataStorage.class) {
                                PrintWriter Overwrite = MasterDataStorage.getPrintWriter(i);
                                Overwrite.printf("[ROOM] *%s* has logged on.\n", ClientDatabase.getName(gotUser));
                                Overwrite.flush();
                            }
                        }
                    }
                    writer.printf("\n[ROOM] There are %d users in this room.\n", count);
                    writer.flush();
                } else {
                    writer.println("fail");
                    writer.flush();
                    System.out.println("[NOTICE] Client validation has failed.");
                    target.close();
                }
            }

        } catch (IOException e) {
            System.out.println("[WARNING] A client validation has Expired.");
        }
    }
    public void interrupt() {
        try {
            target.close();
        } catch (IOException ignored) {
            System.out.println("[WARNING] Abnormal operation.");
        } finally {
            super.interrupt();
        }
    }
}

class ClientValidationTimerThread extends Thread {
    private ClientValidationThread target;
    public ClientValidationTimerThread(ClientValidationThread target) {
        this.target = target;
    }
    @Override
    public void run() {
        try {
            sleep(5000);
            if (target.isAlive() && target.status.equals("waiting")) {
                target.interrupt();
            }
        } catch (InterruptedException e) {
            System.out.println("[NOTICE] A client has almost done validation.");
        }
    }
}

class ClientSenderThread extends Thread {   //客户线程，用于处理与客户端的连接
    private int serial;
    private Socket client;
    private BufferedReader iStream;
    private PrintWriter oStream;

    public ClientSenderThread(int serial) {
        synchronized (MasterDataStorage.class) {
            this.serial = serial;
            this.client = MasterDataStorage.getSocket(serial);
            this.iStream = MasterDataStorage.getBufferedReader(serial);
            this.oStream = MasterDataStorage.getPrintWriter(serial);
        }
    }
    @Override
    public void run() {
        while (true) {
            try {
                String msg = iStream.readLine();
                if (msg == null) {
                    synchronized (ClientDatabase.class) {
                        ClientDatabase.bringOffline(serial);
                        System.out.printf("[Notice] A client has logged off (%d)\n", serial);
                        for (int i = 0; i < ClientDatabase.length; i++) {
                            if (ClientDatabase.isOnline(i)) {
                                if (i == serial) continue;
                                synchronized (MasterDataStorage.class) {
                                    PrintWriter Overwrite = MasterDataStorage.getPrintWriter(i);
                                    Overwrite.printf("[ROOM] *%s* has gone offline.\n", ClientDatabase.getName(serial));
                                    Overwrite.flush();
                                }
                            }
                        }
                        client.close();
                        break;
                    }
                }
                String sendTarget = "PUBLIC";
                msg = msg.trim();
                String[] temp = new String[0];
                String privateMsg = "";
                if (msg.length() > 3 && msg.charAt(0) == '{' && msg.charAt(1) == '{' && msg.indexOf("}}") > 0) {
                    temp = msg.split("}}", 2);
                    if (temp.length == 2) {
                        sendTarget = "PRIVATE";
                        privateMsg = temp[1].trim();
                    }
                }
                synchronized (ClientDatabase.class) {
                    System.out.printf("[CLIENT(%s)-%d] %s\n",ClientDatabase.getName(serial), serial, msg);
                    if (sendTarget.equals("PRIVATE")) {
                        temp = temp[0].split(">", 2);
                        if (temp.length < 2) {
                            oStream.print("[SILENT] Silent info format invalid!\n");
                            oStream.flush();
                            continue;
                        }
                        sendTarget = temp[1].trim();
                        boolean successful = false;
                        for (int i = 0; i < ClientDatabase.length; i++) {
                            if (ClientDatabase.getName(i).equals(sendTarget)) {
                                if (!ClientDatabase.isOnline(i)) {
                                    oStream.printf("[SILENT] *%s* is offline now!\n", sendTarget);
                                    oStream.flush();
                                } else {
                                    synchronized (MasterDataStorage.class) {
                                        PrintWriter writer = MasterDataStorage.getPrintWriter(i);
                                        writer.printf("[%s --> YOU] %s\n", ClientDatabase.getName(serial), privateMsg);
                                        writer.flush();
                                    }
                                    oStream.printf("[SILENT] Message successfully sent to %s.\n", sendTarget);
                                    oStream.flush();
                                }
                                successful = true;
                                break;
                            }
                        }
                        if (!successful) {
                            oStream.printf("[SILENT] There is no user called *%s*\n", sendTarget);
                            oStream.flush();
                        }
                    } else {
                        for (int i = 0; i < ClientDatabase.length; i++) {
                            if (i == serial || !ClientDatabase.isOnline(i)) continue;
                            synchronized (MasterDataStorage.class) {
                                PrintWriter writer = MasterDataStorage.getPrintWriter(i);
                                writer.printf("[%s] %s\n", ClientDatabase.getName(serial), msg);
                                writer.flush();
                            }
                        }
                    }
                }
            } catch (IOException e) {
                synchronized (ClientDatabase.class) {
                    ClientDatabase.bringOffline(serial);
                    System.out.printf("[Notice] A client has logged off (%d)\n", serial);
                }
                return;
            }
        }
    }
}

class ClientDatabase {  //用户数据库，用于验证用户登陆
    private static class Account {
        private String name;
        private String password;
        private boolean isOnline;   //用户是否在线的检查，一个用户只能登陆一次
        public Account(String name, String password) {
            this.isOnline = false;
            this.name = name;
            this.password = password;
        }
        public boolean match(String name, String password) {
            return this.name.equals(name) && this.password.equals(password);
        }
        public void bringOnline() {
            this.isOnline = true;
        }
        public void bringOffline() {
            this.isOnline = false;
        }
        public boolean isOnline() {
            return isOnline;
        }
    }
    private static Account[] accounts = {
        new Account("Jensen", "123456"),
        new Account("Asuna", "123456"),
        new Account("Krito", "123456"),
    };
    public static int length = accounts.length;
    public static int validateUser(String name, String password) {
        for (int i = 0; i < accounts.length; i++) {
            if (accounts[i].match(name, password)) {
                return i;
            }
        }
        return -1;
    }
    public static String getName(int serial) {
        return accounts[serial].name;
    }
    public static void bringOffline(int serial) {
        accounts[serial].bringOffline();
    }
    public static void bringOnline(int serial) {
        accounts[serial].bringOnline();
    }
    public static boolean isOnline(int serial) {
        return accounts[serial].isOnline();
    }
}