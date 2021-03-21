package plastykBot;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.utils.Compression;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import javax.security.auth.login.LoginException;
import java.nio.charset.StandardCharsets;

public class Program
{
    public static void main(String[] args) throws LoginException, InterruptedException
    {


        String botToken = "ODIyOTU2NzU2ODY2ODI2MjUx.YFZ0NA.YJ1tVWAEFzfWPWxC1qtiCmGROMU"; //System.getenv("DiscordBotPlastyk");
        JDABuilder builder = JDABuilder.createDefault(botToken).addEventListeners(new MainEventListener());

        // Disable parts of the cache
        builder.disableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE);
        // Enable the bulk delete event
        builder.setBulkDeleteSplittingEnabled(false);
        // Disable compression (not recommended)
        builder.setCompression(Compression.NONE);
        // Set activity (like "playing Something")
        builder.setActivity(Activity.listening("Mozart"));

        builder.build().awaitReady();
    }
}
