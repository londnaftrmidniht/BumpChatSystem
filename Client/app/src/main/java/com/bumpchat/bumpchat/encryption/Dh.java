package com.bumpchat.bumpchat.encryption;

import android.util.Log;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.KeyAgreement;
import javax.crypto.spec.DHParameterSpec;

public class Dh {
    static final String ALGO = "DH";

    // https://tools.ietf.org/html/rfc3526#page-3 has safe primes of various sizes
    // 2048-bit MODP Group was used for p and g
    // p should be a "safe prime" where (p - 1) / 2 is also a prime
    // p and g are public so don't need to be generated each time
    // p = 2^2048 - 2^1984 - 1 + 2^64 * { [2^1918 pi] + 124476 }
    // p (hex) = FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E088A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7EDEE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3DC2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F83655D23DCA3AD961C62F356208552BB9ED529077096966D670C354E4ABC9804F1746C08CA18217C32905E462E36CE3BE39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9DE2BCBF6955817183995497CEA956AE515D2261898FA051015728E5A8AACAA68FFFFFFFFFFFFFFFF
    private static final BigInteger p = new BigInteger("32317006071311007300338913926423828248817941241140239112842009751400741706634354222619689417363569347117901737909704191754605873209195028853758986185622153212175412514901774520270235796078236248884246189477587641105928646099411723245426622522193230540919037680524235519125679715870117001058055877651038861847280257976054903569732561526167081339361799541336476559160368317896729073178384589680639671900977202194168647225871031411336429319536193471636533209717077448227988588565369208645296636077250268955505928362751121174096972998068410554359584866583291642136218231078990999448652468262416972035911852507045361090559");
    private static final BigInteger g = new BigInteger("2");
    private static final int l = 2048 - 1;

    // Generates a new 2048 bit DH keypair
    public static KeyPair generateNewKeyPair() {
        KeyPair kp = null;
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(ALGO);
            kpg.initialize(new DHParameterSpec(p, g, l));
            kp = kpg.generateKeyPair();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return kp;
    }

    // Generates a 256 byte shared secret
    public static byte[] generateDHSecret(PrivateKey userPrivateKey, PublicKey partnerPublicKey) throws NoSuchAlgorithmException, InvalidKeyException {
        // Create Diffie-Hellman key agreement
        KeyAgreement keyAgreement = KeyAgreement.getInstance("DH");

        // Initialize with user's private key
        keyAgreement.init(userPrivateKey);

        // Feed in other parties public key
        keyAgreement.doPhase(partnerPublicKey, true);

        // Generate shared secret
        return keyAgreement.generateSecret();
    }

    public static PublicKey decodePublicKey(byte[] encoded) throws InvalidKeySpecException, NoSuchAlgorithmException {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGO);
        return keyFactory.generatePublic(keySpec);
    }
}
