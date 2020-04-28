package com.bumpchat.bumpchat.encryption;

import android.util.Base64;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AesGcm {
    private static final String ALGO = "AES_256/GCM/NoPadding";
    private static final String ALGO_KDF = "HmacSHA256";
    private static final int KEY_LENGTH_BYTES = 32;
    private static final int TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;

    // Key derivation function
    public static byte[] deriveKeyFromDh(byte[] dhKey, byte[] saltKey) throws NoSuchAlgorithmException, InvalidKeyException {
        final Mac mac = Mac.getInstance(ALGO_KDF);
        mac.init(new SecretKeySpec(saltKey, ALGO_KDF));
        return mac.doFinal(dhKey);
    }

    public static String encrypt(byte[] encryptionKey, byte[] rawData) {
        if (encryptionKey.length < KEY_LENGTH_BYTES) {
            throw new IllegalArgumentException("Key must be longer than 16 bytes");
        }

        byte[] cipherBytes = null;
        byte[] iv = new byte[IV_LENGTH_BYTES]; // NEVER REUSE THIS IV WITH SAME KEY

        try {
            // Create AES-GCM cipher
            SecureRandom rng = new SecureRandom();

            rng.nextBytes(iv);
            final Cipher cipher = Cipher.getInstance(ALGO);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);

            // Encrypt message and add current timestamp
            //JSONObject associatedData = new JSONObject();
            //associatedData.put("sent_date", Instant.now().getEpochSecond());
            //cipher.updateAAD(associatedData.toString().getBytes());
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encryptionKey, "AES_256"), parameterSpec);
            cipherBytes = cipher.doFinal(rawData);

            // Store iv.length + iv + cipherText into byte[]
            ByteBuffer byteBuffer = ByteBuffer.allocate(4 + iv.length + cipherBytes.length);
            byteBuffer.putInt(iv.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherBytes);

            // Convert to Base64 string for transmission
            return Base64.encodeToString(byteBuffer.array(), Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Arrays.fill(iv, (byte) 0);
            if (cipherBytes != null) {
                Arrays.fill(cipherBytes, (byte) 0);
            }
        }

        return null;
    }

    public static String decrypt(byte[] encryptionKey, String encryptedData) {
        byte[] ivDecrypt = null;
        byte[] cipherBytesDecrypt = null;
        try {
            // Convert back to bytes
            byte[] cipherMessageDecrypt = Base64.decode(encryptedData, Base64.DEFAULT);
            ByteBuffer byteBufferDecrypt = ByteBuffer.wrap(cipherMessageDecrypt);

            // First byte holds the IV length
            int ivLength = byteBufferDecrypt.getInt();

            // Make sure IV length was not poisoned to fill the heap
            if (ivLength < 12 || ivLength >= 16) {
                throw new IllegalArgumentException("Invalid iv length");
            }

            // Get IV
            ivDecrypt = new byte[ivLength];
            byteBufferDecrypt.get(ivDecrypt);

            // Get cipherText from rest of the buffer
            cipherBytesDecrypt = new byte[byteBufferDecrypt.remaining()];
            byteBufferDecrypt.get(cipherBytesDecrypt);

            // Decrypt
            final Cipher cipherDecrypt = Cipher.getInstance(ALGO);
            GCMParameterSpec parameterSpecDecrypt = new GCMParameterSpec(TAG_LENGTH_BITS, ivDecrypt);
            cipherDecrypt.init(Cipher.DECRYPT_MODE, new SecretKeySpec(encryptionKey, "AES_256"), parameterSpecDecrypt);

            return new String(cipherDecrypt.doFinal(cipherBytesDecrypt));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

            if (ivDecrypt != null) {
                Arrays.fill(ivDecrypt, (byte) 0);
            }
            if (cipherBytesDecrypt != null) {
                Arrays.fill(cipherBytesDecrypt, (byte) 0);
            }
        }

        return "";
    }
}
