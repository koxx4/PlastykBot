package plastykBot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;
import java.util.Stack;

public class FunnyCommandHandler
{
    private final String SUCHARS_PAGE = "http://piszsuchary.pl/najnowsze/";
    private final Random RANDOM_GENERATOR = new Random();
    private final int MAX_SUCHARS_PAGES = 131;

    public void handleFunnyCommand(Stack<String> arguments, Message originalMessage)
    {
        String nextArg = arguments.pop();
        MessageChannel sourceChannel = originalMessage.getChannel();
        Member senderMemeber = originalMessage.getMember();
        User sender = originalMessage.getAuthor();

        if (nextArg.equals("s") || nextArg.equals("suchar"))
        {
            printRandomSuchar(sourceChannel);
        }
    }

    private void printRandomSuchar(MessageChannel sourceChannel)
    {
        try
        {
            URL randomSucharImageSrc = getRandomSucharAsImage();
            printCustomImageEmbed(sourceChannel, randomSucharImageSrc, "Suchar", null);
        } catch (IOException e)
        {
            e.printStackTrace();
            printCustomTitleEmbed(sourceChannel,"Blad przy szukaniu suchara!");
        }
    }
    private URL getRandomSucharAsImage() throws IOException
    {
        URL sucharsPage = new URL(SUCHARS_PAGE + RANDOM_GENERATOR.nextInt(MAX_SUCHARS_PAGES+1));
        Document page = Jsoup.parse(sucharsPage.openStream(), "UTF-8", String.valueOf(sucharsPage));

        Elements suchars = page.body().select("div#trescGlownaBox")
                .select("div#trescGlowna")
                .select("div.cytat")
                .select("div.kot_na_suchara")
                .select("a[href]")
                .select("img[src]");
        URL sucharImageURL = new URL(suchars.get(RANDOM_GENERATOR.nextInt(suchars.size())).absUrl("src"));
        return  sucharImageURL;
    }
    private void printCustomTitleEmbed(MessageChannel sourceChannel, String title)
    {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        MessageBuilder messageBuilder = new MessageBuilder();
        embedBuilder.setTitle(title);
        messageBuilder.setEmbed(embedBuilder.build());
        sourceChannel.sendMessage(messageBuilder.build()).queue();
    }
    private void printCustomImageEmbed(MessageChannel sourceChannel, URL imageURL, @Nullable String title, @Nullable String footer) throws IOException
    {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(title).setFooter(footer).setImage("attachment://image.jpg");

        sourceChannel.sendFile(imageURL.openStream(), "image.jpg").embed(embedBuilder.build()).queue();
    }
}
