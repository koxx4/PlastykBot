package plastykBot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Random;
import java.util.Stack;

public class FiszkaCommandHandler implements ContinousCommandHandler
{
    private final String thumbupUnicode = "U+1F44D";
    private final String thumbdownUnicode = "U+1F44E";
    private Serializer serializer = new Persister();
    private Random randomGenerator = new Random();
    private FiszkaCard cardToGuess;

    private enum AnswerAim {
        NAME, AUTHOR, STYLE, PERIOD;
    }

    public void handleFiskzaCommand(Stack<String> arguments, MessageChannel sourceChannel)
    {
        String nextArg = arguments.pop();
        if (nextArg.equals("n") || nextArg.equals("next"))
        {
            printRandomFiszka(sourceChannel);
        }
        else if (nextArg.equals("g") || nextArg.equals("guess"))
        {
            startFiszkaGuessing(sourceChannel);
        }
        else if(nextArg.equals("r") || nextArg.equals("reveal"))
        {
            if(!arguments.isEmpty())
                revealFiszkaCard(sourceChannel, arguments.pop());
        }
    }
    private void revealFiszkaCard(MessageChannel sourceChannel, String cardID)
    {
        FiszkaCard loadedCard = null;
        try
        {
            loadedCard = loadFiszkaCard(cardID);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            printFailToLoadFiszka(sourceChannel);
            return;
        }
        InputStream cardImageStream = null;
        try
        {
            cardImageStream = new URL(loadedCard.imageURL).openStream();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            printFailToLoadFiszkaImage(sourceChannel);
            printFiszkaData(sourceChannel, loadedCard);
            return;
        }
        printFiszkaDataWithImage(sourceChannel, loadedCard, cardImageStream);
    }
    private void startFiszkaGuessing(MessageChannel sourceChannel)
    {
        try
        {
            cardToGuess = loadRandomFiszkaCard();
            MainEventListener.registerContinousCommand(this);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            printFailToLoadFiszka(sourceChannel);
            MainEventListener.unregisterContinousCommand(this);
        }

        InputStream cardImageStream = null;
        try
        {
            cardImageStream = new URL(cardToGuess.imageURL).openStream();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            printFailToLoadFiszkaImage(sourceChannel);
            MainEventListener.unregisterContinousCommand(this);
        }
        EmbedBuilder embedBuilder = new EmbedBuilder();

        embedBuilder.setImage("attachment://card.jpg")
                    .setTitle("Sprobuj to zgadnac:");
        sourceChannel.sendFile(cardImageStream, "card.jpg").embed(embedBuilder.build()).queue();
    }
    private void continueGuessing(Stack<String> args, Message originalMessage)
    {
        AnswerAim aim = getAnswerAim(args.pop().toLowerCase());
        if (aim == null)
        {
            printFailedToRecognizeAnswerAim(originalMessage.getChannel());
            return;
        }

        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < args.size(); i++)
        {
            stringBuilder.append(args.pop());
            stringBuilder.append(" ");
        }

