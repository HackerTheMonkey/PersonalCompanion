package com.opencloud.slee.services.sip.presence;

import javax.slee.ActivityContextInterface;

public interface SubscriptionACI extends ActivityContextInterface {
    
    public int getSubscriberCount();
    public void setSubscriberCount(int i);

}
