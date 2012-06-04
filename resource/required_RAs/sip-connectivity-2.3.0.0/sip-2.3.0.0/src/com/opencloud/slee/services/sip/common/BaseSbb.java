package com.opencloud.slee.services.sip.common;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.slee.*;
import javax.slee.facilities.*;
import javax.slee.nullactivity.NullActivityContextInterfaceFactory;
import javax.slee.nullactivity.NullActivityFactory;
import javax.slee.profile.ProfileFacility;
import javax.slee.serviceactivity.ServiceActivity;
import javax.slee.serviceactivity.ServiceActivityContextInterfaceFactory;
import javax.slee.serviceactivity.ServiceActivityFactory;

public abstract class BaseSbb implements Sbb {

    public void setSbbContext(SbbContext context) {
        this.context = context;
        try {
            Context naming = new InitialContext();

            timerFacility = (TimerFacility) naming.lookup(TimerFacility.JNDI_NAME);
            profileFacility = (ProfileFacility) naming.lookup(ProfileFacility.JNDI_NAME);

            serviceActivityFactory = (ServiceActivityFactory) naming.lookup(ServiceActivityFactory.JNDI_NAME);
            serviceActivityContextInterfaceFactory = (ServiceActivityContextInterfaceFactory) naming.lookup(ServiceActivityContextInterfaceFactory.JNDI_NAME);
            aciNamingFacility = (ActivityContextNamingFacility) naming.lookup(ActivityContextNamingFacility.JNDI_NAME);
            nullACIFactory = (NullActivityContextInterfaceFactory) naming.lookup(NullActivityContextInterfaceFactory.JNDI_NAME);
            nullActivityFactory = (NullActivityFactory) naming.lookup(NullActivityFactory.JNDI_NAME);

            this.sbbTracer = context.getTracer(getTraceMessageType());

        } catch (NamingException ne) {
            System.out.println("Could not set SBB context: " + ne.toString());
        }
    }

    public void unsetSbbContext() { this.context = null; }

    public void sbbCreate() throws CreateException { }

    public void sbbPostCreate() throws CreateException { }

    public void sbbRemove() { }

    public void sbbPassivate() { }

    public void sbbActivate() { }

    public void sbbLoad() { }

    public void sbbStore() { }

    public void sbbExceptionThrown(Exception exception, Object event, ActivityContextInterface aci) { }

    public void sbbRolledBack(RolledBackContext context) { }

    protected final SbbContext getSbbContext() { return context; }

    protected final TimerFacility getTimerFacility() { return timerFacility; }

    protected final ProfileFacility getProfileFacility() { return profileFacility; }

    protected final SbbLocalObject getSbbLocalObject() { return context.getSbbLocalObject(); }

    protected final NullActivityContextInterfaceFactory getNullACIFactory() { return nullACIFactory; }

    protected final NullActivityFactory getNullActivityFactory() { return nullActivityFactory; }

    protected final ServiceActivityFactory getServiceActivityFactory() { return serviceActivityFactory; }

    protected final ServiceActivityContextInterfaceFactory getServiceActivityContextInterfaceFactory() { return serviceActivityContextInterfaceFactory; }

    protected final ActivityContextNamingFacility getACNamingFacility() { return aciNamingFacility; }

    protected abstract String getTraceMessageType();

    protected final Tracer getSbbTracer() { return sbbTracer; }

    protected final void trace(TraceLevel level, String message) {
        sbbTracer.trace(level, message);
    }

    protected final void trace(TraceLevel level, String message, Throwable t) {
        sbbTracer.trace(level, message, t);
    }

    protected final void config(String message) { trace(TraceLevel.CONFIG, message); }
    protected final void config(String message, Throwable t) { trace(TraceLevel.CONFIG, message, t); }

    protected final void info(String message) { trace(TraceLevel.INFO, message); }
    protected final void info(String message, Throwable t) { trace(TraceLevel.INFO, message, t); }

    protected final void fine(String message) { trace(TraceLevel.FINE, message); }
    protected final void fine(String message, Throwable t) { trace(TraceLevel.FINE, message, t); }

    protected final void finer(String message) { trace(TraceLevel.FINER, message); }
    protected final void finer(String message, Throwable t) { trace(TraceLevel.FINER, message, t); }

    protected final void finest(String message) { trace(TraceLevel.FINEST, message); }
    protected final void finest(String message, Throwable t) { trace(TraceLevel.FINEST, message, t); }

    protected final void warn(String message) { trace(TraceLevel.WARNING, message); }
    protected final void warn(String message, Throwable t) { trace(TraceLevel.WARNING, message, t); }

    protected final void severe(String message) { trace(TraceLevel.SEVERE, message); }
    protected final void severe(String message, Throwable t) { trace(TraceLevel.SEVERE, message, t); }

    protected final boolean isTraceable(TraceLevel level) {
        return sbbTracer.isTraceable(level);
    }

    protected void detachAllActivities() {
        ActivityContextInterface[] acis = context.getActivities();
        for (int i = 0; i < acis.length; i++) acis[i].detach(getSbbLocalObject());
    }

    protected void attachServiceActivity() {
        try {
            ServiceActivity service = serviceActivityFactory.getActivity();
            ActivityContextInterface aci = serviceActivityContextInterfaceFactory.getActivityContextInterface(service);
            aci.attach(getSbbLocalObject());
        } catch (UnrecognizedActivityException e) {
            // should never happen
            throw new RuntimeException(e);
        }
    }

    protected void detachServiceActivity() {
        ActivityContextInterface[] acis = getSbbContext().getActivities();
        for (int i = 0; i < acis.length; i++) {
            if (acis[i].getActivity() instanceof ServiceActivity) acis[i].detach(getSbbLocalObject());
        }
    }

    private SbbContext context;
    private TimerFacility timerFacility;
    private ProfileFacility profileFacility;
    private ServiceActivityFactory serviceActivityFactory;
    private ServiceActivityContextInterfaceFactory serviceActivityContextInterfaceFactory;
    private ActivityContextNamingFacility aciNamingFacility;
    private NullActivityFactory nullActivityFactory;
    private NullActivityContextInterfaceFactory nullACIFactory;
    private Tracer sbbTracer;
}
