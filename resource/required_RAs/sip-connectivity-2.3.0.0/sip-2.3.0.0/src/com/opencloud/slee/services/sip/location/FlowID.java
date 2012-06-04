package com.opencloud.slee.services.sip.location;

import java.io.Serializable;

/**
 * Utility class for dealing with flow-ids passed up from the stack.
 */
public final class FlowID implements Serializable {

    public static FlowID fromString(String id) {
        return new FlowID(id);
    }

    private FlowID(String flowID) {
        if (flowID == null) throw new NullPointerException("flowID");
        this.id = flowID;
    }

    public boolean equals(Object obj) {
        return this == obj || (obj instanceof FlowID &&
                id.equals(((FlowID)obj).id));
    }

    public String toString() {
        return id;
    }

    public int hashCode() {
        return id.hashCode();
    }

    private final String id;
}
