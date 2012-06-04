package com.opencloud.slee.services.sip.proxy;

import com.opencloud.javax.sip.header.OCHeaderFactory;
import com.opencloud.slee.services.sip.location.LocationService;
import com.opencloud.javax.sip.slee.OCSleeSipProvider;

import javax.sip.address.AddressFactory;
import javax.sip.message.MessageFactory;

public interface ProxyConfig {
    OCSleeSipProvider getSipProvider();
    AddressFactory getSipAddressFactory();
    OCHeaderFactory getSipHeaderFactory();
    MessageFactory getSipMessageFactory();

    String[] getProxyDomains();

    boolean isRecordRouteEnabled();
    boolean isForkingEnabled();
    boolean isLoopDetectionEnabled();

    LocationService getLocationService();
    boolean useLocationService();
}
