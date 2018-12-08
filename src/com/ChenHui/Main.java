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
        public StorageElement(Socket socket) throws IOException {
            this.enabled = true;
            //将stream转换成BufferedReader，能够使外部操作更加方便
            this.iStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            //将stream转换成PrintWriter，能够使外部操作更加方便
            this.oStream = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.socket = socket;
        }
    }
    private static List<StorageElement> contents = Collections.synchronizedList(new ArrayList<StorageElement>());
    public static void add(Socket socket) throws IOException {
        contents.add(new StorageElement(socket));
    }
    public static void disable(int serial) {
        contents.set(serial, new StorageElement());
    }
    public static boolean enabled(int serial) {
        return contents.get(serial).enabled;
    }
    public static BufferedReader getBufferedReader(int serial) {
        return contents.get(serial).iStream;
    }
    public static PrintWriter getPrintWriter(int serial) {
        return contents.get(serial).oStream;
    }
    public static Socket getSocket(int serial) {
        return contents.get(serial).socket;
    }
    public static int size() {
        return contents.size();
    }
}

class ServerClientRequestReceiverThread extends Thread {
    @Override
    public void run() {
        ServerSocket serverSocket = null;// 创建服务器Socket对象
        try {
            serverSocket = new ServerSocket(9999);// 监听客户端的连接
            while (true) {
                System.out.println("[NOTICE] Ready for new client.");
                Socket clientSocket = serverSocket.accept(); // 阻塞
                synchronized (MasterDataStorage.class) {
                    System.out.println("[NOTICE] Something pop in.");
                    MasterDataStorage.add(clientSocket);
                    (new ClientSenderThread(MasterDataStorage.size() - 1)).start();
                    System.out.printf("[NOTICE] A client has logged online (%d)\n", MasterDataStorage.size() - 1);
                }
            }
        } catch (IOException e) {
            System.out.println("[ERROR] The server has crashed.");
            System.exit(1);
        }

    }
}

class ClientSenderThread extends Thread {
    private int serial;
    private Socket socket;
    private BufferedReader iStream;

    public ClientSenderThread(int serial) {
        this.serial = serial;
        this.socket = MasterDataStorage.getSocket(serial);
        this.iStream = MasterDataStorage.getBufferedReader(serial);
    }
    @Override
    public void run() {
        while (true) {
            try {
                String msg = iStream.readLine();
                synchronized (MasterDataStorage.class) {
                    if (msg.equals("EXIT")) {
                        socket.close();
                        MasterDataStorage.disable(serial);
                        System.out.printf("[NOTICE] A client has logged off (%d)\n", serial);
                        break;
                    }
                    System.out.printf("[CLIENT-%d] %s\n", serial, msg);
                    for (int i = 0; i < MasterDataStorage.size(); i++) {
                        if (i == serial || !MasterDataStorage.enabled(i)) continue;
                        PrintWriter writer = MasterDataStorage.getPrintWriter(i);
                        writer.printf("[CLIENT-%d] %s\n", serial, msg);
                        writer.flush();
                    }
                }
            } catch (IOException e) {
                synchronized (MasterDataStorage.class) {
                    System.out.printf("[WARNING] A client has crashed (%d)\n", serial);
                    MasterDataStorage.disable(serial);
                }
                break;
            }
        }
    }
}
