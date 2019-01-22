
import java.lang.System;
import java.io.IOException;

import java.io.*;
import java.net.*;

//
// This is an implementation of a simplified version of a command 
// line ftp client. The program always takes two arguments
//


public class CSftp
{
    static final int MAX_LEN = 255;
    static final int ARG_CNT = 2;

    private static void user(String param) {
        // merp
    }

    private static void pw(String param) {
        // merp
    }

    private static void get(String param) {

    }

    private static void features() {
        //fd
    }

    private static void cd(String param) {

    }

    private static void dir() {

    }

    public static void main(String [] args)
    {
	byte cmdString[] = new byte[MAX_LEN];

	// Get command line arguments and connected to FTP
	// If the arguments are invalid or there aren't enough of them
        // then exit.

	if (args.length > ARG_CNT || args.length == 0) {
	    System.out.print("Usage: cmd ServerAddress ServerPort\n");
	    return;
    } 
    
    String hostName = args[0];
    // if no port number, default to 21
    int portNumber = (args.length == 2) ? Integer.parseInt(args[1]) : 21;

	try (
        Socket kkSocket = new Socket(hostName, portNumber);
        PrintWriter out = new PrintWriter(kkSocket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(
            new InputStreamReader(kkSocket.getInputStream()));
    ) {
        BufferedReader stdIn =
                new BufferedReader(new InputStreamReader(System.in));
        String fromServer;
        String fromUser;

        while (true) {

            /*fromServer = in.readLine();
            if (fromServer != null) {
                System.out.println("--> " + fromServer);
                if (fromServer.startsWith("221"))
                    break;
            }*/

            System.out.print("csftp> ");

            fromUser = stdIn.readLine();
            if (fromUser.equals("") || fromUser.startsWith("#"))
                continue;

            else if (fromUser != null) {
                System.out.println("--> " + fromUser);
                String[] commands = fromUser.split(" ");

                String param = null;
                if (commands[0].equals("user") || commands[0].equals("pw")
                || commands[0].equals("get") || commands[0].equals("cd")) {
                    // do something with params
                    // check arg number
                    // TODO
                    param = commands[1];
                }

                switch(commands[0]) {
                    case "user":
                        user(param);
                        // out.print()
                        break;
                    case "pw":
                        pw(param);
                        break;
                    case "quit":
                        out.println("quit");
                        break;
                    case "get":
                        get(param);
                        break;
                    case "features":
                        features();
                        break;
                    case "cd":
                        cd(param);
                        break;
                    case "dir":
                        dir();
                        break;
                    default:
                        System.out.println("invalid command");
                        break;
                }
            }
        }

	} catch (IOException exception) {
	    System.err.println("998 Input error while reading commands, terminating.");
	}
    }
}
