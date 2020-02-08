
package myserver;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.swing.*;
import javax.swing.text.BadLocationException;

class CustomOutputStream extends OutputStream {
    private JTextArea textArea;
    
    public CustomOutputStream(JTextArea textArea) {
        this.textArea = textArea;
    }
     
    @Override
    public void write(int b) throws IOException {
        textArea.append(String.valueOf((char)b));
        textArea.setCaretPosition(textArea.getDocument().getLength());
    }
}

public class MyServer extends JFrame {
    private JTextArea textArea;
    private JButton buttonClear = new JButton("Clear"); 
    private PrintStream standardOut;
  
  public MyServer() {
        super("Server Log");
         
        textArea = new JTextArea(50, 10);
        textArea.setEditable(false);
        PrintStream printStream = new PrintStream(new CustomOutputStream(textArea));
        standardOut = System.out;
         
        System.setOut(printStream);
        System.setErr(printStream);
 
        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.insets = new Insets(10, 10, 10, 10);
        constraints.anchor = GridBagConstraints.WEST;         
        constraints.gridx = 1;
        add(buttonClear, constraints); 
        
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;        
        add(new JScrollPane(textArea), constraints);       
        
        buttonClear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                try {
                    textArea.getDocument().remove(0,
                            textArea.getDocument().getLength());
                    standardOut.println("Text area cleared");
                } catch (BadLocationException ex) {
                    ex.printStackTrace();
                }
            }
        });
      
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 400);
        setLocationRelativeTo(null);    
    }
//stores list of connected clients
  private static ArrayList<String> clientList = new ArrayList<String>(); 
  private static String path = "";
  
  public static void main(String[] args) throws Exception { 
     
        MyServer mainFrame = new MyServer();
     
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                mainFrame.setVisible(true);
            }
        });
        
        boolean isValidPath = false;
        
        while(!isValidPath)
        {
            //ask user for server directory path
            path = (String) JOptionPane.showInputDialog(
                mainFrame,
                "Enter Server Directory Path (enter -1 to exit Server): \n",
                "Input Dialog",
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                null 
            );
            
            if ((path != null) && (path.length() > 0)) 
            {    
              if(path.equals("-1"))
              {
                System.exit(0);
              }
                
              isValidPath = checkPath(path);
              if(!isValidPath)
              {
                System.out.println(" !! \"" + path + "\" is not a valid directory path. Try Again.");    
              }
            }
            else //not enter anything 
            {
               System.out.println(" !! You must enter a file path. Try Again.");    
            }
        }
        
        System.out.println("\nCurrent Server Directory: " + path);
        System.out.println("--------------------------------------------------------------------------");

        int port=6789;
        ServerSocket serverSocket = null; 
        boolean serverConnected = false; 
        try 
        {
          serverSocket = new ServerSocket(port);
          serverConnected = true; 
        }
        catch(Exception e)
        {
          System.out.println("Error! Couldn't set up server.\n");
        }
       if(serverConnected)
       {
          System.out.println("Server connected. Listening for requests at port " + port + "...\n");
          while(true)
          {
            Socket clientSocket = serverSocket.accept();  
            HttpRequest request = new HttpRequest(clientSocket, clientList, path);
            Thread thread = new Thread(request);
            thread.start();  
          }
        }        
 
    }
  
    
  private static boolean checkPath(String path) {
      boolean isValid = false;   
      try 
      {
        File test = new File(path);  
        if(test.isFile()) 
          isValid = false;        
        else if(test.isDirectory()) 
          isValid = true;
      }    
      catch(Exception e)
      {
        isValid = false;
        e.printStackTrace();
      }   
      return isValid;
  }

}

// class handles client requests as individual thread
final class HttpRequest implements Runnable 
{
  final String CRLF = "\r\n"; 
  Socket socket;  
  private String client; 
  private ArrayList<String> clientList; //copy of global client list
  private String path;
    
  public HttpRequest(Socket socket, ArrayList<String> clientList, String path) throws Exception 
  {
    this.socket = socket; 
    this.clientList = clientList;
    this.path = path;
  }
  
  @Override
  public void run() 
  {
    try 
    {       
      processRequest();
    }
    catch(Exception e) 
    {
      System.out.println(e);
    }
  }
  
