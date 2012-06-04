/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.opencloud.demo.onenumber.core;

import com.opencloud.demo.onenumber.sip.base.OCSipSbb;
import com.opencloud.javax.sip.header.RSeqHeader;
import com.opencloud.slee.services.location.CallManagementRule;
import com.opencloud.slee.services.location.LocationDbProfileLocalInterface;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import net.java.slee.resource.sip.CancelRequestEvent;
import net.java.slee.resource.sip.DialogActivity;
import net.java.slee.resource.sip.DialogForkedEvent;
import javax.sip.*;
import javax.sip.InvalidArgumentException;
import javax.sip.address.SipURI;
import javax.sip.address.URI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.FromHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;
import javax.slee.*;
import javax.slee.facilities.TraceLevel;
import javax.slee.profile.ProfileLocalObject;
import javax.slee.profile.ProfileTable;

/**
 * Example showing how a simple B2BUA can be built using the
 * JAIN SIP 1.2 RA Type.
 */
public abstract class ServiceManagerSbb extends OCSipSbb
{

    private final String LOCATION_DATABASE_PROFILE_TABLE_NAME = "LocationProfileTable";

    public abstract void setInitialServerTransaction(ActivityContextInterface aci);

    public abstract ActivityContextInterface getInitialServerTransaction();

    public abstract void setIncomingDialog(ActivityContextInterface aci);

    public abstract ActivityContextInterface getIncomingDialog();

    public abstract void setOutgoingDialog(ActivityContextInterface aci);

    public abstract ActivityContextInterface getOutgoingDialog();

    /*
     * Set this flag if B2BUA has just sent a CANCEL - it doesn't have to forward the response 
     * of the cancelled INVITE client transaction.
     */
    public abstract void setCancelled(boolean cancelled);

    public abstract boolean getCancelled();

    public abstract void setForkingListArray(ArrayList arrayList);

    public abstract ArrayList getForkingListArray();
    
    private String[] domains;
    
    private static final String[] OUTGOING_EVENT_MASK = new String[]
    {
        "DialogForked"
    };

    public void onSipInviteRequest(RequestEvent sipInviteEvent , ActivityContextInterface aci)
    {
        tracer.info("SIP INVITE request event has been received...");
        tracer.info("SIP INVITE: " + sipInviteEvent.getRequest());        
        /**
         * Here we need to extract the address of record (AOR) from the received
         * INVITE message, try to locate a profile that corresponds to it and if
         * there is no profile found, then we need to reject the call.
         */
        String addressOfRecord = sipInviteEvent.getRequest().getRequestURI().toString();
        tracer.info("The called AOR is: " + addressOfRecord);
        tracer.info("Locating an associated profile for the called AOR...");
        LocationDbProfileLocalInterface profile = getAssociatedProfile(addressOfRecord);
        if (profile != null)
        {
            /**
             * Process the call
             */
            tracer.info("A profile associated with the AOR: " + addressOfRecord + " has been found");
            tracer.info("Starting CMR parsing process...");
            
            ServerTransaction serverTransaction = sipInviteEvent.getServerTransaction();
            setInitialServerTransaction(aci);

            try
            {
                DialogActivity incomingDialog = (DialogActivity) getSleeSipProvider().getNewDialog(serverTransaction);
                DialogActivity outgoingDialog = getSleeSipProvider().getNewDialog(incomingDialog, true);

                ActivityContextInterface outgoingDialogACI = getSipACIFactory().getActivityContextInterface(outgoingDialog);
                ActivityContextInterface incomingDialogACI = getSipACIFactory().getActivityContextInterface(incomingDialog);
                incomingDialogACI.attach(getSbbLocalObject());
                outgoingDialogACI.attach(getSbbLocalObject());

                sbbContext.maskEvent(OUTGOING_EVENT_MASK, outgoingDialogACI);

                setIncomingDialog(incomingDialogACI);
                setOutgoingDialog(outgoingDialogACI);

                forwardRequest(serverTransaction, outgoingDialog, true);
            }
            catch(Exception ex)
            {
                tracer.warning("failed to forward initial request", ex);
                sendErrorResponse(serverTransaction, Response.SERVER_INTERNAL_ERROR);
            }
        }
        else
        {
            /**
             * Reject the call
             */
            tracer.info("Unable to locate a profile associated with the called AOR: " + addressOfRecord);
            tracer.info("Rejecting the call with TEMPORARILY_UNAVAILABLE (" + Response.TEMPORARILY_UNAVAILABLE + ") response code");
            sendResponse(sipInviteEvent.getServerTransaction() , Response.TEMPORARILY_UNAVAILABLE);
        }
    }

