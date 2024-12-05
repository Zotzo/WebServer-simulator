import java.io.IOException;

public class WebServer {


  // Runs the run method through this main method.
  public static void main(String[] args) throws NumberFormatException, IOException {
  ClientThreads client = new ClientThreads(Integer.parseInt(args[0]), args[1]);
  client.run();
} 
}   
