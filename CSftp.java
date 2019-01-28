
import java.lang.System;
import java.io.IOException;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

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

    private static String getFilename(String param) {
        String[] splitParam = param.split("/"); 
        String fileName;
        if (splitParam.length == 1) {
            fileName = splitParam[0];
        } else {
            fileName = splitParam[splitParam.length - 1];
        }
        return fileName;
    }

    private static void get(String param) {
        sendCommand("PASV");
        String[] hostNumbers = handlePassiveResponse();
        if (hostNumbers == null)
            return;
        String hostName = getIpAddress(hostNumbers);
        int portNumber = getPortNumber(hostNumbers);
        String fileName = getFilename(param);

        Socket dataSocket = null;
        InputStream dataIn = null;
        BufferedOutputStream dataOut = null;

        try {
            dataSocket = new Socket();
            dataSocket.connect(new InetSocketAddress(hostName, portNumber), 10*1000);
            dataIn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

            sendCommand("TYPE I"); // change to binary mode
            handleResponse();

            sendCommand("RETR " + param);
            String response = handleResponse();
            if (!response.equals("150")) {
                dataSocket.close();
                return;
            }
        } catch (SocketTimeoutException e) {
            System.err.println(String.format("0x3A2 Data transfer connection to %s on port %d failed to open.", hostName, portNumber));
            return;
        } catch (UnknownHostException e) {
            System.err.println(String.format("0x3A2 Data transfer connection to %s on port %d failed to open.", hostName, portNumber));
            return;
        } catch (IOException e) {
            System.err.println("0x3A7 Data transfer connection I/O error, closing data connection.");
            System.exit(1);
        }

        try {
            // Get bytes from data connection stream, write to file
            int curr_byte;
            dataIn = dataSocket.getInputStream();
            File file = new File(fileName);
            file.createNewFile();
            dataOut = new BufferedOutputStream(new FileOutputStream(file));
            byte[] buffer = new byte[4096];

            while ((curr_byte = dataIn.read(buffer, 0, buffer.length)) > 0) {
                String bufString = new String(buffer);
                System.out.println("<-- " + bufString);
                dataOut.write(buffer, 0, curr_byte);
            }

            dataIn.close();
            dataOut.close();
            dataSocket.close();
        } catch (IOException e) {
            System.err.println(String.format("0x38E Access to local file %s denied.", fileName));
            System.exit(1);
        } catch (Exception e) {
            System.err.println(String.format("0xFFFF Processing error. %s.", e.getMessage()));
        }
        handleResponse();
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
        sendCommand("PASV");
        String[] hostNumbers = handlePassiveResponse();
        if (hostNumbers == null) {
            return;
        }
        String hostName = getIpAddress(hostNumbers);
        int portNumber = getPortNumber(hostNumbers);

        try {
            Socket dataSocket = new Socket();
            dataSocket.connect(new InetSocketAddress(hostName, portNumber), 10*1000);
            BufferedReader dataIn = new BufferedReader(
                new InputStreamReader(dataSocket.getInputStream()));
            String fromServer;

            sendCommand("LIST");
            handleResponse(); // handle response on Control Connection (1st)

            // read from Data Connection:
            while ((fromServer = dataIn.readLine()) != null) {
                System.out.println("<-- " + fromServer);
            }

            handleResponse(); // handle response on Control Connection (2nd)
            dataSocket.close();
            
        } catch (SocketTimeoutException e) {
            System.err.println(String.format("0x3A2 Data transfer connection to %s on port %d failed to open.", hostName, portNumber));
        } catch (UnknownHostException e) {
            System.err.println(String.format("0x3A2 Data transfer connection to %s on port %d failed to open.", hostName, portNumber));
        } catch (IOException e) {
            System.err.println("0x3A7 Data transfer connection I/O error, closing data connection.");
            System.exit(1);
        } catch (Exception e) {
            System.err.println(String.format("0xFFFF Processing error. %s.", e.getMessage()));
        }
    }

    private static void establishControlConnection(String hostName, int portNumber) {
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(hostName, portNumber), 20*1000);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            handleResponse();
        } catch (SocketTimeoutException e) {
            System.err.println(String.format("0x3A2 Data transfer connection to %s on port %d failed to open.", hostName, portNumber));
            System.exit(1);
        } catch (UnknownHostException e) {
            System.err.println(String.format("0x3A2 Data transfer connection to %s on port %d failed to open.", hostName, portNumber));
            System.exit(1);
        } catch (IOException exception) {
            System.err.println(String.format("0xFFFC Control connection to %s on port %d failed to open.", hostName, portNumber));
            System.exit(1);
        } catch (Exception e) {
            System.err.println(String.format("0xFFFF Processing error. %s.", e.getMessage()));
        }
        return;
    }

    private static String getIpAddress(String[] hostNumbers) {
        return hostNumbers[0] + "." + hostNumbers[1] + "." + hostNumbers[2] + "." + hostNumbers[3];
    }

    private static int getPortNumber(String[] hostNumbers) {
        return Integer.parseInt(hostNumbers[4]) * 256 + Integer.parseInt(hostNumbers[5]);
    }

    private static String[] handlePassiveResponse() {
        // Returns host numbers if a Passive mode connection is established
        String[] hostNumbers;
        try {
            String fromServer;
            fromServer = in.readLine();
            System.out.println("<-- " + fromServer);

            if (fromServer.startsWith("227")) {
                // entering passive mode 
                int hostStart = fromServer.indexOf("(");
                int hostEnd = fromServer.indexOf(")");
                String hostString = fromServer.substring(hostStart + 1, hostEnd);
                hostNumbers = hostString.split(",");
                return hostNumbers;
            } else {
                return null;
            }
        } catch (IOException e) {
            System.err.println("0x3A7 Data transfer connection I/O error, closing data connection.");
            return null;
        } catch (Exception e) {
            System.err.println(String.format("0xFFFF Processing error. %s.", e.getMessage()));
            return null;
        }

    }

    private static String handleResponse() {
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
                if (exit_flag) { // if the program has been flagged to exit
                    // exit after all lines have been printed
                    socket.close();
                    System.exit(0);
                }

            } else {
                // response is single line
                System.out.println("<-- " + fromServer);
                if (fromServer.startsWith("221")) {
                    // connection closed
                    socket.close();
                    System.exit(0);
                }
            }

            return responseCode;
            
        } catch (IOException exception) {
            System.err.println("0xFFFD Control connection I/O error, closing control connection.");
            return "IOError";
        } catch (Exception e) {
            System.err.println(String.format("0xFFFF Processing error. %s.", e.getMessage()));
            return "Processing Error";
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

                cmdString = new byte[MAX_LEN]; // clear byte array after command copied

                // if empty string or starts with #, silently ignore and display prompt
                if (len <= 0 || command.equals("") || command.startsWith("#"))
                    continue;
            
                else {
                    String[] commands = command.split(" ");
                    String param = null;

                    switch(commands[0]) {
                        case "user": case "pw": case "get": case "cd":
                            // Check that the number of arguments is correct
                            if (commands.length == 2) {
                                param = commands[1];
                            } else {
                                System.out.println("0x002 Incorrect number of arguments.");
                                continue;
                            }
                            break;
                        case "quit": case "features": case "dir":
                            if (commands.length != 1){
                                System.out.println("0x002 Incorrect number of arguments.");
                                continue;
                            }
                            break;
                        default:
                            System.out.println("0x001 Invalid command.");
                            break;
                    }

                    switch(commands[0]) {
                        case "user":
                            user(param); break;
                        case "pw":
                            pw(param); break;
                        case "quit":
                            quit(); break;
                        case "get":
                            get(param); break;
                        case "features":
                            features(); break;
                        case "cd":
                            cd(param); break;
                        case "dir":
                            dir(); break;
                        default:
                            break;
                    }
                }
            }
        } catch (IOException exception) {
            System.err.println("0xFFFE Input error while reading commands, terminating.");
        } catch (Exception e) {
            System.err.println(String.format("0xFFFF Processing error. %s.", e.getMessage()));
        }
    }
}
