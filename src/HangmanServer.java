import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HangmanServer implements Runnable
{
    //SERVER LISTENER INFO
    protected boolean serverIsUp;
    protected ServerSocket serverListener;

    //THREAD POOL
    protected ExecutorService threadPool;

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
            System.out.println("> Successfully Started Server ...");

            //INITIALIZE FIXED THREAD POOL
            threadPool = Executors.newFixedThreadPool(4);

            //LOAD FILES UPON SERVER STARTUP
            loadFiles();

            //LISTEN FOR CONNECTIONS
            while(serverIsUp)
            {
                //PROVIDE SOCKETS FOR CLIENT CONNECTIONS
                Socket clientSocket = serverListener.accept();
                System.out.println("> Establishing Connection -> Client " + clientSocket);

                //CREATE CLIENT THREAD AND EXECUTE RUN FUNCTION
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                threadPool.execute(clientHandler);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void loadFiles() throws IOException
    {
        String[] fileNames = {"clients.txt", "scoreHistory.txt", "phrases.txt", "configurations.txt"};

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
            writeToFile("scoreHistory.txt",credentials.split(" ")[1] + " 0");
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
            //ADD 1 ?
//            FileWriter clientFileWriter = new FileWriter("clients.txt", true);
//            clientFileWriter.write(credentials);
//            clientFileWriter.write(System.getProperty( "line.separator" ));
//            clientFileWriter.close();
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

    public int getScore(String username) throws IOException
    {
        FileInputStream fileStream = new FileInputStream("scoreHistory.txt");
        DataInputStream inputStream = new DataInputStream(fileStream);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        int score = -1;
        String fileLine;
        //USERNAME RESERVED
        while ((fileLine = bufferedReader.readLine()) != null)
        {
            if (fileLine.contains(username))
                score = Integer.parseInt(fileLine.split(" ")[1]);
        }
        return score;
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