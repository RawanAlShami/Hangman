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

    //ASSOCIATED CLIENT
    protected String username;

    //CLIENTS TEAM NAME
    protected String teamName;

    //OPPONENT TEAM NAME
    protected String opponentTeamName;

    //CLIENT IN GAME
    protected boolean inGame = false;

    //TEAM PICKED OPPONENT
    protected boolean pickedPlayer = false;

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
                writer.println("- Login");
                writer.println("- Register");
                writer.println("- Leave Game Room");

                //MAI MENU SWITCH
                String request = reader.readLine();
                switch (request)
                {
                    case "Login" ->
                    {
                        //READ INPUTS
                        writer.println("\n> Enter Your Username and Password (Space Separated)");
                        String userInput = reader.readLine();

                        //VALIDATE INPUTS
                        ArrayList<Boolean> validationResults = login(userInput);
                        //OK
                        if (validationResults == null)
                        {
                            boolean logged = true;

                            //DISPLAY CREDENTIALS
                            String username = userInput.split(" ")[0];
                            int score = getScore(username);
                            this.username = username;

                            writer.printf("%75s%s%s%60s%d%n", "Hello ", username,"!", "Score: ",score);
                            writer.flush();

                            while (logged)
                            {
                                //LOGGED IN USERS MENU
                                writer.println("\n> Pick An Opponent: ");
                                writer.println("- AI");
                                writer.println("- Multiplayer");
                                writer.println("- Back");

                                //GAME OPPONENT MENU
                                String opponent = reader.readLine();
                                switch (opponent)
                                {
                                    case "AI" ->
                                    {
                                        writer.println("\n> You Can Quit Any Moment During the Game By Typing '-'. Type 'Start' To Join A Game Now!");
                                        String startInput = reader.readLine();

                                        //START GAME WITH AI
                                        if (startInput.equals("Start"))
                                        {
                                            //MAX ATTEMPTS
                                            int attempts = 6;

                                            writer.println("\n> Starting Game ... ");
                                            writer.println("> Ready!\n");

                                            //GENERATE AND HIDE PHRASE
                                            String phrase = generatePhrase();
                                            String hiddenPhrase = generateHiddenPhrase(phrase, null, null);

                                            char guess;
                                            String updatedHiddenPhrase = hiddenPhrase;
                                            int notGuessedYet = getGuessOccurrences(hiddenPhrase, '_').size();

                                            //ONGOING GAME
                                            boolean AIsTurn = false;
                                            while (attempts > 0 && notGuessedYet > 0)
                                            {
                                                writer.printf("%90s %n", updatedHiddenPhrase);
                                                writer.println("> Attempts Left: " + attempts);
                                                writer.println("> Take A Guess");
                                                guess = reader.readLine().charAt(0);

                                                if (guess != '-')
                                                {
                                                    //CHARACTER HAS ALREADY BEEN GUESSED AND REVEALED
                                                    if (!getGuessOccurrences(updatedHiddenPhrase, guess).isEmpty())
                                                    {
                                                        writer.println("> Character Has Already Been Guessed :|\n");
                                                        attempts--;
                                                    }
                                                    else
                                                    {
                                                        //CORRECT GUESS
                                                        if (generateHiddenPhrase(phrase, updatedHiddenPhrase, guess) != null)
                                                        {
                                                            notGuessedYet -= getGuessOccurrences(phrase, guess).size();
                                                            updatedHiddenPhrase = generateHiddenPhrase(phrase, updatedHiddenPhrase, guess);
                                                            writer.println("> " + generateResponse(true) + "\n");
                                                        }
                                                        //INCORRECT GUESS. SWITCH TO AI
                                                        else
                                                        {
                                                            attempts--;
                                                            writer.println(generateResponse(false) + ". AI's Turn!\n");
                                                            AIsTurn = true;
                                                            while (AIsTurn)
                                                            {
                                                                Random random = new Random();
                                                                char character = 0;
                                                                boolean allowedChar = false;

                                                                //GENERATE A CHARACTER THAT HAS NOT BEEN REVEALED YET
                                                                while (!allowedChar)
                                                                {
                                                                    character = (char) ('a' + random.nextInt(26));
                                                                    if (getGuessOccurrences(updatedHiddenPhrase, character).isEmpty())
                                                                        allowedChar = true;
                                                                }
                                                                writer.println("AI Guessed: " + character);

                                                                //AI CORRECT GUESS
                                                                if (generateHiddenPhrase(phrase, updatedHiddenPhrase, character) != null)
                                                                {
                                                                    notGuessedYet -= getGuessOccurrences(phrase, character).size();
                                                                    updatedHiddenPhrase = generateHiddenPhrase(phrase, updatedHiddenPhrase, character);
                                                                    writer.printf("%90s %n", updatedHiddenPhrase);
                                                                    writer.println();
                                                                    if (notGuessedYet == 0)
                                                                        break;
                                                                    else
                                                                        writer.println("AI's Turn!");
                                                                }
                                                                //AI INCORRECT GUESS. SWITCH TO CLIENTS TURN
                                                                else
                                                                {
                                                                    AIsTurn = false;
                                                                    writer.println("Incorrect Guess! Back To You \n");
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                                //USER QUITS MID GAME
                                                else
                                                {
                                                    addToHistory(username, 100, false, "SINGLE");
                                                    writer.printf("%s%120s%d%n", "> You Quit The Game :( -100pts", "Score: ", getScore(username));
                                                    break;
                                                }
                                            }
                                            //REVEAL PHRASE
                                            writer.printf("%90s%s%n", phrase, "!");

                                            //END OF GAME CONDITIONS
                                            if (notGuessedYet == 0 && !AIsTurn)
                                            {
                                                addToHistory(username, 100, true, "SINGLE");
                                                writer.printf("%s%120s%d%n", "> You Win! +100pts!", "Score: ", getScore(username));
                                            }
                                            else if (notGuessedYet == 0)
                                            {
                                                addToHistory(username, 100, false, "SINGLE");
                                                writer.printf("%s%110s%d%n", "> You Lost Against The AI :( -100pts", "Score: ", getScore(username));
                                            }
                                            else if (attempts == 0)
                                            {
                                                addToHistory(username, 50, false, "SINGLE");
                                                writer.printf("%s%110s%d%n", "> You Ran Out Of Guesses :( -50pts", "Score: ", getScore(username));
                                            }
                                            writer.println();
                                        }
                                    }
                                    case "Multiplayer" ->
                                    {
                                        boolean multiplayerMenu = true;
                                        while (multiplayerMenu)
                                        {
                                            writer.println("\n> Choose An Option");
                                            writer.println("- Create Team");
                                            writer.println("- Play Game");
                                            writer.println("- Back");

                                            //TEAMS MENU
                                            String joinGame = reader.readLine();
                                            switch (joinGame)
                                            {
                                                case "Create Team" ->
                                                {
                                                    boolean teamNameValid = false;
                                                    while (!teamNameValid)
                                                    {
                                                        //PICK TEAM NAME
                                                        writer.println("\n> Pick A Team Name");
                                                        String teamName = reader.readLine();

                                                        //VALID TEAM NAME
                                                        if (validateTeamName(teamName))
                                                        {
                                                            String line = teamName + " " + username;

                                                            //GETS ALL ONLINE USERS FOR CLIENT TO PICK FROM EXCLUDING CURRENT CLIENT
                                                            ArrayList<String> onlineUsers = onlineUsers(username);

                                                            writer.println("\n> Enter the Number of Members in Team");
                                                            int teamSize = Integer.parseInt(reader.readLine());

                                                            //VALIDATE TEAM SIZE
                                                            if (teamSize > onlineUsers.size())
                                                                writer.println("> Not Enough Users Online. Try Again!");
                                                            else if(teamSize < 0)
                                                                writer.println("> Insufficient Number Of Members In Team");
                                                            else
                                                            {
                                                                boolean membersValid = true;

                                                                //MULTIPLAYER TEAM
                                                                if(teamSize != 0)
                                                                {
                                                                    //DISPLAY ONLINE USERS' USERNAME
                                                                    writer.println("\n> Pick Your Teammates (Enter Numbers Space Separated)");

                                                                    for (int j = 0; j < onlineUsers.size(); j++)
                                                                        writer.println(j + 1 + "- " + onlineUsers.get(j));

                                                                    String teamIndex = reader.readLine();
                                                                    String[] teamIndices = teamIndex.split(" ");

                                                                    //VALIDATE USERS PICKED
                                                                    for (String index : teamIndices)
                                                                    {
                                                                        //INDEX PROVIDED GREATER THAN 0 AND LESS THAN TEAM SIZE
                                                                        if ((Integer.parseInt(index) <= teamSize) && (Integer.parseInt(index) > 0))
                                                                            line += " " + onlineUsers.get((Integer.parseInt(index)) - 1);
                                                                        else
                                                                        {
                                                                            membersValid = false;
                                                                            writer.println("> Invalid Team Member Provided! Please Try Again");
                                                                            break;
                                                                        }
                                                                    }
                                                                    if (membersValid)
                                                                    {
                                                                        writer.println("\n> Team Created Successfully!");
                                                                        writeToFile("teams.txt", line);
                                                                        teamNameValid = true;
                                                                    }
                                                                }
                                                                //SOLO TEAM
                                                                else
                                                                {
                                                                    String noMembers = teamName + " " + username;
                                                                    writer.println("\n> Team Created Successfully!");
                                                                    writeToFile("teams.txt", noMembers);
                                                                    teamNameValid = true;
                                                                }
                                                            }
                                                        }
                                                        else
                                                            writer.println("> Team Name Already Occupied! Please Try Again");
                                                    }
                                                }
                                                case "Play Game" ->
                                                {
                                                    //IF CLIENT HANDLER CHOOSES TO PLAY AGAINST A TEAM
                                                    if (!pickedPlayer)
                                                    {
                                                        //GET TEAM NAME
                                                        writer.println("\n> Pick Your Team Name (Existing)");
                                                        this.teamName = reader.readLine();

                                                        //GET OPPONENTS TEAM NAME
                                                        writer.println("\n> Pick An Opponents Team Name");
                                                        this.opponentTeamName = reader.readLine();

                                                        //VALIDATE TEAM FORMATION
                                                        ArrayList<Boolean> gameValidation = validateTeams(username, teamName, opponentTeamName);

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
                                                            //OK
                                                            //TEAM MEMBERS USERNAMES
                                                            String[] teamA = getTeamMembers(teamName);
                                                            String[] teamB = getTeamMembers(opponentTeamName);

                                                            //TEAM MEMBERS CLIENT HANDLERS
                                                            ArrayList<ClientHandler> teamAHandlers = getAllHandlers(teamA);
                                                            ArrayList<ClientHandler> teamBHandlers = getAllHandlers(teamB);

                                                            //SET LEADING TEAMS INFO
                                                            for (ClientHandler clientHandler : teamAHandlers)
                                                            {
                                                                if (!clientHandler.username.equals(username))
                                                                {
                                                                    clientHandler.pickedPlayer = true;
                                                                    clientHandler.teamName = teamName;
                                                                    clientHandler.opponentTeamName = opponentTeamName;
                                                                }
                                                            }
                                                            //SET OPPONENT TEAMS INFO
                                                            for (ClientHandler clientHandler : teamBHandlers)
                                                            {
                                                                if (!clientHandler.username.equals(username))
                                                                {
                                                                    clientHandler.pickedPlayer = true;
                                                                    clientHandler.teamName = opponentTeamName;
                                                                    clientHandler.opponentTeamName = teamName;
                                                                }
                                                            }

                                                            inGame = true;
                                                            //START GAME
                                                            while (inGame)
                                                            {
                                                                writer.println("\n> Waiting For Members Approval ...");
                                                                Thread.sleep(200);
                                                                //IF ALL MEMBERS APPROVED THE GAME. START
                                                                if (playersInGame(teamAHandlers, teamBHandlers))
                                                                    startMultiplayerGame(teamAHandlers, teamBHandlers);
                                                            }
                                                        }
                                                    }
                                                    //IF A TEAM IS CHOSEN
                                                    else
                                                    {
                                                        //NOT ALLOWED TO PICK A TEAM. THEY ONLY APPROVE THE REQUEST
                                                        writer.println("\n> Join a Game [ " + teamName + " VS " + opponentTeamName + " ]");
                                                        writer.println("- Accept");
                                                        writer.println("- Decline");

                                                        String joinOrLeave = reader.readLine();
                                                        switch (joinOrLeave)
                                                        {
                                                            case "Accept" ->
                                                            {
                                                                inGame = true;
                                                                while (inGame)
                                                                {
                                                                    Thread.sleep(200);
                                                                }
                                                            }
                                                            case "Decline" ->
                                                            {
                                                                String[] teamA = getTeamMembers(teamName);
                                                                String[] teamB = getTeamMembers(opponentTeamName);

                                                                ArrayList<ClientHandler> teamAHandlers = getAllHandlers(teamA);
                                                                ArrayList<ClientHandler> teamBHandlers = getAllHandlers(teamB);

                                                                //RELEASE VARIABLE INITIALIZATION TO DENY A REQUEST
                                                                for (ClientHandler clientHandler : teamAHandlers)
                                                                {
                                                                    clientHandler.pickedPlayer = false;
                                                                    clientHandler.teamName = null;
                                                                    clientHandler.opponentTeamName = null;
                                                                    clientHandler.inGame = false;
                                                                }
                                                                for (ClientHandler clientHandler : teamBHandlers)
                                                                {
                                                                    clientHandler.pickedPlayer = false;
                                                                    clientHandler.teamName = null;
                                                                    clientHandler.opponentTeamName = null;
                                                                    clientHandler.inGame = false;
                                                                }
                                                            }
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
                        else if (!(validationResults.get(0)))
                            writer.println("> Invalid Number of Arguments Provided");
                        else if ((validationResults.size() == 2) && (validationResults.get(1)))
                            writer.println("> Wrong Password Provided. Please Try to Login Again");
                        else
                            writer.println("> Username Doesn't Exist. Please Try to Login Again");
                    }
                    case "Register" ->
                    {
                        writer.println("\n> Enter Your Desired Nickname, Username and Password (Space Separated)");
                        ArrayList<Boolean> validationResults = register(reader.readLine());

                        if (validationResults == null)
                            writer.println("> Successfully Registered. Please Login");
                        else if (validationResults.get(0))
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