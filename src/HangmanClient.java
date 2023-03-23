import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

//THREADED TO MAINTAIN AN OPEN CHANNEL WITH SERVER TO SEND AND RECEIVE MESSAGES
public class HangmanClient implements Runnable
{
    //CLIENT SOCKET INFO
    protected boolean clientIsUp;
    protected Socket clientSocket;

    //SEND INFO FROM AND TO SERVER
    protected PrintWriter writer;
    protected BufferedReader reader;

    //READ INPUT FROM CLIENT'S KEYBOARD
    protected BufferedReader input;

    public static void main(String[] args)
    {
        HangmanClient hangmanClient = new HangmanClient();
        hangmanClient.run();
    }

    @Override
    public void run()
    {
        try
        {
            //INSTANTIATE CLIENT SOCKET
            clientSocket = new Socket("127.0.0.1", 9999);
            clientIsUp = true;

            //INITIALIZING READER AND WRITER
            writer = new PrintWriter(clientSocket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            //SINGLE INPUT CHANNEL THREADS
            InputChannel inputChannel = new InputChannel();
            Thread inputThread = new Thread(inputChannel);
            inputThread.start();

            //OUTPUTS SERVER RESPONSE TO CLIENT
            String response;
            while((response = reader.readLine()) != null)
                System.out.println(response);
        }
        catch(IOException e)
        {
            System.out.println("\n> Failed To Establish Connection With Server");
        }
    }

    public class InputChannel implements Runnable
    {
        @Override
        public void run()
        {
            try
            {
                //READ INPUT FROM CLIENT'S KEYBOARD
                input = new BufferedReader(new InputStreamReader(System.in));
                while(clientIsUp)
                {
                    String request = input.readLine();

                    //SEND REQUEST TO SERVER
                    writer.println(request);

                    // CLOSE ALL READERS, WRITERS AND SOCKET IF USER CHOOSES TO LEAVE
                    if(request.equals("Leave Game Room"))
                    {
                        input.close();
                        reader.close();
                        writer.close();
                        clientIsUp = false;
                        clientSocket.close();
                        System.out.println("\n> Leaving Game Room. Goodbye!");
                    }
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
    }
}