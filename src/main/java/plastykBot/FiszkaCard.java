package plastykBot;


import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(name = "fiszkaCard")
public class FiszkaCard
{
    @Element
    String id;
    @Element
    String imageURL;
    @Element
    String name;
    @Element
    String author;
    @Element
    String style;
    @Element
    String period;

    public FiszkaCard(){}
    public FiszkaCard(FiszkaCard sourceCard)
    {
        this.id = sourceCard.id;
        this.imageURL = sourceCard.imageURL;
        this.name = sourceCard.name;
        this.author = sourceCard.author;
        this.style = sourceCard.style;
        this.period = sourceCard.period;
    }
}
