package com.ChenHui;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        Socket s = null;
        PrintWriter pw;
        BufferedReader bfr;
        while (true) {
            try {
                System.out.println("[Auth] Please input your username: ");
                String username = sc.nextLine();
                System.out.println("[Auth] Please input your password: ");
                String password = sc.nextLine();
                s = new Socket("127.0.0.1", 9999);
                pw = new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
                bfr = new BufferedReader(new InputStreamReader(s.getInputStream()));
                pw.println(username + "\n" + password);
                pw.flush();
                if (bfr.readLine().equals("success")) {
                    System.out.println("[Auth] Login success. Now you can send messages.");
                    new SenderThread(sc, s, pw, bfr).start();
                    new ReceiverThread(s, pw, bfr).start();
                    break;
                } else {
                    System.out.println("[Auth] Auth failed, try again.");
                }

            } catch (IOException e) {
                System.out.println("[Notice] Connection failure. Server closed.");
                break;
            }
        }
    }
}

class SenderThread extends Thread {
    Scanner sc;
    Socket s;
    PrintWriter pw;
    BufferedReader bfr;
    public SenderThread(Scanner sc, Socket s, PrintWriter pw, BufferedReader bfr) {
        this.sc = sc;
        this.s = s;
        this.pw = pw;
        this.bfr = bfr;
    }
    public void run() {
        try {
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
    }
}

class ReceiverThread extends Thread {
    Socket s;
    PrintWriter pw;
    BufferedReader bfr;
    public ReceiverThread(Socket s, PrintWriter pw, BufferedReader bfr) {
        this.s = s;
        this.pw = pw;
        this.bfr = bfr;
    }
    public void run() {
        try {
            while (true) {
                String msg = bfr.readLine();
                if (msg == null) {
                    s.close();
                    System.out.println("[Notice] Connection failure. Server closed.");
                    System.exit(0);
                }
                System.out.println(msg);
            }
        } catch (IOException e) {
            System.out.println("[Notice] The connection has been closed.");
        }
    }
}