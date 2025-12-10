package edu.ptithcm.bus;
import module java.base;
public class MessageBus {
    private static final Map<Class<?>, List<Consumer<?>>> listeners = new ConcurrentHashMap<>();

    /**
     *
     * @param eventType class name for event
     * @param listener callback function
     * @return unsubscribe function
     * @param <T> class of event
     */
    public static <T> Runnable subscribe(Class<T> eventType , Consumer<T>listener){
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
        return () -> {
            var list = listeners.get(eventType);
            if (list != null){
                list.remove(listener);
            }
        };
    }

    @SuppressWarnings("unchecked")
    public static <T> void emit(T event){
        var eventType = event.getClass();
        var list = listeners.get(eventType);
        if(list != null){
            for(var callback: list){
                ((Consumer<T>)callback).accept(event);
            }
        }
    }
}
