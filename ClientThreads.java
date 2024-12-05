import java.net.*;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ClientThreads extends Thread {
  
  private int port;
  private Path root;
  private ServerSocket se;
  
  public ClientThreads(int args, String path) throws IOException {
    this.port = args;
    this.root = Paths.get(path).toAbsolutePath().normalize();
    this.se = new ServerSocket(args);
  }
  
  @Override // Main mehtod, everythin gets run through here.
  public void run() {
    // Shows the user what port and path the user is on.
    boolean running = true;
    System.out.println("Listening on port: " + port);
    System.out.println("Path: " + root);
    while (running) {
      try {

        // Tries to connect to the client and socket.
        Socket socket = se.accept();
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        OutputStream out = socket.getOutputStream();
        
        Map<String, String> allHeaders = new HashMap<>();
        String requestString = reader.readLine();
        
        // If nothing, then break.
        if (requestString == null) {
          break;
        }
        
        //Get header and put in in hashmap
        String blankLine;
        while (!(blankLine = reader.readLine()).isBlank()) {
          int separatorIndex = blankLine.indexOf(":");
          if (separatorIndex != -1) {
            String headerName = blankLine.substring(0, separatorIndex).trim().toLowerCase();
            String headerVal = blankLine.substring(separatorIndex + 1).trim();
            allHeaders.put(headerName, headerVal);
          }
        }
        
        running = "keep-alive".equalsIgnoreCase(allHeaders.get("connection"));
        
        String[] requestArray = requestString.split(" ");
        if (requestArray.length < 3) {
          //send error request
          badRequest(out);
          continue;
        }
        
        String method = requestArray[0];
        String path = requestArray[1];
        
        //Checks if we're within the login page
        if("POST".equals(method) && "/login".equals(path)) {
          System.out.println("HEHEHE");
          handleRequest(out, allHeaders, reader);
        } else if ("GET".equals(method)) {
          Path resource = root.resolve(path.substring(1)).normalize();
          System.out.println("Path: " + resource);
         

          // If the resouce is a directory, we automatically append index.html to the request
          if (Files.isDirectory(resource)) {
            resource = resource.resolve("index.html");
            System.out.println(resource);
          }
          // If file exists, send an OK response and upload the page via goodRequest.
          if (Files.exists(resource)) {
            String type = Files.probeContentType(resource);
            System.out.println(type);
            byte[] content = Files.readAllBytes(resource);
            goodRequest(out, "200 OK", type, content);
          } else {
            notFound(out);
          }
        }

        // After the request is finished, see if we need to close the connection between user and webpage.
        if (!running) {
          socket.close();
          break;
        }
      } catch (IOException e) {
        e.printStackTrace();
        System.out.println("Error connecting to the Server.");
      }
    }
  }
  
  
  // refers the user to a bad request page
  private static void badRequest(OutputStream out) throws IOException {
    String msg = "HTTP/1.1 400 Bad Request\r\nContent-Type:text/hmtl;" 
    + " charset=UTF-8\r\n\r\n<h1>400 Bad Request</h1>";
    out.write(msg.getBytes(StandardCharsets.UTF_8));
    out.flush();
  }
  
  // Loads the input html file
  private static void goodRequest(OutputStream out, String status, String type, byte[] content) throws IOException {
    String msg = "HTTP/1.1 " + status + "\r\n" + "Content-Type: " + type + "\r\n" +
    "Content-Length: " + content.length + "\r\n" + "\r\n";
    out.write(msg.getBytes(StandardCharsets.UTF_8));
    out.write(content);
    out.flush();
  }
  
  // Refers the user to a Not found page
  private static void notFound(OutputStream out) throws IOException {
    String msg = "HTTP1.1 404 Not Found\r\nContent-Type: text/html;" 
    + "charset=UTF-8\r\n\r\n<h1>404 Not Found</h1>";
    out.write(msg.getBytes(StandardCharsets.UTF_8));
    System.out.println("404 N0T FOUND");
    out.flush();
  }
  


  // Handles the user request. 
  private static void handleRequest(OutputStream out, Map<String, String> head, BufferedReader reader) throws IOException {
    String header = head.get("content-length");
    if (header != null) {
      int length = Integer.parseInt(header);
      char[] body = new char[length];
      int read = reader.read(body, 0, length);
      if (read != length) {
        badRequest(out);
        return;
      }

      // After retrieving credentials, validate the user. Refer the user to a success page or fail page in case of right or wrong credentials.
      String request = new String(body, 0, read);
      Map<String, String> collection = parseString(request);
      if (validateUser(collection) == true) {
        System.out.println("success");
        String msg = "<h1>Login Successful</h1>";
        goodRequest(out, "200 OK", "text/html", msg.getBytes(StandardCharsets.UTF_8));
      } else {
        System.out.println("fail");
        String fail = "<h1>Login Failed</h1>";
        goodRequest(out, "401 unauthorized", "text/html", fail.getBytes(StandardCharsets.UTF_8));
      }
    } else {
      badRequest(out);
    }
  }
  
  //Parse http query
  private static Map<String, String> parseString(String body) {
    Map<String, String> collection = new HashMap<>();
    for (String param : body.split("&")) {
      String[] key = param.split("=");
      if (key.length > 1) {
        collection.put(key[0], key[1]);
      }
    }
    return collection;
  }
  
  //Checks if user is allowed to log in
  private static boolean validateUser(Map<String, String> map) throws IOException {
    Path validationPath = Paths.get("authorization.txt");
    List<String> information = Files.readAllLines(validationPath, StandardCharsets.UTF_8);
    
    Map<String, String> credInfo = new HashMap<>();
    for (String text : information) {
      String[] sections = text.split("=");
      if (sections.length == 2) {
        credInfo.put(sections[0].trim(), sections[1].trim());
      }
    }
    String user = credInfo.get("username");
    String pass = credInfo.get("password");

    // If credentials are correct, send true, if not, send false.
    if (user.equals(map.get("username")) && pass.equals(map.get("password"))) {
      return true;
    } else {
      return false;
    }
    
  }
  
}


