package hu.bme.crysys.compnet;

import java.io.*;
import java.net.Socket;
import java.util.Date;
import java.util.StringTokenizer;

/**
 * This class has to be completed by the students as their first assignment.
 */
public class StudentWebServer extends Thread{

    Socket socket;
    PrintWriter out;
    BufferedReader in;

    public StudentWebServer(Socket socket){
        this.socket = socket;
    }

    @Override
    public void run(){
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException ioE){
            System.out.println("Fatal error: " + ioE.getLocalizedMessage());
        }

        webServer();
    }

    private void webServer() {

        try {

            //get first line of request from client
            String input = in.readLine();

            //if input is null, return
            if (input == null) {
                return;
            }

            //create StringTokenizer to parse request
            StringTokenizer parse = new StringTokenizer(input);
            //parse out method
            String method = parse.nextToken().toUpperCase();
            //parse out file requested
            String requestedFile = parse.nextToken().toLowerCase();

            //if method name is not GET, HEAD or POST than it's not implemented
            if (!method.equals("GET") && !method.equals("HEAD") && !method.equals("POST")) {

                //create the status code 501 Not Implemented
                createHeader(501, null, 0);

                //it's not implemented, there's nothing to do
                return;
            }

            //if we get here method must be GET or HEAD
            if (requestedFile.endsWith("/")) {

                //default file name
                requestedFile += "index.html";
            }

            //create file object
            File file = new File(requestedFile.substring(1));

            //length of file
            int fileLength = (int) file.length();

            //send file content if GET
            if (method.equals("GET")) {

                FileInputStream fileIn = null;

                //byte array to store data
                byte[] fileData = new byte[fileLength];

                try {
                    //input stream for file
                    fileIn = new FileInputStream(file);

                    //read file into the created byte array
                    fileIn.read(fileData);
                } finally {

                    //if done close fileIn
                    close(fileIn);
                }

                //send HTTP headers if everything is fine
                createHeader(200, getType(requestedFile), fileLength);

                //byte data to UTF-8 String
                String data = new String(fileData, "UTF-8");

                //write it
                out.write(data);
                //flush the writer
                out.flush();

            //POST method
            } else if (method.equals("POST")) {
                //some strings to store some info
                String contentLength = null;
                String boundary = null;
                String filename = null;


                while (in.ready()) {

                    //multipart/form-data
                    if (input.contains("Content-Type: multipart/form-data")) {
                        boundary = input.split("boundary=")[1];
                        // The POST boundary

                        while (true) {
                            input = in.readLine();
                            if (input.contains("Content-Length:")) {
                                contentLength = input.split(" ")[1];
                                break;
                            }
                        }
                    }

                    while (true) {
                        input = in.readLine();
                        if (input.contains("--" + boundary)) {

                            //get the filename from filelist
                            filename = in.readLine().split("filename=")[1].replaceAll("\"", "");
                            String[] fileList = filename.split(String.format("\\%s", System.getProperty("file.separator")));
                            filename = fileList[fileList.length - 1];
                            break;
                        }
                    }

                    //get content type
                    String fileContentType = in.readLine().split(" ")[1];

                    in.readLine();

                    //open a printwriter
                    PrintWriter fout = new PrintWriter(filename);
                    String prevLine = in.readLine();
                    input = in.readLine();

                    //upload the actual file contents
                    while (true) {
                        if (input.equals("--" + boundary + "--")) {
                            fout.print(prevLine);
                            break;
                        } else {
                            fout.println(prevLine);
                        }
                        prevLine = input;
                        input = in.readLine();
                    }

                    //create the 201 header
                    createHeader(201, fileContentType, contentLength.length());
                    fout.close();
                }
            }
        } catch (FileNotFoundException fileNFE){

            //inform client when file isn't found
            createHeader(404, null, 0);

        } catch (IOException ioE){

            //Server error
            System.err.println("Server error: " + ioE);
        } finally {

            //close everything
            close(in);
            close(out);
            close(socket);

        }
    }

    private void createHeader(int code, String fileType, int fileLength){

        //create the header from the codes
        switch(code){
            case 200:
                out.println("HTTP/1.0 200 OK");
                out.println("Server: Adam's Magic Server 1.0");
                out.println("Date: " + new Date());
                out.println("Content-type: " + fileType);
                out.println("Content-length: " + fileLength);
                out.println();
                break;

            case 201:
                out.println("HTTP/1.0 200 Created");
                out.println("Server: Adam's MAgic Server 1.0");
                out.println("Date: " + new Date());
                out.println("Content-type: " + fileType);
                out.println("Content-length: " + fileLength);
                out.println();
                break;

            case 404:
                out.println("HTTP/1.0 404 Not found");
                out.println("Server: Adam's Magic Server 1.0");
                out.println("Date: " + new Date());
                out.println("Content-type: " + fileType);
                out.println();
                out.println("<html>");
                out.println("<head><title> File Not Found </title></head>");
                out.println("<body>");
                out.println("<h2> 404 File Not Found </h2></body></html>");
                break;

            case 501:
                out.println("HTTP/1.0 501 Not implemented");
                out.println("Server: Adam's Magic Server 1.0");
                out.println("Date: " + new Date());
                out.println("Content-type: " + fileType);
                out.println();
                out.println("<html>");
                out.println("<head><title> Not implemented </title></head>");
                out.println("<body>");
                out.println("<h2> 501 Not Implemented </h2></body></html>");
                break;

        }

        out.flush();
    }

    /**
     * gets the content type of the files
     * @param file path of file
     * @return returns the encoding
     */
    private String getType(String file){

        if(file.endsWith(".htm") || file.endsWith(".html"))
            return "text/html";

        else if(file.endsWith(".gif"))
            return "image/gif";

        else if(file.endsWith(".jpg") || file.endsWith(".jpeg"))
            return "image/jpeg";

        else if(file.endsWith(".class") || file.endsWith(".jar"))
            return "application/octet-stream";

        else
            return "text/plain";
    }

    /**
     * a function for everything that must be closed
     * @param stream object to be closed
     */
    private void close(Object stream){
        if(stream == null)
            return;

        try {
            if(stream instanceof Reader)
                ((Reader)stream).close();

            else if(stream instanceof Writer)
                ((Writer)stream).close();

            else if(stream instanceof InputStream)
                ((InputStream)stream).close();

            else if(stream instanceof OutputStream)
                ((OutputStream)stream).close();

            else if(stream instanceof Socket)
                ((Socket)stream).close();

        } catch (Exception e){
            System.err.println("Error closing stream: " + e);
        }
    }
}
