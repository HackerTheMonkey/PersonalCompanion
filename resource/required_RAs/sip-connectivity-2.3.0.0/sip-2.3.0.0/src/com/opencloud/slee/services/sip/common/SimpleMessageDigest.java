package com.opencloud.slee.services.sip.common;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Used by the proxy to calculate a hash of a SIP request, for loop detection.
 */
public class SimpleMessageDigest {
    public SimpleMessageDigest() {
        try {
            this.md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            // should not happen since MD5 is in J2SE
            throw new RuntimeException(e);
        }
    }

    public void update(int i) {
        md.update((byte) ((i & 0xff000000) >> 24));
        md.update((byte) ((i & 0x00ff0000) >> 16));
        md.update((byte) ((i & 0x0000ff00) >> 8));
        md.update((byte) (i & 0x000000ff));
    }

    public void update(String s) {
        if (s == null) return;
        md.update(s.getBytes());
    }

    public void update(byte[] b) {
        if (b == null) return;
        md.update(b);
    }

    public byte[] digest() {
        return md.digest();
    }

    public String digestHex() {
        return printHex(md.digest());
    }

    private static final String printHex(byte[] b) {
        StringBuffer sb = new StringBuffer(b.length * 2);
        for (int i = 0; i < b.length; i++) {
            // high byte
            int c = (char)b[i] & 0xff;
            sb.append(hexChars[c >> 4]);
            // low byte
            sb.append(hexChars[c & 0x0f]);
        }
        return sb.toString();
    }

    private final MessageDigest md;

    private static final char[] hexChars = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
}
