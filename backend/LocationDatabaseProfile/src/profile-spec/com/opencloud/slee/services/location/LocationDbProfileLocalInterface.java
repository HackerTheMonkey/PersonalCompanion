/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.opencloud.slee.services.location;

import java.util.ArrayList;
import javax.sip.header.ContactHeader;
import javax.slee.profile.ProfileLocalObject;

/**
 *
 * @author Hasanein.Khafaji
 */
public interface LocationDbProfileLocalInterface extends ProfileLocalObject, LocationDbProfileSpecCMP
{
    public void addBinding(ContactHeader[] contactHeaders);

    public void removeBinding(String[] contactHeaders);

    public String[] queryBinding();

    /**
     * Here we define the CMR manipulation methods. It is also worth to mention
     * that the implementation details of these methods can be found in the
     * LocationDbAbstractClass
     */
    public void addCMR(CallManagementRule callManagementRule);

    public void removeCMR(CallManagementRule callManagementRule);
    
    public ArrayList queryAllCmrNames();

    public CallManagementRule queryCMR(String cmrName);    
}