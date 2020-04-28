package com.bumpchat.bumpchat.encryption;

import android.util.Base64;

import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

public class Rsa {
    static final String ALGO = "RSA";
    static final int KEY_SIZE = 2048;

    static final String SIGNATURE_ALGORITHM = "SHA512withRSA";

    static final String PUBLICKEY_PREFIX    = "-----BEGIN PUBLIC KEY-----";
    static final String PUBLICKEY_POSTFIX   = "-----END PUBLIC KEY-----";
    static final String PRIVATEKEY_PREFIX   = "-----BEGIN RSA PRIVATE KEY-----";
    static final String PRIVATEKEY_POSTFIX  = "-----END RSA PRIVATE KEY-----";

    public static KeyPair generateNewKeyPair() {
        KeyPair kp = null;
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(ALGO);
            kpg.initialize(KEY_SIZE);
            kp = kpg.generateKeyPair();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return kp;
    }

    public static String convertPublicKeyPem(PublicKey publicKey) {
        try {
            return PUBLICKEY_PREFIX + "\n" + Base64.encodeToString(publicKey.getEncoded(), 16) + PUBLICKEY_POSTFIX;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String convertPrivateKeyPem(PrivateKey privateKey) {
        try {
            return PRIVATEKEY_PREFIX + "\n" + Base64.encodeToString(privateKey.getEncoded(), 16) + PRIVATEKEY_POSTFIX;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String getPublicKeyPemBase64(PublicKey publicKey) {
        return Base64.encodeToString(convertPublicKeyPem(publicKey).getBytes(), Base64.NO_WRAP);
    }

    public static String getHashedPublicKeyPem(PublicKey publicKey) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

            byte[] digest = messageDigest.digest(convertPublicKeyPem(publicKey).getBytes());

            return new java.math.BigInteger(1, digest).toString(16);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String decryptPhpEncryptedBase64Message(PrivateKey privateKey, String encodedMessage) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);

            byte[] decodedStr = Base64.decode(encodedMessage, Base64.NO_WRAP);
            return new String(cipher.doFinal(decodedStr));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String signMessage(PrivateKey privateKey, String message)
    {
        try {
            Signature sig = Signature.getInstance(SIGNATURE_ALGORITHM);
            sig.initSign(privateKey);
            sig.update(message.getBytes());

            byte[] signatureByteArray = sig.sign();

            return "-----BEGIN SIGNATURE-----" + "\n"
                    + android.util.Base64.encodeToString(signatureByteArray, 16).replaceAll("(.{64})", "$1\n") + "\n"
                    + "-----END SIGNATURE-----";
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String encodeKeyToBase64(Key publicKey)
    {
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(publicKey.getEncoded());
        return Base64.encodeToString(x509EncodedKeySpec.getEncoded(), 16);
    }

    public static String encodePrivateKeyToBase64(PrivateKey privateKey)
    {
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(privateKey.getEncoded());
        return Base64.encodeToString(pkcs8EncodedKeySpec.getEncoded(), 16);
    }

    public static KeyPair convertStringToKeyPair(String base64PublicKey, String base64PrivateKey)
    {
        PublicKey publicKey = null;
        PrivateKey privateKey = null;
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(ALGO);
            byte[] publicKeyBytes = Base64.decode(base64PublicKey, 16);
            byte[] privateKeyBytes = Base64.decode(base64PrivateKey, 16);

            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
            publicKey = keyFactory.generatePublic(publicKeySpec);

            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            privateKey = keyFactory.generatePrivate(privateKeySpec);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new KeyPair(publicKey, privateKey);
    }

    public static PublicKey decodePublicKey(byte[] encoded) throws InvalidKeySpecException, NoSuchAlgorithmException {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGO);
        return keyFactory.generatePublic(keySpec);
    }
}
