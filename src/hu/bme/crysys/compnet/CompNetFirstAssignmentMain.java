package hu.bme.crysys.compnet;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class CompNetFirstAssignmentMain {

    public static void main(String[] args){

        try {
            ServerSocket serverSocket = new ServerSocket(8080);

            while(true){
                Socket clientSocket = serverSocket.accept();
                StudentWebServer studentWebServer = new StudentWebServer(clientSocket);
                studentWebServer.start();
            }

        } catch (IOException ioE) {
            System.out.println(ioE.getLocalizedMessage());
        }
    }
}
