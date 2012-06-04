/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.opencloud.slee.services.location;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import javax.sip.header.ContactHeader;
import javax.slee.CreateException;
import javax.slee.facilities.Tracer;
import javax.slee.profile.Profile;
import javax.slee.profile.ProfileContext;
import javax.slee.profile.ProfileVerificationException;

/**
 *
 * @author Hasanein.Khafaji
 */
public abstract class LocationDbAbstractClass implements LocationDbProfileSpecCMP, Profile
{

    private Tracer tracer = null;
    public LocationDbAbstractClass()
    {
        if (tracer != null)
        {
            tracer.info("Empty parenthesis constructor has been called...");
        }
    }

    public void profileActivate()
    {
        if (tracer != null)
        {
            tracer.info("profileActivate() has been called...");
        }
    }

    public void profileInitialize()
    {
        if (tracer != null)
        {
            tracer.info("profileInitialize() has been called...");
        }
    }

    public void profileLoad()
    {
        if (tracer != null)
        {
            tracer.info("profileLoad() has been called...");
        }
    }

    public void profilePassivate()
    {
        if (tracer != null)
        {
            tracer.info("profilePassivate() has been called...");
        }
    }

    public void profilePostCreate() throws CreateException
    {
        /**
         * During this phase, the LocationDatabase need to be initialised.
         */
        if (tracer != null)
        {
            tracer.info("profilePostCreate() has been called...");
            tracer.info("DEBUG: Initialising the Location Database elements...");
            setCallId(null);
            setCallSeq(0L);
            setContacts(new HashMap<String, String>());
            setCallManagementRules(new HashMap<String, CallManagementRule>());
            setThirdPartyRegFlag(false);
            tracer.info("DEBUG: The elements of the Location Database have been initialised...");
        }
    }

    public void profileRemove()
    {
        if (tracer != null)
        {
            tracer.info("profileRemove() has been called...");
        }
    }

    public void profileStore()
    {
        if (tracer != null)
        {
            tracer.info("profileStore() has been called...");
        }
    }

    public void profileVerify() throws ProfileVerificationException
    {
        if (tracer != null)
        {
            tracer.info("profileVerify() has been called...");
        }
    }

    public void setProfileContext(ProfileContext profileContext)
    {
        tracer = profileContext.getTracer("LocationDbAbstractClass_Tracer");
        tracer.info("Profile Context has been set...");
    }

    public void unsetProfileContext()
    {
        if (tracer != null)
        {
            tracer.info("Unsetting the profileContext...");
        }
    }

    public void addBinding(ContactHeader[] contactHeaders)
    {
        /**
         * Check whether the LocationDatabase contacts has been initialised
         * or not.
         */
        HashMap<String, String> mappedContacts = getContacts();

        if (mappedContacts == null)
        {
            /**
             * Initialise the contacts
             */
            tracer.info("DEBUG: Initialising the contacts CMP storage");
            setContacts(new HashMap<String, String>());
        }
        /**
         * For each contact passed-in in the array, check if that contact
         * exist so as to skip the addition, otherwise just add that contact.
         */
        for (ContactHeader contactHeader : contactHeaders)
        {
            /**
             * Get the String representation of the address contained within
             * the contact header field.
             */
            tracer.info("DEBUG: The contact header text value is: " + contactHeader);
            String contactHeaderAddressString = ((contactHeader.getAddress().getURI().toString()).contains(";")) ? ((contactHeader.getAddress().getURI().toString()).split(";")[0]) : (contactHeader.getAddress().getURI().toString());
            String contactHeaderTextValue = contactHeader.toString().trim();
            tracer.info("DEBUG: contactHeaderAddressString: " + contactHeaderAddressString);
            tracer.info("DEBUG: contactHeaderTextValue: " + contactHeaderTextValue);
            String storedContactHeader = mappedContacts.get(contactHeaderAddressString);
            if (storedContactHeader == null)
            {
                tracer.info("DEBUG: Storing " + contactHeaderAddressString + " --> " + contactHeaderTextValue);
                mappedContacts.put(contactHeaderAddressString, contactHeaderTextValue);
            }
            else
            {
                tracer.info("DEBUG: The ContactHeader already exists...");
            }
        }

        /**
         * Just to find out if the CMP field passed by value or by reference
         */
        tracer.info("DEBUG: Mapped contacts are " + mappedContacts.size());
        tracer.info("DEBUG: CMP contacts are " + getContacts().size());
        /**
         * Store back the new version of the modified contacts
         * back into the CMP storage.
         */
        setContacts(mappedContacts);
        tracer.info("DEBUG: CMP contacts are now " + getContacts().size());
    }

    public String[] queryBinding()
    {
        tracer.info("DEBUG: queryBinding() method has been called...");
        /**
         * Get all the contacts currently bound to the given profile
         */
        HashMap<String, String> mappedContacts = getContacts();
        tracer.info("DEBUG: the mappedContacts obtained with a size of: " + mappedContacts.size());
        /**
         * Construct a String array to be returned back to the requestor
         * entity.
         */
        String[] contacts = new String[mappedContacts.size()];
        /**
         * Now we need to iterate over the obtained contacts and obtain the
         * value stored in them to populate the String array to be returned back
         */
        int arrayIndex = 0;
        Iterator iterator = mappedContacts.entrySet().iterator();
        while (iterator.hasNext())
        {
            Entry contactEntry = (Entry) iterator.next();
            tracer.info("DEBUG: Contact Queried: " + contactEntry.getValue().toString().trim());
            contacts[arrayIndex++] = contactEntry.getValue().toString().trim();
        }
        tracer.info("DEBUG: returning contacts: " + contacts.length);
        return contacts;
    }

