package com.cyrust.fftoolspromaxnew.adb;

public class AdbException extends Exception {
    public AdbException(String message, Throwable cause) { super(message, cause); }
    public AdbException(String message) { super(message); }
    public AdbException(Throwable cause) { super(cause); }
    public AdbException() { super(); }
}

class AdbInvalidPairingCodeException extends AdbException {
    public AdbInvalidPairingCodeException() { super(); }
}

class AdbKeyException extends AdbException {
    public AdbKeyException(Throwable cause) { super(cause); }
}
