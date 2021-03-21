package plastykBot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
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
    private static ContinousCommandHandler continousCommandHandler;

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
                handleMainCommand(arguments, originalMessage, messageChannel);
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
    private void handleMainCommand(Stack<String> arguments, Message originalMessage, MessageChannel sourceChannel)
    {
        if(arguments.pop().equals("!p"))
        {
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
                    fiszkaCommandHandler.handleFiskzaCommand(arguments, sourceChannel);
                }
            }
        }
    }
    private void printUserHelp(MessageChannel sourceChannel)
    {
        MessageBuilder messageBuilder = new MessageBuilder();
        EmbedBuilder embedBuilder = new EmbedBuilder();

        MessageEmbed.Field overallField = new MessageEmbed.Field("Ogolne:",
                "h lub help --> Wyswietl pomoc", false);
        MessageEmbed.Field fiszkaField = new MessageEmbed.Field("Fiszki (z przedrostkiem 'f' lub 'fiszka'):",
                "n lub next --> Zgaduj nastepna fiszke", false);

        embedBuilder.setColor(Color.DARK_GRAY);
        embedBuilder.setTitle("Pomoc jest juz tutaj! :\n");
        embedBuilder.addField(overallField);
        embedBuilder.addField(fiszkaField);

        messageBuilder.setEmbed(embedBuilder.build());
        sourceChannel.sendMessage(messageBuilder.build()).queue();
    }
}
