
import java.lang.System;
import java.io.IOException;

import java.io.*;
import java.net.*;

//
// This is an implementation of a simplified version of a command 
// line ftp client. The program always takes two arguments
//


public class CSftp {

    static final int MAX_LEN = 255;
    static final int ARG_CNT = 2;
    private static final int FTP_PORT = 21;

    static Socket socket;
    static PrintWriter out;
    static BufferedReader in;

    private static void user(String param) {
        sendCommand("USER " + param);
        handleResponse();
    }

    private static void pw(String param) {
        sendCommand("PASS " + param);
        handleResponse();
    }

    private static void quit() {
        sendCommand("QUIT");
        handleResponse();
    }

    private static void get(String param) {
    }

    private static void features() {
        sendCommand("FEAT");
        handleResponse();
    }

    private static void cd(String param) {
        sendCommand("CWD " + param);
        handleResponse();
    }

    private static void dir() {
    }

    private static void establishControlConnection(String hostName, int portNumber) {
        try {
            socket = new Socket(hostName, portNumber);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            handleResponse();
        } catch (IOException exception) {
            System.err.println(String.format("0xFFFC Control connection to %s on port %i failed to open.", hostName, portNumber));
        }
        return;
    }

    private static void handleResponse() {
        try {
            String fromServer;
            fromServer = in.readLine();

            // if response is multi-line, it'll start with response code followed
            // immediately by a hyphen (ie. 211-Features)
            String responseCode = fromServer.split(" ")[0];
            int line_num = 0;

            if (responseCode.contains("-")) {
                // response contains multiple lines
                responseCode = responseCode.split("-")[0];
                boolean exit_flag = false;

                while (true) {
                    System.out.println("<-- " + fromServer);
                    if (line_num != 0 && fromServer.startsWith(responseCode))
                        break;
                    else if (fromServer.startsWith("221"))
                        exit_flag = true;
                    fromServer = in.readLine();
                    line_num++;
                }
                if (exit_flag) // if the program has been flagged to exit
                    System.exit(0);

            } else {
                // response is single line
                System.out.println("<-- " + fromServer);
                if (fromServer.startsWith("221")) // connection closed
                    System.exit(0);
            }
            
        } catch (IOException exception) {
            System.err.println("error handling response");
        } 
    }

    private static void sendCommand(String command) {
        out.println(command + "\r\n");
        System.out.println("--> " + command);
    }

    public static void main(String [] args) {
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
        int portNumber = 21;
        try {
            portNumber = (args.length == 2) ? Integer.parseInt(args[1]) : FTP_PORT;
        } catch (NumberFormatException error) {
            System.out.println("0xFFFF Processing error. Port number isn't a number.");
            System.exit(0);
        }
        
        try {
            int len;
            establishControlConnection(hostName, portNumber);

            while (true) {
                System.out.print("csftp> ");
                len = System.in.read(cmdString);
                String command = new String(cmdString).trim();

                // if empty string or starts with #, silently ignore and display prompt
                if (len <= 0 || command.equals("") || command.startsWith("#"))
                    continue;
            
                else {
                    // System.out.println("--> " + fromUser);
                    String[] commands = command.split(" ");

                    String param = null;

                    if (command.equals("user") || command.equals("pw") || command.equals("get")
                            || command.equals("cd")) {
                        // Check that the number of arguments is correct
                        if (commands.length == 2) {
                            param = commands[1];
                        } else {
                            System.out.println("0x002 Incorrect number of arguments.");
                            continue;
                        }
                    } else {
                        if (commands.length != 1){
                            System.out.println("0x002 Incorrect number of arguments.");
                            continue;
                        }
                    }

                    switch(commands[0]) {
                        case "user":
                            user(param); break;
                        case "pw":
                            pw(param); break;
                        case "quit":
                            quit();
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
                            System.out.println("900 Invalid command.");
                            break;
                    }

                    cmdString = new byte[MAX_LEN]; // clear byte array after cmd
                }
            }
        } catch (IOException exception) {
            System.err.println("998 Input error while reading commands, terminating.");
        }
    }
}
