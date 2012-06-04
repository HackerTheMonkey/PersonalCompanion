package com.opencloud.slee.services.base;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.slee.ActivityContextInterface;
import javax.slee.RolledBackContext;
import javax.slee.SbbContext;
import javax.slee.SbbLocalObject;
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
 * This is a convenience Base Sbb. 
 * Your Sbb can extend this class instead of extending the java.slee.Sbb.
 * 
 * @author OpenCloud
 */

public abstract class BaseSbb implements javax.slee.Sbb {

  /**
   * The SLEE invokes this method after a new instance of the SBB abstract
   * class is created. It uses this method to pass an SbbContext object to the
   * SBB object. During this method, an SBB entity has not been assigned to
   * the SBB object. In this method, the SBB object can allocate and
   * initialize state or connect to resources that are to be held by the SBB
   * object during its lifetime. Such state and resources cannot be specific
   * to an SBB entity because the SBB object might be reused during its
   * lifetime to serve multiple SBB entities. In this case, the method
   * initialise a number of references to SLEE facilities and other level
   * properties.
   *
   * @param context the context associated with this type of SBB
   * @see javax.slee.Sbb#setSbbContext(javax.slee.SbbContext)
   */
  public void setSbbContext(SbbContext context) {
      sbbContext = context;
      try {
          Context naming = new InitialContext();

          alarmFacility = (AlarmFacility) naming.lookup(AlarmFacility.JNDI_NAME);
          profileFacility = (ProfileFacility) naming.lookup(ProfileFacility.JNDI_NAME);
          timerFacility = (TimerFacility) naming.lookup(TimerFacility.JNDI_NAME);

          aciNamingFacility = (ActivityContextNamingFacility) naming.lookup(ActivityContextNamingFacility.JNDI_NAME);
          nullACIFactory = (NullActivityContextInterfaceFactory) naming.lookup(NullActivityContextInterfaceFactory.JNDI_NAME);
          nullActivityFactory = (NullActivityFactory) naming.lookup(NullActivityFactory.JNDI_NAME);
          serviceActivityFactory = (ServiceActivityFactory) naming.lookup(ServiceActivityFactory.JNDI_NAME);
          serviceActivityContextInterfaceFactory = (ServiceActivityContextInterfaceFactory) naming.lookup(ServiceActivityContextInterfaceFactory.JNDI_NAME);
          
          tracer = context.getTracer(context.getSbb().getName());
          tracer.finest("setSbbContext - references to: tracer, alarm facility, profile facility, timer facility, aci naming facility, null aci factory & null activity factory have been stored in attributes of this sbb object");
          
      } catch (NamingException e) {
          System.err.println("Could not set SBB context: " + e);
      }
  }

    /**
     * The SLEE invokes this method before terminating the life of the SBB
     * object. During this method, an SBB entity is not assigned to the SBB
     * entity. In this method, the SBB object can free state or resources that
     * are held by it, and that usually had been allocated by the set SbbContext
     * method.
     *
     * @see javax.slee.Sbb#unsetSbbContext()
     */
    public void unsetSbbContext() {
        sbbContext = null;
    }

    /**
     * The SLEE invokes this method on an SBB object before the SLEE creates a
     * new SBB entity in response to an initial event or an invocation of the
     * create method on a ChildRelation object. This method should initialize
     * the SBB object using the CMP field get and set accessor methods, such
     * that when this method returns, the persistent representation of the SBB
     * entity can be created.
     *
     * @throws javax.slee.CreateException when there is an application level problem (rather than SLEE
     *                                    or system level problem). If this method throws this
     *                                    exception, then the SLEE will not create the SBB entity. The
     *                                    SLEE will also propagate the CreateException unchanged to the
     *                                    caller that requested the creation of the SBB entity. The
     *                                    caller may be the SLEE or an SBB object.
     * @see javax.slee.Sbb#sbbCreate()
     */
    public void sbbCreate() throws javax.slee.CreateException {
        tracer.finest("sbbCreate");
    }

