package com.ChenHui;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

/*
 * 具体的实现方法如下：
 * 首先是主线程，负责接收客户端的连接请求。
 * 每当接收一个连接请求，我们都将它的输入流和输出流保存下来
 * 然后我们单纯为每一个客户端开一个接收用的Thread，一旦读取到信息，我们就获取信息，并遍历主存储给在线的client发送消息。
 * 输出信息完全不需要thread。
 */

public class Main {
    public static void main(String[] args) throws IOException {
        ServerClientRequestReceiverThread clientRequestReceiver = new ServerClientRequestReceiverThread();

        clientRequestReceiver.start();
        System.out.println("[INSTANCE] The server has started.");
        Scanner sc = new Scanner(System.in);
        while (true) {
            if (sc.next().equals("EXIT")) {
                System.out.println("[INSTANCE] The server has stopped.");
                System.exit(0);
            }
        }
    }
}

class MasterDataStorage {
    static class StorageElement {
        public boolean enabled;
        public BufferedReader iStream;
        public PrintWriter oStream;
        public Socket socket;
        public StorageElement() {
            this.enabled = false;
        }
        public StorageElement(Socket socket, BufferedReader iStream, PrintWriter oStream) throws IOException {
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
    public static void set(int serial, Socket socket, BufferedReader iStream, PrintWriter oStream) throws IOException {
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
        ServerSocket serverSocket = null;// 创建服务器Socket对象
        try {
            serverSocket = new ServerSocket(9999);// 监听客户端的连接
            while (true) {
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
            this.status = "waiting";
            String username = checker.readLine();
            String password = checker.readLine();
            this.status = "validating";
            synchronized (ClientDatabase.class) {
                int gotUser = ClientDatabase.validateUser(username, password);
                if (gotUser > -1 && !ClientDatabase.isOnline(gotUser)) {
                    synchronized (MasterDataStorage.class) {
                        PrintWriter writer = new PrintWriter(new OutputStreamWriter(target.getOutputStream()));
                        MasterDataStorage.set(gotUser, target, checker, writer);
                        writer.println("success");
                        writer.flush();
                        (new ClientSenderThread(gotUser)).start();
                        System.out.printf("[NOTICE] A client has logged online (%d)\n", gotUser);
                    }
                } else {
                    PrintWriter writer = new PrintWriter(new OutputStreamWriter(target.getOutputStream()));
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

    public ClientSenderThread(int serial) {
        synchronized (MasterDataStorage.class) {
            this.serial = serial;
            this.client = MasterDataStorage.getSocket(serial);
            this.iStream = MasterDataStorage.getBufferedReader(serial);
        }
    }
    @Override
    public void run() {
        while (true) {
            try {
                String msg = iStream.readLine();
                synchronized (ClientDatabase.class) {
                    if (!ClientDatabase.isOnline(serial)) {
                        System.out.print("ABNORMAL\n");
                        client.close();
                        super.interrupt();
                    }
                }
                synchronized (ClientDatabase.class) {
                    System.out.printf("[CLIENT(%s)-%d] %s\n",ClientDatabase.getName(serial), serial, msg);
                    for (int i = 0; i < ClientDatabase.length; i++) {
                        if (i == serial || !ClientDatabase.isOnline(i)) continue;
                        synchronized (MasterDataStorage.class) {
                            PrintWriter writer = MasterDataStorage.getPrintWriter(i);
                            writer.printf("[%s] %s\n", ClientDatabase.getName(serial), msg);
                            writer.flush();
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
        accounts[serial].bringOnline();
    }
    public static boolean isOnline(int serial) {
        return accounts[serial].isOnline();
    }
}