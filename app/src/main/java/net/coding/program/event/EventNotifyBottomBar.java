package net.coding.program.event;

/**
 * Created by chenchao on 16/9/26.
 */
public class EventNotifyBottomBar {
    private static EventNotifyBottomBar ourInstance = new EventNotifyBottomBar();

    public static EventNotifyBottomBar getInstance() {
        return ourInstance;
    }

    private EventNotifyBottomBar() {
    }
}