    /**
     * The SLEE invokes this method on an SBB object after the SLEE creates a
     * new SBB entity. The SLEE invokes this method after the persistent
     * representation of the SBB entity has been created and the SBB object is
     * assigned to the created SBB entity. This method gives the SBB object a
     * chance to initialize additional transient state and acquire additional
     * resources that it needs while it is in the Ready state.
     *
     * @throws javax.slee.CreateException when there is an application level problem (rather than SLEE
     *                                    or system level problem). If this method throws this
     *                                    exception, then the SLEE will not create the SBB entity. The
     *                                    SLEE will also propagate the CreateException unchanged to the
     *                                    caller that requested the creation of the SBB entity. The
     *                                    caller may be the SLEE or an SBB object.
     * @see javax.slee.Sbb#sbbPostCreate()
     */
    public void sbbPostCreate() throws javax.slee.CreateException {
        tracer.finest("sbbPostCreate");
    }

    /**
     * The SLEE invokes this method on an SBB object when the SLEE picks the SBB
     * object in the pooled state and assigns it to a specific SBB entity. This
     * method gives the SBB object a chance to initialize additional transient
     * state and acquire additional resources that it needs while it is in the
     * Ready state.
     *
     * @see javax.slee.Sbb#sbbActivate()
     */
    public void sbbActivate() {
        tracer.finest("sbbActivate");
    }

    /**
     * The SLEE invokes this method on an SBB object when the SLEE decides to
     * disassociate the SBB object from the SBB entity, and to put the SBB
     * object back into the pool of available SBB objects. This method gives the
     * SBB object the chance to release any state or resources that should not
     * be held while the SBB object is in the pool. These state and resources
     * typically had been allocated during the sbbActivate method.
     *
     * @see javax.slee.Sbb#sbbPassivate()
     */
    public void sbbPassivate() {
        tracer.finest("sbbPassivate");
    }

    /**
     * The SLEE invokes the sbbRemove method on an SBB object before the SLEE
     * removes the SBB entity assigned to the SBB object. The SLEE removes an
     * SBB entity when the SBB sub-entity tree or SBB entity tree that the SBB
     * entity belongs to is removed explicitly by an invocation of the remove
     * method on an SBB local object or implicitly by the SLEE when the
     * attachment count of root SBB entity decrements to zero. The SBB object is
     * in the Ready state when sbbRemove is invoked and it will enter the pooled
     * state when the method completes. In this method, you can implement any
     * actions that must be done before the SBB entity’s persistent
     * representation is removed.
     *
     * @see javax.slee.Sbb#sbbRemove()
     */
    public void sbbRemove() {
        tracer.finest("sbbRemove");
    }

    /**
     * The SLEE calls this method to synchronize the state of an SBB object with
     * its assigned SBB entity’s persistent state. The SBB Developer can assume
     * that the SBB object’s persistent state has been loaded just before this
     * method is invoked. It is the responsibility of the SBB Developer to use
     * this method to recompute or initialize the values of any transient
     * instance variables that depend on the SBB entity’s persistent state. In
     * general, any transient state that depends on the persistent state of an
     * SBB entity should be recalculated in this method.
     *
     * @see javax.slee.Sbb#sbbLoad()
     */
    public void sbbLoad() {
        tracer.finest("sbbLoad");
    }

    /**
     * The SLEE calls this method to synchronize the state of the SBB entity’s
     * persistent state with the state of the SBB object. The SBB Developer
     * should use this method to update the SBB object using the CMP field
     * accessor methods before its persistent state is synchronized. The SBB
     * Developer can assume that after this method returns, the persistent state
     * is synchronized.
     *
     * @see javax.slee.Sbb#sbbStore()
     */
    public void sbbStore() {
        tracer.finest("sbbStore");
    }

    /**
     * Default implementation of 'exceptional situation' handling. The SLEE
     * invokes the sbbRolledBack callback method after a transaction used in a
     * SLEE originated invocation has rolled back.
     *
     * @param context -
     *                The RolledBackContext of this Sbb
     * @see javax.slee.Sbb#sbbRolledBack(javax.slee.RolledBackContext)
     */
    public void sbbRolledBack(RolledBackContext context) {
        tracer.warning("sbbRolledBack '" + context.getEvent() + "' on '"
                             + context.getActivityContextInterface().getActivity());
    }