  private void processRequest() throws Exception 
  {
      
    DataOutputStream os = new DataOutputStream(socket.getOutputStream()); 

    BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    
    String clientReq = ""; 
    while(!(clientReq=br.readLine()).equals("</DISCONNECT>"))
    {
        String requestLine = clientReq;
        
        String[] tokens = clientReq.split(" ");
        //GET or POST
        String requestType = tokens[0]; 
        //NAME OF FILE (or filelist)
        String requestedItem = tokens[1]; 
        
        //collect client headerlines
        String headerLine = ""; String temp = "";
        while((temp = br.readLine()).length()!= 0)
        {     
            headerLine += temp + "\n";
        } 
        
        switch(requestType)
        {
            case "GET": if(requestedItem.equals("/File-List")) 
                        {
                            System.out.println("\n******* "+ this.client + " REQUESTED TO VIEW FILE LIST *******");
                            System.out.println(requestLine);
                            System.out.println(headerLine);
                            sendFileList(os);
                        } 
                        else 
                        {
                            System.out.println("\n******* "+ this.client + " REQUESTED TO DOWNLOAD A FILE *******");
                            System.out.println(requestLine);
                            System.out.println(headerLine);
                            sendFile(os,requestedItem);
                        }
                        break;
                        
            case "POST":  if(requestedItem.equals("/<SUBMIT-USERNAME>"))    
                          {
                             saveUsername(br,os);
                             System.out.println("\n******* WELCOME NEW CLIENT: "+ this.client + " *******");
                             System.out.println(requestLine);
                             System.out.println(headerLine);
                                 
                            //DISPLAY list of currently connected clients
                            System.out.println("-----------------------------------------------------------------");
                            System.out.println(">TIME NOW: "+ getHTTPTime()); 
                            System.out.println(">LIST OF CURRENT CLIENTS: " + clientList.toString());
                            System.out.println("-----------------------------------------------------------------");
                          }                
                          else //download file FROM client
                          {
                            System.out.println("\n******* "+ this.client + " REQUESTED TO UPLOAD A FILE *******\n");
                            int success = saveFile(os, requestedItem);
                            System.out.println(requestLine);
                            System.out.println(headerLine);
                            if(success==1)
                             System.out.println("File successfully received from client " + this.client);
                            else
                             System.out.println("Failed to receive file from client.");
                          } 
                          break;                     
        }

    } 
    System.out.println("-----------------------------------------------------------------");
    System.out.println(this.client + " DISOCNNECTED "); 
    clientList.remove(this.client);

    System.out.println("\nTIME NOW: "+ getHTTPTime()); 
    System.out.println("LIST OF CURRENT CLIENTS: " + clientList.toString());
    System.out.println("-----------------------------------------------------------------");
    
    os.close();
    br.close();
    socket.close();
    
  }
  
  // request filename as input and sends file via a neighbour socket to client
  private void sendFile(DataOutputStream os, String filename) throws Exception{
        
    
    filename = path + "\\" + filename;   
    FileInputStream fis = null;
    boolean fileExists = true;
    try {
      fis = new FileInputStream(filename);
    }
    catch (Exception e) 
    {
      fileExists = false;
    }
    
    String statusLine = null;
    double contentLength = 0;
    
    if(fileExists)
    {
      statusLine = "HTTP/1.1 200 OK" + CRLF;
      contentLength = fis.getChannel().size();
    }
    else 
    {
      statusLine = "HTTP/1.1 404 Not Found" + CRLF;
    }
    String headerLines = "Server: AO:6879" + CRLF +
                         "User-Agent: NetBeans IDE/8.1" + CRLF +
                         "Content-Type: " + contentType(filename) + CRLF +
                         "Content-Length: " + contentLength + CRLF +
                         "Date: " + getHTTPTime() + CRLF + CRLF;
    os.writeBytes(statusLine);
    os.writeBytes(headerLines);   
     
    if(fileExists) {
        int serverPORT = 7000; 
        ServerSocket sisterSocket = new ServerSocket(serverPORT);
        Socket sisterClientSocket = sisterSocket.accept();
        DataOutputStream sisterOS = new DataOutputStream(sisterClientSocket.getOutputStream()); 

        byte[] buffer = new byte[1024]; 
        int bytesRead = 0; 
        while ((bytesRead = fis.read(buffer)) != -1) {
          sisterOS.write(buffer, 0, bytesRead); }

        fis.close();
        sisterOS.close();
        sisterClientSocket.close();
        sisterSocket.close();

        System.out.println("File successfully sent to client " + this.client);       
    }
    
  }
  
