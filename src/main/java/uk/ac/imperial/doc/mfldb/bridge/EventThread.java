package uk.ac.imperial.doc.mfldb.bridge;

import com.sun.jdi.*;
import com.sun.jdi.request.*;
import com.sun.jdi.event.*;

import java.util.*;

/**
 * Classifies incoming JDI Events and dispatches them to {@link java.util.EventListener}s
 */
public class EventThread extends Thread {

    private final VirtualMachine vm;
    private final List<JDIEventListener> eventListeners = new ArrayList<>();

    private boolean connected = true;
    private boolean vmDied = true;

    EventThread(VirtualMachine vm) {
        super("JDI Event Dispatch");
        this.vm = vm;
    }

    /**
     * Run the event handling thread.
     * As long as we are connected, get event sets off
     * the queue and dispatch the events within them.
     */
    @Override
    public void run() {
        EventQueue queue = vm.eventQueue();
        while (connected) {
            try {
                EventSet eventSet = queue.remove();
                for (Event event : eventSet) {
                    handleEvent(event);
                }
            } catch (InterruptedException exc) {
                // Ignore
            } catch (VMDisconnectedException discExc) {
                handleDisconnectedException();
                break;
            }
        }
    }

    /**
     * Dispatch incoming events
     */
    private void handleEvent(Event event) {
        synchronized (eventListeners) {
            for (JDIEventListener listener : eventListeners) {
                if (event instanceof ClassPrepareEvent) {
                    listener.classPrepareEvent((ClassPrepareEvent) event);
                } else if (event instanceof VMStartEvent) {
                    listener.vmStartEvent((VMStartEvent) event);
                } else if (event instanceof VMDeathEvent) {
                    vmDied = true;
                    listener.vmDeathEvent((VMDeathEvent) event);
                } else if (event instanceof VMDisconnectEvent) {
                    connected = false;
                    listener.vmDisconnectEvent((VMDisconnectEvent) event);
                } else {
                    throw new Error("Unexpected event type");
                }
            }
        }
    }

    /**
     * A VMDisconnectedException has happened while dealing with
     * another event. We need to flush the event queue, dealing only
     * with exit events (VMDeath, VMDisconnect) so that we terminate
     * correctly.
     */
    synchronized void handleDisconnectedException() {
        EventQueue queue = vm.eventQueue();
        while (connected) {
            try {
                EventSet eventSet = queue.remove();
                for (Event event : eventSet) {
                    if (event instanceof VMDeathEvent || event instanceof VMDisconnectEvent) {
                        handleEvent(event);
                    }
                }
            } catch (InterruptedException exc) {
                // ignore
            }
        }
    }

    public void addEventListener(JDIEventListener jdiEventListener) {
        synchronized (eventListeners) {
            eventListeners.add(jdiEventListener);
        }
    }
}
