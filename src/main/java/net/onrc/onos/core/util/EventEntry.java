package net.onrc.onos.core.util;

/**
 * Class for encapsulating events with event-related data entry.
 */
public class EventEntry<T> {
    /**
     * The event types.
     */
    public enum Type {
        ENTRY_ADD,              // Add or update an entry
        ENTRY_REMOVE,           // Remove an entry
        ENTRY_NOOP              // NO-OP event (No Operation)
    }

    private Type eventType;     // The event type
    private T eventData;        // The relevant event data entry

    /**
     * Constructor for a given event type and event-related data entry.
     *
     * @param eventType the event type.
     * @param eventData the event data entry.
     */
    public EventEntry(EventEntry.Type eventType, T eventData) {
        this.eventType = eventType;
        this.eventData = eventData;
    }

    /**
     * Creates a NO-OP event.
     * <p/>
     * This is a factory method that can be called without an object:
     * <p/>
     * <code>
     *   EventEntry<TopologyEvent> eventEntry = EventEntry.makeNoop();
     * </code>
     *
     * @return a NO-OP event.
     */
    public static <T> EventEntry<T> makeNoop() {
        return new EventEntry<T>(Type.ENTRY_NOOP, null);
    }

    /**
     * Tests whether the event type is ENTRY_ADD.
     *
     * @return true if the event type is ENTRY_ADD, otherwise false.
     */
    public boolean isAdd() {
        return (this.eventType == Type.ENTRY_ADD);
    }

    /**
     * Tests whether the event type is ENTRY_REMOVE.
     *
     * @return true if the event type is ENTRY_REMOVE, otherwise false.
     */
    public boolean isRemove() {
        return (this.eventType == Type.ENTRY_REMOVE);
    }

    /**
     * Tests whether the event type is ENTRY_NOOP.
     *
     * @return true if the event type is ENTRY_NOOP, otherwise false.
     */
    public boolean isNoop() {
        return (this.eventType == Type.ENTRY_NOOP);
    }

    /**
     * Gets the event type.
     *
     * @return the event type.
     */
    public EventEntry.Type eventType() {
        return this.eventType;
    }

    /**
     * Gets the event-related data entry.
     *
     * @return the event-related data entry.
     */
    public T eventData() {
        return this.eventData;
    }
}
