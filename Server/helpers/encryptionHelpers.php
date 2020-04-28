<?php

class encryptionHelpers
{
    public static function encryptRsa($publicKey, $message) {

        openssl_public_encrypt($message, $encrypted, $publicKey);
        return base64_encode($encrypted);
    }
}