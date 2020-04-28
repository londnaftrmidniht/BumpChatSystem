package com.bumpchat.bumpchat.helpers;

import android.util.Log;

import com.bumpchat.bumpchat.encryption.Dh;
import com.bumpchat.bumpchat.encryption.Rsa;

import java.security.KeyPair;
import java.security.PublicKey;
import java.security.SecureRandom;

public class TemporaryKeyStorage {
    // Bitting (From right):
    // 0 = user received keys
    // 1 = partner received keys
    // 2 = user sent keys
    public static byte TransferState = 0;
    public static byte TransferStateUserMask = 0b0001;
    public static byte TransferStatePartnerMask = 0b0010;
    public static byte TransferStateSentMask = 0b0100;
    public static byte TransferStateSyncedMask = 0b0111;

    public static String ContactName = "";
    public static KeyPair UserRsaKey = null;
    public static KeyPair UserDhKey = null;
    public static PublicKey PartnerPublicRsaKey = null;
    public static PublicKey PartnerPublicDhKey = null;
    public static byte[] Aes256EncryptionKey = new byte[32];
    public static byte[] Hmac256Salt = new byte[32];
    public static boolean InUse = false;
    public static boolean SaltShared = false;
    public static boolean InboxClaimed = false;

    public static void Clear() {
        InUse = false;
        ContactName = "";
        TransferState = 0;
        PartnerPublicRsaKey = null;
        PartnerPublicDhKey = null;
        Aes256EncryptionKey = new byte[32];
        Hmac256Salt = new byte[32];
        SaltShared = false;
        InboxClaimed = false;

        try {
            UserRsaKey = Rsa.generateNewKeyPair();
            UserDhKey = Dh.generateNewKeyPair();

            // Create a salt in case we are the first to send
            SecureRandom rng = new SecureRandom();
            rng.nextBytes(TemporaryKeyStorage.Hmac256Salt);

        } catch (Exception e)
        {
            Log.d("ContactNewKD", e.toString());
        }
    }
}
