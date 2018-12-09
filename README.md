# MiniQQ-ServiceDemo

A Service Demo of a multiple chatting software.

This is a working demo of a multiple-chatting software.

As the application itself contains important knowledge, it only has command-line interface.

This working demo contains various functions:
- Room chatting
- Private chatting
- Log on / Log off judgement
- User validation system

Feel free to download my program! Later, I will present a tutor about how to make a program like this.

# How to use

## Getting started

Just simply clone the project and you will see under the "src/com/ChenHUi", there are two files: Server.java and Client.java.

You can use the command line to compile them (Requires JDK 8 or even higher, though the project is developed under JDK 11).

The whole project is developed using IntelliJ Idea Ultimate 2018.3. If you happend to using this IDE, you can open the whole folder as a project.

## Start up

You need to run the Server.java first.

The server is set to listen port 9999, you can change that configuration (just use the searching tools to search "9999", and don't forget the Client.java).

After running the Server.java, you should be able to see the server is running notice, then you start up the Client.java, you should be able to see the validation request on the command-line.

The default server ip is 127.0.0.1 (localhost). If you want yo use it somewhere else, just modify the client.

## User validation

When you see the validation request message on the command-line, user correct username and password to login. 

The system has pre-registered several accounts:

- Jensen 123456
- Asuna 123456
- Krito 123456

If you want to add more users, you are able to modify it easily. They are all defined by Strings.

**CAUTION: You can use spaces, letters and most of the signs for username and password. But make sure that you don't use "}", "{" or ">", or you may cause some serious trouble.**

After inputting the correct username and password, you can see the client giving out the current information about the chatting room.

**When a user logs online, both the server side and online users will recive a notice.**

Actually, the client makes that socket connection as a unveryfied user to make validation. Once the validation is successful, the server will assign a thread for the client and monitor its status.

**If the validation isn't successful or the connection time expires (5 seconds), the connection will be closed by the server.**

## Send public messages

To send messages, just simply inpuut the message into the command-line and press ENTER to submit them. Both the server side and online users will recive your message.

## Send private messages

To send private messages, you need to make your message content looks like this:

```
{{ --> [USERNAME] }} CONTENT
```

By using "{{" and "}}" to surround an arrow "-->" and the username. It's ok that you add more spaces around.

The server side will display the original input of the sender (No privacy at all, uh).

## Shutdown

To shutdown a client or a server, just type "EXIT" to the command-line (all uppercases).

Or, you can just use "kill" in your operating system, Whatever.

When a user logs off, all the online users and the server will recieve a message.

# About coding

This is a java learning code just for learning, not for production. And this code is published in the spirit of sharing and better be useful.

If you have any questions, just make "issues" or "pull requests".

Stars, Follows, come on!

# About author

The author is a college student (1st Grade at the year of 2018).

If you truly love my work and wants to help me, please make a donation. I appreciate your generous help!

My alipay account is: 15553142784, thank you!

# Licence

This program is shared under the licence of **General Public Licence 3.0 (GPL3.0)**.

The details are in the LICENSE file. If you attempt to use them for other uses, please read them first, thank you.
