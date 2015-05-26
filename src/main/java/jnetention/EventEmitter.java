//
//package jnetention;
//
//import javafx.application.Platform;
//
//import java.lang.reflect.Constructor;
//import java.util.concurrent.*;
//
///**
// * Adapted from http://www.recursiverobot.com/post/86215392884/witness-a-simple-android-and-java-event-emitter
// */
//@Deprecated class EventEmitter {
//
//    public interface Observer<O> {
//        public void event(O event);
//    }
//
//    private final ConcurrentMap<Class<?>, ConcurrentMap<Observer, String>> events
//            = new ConcurrentHashMap<Class<?>, ConcurrentMap<Observer, String>>();
//
//    private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
//    private final ExecutorService executorService = new ThreadPoolExecutor(1, 10, 30, TimeUnit.SECONDS, queue);
//
//
//    public boolean hasOn(Class event) {
//        if (events.get(event)!=null)
//            if (events.get(event).size() > 0)
//                return true;
//        return false;
//    }
//
//    public <C> void on(Class<? extends C> event, Observer<? extends C> o) {
//        if (null == event || null == o)
//            return;
//
//        events.putIfAbsent(event, new ConcurrentHashMap<Observer, String>());
//        events.get(event).putIfAbsent(o, "");
//    }
//
//    public void off(Class<?> event, Observer o) {
//        if (null == event || null == o)
//            return;
//
//        if (!events.containsKey(event))
//            return;
//
//        events.get(event).remove(o);
//    }
//
//    public void emit(final Object event) {
//        final Class eclass = event.getClass();
//
//        if (!events.containsKey(eclass))
//            return;
//
//        for (final Observer m : events.get(eclass).keySet()) {
//            executorService.execute(new Runnable() {
//                @Override public void run() {
//                    m.event(event);
//                }
//            });
//        }
//    }
//
//    public void emit(final Class eventClass, final Object... params) {
//        if (hasOn(eventClass)) {
//            Constructor[] c = eventClass.getConstructors();
//            assert(c.length == 1);
//
//            try {
//                emit(c[0].newInstance(params));
//            }
//            catch (Exception e) {
//                System.err.println(e);
//                e.printStackTrace();
//            }
//        }
//    }
//
//    public void emitLater(final Class eventClass, final Object... params) {
//        if (hasOn(eventClass)) {
//            Platform.runLater(new Runnable() {
//
//                @Override
//                public void run() {
//                    emit(eventClass, params);
//                }
//
//            });
//        }
//    }
//}