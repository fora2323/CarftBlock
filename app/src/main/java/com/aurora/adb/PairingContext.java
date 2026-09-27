package com.aurora.adb;

public class PairingContext {

    static {
        System.loadLibrary("adb");
    }

    private long ptr;

    public PairingContext(boolean isClient, byte[] password) {
        ptr = nativeConstructor(isClient, password);
    }

    public byte[] getMsg() {
        return nativeMsg(ptr);
    }

    public boolean initCipher(byte[] theirMsg) {
        return nativeInitCipher(ptr, theirMsg);
    }

    public byte[] encrypt(byte[] in) {
        return nativeEncrypt(ptr, in);
    }

    public byte[] decrypt(byte[] in) {
        return nativeDecrypt(ptr, in);
    }

    public void destroy() {
        if (ptr != 0) {
            nativeDestroy(ptr);
            ptr = 0;
        }
    }

    private native long nativeConstructor(boolean isClient, byte[] password);
    private native byte[] nativeMsg(long ptr);
    private native boolean nativeInitCipher(long ptr, byte[] theirMsg);
    private native byte[] nativeEncrypt(long ptr, byte[] in);
    private native byte[] nativeDecrypt(long ptr, byte[] in);
    private native void nativeDestroy(long ptr);
}