        String userAnswer = stringBuilder.toString().trim().toLowerCase();
        if(getAnswerCorrectness(userAnswer, aim) >= 0.6)
        {
            originalMessage.addReaction(thumbupUnicode).queue();
            printCorrectAnswerMessage(originalMessage.getChannel());
            printFiszkaData(originalMessage.getChannel(), cardToGuess);
        }
        else
        {
            originalMessage.addReaction(thumbdownUnicode).queue();
            printFiszkaData(originalMessage.getChannel(), cardToGuess);
        }
        cardToGuess = null;
        MainEventListener.unregisterContinousCommand(this);
    }
    private void printRandomFiszka(MessageChannel sourceChannel)
    {
        FiszkaCard loadedCard = null;
        try
        {
            loadedCard = loadRandomFiszkaCard();
        } catch (Exception e)
        {
            e.printStackTrace();
            printFailToLoadFiszka(sourceChannel);
        }
        try
        {
            InputStream cardImageStream = new URL(loadedCard.imageURL).openStream();
            printFiszkaDataWithImage(sourceChannel, loadedCard, cardImageStream);
        } catch (IOException e)
        {
            e.printStackTrace();
            printFailToLoadFiszkaImage(sourceChannel);
        }
    }
    private void printFiszkaDataWithImage(MessageChannel sourceChannel, FiszkaCard fiszkaCard, InputStream cardImageStream )
    {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        MessageBuilder messageBuilder = new MessageBuilder();

        embedBuilder.setTitle("Fiszka (" + fiszkaCard.id + "): ");
        embedBuilder.addField("Nazwa: ", fiszkaCard.name, true)
                .addField("Autor: ", fiszkaCard.author, true)
                .addField("Styl: ", fiszkaCard.style, true)
                .addField("Okres: ", fiszkaCard.period, true)
                .setImage("attachment://card.jpg");
        sourceChannel.sendFile(cardImageStream, "card.jpg")
                .embed(embedBuilder.build())
                .queue();
    }
    private void printFiszkaData(MessageChannel sourceChannel, FiszkaCard fiszkaCard)
    {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        MessageBuilder messageBuilder = new MessageBuilder();

        embedBuilder.setTitle("Fiszka (" + fiszkaCard.id + "): ");
        embedBuilder.addField("Nazwa: ", fiszkaCard.name, true)
                .addField("Autor: ", fiszkaCard.author, true)
                .addField("Styl: ", fiszkaCard.style, true)
                .addField("Okres: ", fiszkaCard.period, true);
        messageBuilder.setEmbed(embedBuilder.build());

        sourceChannel.sendMessage(messageBuilder.build())
                .queue();
    }
    private void printFailToLoadFiszka(MessageChannel sourceChannel)
    {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        MessageBuilder messageBuilder = new MessageBuilder();
        embedBuilder.setTitle("Fail! Nie zaladowano poprawnie fiszki :c");
        messageBuilder.setEmbed(embedBuilder.build());
        sourceChannel.sendMessage(messageBuilder.build()).queue();
    }
    private void printFailToLoadFiszkaImage(MessageChannel sourceChannel)
    {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        MessageBuilder messageBuilder = new MessageBuilder();
        embedBuilder.setTitle("Fail! ZS gdynia sie zesrala i nie pozwolila mi pobrac poprawnie zdjecia!");
        messageBuilder.setEmbed(embedBuilder.build());
        sourceChannel.sendMessage(messageBuilder.build()).queue();
    }
    private void printCorrectAnswerMessage(MessageChannel sourceChannel)
    {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        MessageBuilder messageBuilder = new MessageBuilder();
        embedBuilder.setTitle("Brawo! Jeszcze troche i zdasz.");
        messageBuilder.setEmbed(embedBuilder.build());
        sourceChannel.sendMessage(messageBuilder.build()).queue();
    }
    private void printFailedToRecognizeAnswerAim(MessageChannel sourceChannel)
    {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        MessageBuilder messageBuilder = new MessageBuilder();
        embedBuilder.setTitle("Fail! Musisz podac dokladnie co zgadujesz ('autor' lub 'nazwa' lub 'styl' lub 'okres' )!");
        messageBuilder.setEmbed(embedBuilder.build());
        sourceChannel.sendMessage(messageBuilder.build()).queue();
    }
    private FiszkaCard loadRandomFiszkaCard() throws Exception
    {
        String resourcePath = "Cards/fiszki/";
        File cards = new File(resourcePath);

        assert cards.exists();
        assert cards.isDirectory();

        var availableCards = cards.listFiles( (x,y) -> {
            return y.toLowerCase().endsWith(".xml");
        });

        FiszkaCard loadedCard =
                serializer.read(FiszkaCard.class, availableCards[randomGenerator.nextInt(availableCards.length-1)]);

        return loadedCard;
    }
    private FiszkaCard loadFiszkaCard(String cardID) throws Exception
    {
        String resourcePath = "Cards/fiszki/";
        String fileName = "card_" + cardID + ".xml";
        File cards = new File(resourcePath);

        assert cards.exists();
        assert cards.isDirectory();

        var availableCards = cards.listFiles( (x,y) -> {
            return y.equals(fileName);
        });

        FiszkaCard loadedCard =
                serializer.read(FiszkaCard.class, availableCards[0]);

        assert loadedCard.id.equals(cardID);

        return loadedCard;
    }
    private boolean isValidAnswer(String userAnswer, AnswerAim answerAim)
    {
        return switch (answerAim)
                {
                    case NAME -> userAnswer.equals(cardToGuess.name.trim().toLowerCase());
                    case AUTHOR -> userAnswer.equals(cardToGuess.author.trim().toLowerCase());
                    case STYLE -> userAnswer.equals(cardToGuess.style.trim().toLowerCase()) ;
                    case PERIOD -> userAnswer.equals(cardToGuess.period.trim().toLowerCase());
                };
    }
    private double getAnswerCorrectness(String userAnswer, AnswerAim answerAim)
    {
        JaroWinklerSimilarity algorithm = new JaroWinklerSimilarity();
        String correctAnswer = switch (answerAim)
                {
                    case NAME -> cardToGuess.name.trim().toLowerCase();
                    case AUTHOR -> cardToGuess.author.trim().toLowerCase();
                    case STYLE -> cardToGuess.style.trim().toLowerCase();
                    case PERIOD -> cardToGuess.period.trim().toLowerCase();
                };
        return algorithm.apply(userAnswer, correctAnswer);
    }
    private AnswerAim getAnswerAim(String aimArgument)
    {
       return switch (aimArgument){
            case "autor" -> AnswerAim.AUTHOR;
            case "nazwa" -> AnswerAim.NAME;
            case "styl" -> AnswerAim.STYLE;
            case "okres" -> AnswerAim.PERIOD;
            default -> null;
        };
    }
    @Override
    public void continueCommand(Stack<String> arguments, Message originalMessage)
    {
        continueGuessing(arguments, originalMessage);
    }
}
