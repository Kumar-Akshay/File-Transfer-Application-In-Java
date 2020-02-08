package myMasterClient;

import java.io.*;
import static java.lang.Thread.sleep;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

public class myClientController implements Initializable {

    @FXML private TextArea logDisplay;
    @FXML private TextArea serverLog;
    @FXML private Label clientLabel;
    @FXML private Button connect;
    @FXML private Button disconnect;
    @FXML private Button viewBtn;
    @FXML private Button uploadBtn;
    @FXML private Button downloadBtn;
    @FXML private Button showBtn;
    @FXML private Button hideBtn;
    @FXML private Button closeBtn;
    @FXML private Label serverLogLabel;
    
    private final String CRLF = "\r\n"; 
    private final byte[] STOP = "\rSTOP".getBytes();
    private String clientName = ""; 
    private String directoryPath = "";
    private boolean connected = false;
    
    String serverAddress = "127.0.0.1"; 
    int port = 6789; 

    Socket connectionSocket = null; 
    DataOutputStream out = null;  
    BufferedReader in = null;  
 
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        
    }    
  
  private boolean checkPath(String path) {
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
  @FXML
  private void connectEvent(ActionEvent event) {
      TextInputDialog dialog = new TextInputDialog();
      dialog.setTitle("User Input Dialog");
      dialog.setHeaderText("To get path of a folder: \n1. Press Shift+Right click on desired folder\n2. Click \"Copy as path\" from the drop-down menu\n3. Click on input field below and Ctrl+V to paste path into field\n4. Delete quotation marks from the file path before clicking OK");
      dialog.setContentText("Enter path of client directory: ");
      Optional<String> result = dialog.showAndWait();
      boolean userEnteredInput = result.isPresent(); 
      boolean isValidPath = false;
      if (userEnteredInput) 
      {
        directoryPath = result.get(); 
        if(directoryPath.length() == 0) 
           logDisplay.appendText("> No input detected. Click Connect to try again\n");
         else  
         {             
            isValidPath = checkPath(directoryPath); 
            if(!isValidPath) 
               logDisplay.appendText("> "+ directoryPath +" is not a valid directory path. Click Connect to try again\n");           
         } 
      }
      
      if(isValidPath) 
      {
        logDisplay.appendText("> Directory Path: "+directoryPath+"\n"); 
        logDisplay.appendText("----------------------------------------------\n");
      
        try   
        { 
          connectionSocket = new Socket(serverAddress,port); 
          out = new DataOutputStream(connectionSocket.getOutputStream());
          in = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
          this.clientName = generateRandomName(); 
          logDisplay.appendText("> Successfully connected to server.\n> Your username is "+clientName+".\n");
          clientLabel.setText("Client: " + clientName); 
          connect.setDisable(true); 
          logDisplay.appendText("\n********** USERNAME REGISTRATION **********\n");
          sendClientRequest("POST", "<SUBMIT-USERNAME>", out); 
          out.writeBytes(clientName);  
          out.writeBytes(CRLF); 
          out.writeBytes(CRLF);

          serverLog.appendText("> Server Response for USERNAME REGISTRATION: \n");          
          int code = getServerCode(in); 
          serverLog.appendText("--------------------------------------------\n");
                    
          if(code==201)
          {
            logDisplay.appendText("> Username successfully registered at server.\n");
            connected = true;
          }
          else if(code==204)
          {
            logDisplay.appendText("> Error registering username at server.\n");                 
          }

          //enable functional buttons
          disconnect.setDisable(false);
          viewBtn.setDisable(false);
          uploadBtn.setDisable(false);
          downloadBtn.setDisable(false);
        } 
        catch (Exception e) 
        {
          logDisplay.appendText("> Could not connect to server.\n");
        }  
      }
    }
    
  private String generateRandomName() {
        String letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        int random1 = (int)(Math.random()*52); 
        int random2 = (int) (Math.random()*52);
        int random3 = (int) (Math.random()*52);
        int random4 = (int) (Math.random()*100);
        String username = "" + letters.charAt(random1)+letters.charAt(random2)+letters.charAt(random3)+random4;
        return username;
    }
 
  @FXML
  private void onViewEvent(ActionEvent event) throws Exception 
  {
        logDisplay.appendText("\n********** VIEW FILES **********");  
        sendClientRequest("GET", "File-List", out); 
                
        serverLog.appendText("> Server Response for VIEW FILE REQUEST: \n");   
        int statusCode = getServerCode(in); 
        serverLog.appendText("--------------------------------------------\n");
        
        if(statusCode==200) //request OK
        {
          String body = "\n> "; 
          String temp = ""; 
          while((temp = in.readLine()).length()!=0)  
          {
            body += temp + "\n"; 
          } 
          body += "\n";
          logDisplay.appendText(body); 
        }
  }
   
  
  @FXML
  private void onUploadEvent(ActionEvent event) throws Exception
    {         
        logDisplay.appendText("\n********** FILE UPLOAD **********\n");
        String clientFileList = getClientFileList()+"\n"; //get client file list
        logDisplay.appendText(clientFileList);
        String filenameUpload = "";
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Text Input Dialog");
        dialog.setContentText("Enter name of file to upload (with extensions)");
        dialog.setHeaderText(clientFileList); 
        Optional<String> result = dialog.showAndWait();
        boolean userEnteredInput = result.isPresent(); 
        if (userEnteredInput) 
        {
          filenameUpload = result.get(); 
          
          if(filenameUpload.length() != 0){
            logDisplay.appendText(">You requested to upload file '"+filenameUpload+"'\n");
            boolean fileExists = checkFileExists(filenameUpload);
            if(fileExists)
            {
              sendClientRequest("POST",filenameUpload,out);
              sendFile(filenameUpload,out);
              
              serverLog.appendText("> Server Response for UPLOAD REQUEST: \n");               
              int code = getServerCode(in);
              serverLog.appendText("--------------------------------------------\n");
              
              if(code==201) 
              {
                logDisplay.appendText("> Upload Complete!\n");
              }
              else if(code==204) 
              {
               logDisplay.appendText("> Error uploading file to server.\n");

              }     
            }
            else {
                logDisplay.appendText("> File '"+ filenameUpload + "' does not exist in client. Try again\n");
            }
          }
          else 
          {
            logDisplay.appendText("> No filename entered.\n"); 
          }
        }
        
    }
  
  private String getServerFileList(DataOutputStream out) throws Exception{
      String fileList = "";
      sendClientRequest("GET", "File-List", out);
      
      String temp = "";
      while((temp = in.readLine()).length()!= 0) 
      {} 
      temp="";
      while((temp = in.readLine()).length()!=0) 
      {
        fileList += temp + "\n"; 
      } 
      fileList += "\n";
      
      return fileList;
  }
  
  private boolean checkFileExists(String filename) {
        boolean fileExists = true;
        filename = directoryPath + "\\" +filename; 
        FileInputStream fis = null;
        try 
        {
          fis = new FileInputStream(filename); 
        }
        catch (Exception e) 
        {
          fileExists = false;
        }
        return fileExists;     
    }
 
  @FXML
  private void onDownloadEvent(ActionEvent event) throws Exception {
        String serverFileList = getServerFileList(out);
        logDisplay.appendText("\n********** FILE DOWNLOAD **********\n");
        logDisplay.appendText(serverFileList);
        String filename = "";
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Text Input Dialog");
        dialog.setContentText("Enter name of file to download (with extensions)");
        dialog.setHeaderText(serverFileList);
        Optional<String> result = dialog.showAndWait();
        boolean userEnteredInput = result.isPresent(); 
        if (userEnteredInput) 
        {
            filename = result.get();
            if(filename.length() != 0) 
            {
                logDisplay.appendText("> You requested to download file '"+filename+"'\n");
                sendClientRequest("GET",filename, out); 
                
                serverLog.appendText("> Server Response for DOWNLOAD REQUEST: \n");               
                int serverStatusCode = getServerCode(in); 
                serverLog.appendText("--------------------------------------------\n");
     
                if(serverStatusCode==200) 
                {
                   logDisplay.appendText("> File found on server. Downloading...\n");
                   getFile(filename); 
                   logDisplay.appendText("> Download complete!\n");
                } 
                else if(serverStatusCode==404) 
                {
                  logDisplay.appendText("> The requested file '"+filename+"' does not exist on the server.\n");             
                }      
            }       
            else 
            {
               logDisplay.appendText("> No filename entered.\n");          
            }
        }
   }
  
  private void getFile(String filename) throws Exception {
        String serverIP = "127.0.0.1";
        int serverPORT = 7000; 
        
        Socket sisterSocket = new Socket(serverIP, serverPORT);
        File file = new File(directoryPath + "\\" + filename); 
        FileOutputStream fos = new FileOutputStream(file); 
        DataInputStream dis = new DataInputStream(sisterSocket.getInputStream());
        BufferedReader in = new BufferedReader(new InputStreamReader(sisterSocket.getInputStream()));
        int bytesRead;
        byte[] buffer = new byte[1024];     
        while ((bytesRead = dis.read(buffer,0,buffer.length))>0) 
        {
          fos.write(buffer, 0, bytesRead); 
        }   
        sisterSocket.close();
        fos.close();        
  } 
  @FXML
  private void disconnectEvent(ActionEvent event){      
        try {
           out.writeBytes("</DISCONNECT>");
           out.flush();
           sleep(500); 
           out.close();
           in.close();
           connectionSocket.close(); 
            disconnect.setDisable(true);    
            viewBtn.setDisable(true);
            uploadBtn.setDisable(true);
            downloadBtn.setDisable(true);
            connect.setDisable(false); 
            logDisplay.setText("");
            serverLog.setText("");
            clientLabel.setText("Client");
        }
         catch(Exception e) {
           logDisplay.appendText("\n> Could not disconnect.\n");
         }
        finally
        {
            connected = false; 
        }
    }
    
  private String getClientFileList() {
      File clientDir = new File(directoryPath); 
      File[] listOfFiles = clientDir.listFiles();
      String fileList = "Available files (subfolders are in parentheses): \n";
      
      for (File file : listOfFiles) 
      {
        if (file.isFile()) 
           fileList += file.getName()+"  ";        
        
        else if (file.isDirectory()) 
           fileList += "("+file.getName()+")  ";        
      }      
      fileList +="\n"; 
      
      return fileList; 
  }
  
  private void sendClientRequest(String methodType, String filename, DataOutputStream out) throws Exception{
      String requestLine = methodType + " /"+filename+" HTTP/1.1" +CRLF;
      String contentType = "";
      
      if(filename.equals("File-List")) 
        contentType = "text/plain";
      else
        contentType = getContentType(filename); 
    
      String contentLength = "";
      if(methodType.equals("POST")) 
      {
        if(filename.equals("<SUBMIT-USERNAME>")) 
        {
           contentLength = "Content-Length: " + clientName.length() + CRLF;  
        }
        else {
           File file = new File(directoryPath + "\\" + filename);  
           contentLength = "Content-Length: " + file.length() + CRLF; 
        }       
      }
      String headerLines = "Client-Name: " + this.clientName + CRLF +
                           "Host: AO:6879" + CRLF +
                           "User-Agent:  NetBeans IDE/8.1" + CRLF +
                           "Content-Type: " + contentType + CRLF +
                           contentLength + 
                           "Date: " + getHTTPTime() + CRLF + CRLF;
      out.writeBytes(requestLine);
      out.writeBytes(headerLines);
  }
  
  private int getServerCode(BufferedReader in) throws Exception {
      String statusLine = in.readLine() + "\n";             
      String statusHeaderLines = "";
      String temp = "";
      while((temp = in.readLine()).length()!= 0) {
        statusHeaderLines += temp + "\n";} 
      serverLog.appendText(statusLine); 
      serverLog.appendText(statusHeaderLines);       
      String[] parts = statusLine.split(" "); 
      String statusCode = parts[1]; 
      int code=0;
      try 
      {
        code = Integer.parseInt(statusCode);
      }
      catch(Exception e)
      {
        logDisplay.appendText("> Error reading server response code.\n");
      }
      return code;
    }  
  
  private static String getContentType(String filename)
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
  
  private static String getHTTPTime() {
    Calendar calendar = Calendar.getInstance();
    SimpleDateFormat dateFormat = new SimpleDateFormat(
        "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    return dateFormat.format(calendar.getTime());
  } 
  
  private void sendFile(String filename, DataOutputStream os) throws Exception {
    filename = directoryPath + "\\" + filename; 
    FileInputStream fis = null;
    boolean fileExists = true;
    try{
      fis = new FileInputStream(filename);
    }catch (Exception e){
      fileExists = false;
    }
    if (fileExists) {
       try{
            int serverPORT = 7000; 
            ServerSocket sisterSocket = new ServerSocket(serverPORT);
            Socket sisterClientSocket = sisterSocket.accept();
            DataOutputStream sisterOS = new DataOutputStream(sisterClientSocket.getOutputStream()); 

            byte[] buffer = new byte[1024];
            int bytesRead = 0; 
            while ((bytesRead = fis.read(buffer)) != -1) 
            {
              sisterOS.write(buffer, 0, bytesRead); 
            }
            fis.close();
            sisterOS.close();
            sisterClientSocket.close();
            sisterSocket.close();
        }
        catch (IOException e) 
        {
          logDisplay.appendText("> Error uploading file to client.\n");
        }
        finally
        {
           if(fis!=null)
             fis.close();
        }
    } 
    else
    {
        logDisplay.appendText("> File does not exist in client directory.\n");
    }
  }
  
  @FXML
  private void onCloseEvent(ActionEvent event) throws Exception {
      if(connected) 
      {
           out.writeBytes("</DISCONNECT>");
           out.flush();
           sleep(500); 
           out.close();
           in.close();
           connectionSocket.close();      
      }
      Stage stage = (Stage) closeBtn.getScene().getWindow();
      stage.close();     
  }
}
 