    public void removeBinding(String[] contactHeadersAddressesTextValue)
    {
        /**
         * Pull the contact headers HashMap from the CMP storage
         */
        HashMap<String, String> mappedContacts = getContacts();
        /**
         * For each contact passed-in in the array, check if that contact
         * exist so as to skip the removal, otherwise just remove that contact.
         */
        for (String contactHeaderAddressTextValue : contactHeadersAddressesTextValue)
        {
            /**
             * Get the String representation of the address contained within
             * the contact header field.
             */
            String storedContactHeaderTextValue = mappedContacts.get(contactHeaderAddressTextValue);
            if (storedContactHeaderTextValue != null)
            {
                String key = mappedContacts.remove(contactHeaderAddressTextValue);
                tracer.info("DEBUG: Removing " + key + " --> " + storedContactHeaderTextValue + " from the mapped contacts persistent storage");

            }
            else
            {
                if (tracer != null)
                {
                    tracer.info("DEBUG: The ContactHeader " + contactHeaderAddressTextValue + " --> " + storedContactHeaderTextValue + " does not exist...");
                }
            }
        }
        /**
         * Here we need to store the modified mappedContact back again into
         * the CMP contacts to update the requested removal of the given
         * contact headers.
         */
        tracer.info("DEBUG: Committing the changes made to the mappedContacts into the CMP contacts...");
        setContacts(mappedContacts);
    }

    public void removeCMR(CallManagementRule callManagementRule)
    {
        /**
         * Pull the call management rules HashMap from the CMP storage
         */
        HashMap<String, CallManagementRule> retrievedCMRs = getCallManagementRules();
        /**
         * Check whether or not, the passed in
         */
        CallManagementRule retrievedCallManagementRule = retrievedCMRs.get(callManagementRule.getRuleName());
        if (retrievedCallManagementRule != null)
        {
            retrievedCMRs.remove(retrievedCallManagementRule.getRuleName());
        }
        else
        {
            if (tracer != null)
            {
                tracer.info("DEBUG: The Call Management Rule " + callManagementRule.getRuleName() + " does not exist...");
            }
        }
        /**
         * Here we need to store the modified call management rules back again into
         * the CMP-stored rules to update the requested removal of the given
         * CMR.
         */
        tracer.info("DEBUG: Committing the changes made to the retrievedContacts into the CMP contacts...");
        setCallManagementRules(retrievedCMRs);
    }

    /**
     * This method return back a String array containing all of the Call
     * Management Rules that are stored inside the given profile.
     */
    public ArrayList queryAllCmrNames()
    {
        ArrayList callManagementRulesArray = new ArrayList();
        /**
         * Get the HashMap that contain the list of the Call Management
         * Rules contained within.
         */
        HashMap<String, CallManagementRule> retrievedCMRs = getCallManagementRules();        
        /**
         * Here we need to iterate over the retrieved CMRs and fill up the
         * array with the retrieved CMR's names.
         */
        Iterator iterator = retrievedCMRs.entrySet().iterator();
        while(iterator.hasNext())
        {
            Map.Entry entryPair = (Entry) iterator.next();
            callManagementRulesArray.add(entryPair.getKey());
        }
        return callManagementRulesArray;
    }

    public CallManagementRule queryCMR(String cmrName)
    {
        return getCallManagementRules().get(cmrName);
    }

    public void addCMR(CallManagementRule callManagementRule)
    {
        /**
         * Check whether the LocationDatabase CMRs has been initialised
         * or not. The locationDatabase initialisation take place inside the
         * profileSpecPostActivate phase.
         */
        HashMap<String, CallManagementRule> retrievedCmrHashMap = getCallManagementRules();

        if (retrievedCmrHashMap == null)
        {
            /**
             * Initialise the contacts
             */
            tracer.info("DEBUG: Initialising the Call Management Rules CMP storage");
            setCallManagementRules(new HashMap<String, CallManagementRule>());
        }
        /**
         * Here we need to check for the passed CallManagementRule, if there
         * is a rule exists with the same name as this one, then we should
         * delete it and then re-add the new one. Otherwise, we simply just add
         * the new CMR into the profile database.
         */
         CallManagementRule storedCallManagementRule = retrievedCmrHashMap.get(callManagementRule.getRuleName());
         if(storedCallManagementRule != null)
         {            
            /**
             * This means that a rule with the same name already exist which
             * implies that this is an update operation, so we need to remove
             * the old rule before adding the new one.
             */
             retrievedCmrHashMap.remove(callManagementRule.getRuleName());
         }
         retrievedCmrHashMap.put(callManagementRule.getRuleName(), callManagementRule);

        /**
         * Just to find out if the CMP field passed by value or by reference
         */
        tracer.info("DEBUG: retrieved CMRs are now: " + retrievedCmrHashMap.size());
        tracer.info("DEBUG: currently stored CMRs are: " + getCallManagementRules().size());
        /**
         * Store back the new version of the modified contacts
         * back into the CMP storage.
         */
        setCallManagementRules(retrievedCmrHashMap);
        tracer.info("DEBUG: CMP contacts are now " + getCallManagementRules().size());
    }

}
