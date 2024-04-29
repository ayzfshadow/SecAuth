package ayzf.project;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class JSONRecord
{
    private static final String FILE_TAG = "user.json";
    public static boolean isAlreadyReceived(long userId, String token)
    {
        List<Long> receivedUsers = loadReceivedUsers(token);

        return receivedUsers.contains(userId);
    }

    public static void confirmReceived(long userId, String token)
    {
        List<Long> receivedUsers = loadReceivedUsers(token);
        receivedUsers.add(userId);
        saveReceivedUsers(receivedUsers, token);
    }

    private static List<Long> loadReceivedUsers(String token)
    {
        List<Long> receivedUsers = new ArrayList<>();

        if (new File(token + "/" + FILE_TAG).exists())
        {
            try
            {
                String jsonContent = new String(Files.readAllBytes(Paths.get(token + "/" + FILE_TAG)));
                JSONArray jsonArray = new JSONArray(jsonContent);
                for (int i = 0; i < jsonArray.length(); i++)
                    receivedUsers.add(jsonArray.getLong(i));
            }
            catch (IOException | JSONException ignored)
            {}
        }
        return receivedUsers;
    }

    private static void saveReceivedUsers(List<Long> receivedUsers, String token)
    {
        try
        {
            JSONArray jsonArray = new JSONArray(receivedUsers);
            BufferedWriter writer = new BufferedWriter(new FileWriter(token + "/" + FILE_TAG));
            writer.write(jsonArray.toString());
            writer.close();
        }
        catch (IOException ignored)
        {}
    }
}
