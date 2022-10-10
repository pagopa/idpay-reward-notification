package it.gov.pagopa.reward.notification.service;

public interface LockService {
    int getBuketSize();
    void acquireLock(int lockId);
    void releaseLock(int lockId);
}
