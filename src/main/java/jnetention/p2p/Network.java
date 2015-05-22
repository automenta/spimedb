/**
 * Copyright (c) 2011 jolira. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the GNU Public
 * License 2.0 which is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package jnetention.p2p;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple Gossip-inspires service.
 *
 * @author jfk
 * @date Mar 2, 2011 12:20:05 PM
 * @since 1.0
 *
 */
public class Network {
    static final Logger LOG = LoggerFactory.getLogger(Network.class);
    private int maxPacketSize = 8 * 65536;
    private static final long DEFAULT_HEARTBEAT = 1000; //Integer.MAX_VALUE;
    private static final String HEARTBEAT_TOPIC = ":H:";
    private final static String ACK_TOPIC = ":A:";
    
    public final Deque<DatagramPacket> incoming =  new ConcurrentLinkedDeque();
    
    /** The initial collection of seeds passed when the service was created */
    private final Collection<InetSocketAddress> seeds;
    /** Lists all the known peers we have ever known; some of them may be dead */
    private final Collection<InetSocketAddress> peers = Collections.synchronizedSet(new HashSet<InetSocketAddress>());
    /**
     * All the peer we do not know if they are dead or alive; we have sent at
     * least one message to them, but we have not heard back from them.
     */
    private final Collection<InetSocketAddress> unconfirmed = Collections.synchronizedSet(new HashSet<InetSocketAddress>());
    /**
     * Messages by their ID.
     */
    private final Map<String, Message> messageByID = Collections.synchronizedMap( new HashMap<String, Message>() );
    private final Random random = new Random();
    /** The socket used for reading and writing */
    private final DatagramSocket socket;
    private final String id = makeID();

    private final Map<Pattern, Listener> listenerByPattern = new HashMap<Pattern, Listener>();
    private final long heartbeat;
    private Thread sendThread;
    
    private static String makeID() {
        final UUID uuid = UUID.randomUUID();

        return uuid.toString();
    }

    private static Collection<InetSocketAddress> parsePeers(final String _peers) {
        try {
            return unmarshalPeers(_peers);
        } catch (final UnknownHostException e) {
            LOG.error("error parsing " + e);
        }

        return Collections.EMPTY_LIST;
    }

    private static Collection<InetSocketAddress> unmarshalPeers(final String peers) throws UnknownHostException {
        final Collection<InetSocketAddress> result = new LinkedList<InetSocketAddress>();
        final StringTokenizer izer = new StringTokenizer(peers, ";");

        while (izer.hasMoreTokens()) {
            final String token = izer.nextToken();
            final int pos = token.indexOf(':');

            if (pos == -1) {
                continue;
            }

            final String host = token.substring(0, pos);
            final String port = token.substring(pos + 1);
            final InetAddress remote = InetAddress.getByName(host);
            final int _port = Integer.parseInt(port);
            final InetSocketAddress socketAddr = new InetSocketAddress(remote, _port);

            result.add(socketAddr);
        }

        return result;
    }




    /**
     * Create the service.
     *
     * @param listener
     *            the socket to listen to
     * @param seeds
     *            the initial peers
     * @throws SocketException
     *             could not create the socket
     */
    public Network(final InetSocketAddress listener, final InetSocketAddress... seeds) throws SocketException {
        this(listener, seeds, DEFAULT_HEARTBEAT);

    }
    
    public Network(int port, final InetSocketAddress... seeds) throws SocketException, UnknownHostException {
        this(new InetSocketAddress(InetAddress.getByName("localhost"), port), seeds);
    }
    