    /**
     * Default implementation of 'exceptional situation' handling.
     *
     * @param exception the exception thrown by one of the methods invoked by the
     *                  SLEE, e.g. a life cycle method, an event handler method, or a
     *                  local interface method.
     * @param event     If the method that threw the exception is an event handler
     *                  method, the event argument will be the event and activity
     *                  arguments of the event handler method. Otherwise, the event
     *                  argument will be null.
     * @param aci       If the method that threw the exception is an event handler
     *                  method, the activity argument will be the event and activity
     *                  arguments of the event handler method. Otherwise, the activity
     *                  argument will be null.
     * @see javax.slee.Sbb#sbbExceptionThrown(java.lang.Exception,
            *      java.lang.Object, javax.slee.ActivityContextInterface)
     */
    public void sbbExceptionThrown(Exception exception, Object event, ActivityContextInterface aci) {
        tracer.severe("sbbExceptionThrown '" + event + "' on '"
                            + aci.getActivity() + "' " + exception);
    }

    //
    // Utility methods
    //
    /**
     * This method returns an SBB local object that represents the SBB entity
     * assigned to the SBB object of the SbbContext object.
     *
     * @return an object that implements the SBB local interface of the SBB
     *         entity.
     */
    protected final SbbLocalObject getSbbLocalObject() {
        return sbbContext.getSbbLocalObject();
    }

    /**
     * Convenience method to retrieve the SbbContext object stored in
     * setSbbContext.
     *
     * @return this SBB's SbbContext object
     */
    protected final SbbContext getSbbContext() {
        return sbbContext;
    }

    /**
     * Return the reference to the Alarm Facility stored in setSbbContext
     *
     * @return the reference to the SLEE Alarm Facility
     */
    protected final AlarmFacility getAlarmFacility() { 
        return alarmFacility;
    }
    
    /**
     * Return the reference to the Profile Facility stored in setSbbContext
     *
     * @return the reference to the SLEE Profile Facility
     */
    protected final ProfileFacility getProfileFacility() {
        return profileFacility;
    }

    /**
     * Return the reference to the Timer Facility stored in setSbbContext
     *
     * @return the reference to the SLEE Timer Facility
     */
    protected final TimerFacility getTimerFacility() {
        return timerFacility;
    }

    /**
     * Return the reference to the NullActivityContextInteface stored in
     * setSbbContext
     *
     * @return the reference to the SLEE NullActivityContextInteface
     */
    protected final NullActivityContextInterfaceFactory getNullACIFactory() {
        return nullACIFactory;
    }

    /**
     * Return the reference to the NullActivityFactory stored in setSbbContext
     *
     * @return the reference to the SLEE NullActivityFactory
     */
    protected final NullActivityFactory getNullActivityFactory() {
        return nullActivityFactory;
    }

    /**
     * Return the reference to the ActivityContextNaming Facility stored in
     * setSbbContext
     *
     * @return the reference to the SLEE ActivityContextNaming Facility
     */
    protected final ActivityContextNamingFacility getACNamingFacility() {
        return aciNamingFacility;
    }
    
    /**
     * Return the reference to the ServiceActivityFactory stored in setSbbContext
     *
     * @return the reference to the SLEE ServiceActivityFactory
     */
    protected final ServiceActivityFactory getServiceActivityFactory() { 
        return serviceActivityFactory; 
    }

    /**
     * Return the reference to the ServiceActivityContextInterfaceFactory stored in setSbbContext
     *
     * @return the reference to the SLEE ServiceActivityContextInterfaceFactory
     */
    protected final ServiceActivityContextInterfaceFactory getServiceActivityContextInterfaceFactory() {
        return serviceActivityContextInterfaceFactory; 
    }

    //
    // Tracing
    //
    
    protected final Tracer getTracer()
    {
      return tracer;
    }

    // SLEE Facilities
    private AlarmFacility alarmFacility = null;
    private ProfileFacility profileFacility = null;
    private TimerFacility timerFacility = null;
    private ActivityContextNamingFacility aciNamingFacility = null;
    private NullActivityFactory nullActivityFactory = null;
    private NullActivityContextInterfaceFactory nullACIFactory = null;
    private ServiceActivityFactory serviceActivityFactory = null;
    private ServiceActivityContextInterfaceFactory serviceActivityContextInterfaceFactory = null;
    

    private Tracer tracer = null;
    private SbbContext sbbContext = null;
}
