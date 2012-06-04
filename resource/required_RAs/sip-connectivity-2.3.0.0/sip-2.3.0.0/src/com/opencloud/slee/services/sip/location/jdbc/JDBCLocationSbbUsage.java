package com.opencloud.slee.services.sip.location.jdbc;

/**
 */
public interface JDBCLocationSbbUsage {
    void incrementRegistrationsRead(long value);
    void incrementRecordsRead(long value);
    void incrementRecordsRemoved(long value);
    void incrementRecordsUpdated(long value);
    void incrementRecordsInserted(long value);    
}