    /**
     * Create the service.
     *
     * @param listener
     *            the socket to listen to
     * @param seeds
     *            the initial peers
     * @param heartbeat
     *            the duration of the heartbeat in ms
     * @throws SocketException
     *             could not create the socket
     */
    public Network(final InetSocketAddress listener, final InetSocketAddress[] seeds, final long heartbeat)
            throws SocketException {
        this.heartbeat = heartbeat;
        this.seeds = Arrays.asList(seeds);
        socket = new DatagramSocket(listener);

        socket.setReceiveBufferSize(maxPacketSize * 1024);
    
        maxPacketSize = socket.getReceiveBufferSize();
        
        //System.out.println("max packet size: " + maxPacketSize);

        final Thread receiver = createThread(new Runnable() {
            @Override
            public void run() {
                receive();
            }
        }, "gossip-receiver");
        final Thread sender = createThread(new Runnable() {
            @Override
            public void run() {
                send();
            }
        }, "gossip-sender");

        receiver.start();
        sender.start();
    }

    public boolean connect(String host, int port) throws UnknownHostException {
        return connect(new InetSocketAddress(InetAddress.getByName(host), port));
    }
    
    public boolean connect(final InetSocketAddress remote) {
        if (peers.add(remote)) {
            System.err.println("found new peer " + remote);
            return true;
        }

        return false;
    }

    /**
     * Add a listener.
     *
     * @param topic
     *            a regex that matches the topic to listen for
     * @param listener
     *            the listener
     */
    public void listen(final String topic, final Listener listener) {
        final Pattern p = Pattern.compile(topic);

        synchronized (listenerByPattern) {
            listenerByPattern.put(p, listener);
        }
    }

    final Thread createThread(final Runnable runnable, final String name) {
        final Thread receiver = new Thread(runnable);

        if (name!=null)
            receiver.setName(name);
        receiver.setDaemon(true);

        return receiver;
    }

    private void enqueue(final String topic, final String message) {
        final String _id = makeID();
        final Message msg = new Message(_id, topic, message);

        messageByID.put(_id, msg);        
    }

    @SuppressWarnings("unchecked")
    private Iterable<Entry<Pattern, Listener>> getListeners() {
        synchronized (listenerByPattern) {
            return listenerByPattern.entrySet();
            //final int size = entries.size();

            //return entries.toArray(new Entry[size]);
        }
    }

    public Collection<InetSocketAddress> getPeers() {
        return peers;
    }

    private Collection<Message> getPendingMessages() {
        return new ArrayList(messageByID.values());
    }

    private byte[] serialize(final Message message) {
        final ByteArrayOutputStream _out = new ByteArrayOutputStream();
        final PrintWriter out = new PrintWriter(_out);
        final String messageID = message.getId();
        final List<InetSocketAddress> sentTo = message.getSentTo();
        final Collection<InetSocketAddress> _peers = getPeers();
        final String msg = message.getMessage();
        final String topic = message.getTopic();

        out.println(id);
        marshal(out, _peers);
        out.println(messageID);
        marshal(out, sentTo);
        out.println(topic == null ? "" : topic);

        if (msg != null) {
            out.print(msg);
        }

        out.close();

        return _out.toByteArray();
    }

    private void marshal(final PrintWriter out, final InetSocketAddress[] sentTo) {
        for (final InetSocketAddress target : sentTo) {
            final InetAddress address = target.getAddress();
            final String hostname = address.getHostAddress();
            final int port = target.getPort();

            out.print(hostname);
            out.print(':');
            out.print(port);
            out.print(';');
        }

        out.println();
    }
    private void marshal(final PrintWriter out, final Iterable<? extends InetSocketAddress> sentTo) {
        for (final InetSocketAddress target : sentTo) {
            final InetAddress address = target.getAddress();
            final String hostname = address.getHostAddress();
            final int port = target.getPort();

            out.print(hostname);
            out.print(':');
            out.print(port);
            out.print(';');
        }

        out.println();
    }

    private boolean needsSending(final Message message) {
        final long sent = message.getSent();

        if (sent <= 0) {
            return true;
        }

        final long now = System.currentTimeMillis();

        return now > sent + heartbeat;
    }

    final void process(final DatagramPacket packet) throws IOException {
        final InetSocketAddress remote = (InetSocketAddress) packet.getSocketAddress();
        final byte[] data = packet.getData();
        final ByteArrayInputStream _in = new ByteArrayInputStream(data);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(_in));
        final String _id = reader.readLine();
        final String _peers = reader.readLine();
        final String messageID = reader.readLine();
        final String _sentTo = reader.readLine();
        final String topic = reader.readLine();
        final String msg = readMessage(reader);


