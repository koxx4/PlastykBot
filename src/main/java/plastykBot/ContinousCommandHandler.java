package plastykBot;

import net.dv8tion.jda.api.entities.Message;

import java.util.Stack;

public interface ContinousCommandHandler
{
    void continueCommand(Stack<String> arguments, Message originalMessage);
}
