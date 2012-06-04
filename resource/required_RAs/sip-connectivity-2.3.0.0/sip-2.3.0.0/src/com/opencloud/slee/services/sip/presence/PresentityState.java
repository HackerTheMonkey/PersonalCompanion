package com.opencloud.slee.services.sip.presence;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PresentityState implements Serializable {

    public PresentityState(String basic, String note) {
        if (basic == null) throw new NullPointerException("basic");
        basic = correctBasicState(basic);
        if (!basic.equalsIgnoreCase(OPEN) && !basic.equalsIgnoreCase(CLOSED)) {
            throw new IllegalArgumentException("Basic status must be either 'open' or 'closed', but value was '" + basic + "'");
        }
        this.basic = basic;
        if (note != null) this.note = note;
    }

    public String toString() {
        return "basic:" + basic + "/" + "note:" + (note == null ? "null" : note);
    }
    
    public String getBasicState() {
        return this.basic;
    }
    
    public String getNoteState() {
        return this.note;
    }

    public int hashCode() {
        return basic.hashCode();
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else {
            if (o instanceof PresentityState) {
                if (((PresentityState)o).basic.equals(basic)) {
                    String otherNote = ((PresentityState)o).note;
                    if ((note == null && otherNote == null) || (note != null && note.equals(otherNote))) return true;
                }
            }
            return false;
        }
    }
    
    private String correctBasicState (String basic) {
        
        // correct any obvious spelling errors or case problems with the provided basic state
        if (basic.toLowerCase().matches("open")) return OPEN;
        if (basic.toLowerCase().matches("close\\D?")) return CLOSED;
        return basic;
    }

    public static final String OPEN = "open";
    public static final String CLOSED = "closed";

    private String basic = null;
    private String note = null;
}
