package com.ChenHui;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client2 {
    public static void main(String[] args) throws IOException {
        Scanner sc = new Scanner(System.in);
        Socket so;
        while (true) {
            try {
                System.out.println("[Auth] Please input your username: ");
                String username = sc.nextLine();
                System.out.println("[Auth] Please input your password: ");
                String password = sc.nextLine();
                so = new Socket("127.0.0.1", 9999);
                PrintWriter pw = new PrintWriter(new OutputStreamWriter(so.getOutputStream()));
                BufferedReader bfr = new BufferedReader(new InputStreamReader(so.getInputStream()));
                pw.println(username + "\n" + password);
                pw.flush();
                if (bfr.readLine().equals("success")) {
                    System.out.println("[Auth] Login success. Now you can send messages.");
                    break;
                } else {
                    System.out.println("[Auth] Auth failed, try again.");
                }
            } catch (IOException e) {
                System.out.println("[Notice] Connection failure. Server closed.");
            }
        }
        Socket s = so;
        new Thread(() -> {
            try {
                PrintWriter pw = new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
                InputStreamReader isr = new InputStreamReader(s.getInputStream());
                pw.flush();
                while (true) {
                    String str = sc.nextLine();
                    if (str.equals("EXIT")) {
                        s.close();
                        System.out.println("[Notice] The connection has been closed.");
                        System.exit(0);
                    }
                    pw.println(str);
                    pw.flush();
                }
            } catch (IOException e) {
                System.out.println("[Notice] Connection failure. Server closed.");
            }
        }).start();

        new Thread(() -> {
            try {
                InputStreamReader isr = new InputStreamReader(s.getInputStream());
                BufferedReader br = new BufferedReader(isr);
                while (true) {
                    String msg = br.readLine();
                    System.out.println(msg);
                }
            } catch (IOException e) {
                System.out.println("[Notice] The connection has been closed.");
            }
        }).start();
    }
}