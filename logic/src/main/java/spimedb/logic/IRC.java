package spimedb.logic;

import com.google.common.collect.ImmutableSortedSet;
import org.eclipse.collections.impl.factory.Iterables;
import org.pircbotx.Channel;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.output.OutputIRC;

import java.io.IOException;

/**
 * Generic IRC Bot interface via PircBotX
 */
public class IRC extends ListenerAdapter {
//
//    static {
//        //set the logging levels:
//        $.logLevel(InputParser.class, Level.WARN);
//        $.logLevel(ThreadedListenerManager.class, Level.WARN);
//        $.logLevel(OutputRaw.class, Level.INFO);
//    }

    public final PircBotX irc;

//    @Override
//    public void onGenericMessage(GenericMessageEvent event) {
//        //When someone says ?helloworld respond with "Hello World"
//        if (event.getMessage().startsWith("?helloworld"))
//            event.respond("Hello world!");
//    }

    public IRC(String nick, String server, String... channels) {
        this(new Configuration.Builder()
                .setName(nick)
                .setRealName(nick)
                .setLogin("root")
                .setVersion("unknown")
                .addServer(server)
                .addAutoJoinChannels(Iterables.mList(channels))
                .setAutoReconnect(true)
                .setAutoNickChange(true)
        );
    }

    public IRC(Configuration.Builder cb)  {

        cb.addListener(this);

        this.irc = new PircBotX(cb.buildConfiguration());

    }

    public final IRC start() throws IOException, IrcException {
        irc.startBot();
        return this;
    }

    public final void stop() {
        irc.stopBotReconnect();
    }


    public void broadcast(String message) {
        if (irc.isConnected()) {
            ImmutableSortedSet<Channel> chans = irc.getUserChannelDao().getAllChannels();
            for (Channel c : chans) {
                irc.send().message(c.getName(), message);
            }

        }
    }

    public Runnable send(String[] channels, String message) {
        if (irc.isConnected()) {
            OutputIRC out = irc.send();
            return ()-> {
                for (String c : channels)
                    out.message(c, message);
            };
        } else {
            return null;
        }
    }

    public static void main(String[] args) throws Exception {

        IRC bot = new IRC("experiment1", "irc.freenode.net", "#123xyz");

    }


}
