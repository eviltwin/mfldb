package uk.ac.imperial.doc.mfldb.bridge;

import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.*;

/**
 * Tracks threads within the target VirtualMachine.
 */
class ThreadManager {

    private final VirtualMachine vm;
    private ThreadReference currentThread;

    public ThreadManager(VirtualMachine vm) {
        this.vm = vm;
    }

    public ThreadReference getCurrentThread() {
        return currentThread;
    }

    public void updateCurrentThread(EventSet events) {
        ThreadReference thread;
        if (events.size() > 0) {
            /*
             * If any event in the set has a thread associated with it,
             * they all will, so just grab the first one.
             */
            Event event = events.iterator().next();
            thread = eventThread(event);
        } else {
            thread = null;
        }
        currentThread = thread;
    }

    private ThreadReference eventThread(Event event) {
        if (event instanceof ClassPrepareEvent) {
            return ((ClassPrepareEvent)event).thread();
        } else if (event instanceof LocatableEvent) {
            return ((LocatableEvent)event).thread();
        } else if (event instanceof ThreadStartEvent) {
            return ((ThreadStartEvent)event).thread();
        } else if (event instanceof ThreadDeathEvent) {
            return ((ThreadDeathEvent)event).thread();
        } else if (event instanceof VMStartEvent) {
            return ((VMStartEvent)event).thread();
        } else {
            return null;
        }
    }
}