        if (id.equals(_id)) {
            // We inadvertently sent the message to ourselves.
            resend(messageID);
            return;
        }
        
        System.err.println("message received from " + remote + ", id=" + _id + ", peers=" + _peers + ", messageID=" + messageID
                + ", sentTo=" + _sentTo + ", topic=" + topic + ", msg=" + msg);

        processPeers(remote, _peers);

        if (ACK_TOPIC.equals(topic)) {
            processAck(remote, messageID);
            return;
        }

        send(remote, new Message(messageID, ACK_TOPIC, null));

        if (HEARTBEAT_TOPIC.equals(topic)) {
            return;
        }

        for (final Entry<Pattern, Listener> entry : getListeners()) {
            final Pattern pattern = entry.getKey();
            final Matcher matcher = pattern.matcher(topic);

            if (matcher.matches()) {
                final Listener listener = entry.getValue();

                listener.handleMessage(topic, msg);
            }
        }
    }

    private void processAck(final InetSocketAddress remote, final String messageID) {
        
        final Message removed = messageByID.remove(messageID);

        if (removed == null) {
            LOG.warn("message " + messageID + " acknowledged by " + remote + " not found.");
        }
        
    }

    /**
     * We received a message from a peer. This is proof that this remote server
     * is alive. This method makes sure that we remove the remote peer from the
     * unconfirmed list and import all the other peers the remote listed in the
     * message.
     *
     * @param remote
     * @param _peers
     */
    private void processPeers(final InetSocketAddress remote, final String _peers) {
        if (!connect(remote)) {
            // If the remote was already known, we should also make
            // sure it is also removed from the unconfirmed list.
            unconfirmed.remove(remote);
        }

        // Parse the list the remte server sent
        // add all the new peers
        for (final InetSocketAddress peer : parsePeers(_peers)) {
            connect(peer);
        }
    }

    private String readMessage(final Reader reader) throws IOException {
        final StringBuilder buf = new StringBuilder();

        for (;;) {
            final char[] c = new char[2048];
            final int read = reader.read(c);

            if (read == -1) {
                break;
            }

            buf.append(c);
        }

        return buf.toString();
    }

    final void receive() {
        final Executor executor = Executors.newCachedThreadPool(new ThreadFactory() {
            private int number = 0;

            @Override
            public synchronized Thread newThread(final Runnable r) {
                final int no = number++;

                return createThread(r, null);
            }
        });

        final Thread current = Thread.currentThread();
        //final byte buf[] = new byte[maxPacketSize];

        while (true) {
            /*if (current.isInterrupted())
                break;            */

            incoming.add(receivePckg(new byte[maxPacketSize]));
            
            executor.execute(processNextPacket);
        }
    }
    
    public final Runnable processNextPacket = new Runnable() {
        @Override
        public void run() {
            if (incoming.isEmpty())
                return;

            try {                
                DatagramPacket next = incoming.removeFirst();
                if (next!=null)
                    process(next);
            } catch (final IOException e) {
                System.err.println("failed to process message" + e);
            }
        }
    };

    private DatagramPacket receivePckg(final byte buf[]) {
        final DatagramPacket packet = new DatagramPacket(buf, buf.length);

        try {
            socket.receive(packet);
        } catch (final IOException e) {
            LOG.error("receive failed", e);
        }

        return packet;
    }

    private void resend(final String messageID) {
        Message message;

        message = messageByID.get(messageID);        

        if (message == null) {
            LOG.error("cannot resend message " + messageID + " as was not found");
            return;
        }

        synchronized (message) {
            message.resend();
        }

        LOG.info("resending message " + messageID + " as it was sent by self");

        signalSendNow();
    }

    private InetSocketAddress selectPeer(final Message message) {
        final Set<InetSocketAddress> selectable = new HashSet<InetSocketAddress>();

        selectable.addAll(peers);
        
        selectable.removeAll(unconfirmed);

        final List<InetSocketAddress> _sentTo = message.getSentTo();
        

        selectable.removeAll(_sentTo);

        final int size = selectable.size();

        if (size == 0) {
            return selectSeed(_sentTo);
        }

        final InetSocketAddress[] sockets = selectable.toArray(new InetSocketAddress[size]);

        return selectRandom(sockets);
    }

    private InetSocketAddress selectRandom(final InetSocketAddress[] sockets) {
        int pos;

        pos = random.nextInt(sockets.length);

        return sockets[pos];
    }

    private InetSocketAddress selectSeed(final Collection<InetSocketAddress> _sentTo) {
        final Set<InetSocketAddress> selectable = new HashSet<InetSocketAddress>();

        selectable.addAll(seeds);
        selectable.removeAll(_sentTo);

        selectable.removeAll(unconfirmed);
        

        final int size = selectable.size();

        if (size == 0) {
            LOG.error("exhausted all seeds for " + this);
            return null;
        }

        final InetSocketAddress[] sockets = selectable.toArray(new InetSocketAddress[size]);

        return selectRandom(sockets);
    }

    /**
     * The sender thread. Runs until interrupted. Sends all messages in the
     * queue. If there are no messages to send, we sleep for the time of the
     * heartbeat, the service sends a heartbeat message.
     *
     */
    final void send() {
        int cyclesWithoutSend = 0;
        final Thread current = Thread.currentThread();
        sendThread = current;
        
        enqueue(HEARTBEAT_TOPIC, null);

        for (;;) {
            /*if (current.isInterrupted()) {
                break;
            }*/

            final int count = sendQueuedMessages();

            if (count > 0) {
                cyclesWithoutSend = 0;
                continue;
            }

            if (++cyclesWithoutSend >= 2) {
                enqueue(HEARTBEAT_TOPIC, null);
                continue;
            }

            try {
                /*
                semaphore = new Semaphore(0);
                
                try {
                semaphore.tryAcquire(heartbeat, TimeUnit.MILLISECONDS);
                } catch (final InterruptedException e) {
                break;
                } finally {
                semaphore = null;
                }*/
                Thread.sleep(heartbeat);
                System.out.println("send sleep interrupted");
            } catch (InterruptedException ex) {
                
            }
        }
    }

    private void send(final InetSocketAddress target, final Message message) {
        message.addSentTo(target);

        System.err.println("sending message " + message + " to " + target);

        final byte[] raw = serialize(message);

        try {
            final DatagramPacket packet = new DatagramPacket(raw, raw.length, target);

            socket.send(packet);
        } catch (final SocketException e) {
            LOG.error("socket exception while sending " + message, e);
        } catch (final IOException e) {
            LOG.error("I/O exception while sending " + message, e);
        } finally {
            message.sent();
        }
    }

    /**
     * Send a message to all peers.
     *
     * @param message
     *            the message to be sent to the client
     * @param topic
     *            the topic of the message
     */
    public void send(final String topic, final String message) {
        enqueue(topic, message);
        signalSendNow();
    }

    private boolean sendMessage(final Message message) {
        //synchronized (message) {
            if (!needsSending(message)) {
                return false;
            }

            final InetSocketAddress target = selectPeer(message);

            if (target == null) {
                return true;
            }

            send(target, message);
        //}

        return true;
    }

    final int sendQueuedMessages() {
        int count = 0;

        for (final Message message : getPendingMessages()) {
            if (sendMessage(message)) {
                count++;
            }
        }

        return count;
    }

    private void signalSendNow() {
        sendThread.interrupt();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();

        builder.append("Service [id=");
        builder.append(id);
        builder.append(", seeds=");
        builder.append(seeds);
        builder.append(", peers=");

        builder.append(peers);        

        builder.append(", unconfirmed=");

        builder.append(unconfirmed);

        builder.append(", pending=");

        
        builder.append(messageByID);        

        builder.append(", id=");
        builder.append(id);
        builder.append("]");

        return builder.toString();
    }
}
