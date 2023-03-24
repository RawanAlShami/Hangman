import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
@SuppressWarnings("resource")
public class HangmanServer implements Runnable
{
    //SERVER LISTENER INFO
    protected boolean serverIsUp;
    protected ServerSocket serverListener;

    //THREAD POOL
    protected ExecutorService threadPool;

    //TRACK ALL SERVER CONNECTIONS
    protected static ArrayList<ClientHandler> connections = new ArrayList<>();

    public static void main(String[] args)
    {
        HangmanServer Server = new HangmanServer();
        Server.run();
    }

    @Override
    public void run()
    {
        try
        {
            //INSTANTIATE SERVER LISTENER
            serverListener = new ServerSocket(9999);
            serverIsUp = true;
            System.out.println("\n> Successfully Started Server ...");

            //INITIALIZE FIXED THREAD POOL
            threadPool = Executors.newFixedThreadPool(4);

            //LOAD FILES UPON SERVER STARTUP
            loadFiles();

            System.out.println("> Listening For Connections");

            //LISTEN FOR CONNECTIONS
            while(serverIsUp)
            {
                //PROVIDE SOCKETS FOR CLIENT CONNECTIONS
                Socket clientSocket = serverListener.accept();
                System.out.println("> Establishing Connection -> Client " + clientSocket);

                //CREATE CLIENT THREAD AND EXECUTE RUN FUNCTION
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                threadPool.execute(clientHandler);
                connections.add(clientHandler);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void loadFiles() throws IOException
    {
        String[] fileNames = {"clients.txt", "scoreHistory.txt", "phrases.txt"};

        for (String name:fileNames)
        {
            File file = new File(name);
            if(!file.exists())
            {
                File newFile = new File(name);
                newFile.createNewFile();
            }
        }
        System.out.println("> Files Loaded Successfully");
    }

    public void writeToFile(String fileName, String line) throws IOException
    {
        FileWriter fileWriter = new FileWriter(fileName, true);
        fileWriter.write(line);
        fileWriter.write(System.getProperty( "line.separator" ));
        fileWriter.close();
    }

    public ArrayList<Boolean> register(String credentials) throws IOException
    {
        ArrayList<Boolean> validationResults = validateRegistration(credentials);
        if((validationResults.size() == 1) && (validationResults.get(0)))
        {
            writeToFile("clients.txt",credentials);
            writeToFile("scoreHistory.txt",credentials.split(" ")[1] + " " + Timestamp.from(Instant.now()) + " REGISTERED 100");
            return null;
        }
        return validationResults;
    }

    public ArrayList<Boolean> validateRegistration(String credentials)
    {
        //TO CLASSIFY INVALID INPUT
        ArrayList<Boolean> validationResults = new ArrayList<>();
        try
        {
            //OPEN CREDENTIALS FILE
            FileInputStream fileStream = new FileInputStream("clients.txt");
            DataInputStream inputStream = new DataInputStream(fileStream);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            //SPLIT CLIENT INPUT
            String[] splitInput = credentials.split(" ");
            //INVALID NUMBER OF ARGUMENTS
            if(splitInput.length != 3)
            {
                validationResults.add(false);
                return validationResults;
            }

            validationResults.add(true);
            String providedUsername = splitInput[1];

            String fileLine;
            //USERNAME RESERVED
            while ((fileLine = bufferedReader.readLine()) != null)
            {
                if(fileLine.split(" ")[1].equals(providedUsername))
                {
                    validationResults.add(false);
                    return validationResults;
                }
            }
            inputStream.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return validationResults;
    }

    public ArrayList<Boolean> login(String credentials)
    {
        ArrayList<Boolean> validationResults = validateLogin(credentials);
        if((validationResults.size() == 1) && (validationResults.get(0)))
        {
//            onlineUsers.add(credentials.split(" ")[0]);
            return null;
        }
        return validationResults;
    }

    public ArrayList<Boolean> validateLogin(String credentials)
    {
        //TO CLASSIFY INVALID INPUT
        ArrayList<Boolean> validationResults = new ArrayList<>();
        try
        {
            //OPEN CREDENTIALS FILE
            FileInputStream fileStream = new FileInputStream("clients.txt");
            DataInputStream inputStream = new DataInputStream(fileStream);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            //SPLIT CLIENT INPUT
            String[] splitInput = credentials.split(" ");
            //INVALID NUMBER OF ARGUMENTS
            if(splitInput.length != 2)
            {
                validationResults.add(false);
                return validationResults;
            }

            validationResults.add(true);
            String providedUsername = splitInput[0];
            String providedPassword = splitInput[1];

            String fileLine;
            //USERNAME EXISTS
            while ((fileLine = bufferedReader.readLine()) != null)
            {
                if(fileLine.split(" ")[1].equals(providedUsername))
                {
                    //VALIDATE PASSWORD
                    if(!(fileLine.split(" ")[2].equals(providedPassword)))
                        validationResults.add(true);
                    return validationResults;
                }
            }
            validationResults.add(false);
            inputStream.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return validationResults;
    }

    public int getScore(String username)
    {
        try
        {
            Path file = Paths.get("scoreHistory.txt");

            String[] splitLine = null;
            int noOfLines = (int) Files.lines(file).count();

            for (int i = noOfLines-1 ; i >= 0 ; i--)
            {
                String fileLine = Files.readAllLines(file).get(i);
                if(fileLine.contains(username))
                {
                    splitLine = fileLine.split(" ");
                    break;
                }
            }

            if(splitLine!=null)
                return  Integer.parseInt(splitLine[splitLine.length-1]);
            else
                return -1;
        }
        catch (IOException e)
        {
            e.getStackTrace();
        }
        return 0;
    }

    public String generatePhrase() throws IOException
    {
        //GENERATE RANDOM LINE NUMBER
        Random random = new Random();
        int randomLine = random.nextInt(148)+1;
        //RETURN RANDOM PHRASE AT RANDOM LINE FROM FILE
        return Files.readAllLines(Paths.get("phrases.txt")).get(randomLine);
    }

    public ArrayList<Integer> getGuessOccurrences(String phrase, Character guess)
    {
        ArrayList<Integer> occurrences = new ArrayList<>();
        int i = 0;
        while(i < phrase.length())
        {
            if(((Character.toUpperCase(phrase.charAt(i))) == guess) || ((Character.toLowerCase(phrase.charAt(i))) == guess))
                occurrences.add(i);
            i++;
        }
        return occurrences;
    }

    public String generateHiddenPhrase(String phrase, String hiddenPhrase, Character guess)
    {
        if(hiddenPhrase == null)
        {
            String updatedPhrase = "";
            for (int i = 0; i < phrase.length(); i++)
            {
                if(phrase.charAt(i)!=' ')
                    updatedPhrase = updatedPhrase.concat("_");
                else
                    updatedPhrase = updatedPhrase.concat(" ");
            }
            return updatedPhrase;
        }

        ArrayList<Integer> hiddenPhraseOccurrences = getGuessOccurrences(phrase, guess);
        StringBuilder replacedCharacters = new StringBuilder(hiddenPhrase);

        if(!hiddenPhraseOccurrences.isEmpty())
        {
            for (Integer index:hiddenPhraseOccurrences)
            {
                char character = phrase.charAt(index);
                replacedCharacters.setCharAt(index, character);
            }
            return replacedCharacters.toString();
        }
        else
            return null;
    }

    public String generateResponse(boolean expectedResponse)
    {
        //GENERATE RANDOM RESPONSE
        Random random = new Random();
        String[] positiveResponse = {"Bingo!", "Correct! On a Streak", "Nice Shot!", "Right On!", "Correct Guess!", "Great Play!", "Well Played!", "Spot On!", "Yes!"};
        String[] negativeResponse = {"Nope", "Try A little Harder", "Good Guess but Also a Wrong One", "Tough Luck", "Incorrect", "Not Exactly", "Maybe Next Time", "Oops", "Unlucky"};

        if(expectedResponse)
            return positiveResponse[random.nextInt(positiveResponse.length)];
        else
            return negativeResponse[random.nextInt(positiveResponse.length)];
        }

    public void addToHistory(String username, int points, boolean win, String mode) throws IOException
    {
        if(win)
        {
            int newScore = getScore(username) + points;
            String line = username + " " + Timestamp.from(Instant.now()) + " " + mode + " " + newScore;
            writeToFile("scoreHistory.txt",line);
        }
        else
        {
            int newScore = getScore(username) - points;
            String line = username + " " + Timestamp.from(Instant.now()) + " " + mode + " " + newScore;
            writeToFile("scoreHistory.txt",line);
        }
    }

    public void broadCast(String message, ArrayList<ClientHandler> clientHandlers)
    {
        for (ClientHandler clients:clientHandlers)
        {
            if(clients != null)
                clients.writer.println(message);
        }
    }

    public ArrayList<String> onlineUsers(String username)
    {
        ArrayList<String> onlineUsers = new ArrayList<>();

        for (ClientHandler clientHandler: connections)
        {
            if(clientHandler.username != null && !clientHandler.inGame && !clientHandler.username.equals(username))
                onlineUsers.add(clientHandler.username);
        }
        return onlineUsers;
    }

    public boolean validateTeamName(String teamName) throws IOException
    {
        //OPEN TEAMS FILE
        FileInputStream fileStream = new FileInputStream("teams.txt");
        DataInputStream inputStream = new DataInputStream(fileStream);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        String fileLine;
        //USERNAME EXISTS
        while ((fileLine = bufferedReader.readLine()) != null)
        {
            if(fileLine.split(" ")[0].equals(teamName))
                return false;
        }
        inputStream.close();
        return true;
    }

    public ArrayList<Boolean> validateTeams(String username, String team, String ops) throws IOException
    {
        ArrayList<Boolean> validationResults = new ArrayList<>();
        //OPEN TEAMS FILE
        FileInputStream fileStream = new FileInputStream("teams.txt");
        DataInputStream inputStream = new DataInputStream(fileStream);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        String[] opsMembers = null;
        String[] teamMembers = null;

        String fileLine;
        while ((fileLine = bufferedReader.readLine()) != null)
        {
            String[] splitLine = fileLine.split(" ");
            if(splitLine[0].equals(team))
                teamMembers = fileLine.split(" ");
            if(splitLine[0].equals(ops))
                opsMembers = fileLine.split(" ");
        }

        if(opsMembers==null || teamMembers==null)
            return validationResults;

        boolean inTeam = false;
        for (int i = 1; i < teamMembers.length-1 ; i++)
        {
            if(teamMembers[i].equals(username))
            {
                inTeam = true;
                break;
            }
        }
        if(!inTeam)
        {
            validationResults.add(false);
            return validationResults;
        }

        boolean inOps = false;
        validationResults.add(true);
        for (int i = 1; i < opsMembers.length-1 ; i++)
        {
            if(opsMembers[i].equals(username))
            {
                inOps = true;
                break;
            }
        }
        if(inOps)
        {
            validationResults.add(false);
            return validationResults;
        }

        validationResults.add(true);

        if(teamMembers.length != opsMembers.length)
        {
            validationResults.add(false);
            return validationResults;
        }

        validationResults.add(true);

        List<String> allMembers = new ArrayList<>(opsMembers.length + teamMembers.length);
        Collections.addAll(allMembers, opsMembers);
        Collections.addAll(allMembers, teamMembers);

        boolean inGame = false;
        for (ClientHandler clientHandler: connections)
        {
            for (String member: allMembers)
            {
                System.out.println(clientHandler.username);
                if(clientHandler.username.equals(member) && clientHandler.inGame)
                {
                    inGame = true;
                    break;
                }
            }
            if(inGame)
            {
                validationResults.add(false);
                break;
            }
        }

        if(!inGame)
            validationResults.add(true);

        inputStream.close();
        return validationResults;
    }

    public ClientHandler getHandler(Socket socket)
    {
        for (ClientHandler clientHandler: connections)
        {
            if(clientHandler.clientSocket==socket)
                return clientHandler;
        }
        return null;
    }
}

/*
REGISTER VALIDATION - 0  -> ARGUMENTS
                      10 -> RESERVED
                      1  -> OK

LOGIN VALIDATION    - 0  -> ARGUMENTS
                      10 -> INVALID USERNAME
                      11 -> INVALID PASSWORD
                      1  -> OK
*/