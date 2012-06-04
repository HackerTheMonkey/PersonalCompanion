package com.opencloud.slee.services.sip.location.profile;

import com.opencloud.slee.services.sip.location.ContactImpl;

public interface RegistrationProfileCMP {

    /** SIP Address-Of-Record (public address) for this registration */
    public String getAddressOfRecord();
    public void setAddressOfRecord(String s);

    /** bindings - currently registered contact addresses */
    public ContactImpl[] getBindings();
    public void setBindings(ContactImpl[] contacts);

}
