package com.ChenHui;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client1 {
    public static void main(String[] args) throws IOException {

        Socket s = new Socket("127.0.0.1", 9999);
        new Thread(() -> {
            try {
                PrintWriter pw = new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
                while (true) {
                    Scanner sc = new Scanner(System.in);
                    String str = sc.next();
                    pw.println(str);
                    pw.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            try {
                InputStreamReader isr = new InputStreamReader(s.getInputStream());
                BufferedReader br = new BufferedReader(isr);
                while (true) {
                    String msg = br.readLine();
                    System.out.println("接收到:" + msg);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
