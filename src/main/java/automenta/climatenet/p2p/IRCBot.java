//package automenta.climatenet.p2p;
//
//import automenta.climatenet.data.elastic.ElasticSpacetime;
//import org.apache.log4j.Level;
//import org.apache.log4j.Logger;
//import org.pircbotx.Channel;
//import org.pircbotx.Configuration;
//import org.pircbotx.PircBotX;
//import org.pircbotx.User;
//import org.pircbotx.hooks.ListenerAdapter;
//import org.pircbotx.hooks.events.*;
//
///**
// * https://code.google.com/p/pircbotx/wiki/Documentation
// */
//public class IRCBot extends ListenerAdapter {
//
//    static {
//        Logger.getLogger("org.pircbotx.InputParser").setLevel(Level.WARN);
//    }
//
//    private final PircBotX irc;
//    public final automenta.knowtention.Channel.FeedChannel<IRCMessage> serverChannel;
//    private final String server;
//
//    public IRCBot(ElasticSpacetime db, String nick, String server, String... channels) throws Exception {
//
//
//        this.server = server;
//
//        //serverChannel = new ElasticChannel(db, server, "feature");
//        serverChannel = new automenta.knowtention.Channel.FeedChannel(server, 128);
//
//        Configuration.Builder<PircBotX> config = new Configuration.Builder()
//                .setName(nick) //Nick of the bot. CHANGE IN YOUR CODE
//                .setLogin(nick) //Login part of hostmask, eg name:login@host
//                .setVersion("xchat 2.8.8 Linux 3.19.3-3-ARCH [x86_64/1.40GHz/SMP]")
//                .setAutoNickChange(true) //Automatically change nick when the current one is in use
//                .setServer(server, 6667);
//
//        for (String c : channels)
//            config.addAutoJoinChannel(c);
//
//
//        config.addListener(this);
//        this.irc = new PircBotX(config.buildConfiguration());
//
//        new Thread(new Runnable() {
//            @Override public void run() {
//                try {
//                    irc.startBot();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();
//
//
//    }
//
//    @Override
//    public void onPing(PingEvent event) throws Exception {
//
//    }
//
//    @Override
//    public void onMessage(MessageEvent m) throws Exception {
//        log(m.getChannel(), m.getUser(), Event.MESSAGE, m.getMessage());
//    }
//
//    @Override
//    public void onPrivateMessage(PrivateMessageEvent m) throws Exception {
//        log(m.getUser().getServer(), m.getUser().getNick(), Event.PRIVATE, m.getMessage());
//    }
//
//    @Override
//    public void onChannelInfo(ChannelInfoEvent event) throws Exception {
//
//    }
//
//    @Override
//    public void onJoin(JoinEvent event) throws Exception {
//        onJoin(event.getChannel(), event.getUser());
//    }
//
//    @Override
//    public void onPart(PartEvent event) throws Exception {
//        log(event.getChannel(), event.getUser(), Event.LEAVE, event.getReason());
//    }
//
//    @Override
//    public void onTopic(TopicEvent event) throws Exception {
//
//    }
//
//    @Override
//    public void onWhois(WhoisEvent event) throws Exception {
//        log(event.getServer(), event.getNick(), Event.IS,
//                event.getHostname() + " " + event.getRealname() + " " + event.getChannels().toString());
//    }
//
//    @Override
//    public void onUserList(UserListEvent u) throws Exception {
//        for (Object x : u.getUsers())
//            onJoin(u.getChannel(), (User)x);
//    }
//
//    protected void onJoin(Channel channel, User user) {
//        log(channel, user, Event.JOIN, null);
//    }
//    protected void log(Channel channel, User user, Event event, String value) {
//        log(channel.getName(), user.getNick(), event, value);
//    }
//
//    public enum Event {
//        IS, JOIN, LEAVE, MESSAGE, PRIVATE
//    }
//
////    final Map<String,IRCChannel> channels = new HashMap();
////
////    public static class IRCChannel extends NObject {
////        final int maxMessages = 8;
////
////        public IRCChannel(String server, String channel) {
////            super("irc://" + server + "/" + channel, channel);
////        }
////
////        public Collection<ObjectNode> getMessages() {
////            return messages;
////        }
////
////        public synchronized void addMessage(ObjectNode s) {
////            messages.add(s);
////            while (messages.size() > maxMessages) {
////                messages.remove(0);
////            }
////        }
////    }
//
//
//
//    public static class IRCMessage extends NObject {
//
//        private String author;
//        private String message;
//
//        public void setAuthor(String nick) {
//            this.author = nick;
//        }
//        public void setMessage(String m) {
//            this.message = m;
//        }
//
//        public String getMessage() {
//            return message;
//        }
//
//        public String getAuthor() {
//            return author;
//        }
//    }
//
//
//    protected void log(String channel, String nick, Event event, String value) {
//        //System.out.println(channel + " " + nick + " " + event + " " + value);
//
//
//        IRCMessage m = new IRCMessage();
//
//        switch (event) {
//            case IS:
//                m.tag("Identity", 1);
//                break;
//            case JOIN:
//                m.tag("Join", 1);
//                break;
//            case LEAVE:
//                m.tag("Leave", 1);
//                break;
//            case PRIVATE:
//                m.tag("Private", 1);
//            case MESSAGE:
//                m.tag("Message", 1);
//                break;
//        }
//
//        if (channel!=null)
//            m.tag(server + "/" + channel);
//
//        if (nick!=null)
//            m.setAuthor(nick);
//
//        if (value!=null)
//            m.setMessage(value);
//
//        m.when(System.currentTimeMillis());
//        m.setMessage(value);
//
//        serverChannel.append(m);
//
//    }
//}
