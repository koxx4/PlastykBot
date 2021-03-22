package plastykBot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;
import java.util.Stack;

public class NSFWCommandHandler
{
    private final String PH_ALBUMS_PAGE = "https://pornhub.com/albums/female-straight-uncategorized?o=mv&t=a";
    private final Random RANDOM_GENERATOR = new Random();

    private class NSFWImageData
    {
        public URL imageSourceURL;
        public URL albumURL;
        public URL imagePageURL;
    }

    public void handleNSFWCommand(Stack<String> arguments, Message originalMessage)
    {
        String nextArg = arguments.pop();
        MessageChannel sourceChannel = originalMessage.getChannel();
        Member senderMemeber = originalMessage.getMember();
        User sender = originalMessage.getAuthor();

        if (nextArg.equals("rp") || nextArg.equals("random-photo"))
        {
            printRandomNSFWPhoto(sourceChannel);
        }
    }
    private void printRandomNSFWPhoto(MessageChannel sourceChannel)
    {
        NSFWImageData fetchedImage;
        InputStream imageStream = null;
        try
        {
            fetchedImage = assembleRandomNSFWImageData();
            imageStream = fetchedImage.imageSourceURL.openStream();
        } catch (Exception e)
        {
            e.printStackTrace();
            printErrorLoadingNSFWPhoto(sourceChannel);
            return;
        }
        EmbedBuilder embedBuilder = new EmbedBuilder();
        MessageBuilder messageBuilder = new MessageBuilder();

        embedBuilder.setTitle("To nie fiszka :3 ")
                .setImage("attachment://nsfw-image.jpg")
                .setFooter("Album: " + fetchedImage.albumURL + "\nZdjecie: " + fetchedImage.imagePageURL);
        sourceChannel.sendFile(imageStream, "nsfw-image.jpg")
                .embed(embedBuilder.build())
                .queue();
    }
    private NSFWImageData assembleRandomNSFWImageData() throws Exception
    {
        //Connects to page and fetches html doc
        Document page = Jsoup.parse(new URL(PH_ALBUMS_PAGE).openStream(), "UTF-8", PH_ALBUMS_PAGE);

        NSFWImageData fetchedImageData = new NSFWImageData();
        fetchedImageData.albumURL = getRandomAlbumUrl(page);

        page = Jsoup.parse(fetchedImageData.albumURL.openStream(), "UTF-8", String.valueOf(fetchedImageData.albumURL));
        fetchedImageData.imagePageURL = getRandomNSFWImagePageURL(page);

        page = Jsoup.parse(fetchedImageData.imagePageURL.openStream(), "UTF-8", String.valueOf(fetchedImageData.imagePageURL));
        fetchedImageData.imageSourceURL = getRandomNSFWImageSourceURL(page);

        return fetchedImageData;
    }
    private URL getRandomNSFWImagePageURL(Document imagesLoadedPage) throws Exception
    {
        Elements imagesLinks = imagesLoadedPage.body().select("div.wrapper")
                .select("div.container")
                .select("div.photoBlockBox")
                .select("ul.photosAlbumsListing.albumViews.preloadImage")
                .select("li.photoAlbumListContainer")
                .select("div.js_lazy_bkg.photoAlbumListBlock")
                .select("a[href]");
        String randomImagePageLink = imagesLinks.get(RANDOM_GENERATOR.nextInt(imagesLinks.size()-1))
                .absUrl("href");

        return new URL(randomImagePageLink);
    }
    private URL getRandomNSFWImageSourceURL(Document imageLoadedPage) throws Exception
    {
        Elements images = imageLoadedPage.body()
                .select("div.wrapper")
                .select("div.container")
                .select("div#photoWrapper")
                .select("div.photoColumnLeft.float-left")
                .select("div#photoImageSection")
                .select("div.centerImage")
                .select("a[href]")
                .select("img");
        String randomImageSource = images.get(0)
                .absUrl("src");

        return new URL(randomImageSource);
    }
    private URL getRandomAlbumUrl(Document albumsLoadedPage) throws MalformedURLException
    {
       Elements imagesLinks = albumsLoadedPage.body().select("div.wrapper")
                .select("div.container")
                .select("div.gridWrapper")
                .select("div.nf-videos")
                .select("div.sectionWrapper")
                .select("ul#photosAlbumsSection")
                .select("li.photoAlbumListContainer")
                .select("div.photoAlbumListBLock")
                .select("a[href]");
       String albumLink = imagesLinks.get(RANDOM_GENERATOR.nextInt(imagesLinks.size()-1)).absUrl("href");

       return new URL(albumLink);
    }
    private void printErrorLoadingNSFWPhoto(MessageChannel sourceChannel)
    {
        printCustomTitleEmbed(sourceChannel, "Blad przy wybieraniu losoweg zdjecia!");
    }
    private void printCustomTitleEmbed(MessageChannel sourceChannel, String title)
    {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        MessageBuilder messageBuilder = new MessageBuilder();
        embedBuilder.setTitle(title);
        messageBuilder.setEmbed(embedBuilder.build());
        sourceChannel.sendMessage(messageBuilder.build()).queue();
    }
    //private void printImageEmbed(MessageChannel s)
}
