package com.opencloud.slee.services.myservice;

import javax.slee.ActivityContextInterface;
import javax.slee.SLEEException;
import javax.slee.profile.ProfileID;
import javax.slee.profile.UnrecognizedProfileNameException;
import javax.slee.profile.UnrecognizedProfileTableNameException;
import javax.slee.serviceactivity.ServiceStartedEvent;

import com.opencloud.slee.services.location.LocationDbInterfaceCMP;
import com.opencloud.slee.services.base.BaseSbb;

/**
 * This is an Sbb implementation that extends BaseSbb
 * 
 * @author Open Cloud
 */
public abstract class MySbb extends BaseSbb
{

  // Initial event
  public void onServiceStartedEvent(ServiceStartedEvent event, ActivityContextInterface aci)
  {

    getTracer().info("Hello " + getMyProfile().getWorld() + " World...");

  }

  public abstract LocationDbInterfaceCMP getMyProfile(ProfileID id) throws UnrecognizedProfileTableNameException,
      UnrecognizedProfileNameException;

  private LocationDbInterfaceCMP getMyProfile()
  {
    ProfileID myProfileId = new ProfileID("MyProfileTable", "MyProfile");
    try
    {
      return getMyProfile(myProfileId);
    }
    catch (UnrecognizedProfileTableNameException e)
    {
      throw new SLEEException("Profile table name " + myProfileId.getProfileTableName() + " does not exist", e);
    }
    catch (UnrecognizedProfileNameException e)
    {
      throw new SLEEException("Profile named " + myProfileId.getProfileName() + " does not exist in table "
          + myProfileId.getProfileTableName(), e);
    }
  }
}
