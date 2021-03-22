package plastykBot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

public class MainEventListener implements EventListener
{
    private final FiszkaCommandHandler fiszkaCommandHandler = new FiszkaCommandHandler();
    private final NSFWCommandHandler nsfwCommandHandler = new NSFWCommandHandler();
    private static ContinousCommandHandler continousCommandHandler;
    private String commandPrefix = "!p";

    @Override
    public void onEvent(@NotNull GenericEvent event)
    {
        if(event instanceof MessageReceivedEvent)
        {
            MessageReceivedEvent messageEvent = (MessageReceivedEvent)event;
            MessageChannel messageChannel = messageEvent.getChannel();
            Message originalMessage = messageEvent.getMessage();
            String receivedMessage = originalMessage.getContentRaw().trim();
            Stack<String> arguments = constructArgumentsStack(receivedMessage);

            if(arguments.size() >= 2)
            {
                handleMainCommand(arguments, originalMessage);
            }
        }
    }
    public static void registerContinousCommand(ContinousCommandHandler subscriber)
    {
        continousCommandHandler = subscriber;
    }
    public static void unregisterContinousCommand(ContinousCommandHandler subscriber)
    {
        if(continousCommandHandler == subscriber)
            continousCommandHandler = null;
    }
    private Stack<String> constructArgumentsStack(String receivedMessage)
    {
        List<String> receivedMessageSplit = Arrays.asList(receivedMessage.split(" "));

        Stack<String> arguments = new Stack<String>();
        for(int i = receivedMessageSplit.size() - 1 ; i >= 0; i-- )
        {
            arguments.push(receivedMessageSplit.get(i));
        }

        return arguments;
    }
    private void handleMainCommand(Stack<String> arguments, Message originalMessage)
    {
        if(arguments.pop().equals(commandPrefix))
        {
            User sender = originalMessage.getAuthor();
            MessageChannel sourceChannel = originalMessage.getChannel();

            if(continousCommandHandler != null)
            {
                continousCommandHandler.continueCommand(arguments, originalMessage);
            }
            else
            {
                String nextArg = arguments.pop();
                if (nextArg.equals("h") || nextArg.equals("help"))
                {
                    printUserHelp(sourceChannel);
                }
                else if (nextArg.equals("f") || nextArg.equals("fiszka"))
                {
                    fiszkaCommandHandler.handleFiskzaCommand(arguments, originalMessage);
                }
                else if (nextArg.equals("nsfw") || nextArg.equals("szreks"))
                {
                    nsfwCommandHandler.handleNSFWCommand(arguments, originalMessage);
                }
                else if (nextArg.equals("p") || nextArg.equals("prefix"))
                {
                    if(!arguments.isEmpty())
                        changeCommandPrefix(sourceChannel, arguments.pop());
                }
            }
        }
    }
    private void printUserHelp(MessageChannel sourceChannel)
    {
        MessageBuilder messageBuilder = new MessageBuilder();
        EmbedBuilder embedBuilder = new EmbedBuilder();

        MessageEmbed.Field overallField = new MessageEmbed.Field("Ogolne:",
                "h lub help --> Wyswietl pomoc" +
                        "p lub prefix --> Zmien prefix do wywolywania komend bota", false);
        MessageEmbed.Field fiszkaField = new MessageEmbed.Field("Fiszki (z przedrostkiem 'f' lub 'fiszka'):",
                "n lub next --> Pokaz nastepna losowa fiszke\ng lub guess -> Zgaduj nastepna fiszke\n" +
                        "r {id} lub reveal {id} --> pokaz fiszke z takim {id}\n" +
                        "t {wartosc} lub threshold {wartosc} --> ustaw nowy prog akceptacji odpowiedzi (wartosci od 0.0 dp 1.0)",
                        false);

        embedBuilder.setColor(Color.DARK_GRAY);
        embedBuilder.setTitle("Pomoc jest juz tutaj! :\n");
        embedBuilder.addField(overallField);
        embedBuilder.addField(fiszkaField);

        messageBuilder.setEmbed(embedBuilder.build());
        sourceChannel.sendMessage(messageBuilder.build()).queue();
    }
    private void changeCommandPrefix(MessageChannel sourceChannel,String newPrefix)
    {
        commandPrefix = newPrefix;
        printCustomTitleEmbed(sourceChannel, "Ustawiono nowy prefix na '" + newPrefix + "'");
    }
    private void printCustomTitleEmbed(MessageChannel sourceChannel, String title)
    {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        MessageBuilder messageBuilder = new MessageBuilder();
        embedBuilder.setTitle(title);
        messageBuilder.setEmbed(embedBuilder.build());
        sourceChannel.sendMessage(messageBuilder.build()).queue();
    }
}
