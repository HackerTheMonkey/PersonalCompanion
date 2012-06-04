/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.opencloud.demo.onenumber.sip.base;

import com.opencloud.javax.sip.slee.OCSipActivityContextInterfaceFactory;
import com.opencloud.javax.sip.slee.OCSleeSipProvider;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.slee.*;
import javax.slee.CreateException;
import javax.slee.RolledBackContext;
import javax.slee.Sbb;
import javax.slee.facilities.ActivityContextNamingFacility;
import javax.slee.facilities.AlarmFacility;
import javax.slee.facilities.TimerFacility;
import javax.slee.facilities.Tracer;
import javax.slee.nullactivity.NullActivityContextInterfaceFactory;
import javax.slee.nullactivity.NullActivityFactory;
import javax.slee.profile.ProfileFacility;
import javax.slee.serviceactivity.ServiceActivityContextInterfaceFactory;
import javax.slee.serviceactivity.ServiceActivityFactory;

/**
 *
 * @author Hasanein.Khafaji
 */
public abstract class BaseSbb implements Sbb
{

    /**
     * Private variables definition
     */
    protected SbbContext sbbContext = null;
    protected AlarmFacility alarmFacility = null;
    protected ProfileFacility profileFacility = null;
    protected TimerFacility timerFacility = null;
    protected ActivityContextNamingFacility acNamingFacility = null;
    protected NullActivityContextInterfaceFactory nullActivityContextInterfaceFactory = null;
    protected NullActivityFactory nullActivityFactory = null;
    protected ServiceActivityFactory serviceActivityFactory = null;
    protected ServiceActivityContextInterfaceFactory serviceActivityContextInterfaceFactory = null;
    protected Tracer tracer = null;
    protected OCSipActivityContextInterfaceFactory ocSipAciFactory = null;
    protected OCSleeSipProvider ocSleeSipProvider = null;
    /**
     * Set the SbbContext and get the tracer and the facilities
     * provided by the JSLEE Container
     * @param sc
     */
    public void setSbbContext(SbbContext sbbContext)
    {
        this.sbbContext = sbbContext;
        /**
         * get the tracer
         */
        tracer = sbbContext.getTracer(sbbContext.getSbb().getName());
        tracer.info("The SbbContext has been set...");
        /**
         * Query the JNDI for the JSLEE-provided facilities
         */
        try
        {
            tracer.info("Looking up the JNDI for the JSLEE-provided facilities...");
            /**
             * Set a JNDI initial context to start the lookup
             * operation from
             */
            InitialContext initialContext = new InitialContext();
            /**
             * Starting from the initial context, query for the
             * desired JSLEE-injected facilities
             */
            alarmFacility = (AlarmFacility) initialContext.lookup(alarmFacility.JNDI_NAME);
            profileFacility = (ProfileFacility) initialContext.lookup(profileFacility.JNDI_NAME);
            timerFacility = (TimerFacility) initialContext.lookup(timerFacility.JNDI_NAME);
            acNamingFacility = (ActivityContextNamingFacility) initialContext.lookup(acNamingFacility.JNDI_NAME);
            nullActivityContextInterfaceFactory = (NullActivityContextInterfaceFactory) initialContext.lookup(nullActivityContextInterfaceFactory.JNDI_NAME);
            nullActivityFactory = (NullActivityFactory) initialContext.lookup(nullActivityFactory.JNDI_NAME);
            serviceActivityFactory = (ServiceActivityFactory) initialContext.lookup(serviceActivityFactory.JNDI_NAME);
            serviceActivityContextInterfaceFactory = (ServiceActivityContextInterfaceFactory) initialContext.lookup(serviceActivityContextInterfaceFactory.JNDI_NAME);

            tracer.info("The facilities have been successfully obtained from the JNDI...");
        }
        catch (NamingException ex)
        {
            tracer.severe(ex.getMessage());
        }
    }

    public void sbbActivate()
    {
        tracer.info("SbbActivate method has been called...");
    }

    public void sbbCreate() throws CreateException
    {
        tracer.info("SbbCreate method has been called...");
    }

    public void sbbExceptionThrown(Exception excptn, Object o, ActivityContextInterface aci)
    {
        tracer.info("SbbExceptionThrown method has been called...");
    }

    public void sbbLoad()
    {
        tracer.info("SbbLoad method has been called...");
    }

    public void sbbPassivate()
    {
        tracer.info("SbbPassivate method has been called...");
    }

    public void sbbPostCreate() throws CreateException
    {
        tracer.info("SbbPostCreate method has been called...");
    }

    public void sbbRemove()
    {
        tracer.info("SbbRemove method has been called...");
    }

    public void sbbRolledBack(RolledBackContext rbc)
    {
        tracer.info("SbbRolledBack method has been called...");
    }

    public void sbbStore()
    {
        tracer.info("SbbStore method has been called...");
    }

    public void unsetSbbContext()
    {
        tracer.info("Clearing the SbbContext...");
        sbbContext = null;
        tracer.info("The SbbContext has been cleared out...");

        tracer.info("Clearing the JSLEE-provided facilities...");
        clearFacilities();
        tracer.info("The JSLEE-provided facilities have been cleared out.");
    }

    private void clearFacilities()
    {
        alarmFacility = null;
        profileFacility = null;
        timerFacility = null;
        acNamingFacility = null;
        nullActivityContextInterfaceFactory = null;
        nullActivityFactory = null;
        serviceActivityFactory = null;
        serviceActivityContextInterfaceFactory = null;
    }

}
