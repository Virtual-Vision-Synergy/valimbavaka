package org.vvs.valimbavaka.feature.transfer.domain;

/**
 * Status of a transfer (TRF-03). A stock return has its own status so that it is told apart from the other
 * transfers at a glance; any other transfer is validated as soon as it is recorded (TRF-02).
 */
public enum TransferStatus {
    VALIDATED,
    RETURN
}
