package plastykBot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
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
    private double correctnessThreshold = 0.65;
    private Serializer serializer = new Persister();
    private Random randomGenerator = new Random();
    private FiszkaCard cardToGuess;

    private enum AnswerAim {
        NAME, AUTHOR, STYLE, PERIOD;
    }
    private enum PremissionLevel
    {
        ADMIN, MODERATOR, ORDINARY_USER
    }

    public void handleFiskzaCommand(Stack<String> arguments, Message originalMessage)
    {
        String nextArg = arguments.pop();
        MessageChannel sourceChannel = originalMessage.getChannel();
        Member senderMemeber = originalMessage.getMember();
        User sender = originalMessage.getAuthor();

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
        else if(nextArg.equals("t") || nextArg.equals("threshold"))
        {
            if(!arguments.isEmpty())
                setThreshold(sourceChannel, senderMemeber, arguments.pop());
        }
    }
    private void setThreshold(MessageChannel sourceChannel, Member senderMember, String newValue)
    {
        if(!hasRole(senderMember, "BotManager"))
        {
            printInsufficientPremission(sourceChannel);
            return;
        }
        double newThresholdValue;
        try
        {
            newThresholdValue = Double.parseDouble(newValue);
            if(newThresholdValue < 0.0 || newThresholdValue > 1.0)
                throw new NumberFormatException("Threshold must be in between 0.0 - 1.0");
        }
        catch (NumberFormatException e)
        {
            e.printStackTrace();
            printIncorrectThresholdValue(sourceChannel);
            return;
        }
        double oldValue = this.correctnessThreshold;
        this.correctnessThreshold = newThresholdValue;
        printCustomTitleEmbed(sourceChannel, "Ustawiono now prog z " + oldValue + " na " + newThresholdValue);
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
        if(getAnswerCorrectness(userAnswer, aim) >= correctnessThreshold)
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
        printCustomTitleEmbed(sourceChannel, "Fail! Nie zaladowano poprawnie fiszki :c");
    }
    private void printFailToLoadFiszkaImage(MessageChannel sourceChannel)
    {
        printCustomTitleEmbed(sourceChannel, "Fail! ZS gdynia sie zesrala i nie pozwolila mi pobrac poprawnie zdjecia!");
    }
    private void printCorrectAnswerMessage(MessageChannel sourceChannel)
    {
        printCustomTitleEmbed(sourceChannel, "Brawo! Jeszcze troche i zdasz.");
    }
    private void printFailedToRecognizeAnswerAim(MessageChannel sourceChannel)
    {
        printCustomTitleEmbed(sourceChannel,
                "Fail! Musisz podac dokladnie co zgadujesz ('autor' lub 'nazwa' lub 'styl' lub 'okres' )!");
    }
    private void printIncorrectThresholdValue(MessageChannel sourceChannel)
    {
        printCustomTitleEmbed(sourceChannel, "Niepoprawna wartosc progu! (0.00 do 1.00)");
    }
    private void printInsufficientPremission(MessageChannel sourceChannel)
    {
        printCustomTitleEmbed(sourceChannel, "Niewyswtarczajace uprawnienia do wykonania tej komendy!");
    }
    private void printCustomTitleEmbed(MessageChannel sourceChannel, String title)
    {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        MessageBuilder messageBuilder = new MessageBuilder();
        embedBuilder.setTitle(title);
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
    private boolean hasRole(Member user, String eligibleRole)
    {
        return  user.getRoles().stream().anyMatch( x -> x.getName().equals(eligibleRole));
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