  private void saveUsername(BufferedReader br, DataOutputStream os) throws Exception{
    String username = "";
    while((username=br.readLine()).length()!=0)
    {    
      this.client = username; 
    } 
    clientList.add(this.client);
   
    String statusLine = "HTTP/1.1 201 Created" + CRLF;
    String headerLines = "Server: AO:6879" + CRLF +
                         "User-Agent: NetBeans IDE/8.1" + CRLF +
                         "Content-Type: text/plain" + CRLF +
                         "Content-Length: " + client.length() + CRLF +
                         "Date: " + getHTTPTime() + CRLF + CRLF;
    os.writeBytes(statusLine);
    os.writeBytes(headerLines);
  }
  
  private static String contentType(String filename)
  {
    if(filename.endsWith(".htm")||filename.endsWith(".html"))
      return "text/html";
    else if(filename.endsWith(".jpeg")||filename.endsWith(".jpg"))
      return "image/jpeg";
    else if(filename.endsWith(".pdf"))
      return "application/pdf";
    else if(filename.endsWith(".gif"))
      return "image/gif";
    else if(filename.endsWith(".png"))
      return "image/png";
    else if(filename.endsWith(".doc")||filename.endsWith(".docx"))
      return "application/msword";
    else if(filename.endsWith(".txt"))
      return "text/plain";    
    return "aplication/octet-stream"; 
  }
  
  private void sendFileList(DataOutputStream os) throws Exception{
     File serverDir = new File(path); 
      File[] listOfFiles = serverDir.listFiles(); 
      String fileList = "Available files at Server (subfolders are in parentheses): \n";
      
      for (File file : listOfFiles) 
      {
        if (file.isFile()) 
           fileList += file.getName()+"  ";                
        else if (file.isDirectory()) 
           fileList += "("+file.getName()+")  ";        
      }      
      fileList += "\n";
      String status = "HTTP/1.1 200 OK"+CRLF;
      String header =  "Content-Type: text/plain" + CRLF +
                       "Content-Length: " + fileList.length() + CRLF +
                       "Server: AO:6789" + CRLF +
                       "Date: " + getHTTPTime() + CRLF +
                       "User-Agent: NetBeans 8.1" + CRLF + CRLF;
      
      os.writeBytes(status);
      os.writeBytes(header);    
      os.writeBytes(fileList);
      os.writeBytes(CRLF);
  }
  
  private int saveFile(DataOutputStream os,String filename) throws Exception
  {
    int success = 0; //assume upload failed
    filename = filename.substring(1);    
    try    {
        String serverIP = "127.0.0.1";
        int serverPORT = 7000; 
        Socket sisterSocket = new Socket(serverIP, serverPORT);
        File file = new File(path + "\\" + filename); 
        FileOutputStream fos = new FileOutputStream(file);
        DataInputStream dis = new DataInputStream(sisterSocket.getInputStream());
        int bytesRead; 
        byte[] buffer = new byte[1024];        
        while ((bytesRead = dis.read(buffer,0,buffer.length))>0) 
        {
          fos.write(buffer, 0, bytesRead); 
        }   
        sisterSocket.close();
        fos.close();        
        String statusLine = "HTTP/1.1 201 Created" + CRLF;
        String headerLines = "Server: AO:6879" + CRLF +
                             "User-Agent: NetBeans IDE/8.1" + CRLF +
                             "Content-Type: " + contentType(filename) + CRLF +
                             "Date: " + getHTTPTime() + CRLF + CRLF;
        os.writeBytes(statusLine);
        os.writeBytes(headerLines);
        
        success = 1;
    }
    catch(Exception e)
    {
        String statusLine = "HTTP/1.1 204 No Content" + CRLF;
        String headerLines = "Server: AO:6879" + CRLF +
                             "User-Agent: NetBeans IDE/8.1" + CRLF +
                             "Content-Type: " + contentType(filename) + CRLF +
                             "Date: " + getHTTPTime() + CRLF + CRLF;
        os.writeBytes(statusLine);
        os.writeBytes(headerLines);
    }
    return success;
  }
  
  private static String getHTTPTime() {
    Calendar calendar = Calendar.getInstance();
    SimpleDateFormat dateFormat = new SimpleDateFormat(
        "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    return dateFormat.format(calendar.getTime());
  } 

}


