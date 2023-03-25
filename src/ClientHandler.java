import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;

//CLASS TO HANDLE INDIVIDUAL CLIENT CONNECTIONS FROM SERVER
public class ClientHandler extends HangmanServer implements Runnable
{
    //CLIENT SOCKET
    protected Socket clientSocket;

    //CLIENT IN GAME
    protected boolean inGame = false;

    //READY TO PLAY
    protected boolean ready = false;

    //TEAM PICKED OPPONENT
    protected boolean pickedPlayer = false;

    //ASSOCIATED CLIENT
    protected String username;

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
                writer.println();
                writer.printf("%90s %n", "Welcome Back To The Game Room!");
                writer.println("> Would You Like To: ");
                writer.println("1- Login");
                writer.println("2- Register");
                writer.println("3- Leave Game Room");

                String request = reader.readLine();
                switch (request)
                {
                    case "Login" ->
                    {
                        writer.println("\n> Enter Your Username and Password (Space Separated)");
                        String userInput = reader.readLine();
                        ArrayList<Boolean> validationResults = login(userInput);

                        if (validationResults == null)
                        {
                            boolean logged = true;

                            String username = userInput.split(" ")[0];
                            int score = getScore(username);
                            this.username = username;

                            writer.printf("%75s%s%s%s%d%n", "Hello ", username,"!", "Score: ",score);
                            writer.flush();

                            while(logged)
                            {
                                writer.println("\n> Pick An Opponent: ");
                                writer.println("1- AI");
                                writer.println("2- Multiplayer");
                                writer.println("3- Back");

                                String opponent = reader.readLine();
                                switch(opponent)
                                {
                                    case "AI" ->
                                    {
                                        writer.println("\n> You Can Quit Any Moment During the Game By Typing '-'. Type Start To Join A Game Now!");
                                        String startInput = reader.readLine();
                                        if(startInput.equals("Start"))
                                        {
                                            int attempts = 6;

                                            writer.println("\n> Starting Game ... ");
                                            writer.println("> Ready!\n");

                                            String phrase = generatePhrase();
                                            String hiddenPhrase = generateHiddenPhrase(phrase, null, null);

                                            char guess;
                                            String updatedHiddenPhrase = hiddenPhrase;
                                            int notGuessedYet = getGuessOccurrences(hiddenPhrase, '_').size();

                                            boolean AIsTurn=false;
                                            while(attempts > 0 && notGuessedYet > 0)
                                            {
                                                writer.printf("%90s %n", updatedHiddenPhrase);
                                                writer.println("> Attempts Left: " + attempts);
                                                writer.println("> Take A Guess");
                                                guess = reader.readLine().charAt(0);

                                                if(guess != '-')
                                                {
                                                    if(!getGuessOccurrences(updatedHiddenPhrase, guess).isEmpty())
                                                    {
                                                        writer.println("> Character Has Already Been Guessed :|\n");
                                                        attempts--;
                                                    }
                                                    else
                                                    {
                                                        if(generateHiddenPhrase(phrase, updatedHiddenPhrase, guess) != null)
                                                        {
                                                            notGuessedYet-=getGuessOccurrences(phrase, guess).size();
                                                            updatedHiddenPhrase = generateHiddenPhrase(phrase, updatedHiddenPhrase, guess);
                                                            writer.println("> " + generateResponse(true) + "\n");
                                                        }
                                                        else
                                                        {
                                                            attempts--;
                                                            writer.println(generateResponse(false) + ". AI's Turn!\n");
                                                            AIsTurn=true;
                                                            while(AIsTurn)
                                                            {
                                                                Random random = new Random();
                                                                char character = 0;
                                                                boolean allowedChar = false;

                                                                while (!allowedChar)
                                                                {
                                                                    character = (char) ('a' + random.nextInt(26));
                                                                    if (getGuessOccurrences(updatedHiddenPhrase, character).isEmpty())
                                                                        allowedChar = true;
                                                                }
                                                                writer.println("AI Guessed: " + character);

                                                                if(generateHiddenPhrase(phrase, updatedHiddenPhrase, character) != null)
                                                                {
                                                                    notGuessedYet-=getGuessOccurrences(phrase, character).size();
                                                                    updatedHiddenPhrase = generateHiddenPhrase(phrase, updatedHiddenPhrase, character);
                                                                    writer.printf("%90s %n", updatedHiddenPhrase);
                                                                    writer.println();
                                                                    if(notGuessedYet==0)
                                                                        break;
                                                                    else
                                                                        writer.println("AI's Turn!");
                                                                }
                                                                else
                                                                {
                                                                    AIsTurn = false;
                                                                    writer.println("Incorrect Guess! Back To You \n");
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                                else
                                                {
                                                    writer.println("\n> You Quit The Game :( -100pts");
                                                    break;
                                                }
                                            }

                                            writer.printf("%90s%s%n", phrase, "!");
                                            if(notGuessedYet == 0 && !AIsTurn)
                                            {
                                                addToHistory(username,100,true,"SINGLE");
                                                writer.printf("%s%120s%d%n", "> You Win! +100pts!","Score: ",getScore(username));
                                            }
                                            else if(notGuessedYet == 0)
                                            {
                                                addToHistory(username,100,false,"SINGLE");
                                                writer.println("> You Lost Against The AI :( -100pts");
                                            }
                                            else if(attempts == 0)
                                            {
                                                addToHistory(username,50,false,"SINGLE");
                                                writer.println("> You Ran Out Of Guesses :( -50pts");
                                            }
                                            writer.println();
                                        }
                                    }
                                    case "Multiplayer" ->
                                    {
                                        boolean multiplayerMenu = true;
                                        while(multiplayerMenu)
                                        {
                                            writer.println("\n> Choose An Option");
                                            writer.println("1- Create Team");
                                            writer.println("2- Play Game");
                                            writer.println("3- Back");

                                            String joinGame = reader.readLine();
                                            switch(joinGame)
                                            {
                                                case "Create Team" ->
                                                {
                                                    boolean teamNameValid = false;

                                                    while(!teamNameValid)
                                                    {
                                                        writer.println("\n> Pick A Team Name");
                                                        String teamName = reader.readLine();

                                                        if(validateTeamName(teamName))
                                                        {
                                                            String line = teamName + " " + username;

                                                            ArrayList<String> onlineUsers = onlineUsers(username);

                                                            writer.println("\n> Enter the Number of Members in Team");
                                                            int teamSize = Integer.parseInt(reader.readLine());

                                                            if(teamSize > onlineUsers.size())
                                                                writer.println("Not Enough Users Online. Try Again!");
                                                            else
                                                            {
                                                                boolean membersValid = true;

                                                                writer.println("\n> Pick Your Teammates (Enter Numbers Space Separated)");

                                                                for (int j = 0; j < onlineUsers.size(); j++)
                                                                    writer.println(j+1 + "- "+ onlineUsers.get(j));

                                                                String teamIndex = reader.readLine();
                                                                String[] teamIndices = teamIndex.split(" ");

                                                                for(int i = 0; i < teamIndices.length ; i++)
                                                                {
                                                                    if((Integer.parseInt(teamIndices[i]) <= teamSize) && (Integer.parseInt(teamIndices[i]) > 0))
                                                                        line+= " " + onlineUsers.get( (Integer.parseInt(teamIndices[i])) - 1);
                                                                    else
                                                                    {
                                                                        membersValid = false;
                                                                        writer.println("Invalid Team Member Provided! Please Try Again");
                                                                        break;
                                                                    }
                                                                }
                                                                if(membersValid)
                                                                {
                                                                    writer.println("\n Team Created Successfully!");
                                                                    writeToFile("teams.txt", line);
                                                                    teamNameValid = true;
                                                                }
                                                            }
                                                        }
                                                        else
                                                            writer.println("Team Name Already Occupied! Please Try Again");
                                                    }
                                                }
                                                case "Play Game" ->
                                                {
                                                    String team = "";
                                                    String ops = "";

                                                    if(!pickedPlayer)
                                                    {
                                                        writer.println("\n> Pick An Existing Team Name");
                                                        team = reader.readLine();

                                                        writer.println("\n> Pick An Opponents Team Name");
                                                        ops = reader.readLine();

                                                        ArrayList<Boolean> gameValidation = validateTeams(username, team, ops);

                                                        if (gameValidation.isEmpty())
                                                            writer.println("\n> Invalid Team Name!");
                                                        else if (!gameValidation.get(0))
                                                            writer.println("\n> You Are Not A Part Of This Team!");
                                                        else if (!gameValidation.get(1))
                                                            writer.println("\n> You Can Not Exist In Both Teams!");
                                                        else if (!gameValidation.get(2))
                                                            writer.println("\n> Unequal Number of Members on Each Team!");
                                                        else if (!gameValidation.get(3))
                                                            writer.println("\n> Not All Members Are Active At This Moment");
                                                        else
                                                        {
                                                            ready = true;
                                                            inGame = true;

                                                            String[] teamA = getTeamMembers(team);
                                                            String[] teamB = getTeamMembers(ops);

                                                            ArrayList<ClientHandler> teamAHandlers = getAllHandlers(teamA);
                                                            ArrayList<ClientHandler> teamBHandlers = getAllHandlers(teamB);

                                                            for (ClientHandler clientHandler: teamAHandlers)
                                                            {
                                                                if(!clientHandler.username.equals(username))
                                                                    clientHandler.pickedPlayer = true;
                                                            }

                                                            while (inGame)
                                                            {
                                                                startMultiplayerGame(teamAHandlers, teamBHandlers);
                                                            }
                                                        }
                                                    }
                                                    else
                                                    {
                                                        writer.println("\n> Join a Game: " + team + " vs " + ops);
                                                        writer.println("1- Join");
                                                        writer.println("2- Back");

                                                        String joinOrLeave = reader.readLine();
                                                        switch (joinOrLeave)
                                                        {
                                                            case "Join" ->
                                                            {

                                                            }
                                                            case "Back" -> {}
                                                            default -> writer.println("> Invalid Command");
                                                        }
                                                    }
                                                }
                                                case "Back" -> multiplayerMenu = false;
                                                default -> writer.println("> Invalid Command");
                                            }
                                        }
                                    }
                                    case "Back" -> logged = false;
                                    default -> writer.println("> Invalid Command");
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
                        writer.println("\n> Enter Your Desired Nickname, Username and Password (Space Separated)");
                        ArrayList<Boolean> validationResults = register(reader.readLine());

                        if (validationResults == null)
                            writer.println("> Successfully Registered. Please Login");
                        else if(validationResults.get(0))
                            writer.println("> Username Already Occupied. Please Login or Register With An Unoccupied Username");
                        else
                            writer.println("> Invalid Number of Arguments Provided");
                    }
                    case "Leave Game Room" ->
                    {
                        writer.close();
                        reader.close();

                        ClientHandler handler = getHandler(clientSocket);
                        connections.remove(handler);

                        backToMainMenu = false;
                        System.out.println("> Destroying Connection   -> Client " + clientSocket);
                        clientSocket.close();
                    }
                    default -> writer.println("> Invalid Command");
                }
            }
        }
        catch(Exception e)
        {
            ClientHandler handler = getHandler(clientSocket);
            connections.remove(handler);

            System.out.println("> Connection Disrupted    -> Client " + clientSocket);
        }
    }
}