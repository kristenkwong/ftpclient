
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

    public static void main(String [] args)
    {
	byte cmdString[] = new byte[MAX_LEN];

	// Get command line arguments and connected to FTP
	// If the arguments are invalid or there aren't enough of them
        // then exit.

	if (args.length != ARG_CNT) {
	    System.out.print("Usage: cmd ServerAddress ServerPort\n");
	    return;
    }
    
    String hostName = args[0];
    int portNumber = Integer.parseInt(args[1]);

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

        while ((fromServer = in.readLine()) != null) {
            System.out.println("--> " + fromServer);

            System.out.print("csftp> ");
            fromUser = stdIn.readLine();
            if (fromUser != null) {
                System.out.println("<-- " + fromUser);
                out.println(fromUser);
            }
        }

	} catch (IOException exception) {
	    System.err.println("998 Input error while reading commands, terminating.");
	}
    }
}
