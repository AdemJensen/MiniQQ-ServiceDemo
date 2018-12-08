package com.ChenHui;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/*
 * 具体的实现方法如下：
 * 首先是主线程，负责接收客户端的连接请求。
 * 每当接收一个连接请求，我们都将它的输入流和输出流保存下来
 * 然后我们单纯为每一个客户端开一个接收用的Thread，一旦读取到信息，我们就获取信息。
 * 输出信息完全不需要thread。
 */

public class Main {
    public static void main(String[] args) throws IOException {
        ServerClientRequestReceiverThread clientRequestReceiver = new ServerClientRequestReceiverThread();

        clientRequestReceiver.start();
        /*
        while (true) {

            // 获取输入流
            InputStream is = s.getInputStream();
            byte[] bys = new byte[1024];
            int len = is.read(bys); // 阻塞
            String server = new String(bys, 0, len);
            System.out.println("server:" + server);

            // 获取输出流
            OutputStream os = s.getOutputStream();
            os.write("数据已经收到".getBytes());

            // 释放资源
            s.close();
            // ss.close();
        }*/
    }
}

class ServerClientRequestReceiverThread extends Thread {
    @Override
    public void run() {
        ServerSocket ss = null;// 创建服务器Socket对象
        try {
            ss = new ServerSocket(9999);// 监听客户端的连接
            while (true) {
                Socket s = ss.accept(); // 阻塞

                //TODO:存储获得的输入输出流！

            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error! The server exited unexpectedly.");
            System.exit(1);
        }

    }
}

class ClientSenderThread implements Runnable {

    @Override
    public void run() {

    }
}