    private LocationDbProfileLocalInterface getAssociatedProfile(String addressOfRecord)
    {
        LocationDbProfileLocalInterface aorProfile = null;
        try
        {
            ProfileTable locationProfileTable = profileFacility.getProfileTable(LOCATION_DATABASE_PROFILE_TABLE_NAME);
            if (locationProfileTable != null) // If the ProfileTable exists
            {
                ProfileLocalObject profileLocalObject = locationProfileTable.find(addressOfRecord);
                if (profileLocalObject != null)
                {                    
                    aorProfile = (LocationDbProfileLocalInterface) profileLocalObject;
                }
            }
            else
            {
                tracer.severe("ProfileTable with the name: LocationProfileTable does not exist");
                tracer.severe("Please contact the JSLEE container administrator to resolve the issue...");
                /**
                 * We need to design an exception to be thrown at this point.
                 */
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return aorProfile;
    }

    private void startCmrParsing(RequestEvent sipInviteEvent , LocationDbProfileLocalInterface profile , ActivityContextInterface aci)
    {
        
        tracer.info("Start parsing the configured CallManagementRules...");
        tracer.info("Building the criteria CallManagementRule");
        
        CallManagementRule criteriaCallManagementRule = buildCriteriaCMR(sipInviteEvent , profile);

        tracer.info("Extract the profile-related configured CMRs");
        
        CallManagementRule[] configuredCMRs = extractConfiguredCMRs(profile);

        tracer.info("Obtain a matching CallManagementRule");
        
        CallManagementRule matchingCMR = getMatchingCMR(criteriaCallManagementRule , configuredCMRs);

        tracer.info("Start Call Processing...");
        
        processCall(sipInviteEvent , matchingCMR , aci , profile);
    }

    private CallManagementRule buildCriteriaCMR(RequestEvent sipInviteEvent , LocationDbProfileLocalInterface profile)
    {
        /**
         * Create a new CMR, set the important parameters and return it
         * back to the caller subroutine.
         */
        tracer.info("Start building the CMR operation");
        
        CallManagementRule callManagementRule = new CallManagementRule();


        String[] incomingNumbers =
        {
            ((FromHeader) sipInviteEvent.getRequest().getHeader("From")).getAddress().getURI().toString()
        };
                
        tracer.info("Incoming Number: " + incomingNumbers[0]);        

        callManagementRule.setIncomingNumbers(incomingNumbers);
        callManagementRule.setLatitude(profile.getLatitude());
        callManagementRule.setLongitude(profile.getLongitude());
        callManagementRule.setRuleStartTimeStamp(new Date());
        callManagementRule.setRuleName("CriteriaCmr");

        tracer.info("Finish building the criteria CallManagementRule");

        return callManagementRule;
    }

    private CallManagementRule[] extractConfiguredCMRs(LocationDbProfileLocalInterface profile)
    {
        tracer.info("Start extracting the configured CMRs from the profile: " + profile.getProfileName());

        HashMap<String , CallManagementRule> configuredCmrHashMap = profile.getCallManagementRules();
        tracer.info("There are " + configuredCmrHashMap.size() + " CMRS for " + profile.getProfileName());

        CallManagementRule[] callManagementRules = new CallManagementRule[configuredCmrHashMap.size()];
        Iterator iterator = configuredCmrHashMap.entrySet().iterator();
        int arrayIndex = 0;
        while (iterator.hasNext())
        {
            Map.Entry entrySet = (Entry) iterator.next();
            callManagementRules[arrayIndex++] = (CallManagementRule) entrySet.getValue();
        }
        return callManagementRules;
    }

    private CallManagementRule getMatchingCMR(CallManagementRule criteriaCallManagementRule , CallManagementRule[] configuredCMRs)
    {
        tracer.info("Starting rule matching process...");
        
        /**
         * Here we need to iterate over the the configured CMRs and compare them
         * iteratively to the criteria CMR. This method is going to return the
         * first rule that makes a match. For a rule to create a match, all
         * the parameters within the Criteria CMR need to match a corresponding
         * rule from within the configured CMRs.
         *
         * P.S. Only the information that are pertinent to the call matching are
         * considered in the matching operation. If the comparison operation is
         * not resulted in any matching rule, then a NULL must be returned causing
         * the call to be rejected eventually.
         */
        for ( CallManagementRule callManagementRule : configuredCMRs )
        {
            if(callManagementRule.getRuleEndTimeStamp() == null || callManagementRule.getRuleStartTimeStamp() == null)
            {
                tracer.info("Rule storing is in erroorrrr");
            }
            boolean isRuleMatching = false;

            boolean callStartTimeComparison = compareCallStarTimes(criteriaCallManagementRule.getRuleStartTimeStamp() , callManagementRule.getRuleStartTimeStamp() , callManagementRule.getRuleEndTimeStamp());
            boolean incomingNumbersComparison = compareIncomingNumbers(criteriaCallManagementRule.getIncomingNumbers() , callManagementRule.getIncomingNumbers());
            //boolean positionDistanceComparison = compareDistance(getDistance(criteriaCallManagementRule.getLongitude(), callManagementRule.getLongitude(), criteriaCallManagementRule.getLatitude(), callManagementRule.getLatitude()), callManagementRule.getProximity());
            boolean positionDistanceComparison = compareDistance(getDistance("0.152386" , "0.148996" , "52.236136" , "52.232326") , 150000);

            isRuleMatching = incomingNumbersComparison && positionDistanceComparison && callStartTimeComparison;

            tracer.info("Rule comparison result: " + isRuleMatching);

            if (isRuleMatching)
            {
                tracer.info("A matching CMR has been found: " + callManagementRule.getRuleName());
                return callManagementRule;
            }
        }
        return null;
    }

    private double getDistance(String longitude1 , String longitude2 , String latitude1 , String latitude2)
    {
        int earthRaduis = 6371;
        double longitudeDistance = Math.toRadians((Double.parseDouble(longitude1) - Double.parseDouble(longitude2)));
        double latitudeDistance = Math.toRadians((Double.parseDouble(latitude1) - Double.parseDouble(latitude2)));
        double a = Math.sin(latitudeDistance / 2) * Math.sin(latitudeDistance / 2) + Math.cos(Math.toRadians(Double.parseDouble(latitude1))) * Math.cos(Math.toRadians(Double.parseDouble(latitude2))) * Math.sin(longitudeDistance / 2) * Math.sin(longitudeDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a) , Math.sqrt(1 - a));
        return c * earthRaduis;
    }

    private void processCall(RequestEvent sipInviteEvent , CallManagementRule matchingCMR , ActivityContextInterface aci , LocationDbProfileLocalInterface profile)
    {
        tracer.info("Call Processing started...");
        if(matchingCMR == null)
        {
            tracer.info("Matching CMR is null....");
        }
        else
        {
            tracer.info("RuleName: " + matchingCMR.getRuleName());                               
        }
        /**
         * Prepare an ArrayList to store the contacts that the call is
         * going to be forked to.
         */
        ArrayList outgoingDialogsArray = new ArrayList();

        tracer.info("Starting Call processing...");
        if (matchingCMR != null)
        {
            tracer.info("A matching CMR has been found...");
            tracer.info("There are " + profile.queryBinding().length + " bindings for this AOR profile");            
                        
            for ( String contact : profile.queryBinding() )
            {
                tracer.info(contact + ": before normalization");
                outgoingDialogsArray.add(contact.replace("Contact: <" , "").replace(">" , "").trim());
                tracer.info(contact + ": after normalization");
            }

            /**
             * Get the call handling method from the matched CMR and process the
             * call accordingly.
             */
            String callHandlingMethod = matchingCMR.getCallHandlingMethod();

            if (callHandlingMethod.equals("ACCEPT"))
            {
                tracer.info("Accepting the incoming SIP call as per the user-defined CMR...");
                /**
                 * Set the CMP field accordingly
                 */
                tracer.info("Settings the outgoing dialogs CMP fields...");
                setForkingListArray(outgoingDialogsArray);
                /**
                 * Obtain the ServerTransaction to serve the incoming request
                 */
                ServerTransaction serverTransaction = sipInviteEvent.getServerTransaction();
                /**
                 * Store the initial serverTransaction in a CMP field for later use.
                 */
                tracer.info("Storing the initial serverTransaction in a CMP field...");                
                setInitialServerTransaction(aci);
                                
                tracer.info("received initial INVITE:\n" + sipInviteEvent.getRequest());                
                
                try
                {
                    /**
                     * Obtain an instance of the incoming DialogActivity, use it to
                     * obtain an instance of its associated ActivityContextInterface
                     * then save that ACI instance in the appropriate CMP field. Then we
                     * need to attach to the ACI
                     */
                    DialogActivity incomingDialogActivity = (DialogActivity) getSleeSipProvider().getNewDialog(serverTransaction);
                    ActivityContextInterface incomingACI = getSipACIFactory().getActivityContextInterface(incomingDialogActivity);
                    setIncomingDialog(incomingACI);
                    incomingACI.attach(sbbContext.getSbbLocalObject());
                    /**
                     * Here we need to handle the required forking of the incoming requests, iterate over the forking list array
                     * and create outgoing DialogActivities, get their associated ACIs and attach
                     * to them, afterwards the request need to be forwarded to its
                     * ultimate destination.
                     */
                    for ( int i = 0 ; i < outgoingDialogsArray.size() ; i++ )
                    {
                        DialogActivity outgoingDialogActivity = getSleeSipProvider().getNewDialog(incomingDialogActivity , true);
                        ActivityContextInterface outgoingACI = getSipACIFactory().getActivityContextInterface(outgoingDialogActivity);
                        //setOutgoingDialog(outgoingACI);
                        outgoingACI.attach(sbbContext.getSbbLocalObject());
                        forwardRequest(serverTransaction , outgoingDialogActivity , true);
                    }
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                }
            }
            else
            {
                tracer.info("The call is being rejected as per the user preferences");
                tracer.info("Responding back with NOT FOUND SIP Response");
                sendErrorResponse(sipInviteEvent.getServerTransaction() , Response.NOT_FOUND);
            }
        }
        else
        {

            tracer.info("matching cmr is null, default call handling apply");

            tracer.info("There are " + profile.queryBinding().length + " bindings for this AOR profile");
            tracer.info("There are " + profile.queryBinding().length + " bindings for this AOR profile");
            for ( String contact : profile.queryBinding() )
            {
                tracer.info(contact);
                outgoingDialogsArray.add(contact.replace("Contact: <" , "").replace(">" , "").trim());
            }
            /**
             * Set the CMP field accordingly
             */
            tracer.info("Settings the outgoing dialogs CMP fields...");
            setForkingListArray(outgoingDialogsArray);

            /**
             * Obtain the ServerTransaction to serve the incoming request
             */
            ServerTransaction serverTransaction = sipInviteEvent.getServerTransaction();
            setInitialServerTransaction(aci);
            if (tracer.isTraceable(TraceLevel.FINEST))
            {
                tracer.finest("received initial INVITE:\n" + sipInviteEvent.getRequest());
            }
            try
            {
                /**
                 * Obtain an instance of the incoming DialogActivity, use it to
                 * obtain an instance of its associated ActivityContextInterface
                 * then save that ACI instance in the appropriate CMP field. Then we
                 * need to attach to the ACI
                 */
                DialogActivity incomingDialogActivity = (DialogActivity) getSleeSipProvider().getNewDialog(serverTransaction);
                ActivityContextInterface incomingACI = getSipACIFactory().getActivityContextInterface(incomingDialogActivity);
                setIncomingDialog(incomingACI);
                incomingACI.attach(sbbContext.getSbbLocalObject());
                /**
                 * Here we need to handle the required forking of the incoming requests, iterate over the forking list array
                 * and create outgoing DialogActivities, get their associated ACIs and attach
                 * to them, afterwards the request need to be forwarded to its
                 * ultimate destination.
                 */
                for ( int i = 0 ; i < outgoingDialogsArray.size() ; i++ )
                {
                    DialogActivity outgoingDialogActivity = getSleeSipProvider().getNewDialog(incomingDialogActivity , true);
                    ActivityContextInterface outgoingACI = getSipACIFactory().getActivityContextInterface(outgoingDialogActivity);
                    setOutgoingDialog(outgoingACI);
                    outgoingACI.attach(sbbContext.getSbbLocalObject());
                    forwardRequest(serverTransaction , outgoingDialogActivity , true);
                }
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }
    }

    private boolean compareCallStarTimes(Date callArrivalTime , Date ruleStartTimeStamp , Date ruleEndTimeStamp)
    {
        boolean comparisonResult = false;
        tracer.info(callArrivalTime.toString() + ":" + ruleEndTimeStamp.toString() + ":" + ruleStartTimeStamp.toString());
        tracer.info(callArrivalTime.getTime() + ":" + ruleEndTimeStamp.getTime() + ":" + ruleStartTimeStamp.getTime());
        if ((callArrivalTime.getTime() >= ruleStartTimeStamp.getTime()) && (callArrivalTime.getTime() <= ruleEndTimeStamp.getTime()))
        {
            tracer.info("Call start times is matched...");
            comparisonResult = true;
        }
        return comparisonResult;
    }

    private boolean compareIncomingNumbers(String[] incomingNumber , String[] incomingNumbersScreeningList)
    {
        String incomingCallNumber = incomingNumber[0];
        /**
         * Here we need to iterate over the configuredCMR numbers, if at
         * least one number is matching then a true will be returned, otherwise
         * it is a false
         */
        boolean comparisonResult = false;
        for ( String configuredCmrsIncomingNumber : incomingNumbersScreeningList )
        {
            tracer.info("Comparing: incomingNumber " + incomingCallNumber + " to screening list number: " + configuredCmrsIncomingNumber);
            if (configuredCmrsIncomingNumber != null && incomingCallNumber.startsWith("sip:" + configuredCmrsIncomingNumber))
            {
                tracer.info("Incoming number is a match..." + incomingCallNumber + ":" + configuredCmrsIncomingNumber);
                comparisonResult = true;
                return comparisonResult;
            }
        }
        return comparisonResult;
    }

    private boolean compareDistance(double distance , int proximity)
    {
        if (distance > proximity)
        {
            tracer.info("Distance mis-match: " + distance + "|" + proximity);
            return false;
        }
        else
        {
            tracer.info("Distance match: " + distance + "|" + proximity);
            return true;
        }
    }

    /**
     * A CANCEL request was received for the initial INVITE or re-INVITE.
     * CANCELs are hop-by-hop, so instead of forwarding the request, we
     * must create a new CANCEL on the appropriate client transaction.
     */
    public void onCancel(CancelRequestEvent event , ActivityContextInterface aci)
    {
        if (tracer.isTraceable(TraceLevel.FINEST))
        {
            tracer.finest("received CANCEL request:\n" + event.getRequest());
        }
        // Which leg did the CANCEL arrive on? Send cancel on the opposite leg.
        // If it arrived on the initial server transaction, send on outgoing leg.
        handleCancel(event , aci.equals(getInitialServerTransaction()) || aci.equals(getIncomingDialog())
                ? getOutgoingDialog() : getIncomingDialog());
    }

    /**
     * A forked 1xx response has arrived, creating a new dialog.<p>
     * This is an initial event, we setup a new B2BUA entity tree to handle
     * the new dialog.
     */
    public void onDialogForked(DialogForkedEvent event , ActivityContextInterface aci)
    {
        tracer.finest("dialog forked");
        handleFork(event , aci);
    }

    // Responses
    public void on1xxResponse(ResponseEvent event , ActivityContextInterface aci)
    {
        Response response = event.getResponse();
        if (response.getHeader(RSeqHeader.NAME) != null)
        {
            processReliableResponse(event , aci);
        }
        else
        {
            processResponse(event , aci);
        }
    }

    public void on2xxResponse(ResponseEvent event , ActivityContextInterface aci)
    {
        processResponse(event , aci);
    }

    public void on3xxResponse(ResponseEvent event , ActivityContextInterface aci)
    {
        processResponse(event , aci);
    }

    public void on4xxResponse(ResponseEvent event , ActivityContextInterface aci)
    {
        processResponse(event , aci);
    }

    public void on5xxResponse(ResponseEvent event , ActivityContextInterface aci)
    {
        processResponse(event , aci);
    }

    public void on6xxResponse(ResponseEvent event , ActivityContextInterface aci)
    {
        processResponse(event , aci);
    }

    // Mid-dialog requests
    public void onAck(RequestEvent event , ActivityContextInterface aci)
    {
        processMidDialogRequest(event , aci);
    }

    public void onBye(RequestEvent event , ActivityContextInterface aci)
    {
        processMidDialogRequest(event , aci);
    }

    public void onReInvite(RequestEvent event , ActivityContextInterface aci)
    {
        processMidDialogRequest(event , aci);
    }

    public void onPrack(RequestEvent event , ActivityContextInterface aci)
    {
        processMidDialogRequest(event , aci);
    }

    public void onUpdate(RequestEvent event , ActivityContextInterface aci)
    {
        processMidDialogRequest(event , aci);
    }

    public void onInfo(RequestEvent event , ActivityContextInterface aci)
    {
        processMidDialogRequest(event , aci);
    }

    public void onSubscribe(RequestEvent event , ActivityContextInterface aci)
    {
        processMidDialogRequest(event , aci);
    }

    public void onNotify(RequestEvent event , ActivityContextInterface aci)
    {
        processMidDialogRequest(event , aci);
    }

    public void onPublish(RequestEvent event , ActivityContextInterface aci)
    {
        processMidDialogRequest(event , aci);
    }

    public void onRefer(RequestEvent event , ActivityContextInterface aci)
    {
        processMidDialogRequest(event , aci);
    }

    public void onMessage(RequestEvent event , ActivityContextInterface aci)
    {
        processMidDialogRequest(event , aci);
    }

    public void onUnknownRequest(RequestEvent event , ActivityContextInterface aci)
    {
        processMidDialogRequest(event , aci);
    }

    // Transaction Timeout - don't need to forward a timeout response, just consume timeout (see RFC4320).
    // Forwarding timeout responses is unnecessary as upstream hosts will have already timed out.
    public void onTransactionTimeout(TimeoutEvent event , ActivityContextInterface aci)
    {
        if (event.isServerTransaction())
        {
            return;
        }
        ClientTransaction ct = event.getClientTransaction();
        if (tracer.isTraceable(TraceLevel.FINER))
        {
            tracer.finer("transaction " + ct + " timed out");
        }
        if (getCancelled())
        {
            // is this an error response for a cancelled INVITE? If so, consume the response here, RA has
            // already responded to upstream host.
            if (Request.INVITE.equals(ct.getRequest().getMethod()))
            {
                if (tracer.isTraceable(TraceLevel.FINER))
                {
                    tracer.finer("no need to forward timeout from cancelled INVITE");
                }
                setCancelled(false); // reset
            }
        }
    }

    /**
     * Initialise a new entity tree for the new forked dialog
     */
    private void handleFork(DialogForkedEvent event , ActivityContextInterface aci)
    {
        // Don't need to stay attached to the original dialog
        aci.detach(getSbbLocalObject());
        ResponseEvent responseEvent = event.getResponseEvent();
        ServerTransaction st;
        DialogActivity uac;
        if (tracer.isTraceable(TraceLevel.FINEST))
        {
            tracer.finest("received forked response on dialog " + aci.getActivity() + ":\n" + responseEvent.getResponse());
        }
        try
        {
            uac = event.getNewDialog();
            ActivityContextInterface uacACI = getSipACIFactory().getActivityContextInterface(uac);
            uacACI.attach(getSbbLocalObject());
            setOutgoingDialog(uacACI);
            st = uac.getAssociatedServerTransaction(responseEvent.getClientTransaction());
        }
        catch (Exception e)
        {
            tracer.warning("unable to continue with forked UAC dialog" , e);
            return;
        }

        try
        {
            DialogActivity uas = getSleeSipProvider().forwardForkedResponse(st , responseEvent.getResponse());
            ActivityContextInterface uasACI = getSipACIFactory().getActivityContextInterface(uas);
            uasACI.attach(getSbbLocalObject());
            setIncomingDialog(uasACI);
        }
        catch (Exception e)
        {
            tracer.warning("unable to continue with forked UAS dialog" , e);
            uac.delete();
        }
    }

    /**
     * Send a CANCEL on the given dialog
     */
    private void handleCancel(CancelRequestEvent cancelEvent , ActivityContextInterface dialogACI)
    {
        // Get the RA to respond to the CANCEL
        if (getSleeSipProvider().acceptCancel(cancelEvent , false))
        {
            // Cancel matched our INVITE - forward the CANCEL
            DialogActivity dialog = (DialogActivity) dialogACI.getActivity();
            if (tracer.isTraceable(TraceLevel.FINEST))
            {
                tracer.finest("sending CANCEL on dialog " + dialog);
            }
            try
            {
                dialog.sendCancel();
                setCancelled(true); // This stops us from forwarding the CANCEL response
            }
            catch (SipException e)
            {
                tracer.warning("failed to send CANCEL" , e);
            }
        }
        // else CANCEL did not match. RA has sent 481 response, nothing to do.
    }

    // Helpers
    /**
     * A request was received on one of our dialogs. Forward it to the other dialog.
     */
    private void processMidDialogRequest(RequestEvent event , ActivityContextInterface dialogACI)
    {
        if (tracer.isTraceable(TraceLevel.FINEST))
        {
            tracer.finest("received mid-dialog request on dialog " + dialogACI.getActivity() + ":\n" + event.getRequest());
        }
        try
        {
            // Find the dialog to forward the request on
            ActivityContextInterface peerACI = getPeerDialog(dialogACI);
            forwardRequest(event.getServerTransaction() , (DialogActivity) peerACI.getActivity() , false);
        }
        catch (SipException e)
        {
            tracer.warning("failed to forward request" , e);
            sendErrorResponse(event.getServerTransaction() , Response.SERVER_INTERNAL_ERROR);
        }
    }

    /**
     * A response was received on one of our dialogs. Forward it to the other dialog.
     */
    private void processResponse(ResponseEvent event , ActivityContextInterface aci)
    {
        Response response = event.getResponse();
        if (tracer.isTraceable(TraceLevel.FINEST))
        {
            tracer.finest("received response on dialog " + aci.getActivity() + ":\n" + event.getResponse());
        }
        if (getCancelled())
        {
            // is this an error response for a cancelled INVITE? If so, consume the response here, RA has
            // already responded to upstream host, and the peer dialog will have ended.
            if (response.getStatusCode() >= 400
                    && Request.INVITE.equals(((CSeqHeader) response.getHeader(CSeqHeader.NAME)).getMethod()))
            {
                if (tracer.isTraceable(TraceLevel.FINER))
                {
                    tracer.finer("no need to forward response from cancelled INVITE");
                }
                setCancelled(false); // reset
                return;
            }
        }

        try
        {
            // Find the dialog to forward the response on
            ActivityContextInterface peerACI = getPeerDialog(aci);
            forwardResponse((DialogActivity) aci.getActivity() , (DialogActivity) peerACI.getActivity() , event.getClientTransaction() , response);
        }
        catch (SipException e)
        {
            tracer.warning("failed to forward response" , e);
        }
    }

    private void processReliableResponse(ResponseEvent event , ActivityContextInterface aci)
    {
        Response response = event.getResponse();
        if (tracer.isTraceable(TraceLevel.FINEST))
        {
            tracer.warning("received reliable response on dialog " + aci.getActivity() + ":\n" + event.getResponse());
        }

        try
        {
            // Find the dialog to forward the response on
            ActivityContextInterface peerACI = getPeerDialog(aci);
            forwardReliableResponse((DialogActivity) aci.getActivity() , (DialogActivity) peerACI.getActivity() , event.getClientTransaction() , response);
        }
        catch (SipException e)
        {
            tracer.warning("failed to forward response" , e);
        }
    }

    private ActivityContextInterface getPeerDialog(ActivityContextInterface aci) throws SipException
    {
        if (aci.equals(getIncomingDialog()))
        {
            return getOutgoingDialog();
        }
        if (aci.equals(getOutgoingDialog()))
        {
            return getIncomingDialog();
        }
        throw new SipException("could not find peer dialog");
    }

    private void forwardRequest(ServerTransaction st , DialogActivity out , boolean initial) throws SipException
    {
        // Copies the request, setting the appropriate headers for the dialog.
        Request incomingRequest = st.getRequest();
        Request outgoingRequest = out.createRequest(incomingRequest);

        if (initial)
        {
            // On initial request only, check if the destination address is inside one of our domains
            SipURI requestURI = (SipURI) incomingRequest.getRequestURI();
            SipURI registeredAddress = (SipURI) lookupRegisteredAddress(requestURI);

            if (registeredAddress == null)
            {
                if (tracer.isTraceable(TraceLevel.FINE))
                {
                    tracer.fine("no registered address found for " + requestURI);
                }
                sendErrorResponse(st , Response.TEMPORARILY_UNAVAILABLE);
                return;
            }

            if (tracer.isTraceable(TraceLevel.FINE))
            {
                tracer.fine("found registered address: " + registeredAddress);
            }
            outgoingRequest.setRequestURI(registeredAddress);
        }
        if (tracer.isTraceable(TraceLevel.FINEST))
        {
            tracer.finest("forwarding request on dialog " + out + ":\n" + outgoingRequest);
        }
        if (incomingRequest.getMethod().equals(Request.ACK))
        {
            // Just forward the ACK statelessly - don't need to remember transaction state
            out.sendAck(outgoingRequest);
        }
        else
        {
            // Send the request on the dialog activity
            ClientTransaction ct = out.sendRequest(outgoingRequest);
            // Record an association with the original server transaction, so we can retrieve it
            // when forwarding the response.
            out.associateServerTransaction(ct , st);
        }
    }

    @SuppressWarnings("unchecked")
    private URI lookupRegisteredAddress(URI publicAddress)
    {
        try
        {
            return getSipAddressFactory().createSipURI("1234", "192.168.140.184:5060");
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
        return null;
    }

    private void forwardResponse(DialogActivity in , DialogActivity out , ClientTransaction ct , Response receivedResponse) throws SipException
    {
        // Find the original server transaction that this response should be forwarded on.
        ServerTransaction st = in.getAssociatedServerTransaction(ct); // could be null


        if (st == null)
        {
            throw new SipException("could not find associated server transaction");


        } // Copy the response across, setting the appropriate headers for the dialog
        Response outgoingResponse = out.createResponse(st , receivedResponse);


        if (tracer.isTraceable(TraceLevel.FINEST))
        {
            tracer.finest("forwarding response on dialog " + out + ":\n" + outgoingResponse);


        } // Forward response upstream.
        try
        {
            st.sendResponse(outgoingResponse);


        }
        catch (InvalidArgumentException e)
        {
            throw new SipException("invalid response" , e);


        }
    }

    private void forwardReliableResponse(DialogActivity in , DialogActivity out , ClientTransaction ct , Response receivedResponse) throws SipException
    {
        // Find the original server transaction that this response should be forwarded on.
        ServerTransaction st = in.getAssociatedServerTransaction(ct); // could be null


        if (st == null)
        {
            throw new SipException("could not find associated server transaction");


        } // Copy the response across, setting the appropriate headers for the dialog
        Response outgoingResponse = out.createResponse(st , receivedResponse);


        if (tracer.isTraceable(TraceLevel.FINEST))
        {
            tracer.finest("forwarding reliable response on dialog " + out + ":\n" + outgoingResponse);


        } // Forward response upstream.
        out.sendReliableProvisionalResponse(outgoingResponse);


    }

    private void sendErrorResponse(ServerTransaction st , int statusCode)
    {
        try
        {
            Response response = getSipMessageFactory().createResponse(statusCode , st.getRequest());
            st.sendResponse(response);


        }
        catch (Exception e)
        {
            tracer.warning("failed to send error response" , e);

        }
    }
}