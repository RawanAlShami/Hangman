import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

//CLASS TO HANDLE INDIVIDUAL CLIENT CONNECTIONS FROM SERVER
public class ClientHandler extends HangmanServer implements Runnable
{
    //CLIENT SOCKET
    protected Socket clientSocket;

    //SEND INFO FROM AND TO SERVER
    protected PrintWriter writer;
    protected BufferedReader reader;

    //CONSTRUCTOR
    public ClientHandler(Socket clientSocket) throws IOException
    {
        this.clientSocket = clientSocket;
        writer = new PrintWriter(clientSocket.getOutputStream(), true);
        reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }

    @Override
    public void run()
    {
        try
        {
            boolean backToMainMenu = true;
            while(backToMainMenu)
            {
                writer.println(" \n>> Welcome Back To The Game Room!");
                writer.println(">> Would You Like To: ");
                writer.println("Login");
                writer.println("Register");
                writer.println("Leave Game Room");

                String request = reader.readLine();
                switch (request)
                {
                    case "Login" ->
                    {
                        writer.println("\n>> Enter Your Username and Password");
                        String userInput = reader.readLine();
                        ArrayList<Boolean> validationResults = login(userInput);

                        if (validationResults == null)
                        {
                            String username = userInput.split(" ")[0];
                            writer.println("\nWelcome Back " + username + " !");
                            writer.println("Score: " + getScore(username) + "\n");

                            writer.println("You Can Quit Any Moment From Now On By Typing Quit. Type Start To Join A Game Now!");
                            String startInput = reader.readLine();

                            if(startInput.equals("Start"))
                            {
                                writer.println("\n>> Pick An Opponent: ");
                                writer.println("AI");
                                writer.println("MultiPlayer\n");

                                String opponent = reader.readLine();
                                switch(opponent)
                                {
                                    case "AI" ->
                                    {
                                        writer.println(">> Starting Game ... ");
                                        writer.println(">> Ready!");
                                        //OP PHRASE W START GAME
                                    }
                                    case "MultiPlayer" ->
                                    {
                                        writer.println("\n>> Online Players: ");
                                        //Op list of nicknames
                                        writer.println("\n>> Pick Your Team: ");
                                        //
                                        writer.println("\n>> Pick Your Opponents");
                                        //
                                        writer.println("\n>> Pick Team Name");
                                        //
                                    }
                                    //REPLACE CASE DI B WHILE REQUEST != QUIT
                                    //case "Quit" -> {}
                                    default -> writer.println(">> Invalid Command");
                                }
                            }
                        }
                        else if(!(validationResults.get(0)))
                            writer.println("Invalid Number of Arguments Provided");
                        else if((validationResults.size() == 2) && (validationResults.get(1)))
                            writer.println("Wrong Password Provided. Please Try to Login Again");
                        else
                            writer.println("Username Doesn't Exist. Please Try to Login Again");
                    }
                    case "Register" ->
                    {
                        writer.println("\n>> Enter Your Desired Nickname, Username and Password");
                        ArrayList<Boolean> validationResults = register(reader.readLine());

                        if (validationResults == null)
                            writer.println("Successfully Registered. Please Login");
                        else if(validationResults.get(0))
                            writer.println("Username Already Occupied. Please Login or Register With An Unoccupied Username");
                        else
                            writer.println("Invalid Number of Arguments Provided");
                    }
                    case "Leave Game Room" ->
                    {
                        writer.close();
                        reader.close();
                        clientSocket.close();
                        backToMainMenu = false;
                        System.out.println("> Destroying Connection   -> Client " + clientSocket);
                    }
                    default -> writer.println(">> Invalid Command");
                }
            }
        }
        catch(Exception e)
        {
            System.out.println("> Connection Disrupted    -> Client " + clientSocket);
        }
    }
}