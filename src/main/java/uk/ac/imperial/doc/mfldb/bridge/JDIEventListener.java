package uk.ac.imperial.doc.mfldb.bridge;

import com.sun.jdi.Field;
import com.sun.jdi.event.*;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.ModificationWatchpointRequest;

import java.util.List;

/**
 * Created by graham on 09/05/14.
 */
public interface JDIEventListener {
    void vmStartEvent(VMStartEvent event);
    void classPrepareEvent(ClassPrepareEvent event);
    void breakpointEvent(BreakpointEvent event);
    void vmDeathEvent(VMDeathEvent event);
    void vmDisconnectEvent(VMDisconnectEvent event);
}
