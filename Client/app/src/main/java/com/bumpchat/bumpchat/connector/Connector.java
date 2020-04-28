package com.bumpchat.bumpchat.connector;

import android.util.Base64;

import com.bumpchat.bumpchat.encryption.AesGcm;
import com.bumpchat.bumpchat.encryption.Rsa;
import com.bumpchat.bumpchat.models.Message;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

public class Connector {
    private static final String HOST = "https://bumpchat.adroitwebdesigns.com/";
    private static final int TIMEOUT = 7000;
    private String responseMessage = "";
    private HttpsURLConnection conn;

    public boolean registerInbox(final KeyPair keyPair) {
        boolean success = false;
        int retries = 3;

        while (retries > 0) {
            // This is a terrible way to handle network connections
            // Either use a library or create a reusable service thread with callbacks
            try {
                JSONObject jsonParam = new JSONObject();
                // Identifier is sha1(base64(publicKey)) as it is short enough to index easily
                // Both client and server calculate identifier independently
                jsonParam.put("public_key_pem", Rsa.convertPublicKeyPem(keyPair.getPublic()));
                JSONObject response = sendJsonPost("inbox/register.php", jsonParam);

                if (response.getBoolean("error")) {
                    responseMessage = response.getString("message");
                } else {
                    // Decrypt challenge given by server
                    // Respond to challenge with identifier and decrypted challenge
                    jsonParam = new JSONObject();
                    jsonParam.put("identifier", Rsa.getHashedPublicKeyPem(keyPair.getPublic()));
                    jsonParam.put("challenge_response", decryptChallenge(response.getString("challenge"), keyPair.getPrivate()));
                    response = sendJsonPost("inbox/register_verify.php", jsonParam);

                    if (response.getBoolean("error")) {
                        responseMessage = response.getString("message");
                    } else {
                        responseMessage = "Inbox created successfully!";
                        success = true;
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            retries--;
        }

        if (retries == 0) {
            responseMessage = "Inbox verification failed. Check your internet and try again later.";
        }

        return success;
    }

    public boolean sendMessage(final KeyPair keyPair, String recipientIdentifier, String sharedAesKey, String message) {
        boolean success = false;
        int retries = 3;

        while (retries > 0) {
            // This is a terrible way to handle network connections
            // Either use a library or create a reusable service thread with callbacks
            try {
                JSONObject jsonParam = new JSONObject();
                jsonParam.put("identifier", Rsa.getHashedPublicKeyPem(keyPair.getPublic()));
                JSONObject response = sendJsonPost("challenge/generate.php", jsonParam);

                if (response.getBoolean("error")) {
                    responseMessage = response.getString("message");
                } else {
                    final byte[] aesEncryptionKey = Base64.decode(sharedAesKey, 16);

                    // Decrypt challenge given by server
                    // Respond to challenge with identifier and decrypted challenge
                    jsonParam = new JSONObject();
                    jsonParam.put("identifier", Rsa.getHashedPublicKeyPem(keyPair.getPublic()));
                    jsonParam.put("recipient_identifier", recipientIdentifier);
                    jsonParam.put("message", AesGcm.encrypt(aesEncryptionKey, message.getBytes()));
                    jsonParam.put("challenge_response", decryptChallenge(response.getString("challenge"), keyPair.getPrivate()));
                    response = sendJsonPost("message/send.php", jsonParam);

                    if (response.getBoolean("error")) {
                        responseMessage = response.getString("message");
                    } else {
                        responseMessage = "Message sent successfully!";
                        success = true;
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            retries--;
        }

        if (retries == 0) {
            responseMessage = "Sending message failed. Check your internet and try again later.";
        }

        return success;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public String decryptChallenge(String challenge, PrivateKey privateKey)
    {
        String decryptedChallenge = "";
        try {
            // PHP uses ECB mode with PKCS1Padding for encryption
            // Vulnerable to padding oracle attacks but challenge is very short lived
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);

            byte[] decodedStr = android.util.Base64.decode(challenge, Base64.DEFAULT);
            decryptedChallenge = new String(cipher.doFinal(decodedStr));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return decryptedChallenge;
    }

    public List<Message> getMessages(final KeyPair keyPair, String sharedAesKey) {
        List<Message> messages = new ArrayList<>();
        int retries = 3;

        while (retries > 0) {
            try {
                JSONObject jsonParam = new JSONObject();
                jsonParam.put("identifier", Rsa.getHashedPublicKeyPem(keyPair.getPublic()));
                JSONObject response = sendJsonPost("challenge/generate.php", jsonParam);

                if (response.getBoolean("error")) {
                    responseMessage = response.getString("message");
                } else {
                    final byte[] aesEncryptionKey = Base64.decode(sharedAesKey, 16);

                    // Decrypt challenge given by server
                    // Respond to challenge with identifier and decrypted challenge
                    jsonParam = new JSONObject();
                    jsonParam.put("identifier", Rsa.getHashedPublicKeyPem(keyPair.getPublic()));
                    jsonParam.put("challenge_response", decryptChallenge(response.getString("challenge"), keyPair.getPrivate()));
                    response = sendJsonPost("message/retrieve_all.php", jsonParam);

                    if (!response.getBoolean("error")) {

                        JSONArray messageArray = response.getJSONArray("messages");
                        String lastMessageId = "";

                        for (int i = 0; i < messageArray.length(); i++) {
                            String decryptedMessage = AesGcm.decrypt(
                                    aesEncryptionKey,
                                    messageArray.getJSONObject(i).getString("message")
                            );

                            Message message = new Message(
                                messageArray.getJSONObject(i).getString("identifier"),
                                    decryptedMessage,
                                messageArray.getJSONObject(i).getLong("received"),
                                true
                            );

                            lastMessageId = messageArray.getJSONObject(i).getString("message_id");

                            messages.add(message);
                        }

                        // Delete message from server
                        if (messageArray.length() > 0) {
                            clearMessages(keyPair, lastMessageId);
                        }

                        break;
                    } else {
                        responseMessage = "Error retrieving messages: " + response.getString("message");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            retries--;
        }

        if (retries == 0) {
            responseMessage = "Retrieving messages failed. Check your internet and try again later.";
        }

        return messages;
    }

    public boolean clearMessages(final KeyPair keyPair, String lastMessageId) {
        int retries = 3;

        while (retries > 0) {
            try {
                JSONObject jsonParam = new JSONObject();
                jsonParam.put("identifier", Rsa.getHashedPublicKeyPem(keyPair.getPublic()));
                JSONObject response = sendJsonPost("challenge/generate.php", jsonParam);

                if (response.getBoolean("error")) {
                    responseMessage = response.getString("message");
                } else {
                    // Decrypt challenge given by server
                    // Respond to challenge with identifier and decrypted challenge
                    jsonParam = new JSONObject();
                    jsonParam.put("identifier", Rsa.getHashedPublicKeyPem(keyPair.getPublic()));
                    jsonParam.put("challenge_response", decryptChallenge(response.getString("challenge"), keyPair.getPrivate()));
                    jsonParam.put("message_id_upper", lastMessageId);
                    response = sendJsonPost("message/clear_all.php", jsonParam);

                    if (!response.getBoolean("error")) {
                        return true;
                    } else {
                        responseMessage = "Error retrieving messages: " + response.getString("message");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            retries--;
        }

        if (retries == 0) {
            responseMessage = "Sending message failed. Check your internet and try again later.";
        }

        return false;
    }

    public void getChallenge(String inboxIdentifier, String publicKey) {
        try {
            JSONObject jsonParam = new JSONObject();
            jsonParam.put("identifier", inboxIdentifier);
            jsonParam.put("publicKey", publicKey);

            JSONObject response = sendJsonPost("challenge/generate.php", jsonParam);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void sendResponse(String inboxIdentifier, String publicKey, String challenge) {
        try {
            JSONObject jsonParam = new JSONObject();
            jsonParam.put("identifier", inboxIdentifier);
            jsonParam.put("publicKey", publicKey);

            JSONObject response = sendJsonPost("challenge/generate.php", jsonParam);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private JSONObject sendJsonPost(final String endpoint, final JSONObject jsonData) {
        JSONObject jsonResponse = null;
        try {
            URL url = new URL(HOST + endpoint);

            // Create HTTPS connection
            conn = (HttpsURLConnection) url.openConnection();
            conn.setReadTimeout(TIMEOUT);
            conn.setConnectTimeout(TIMEOUT);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
            conn.setDoInput(true);

            // Create the SSL connection
            SSLContext sc;
            sc = SSLContext.getInstance("TLS");
            sc.init(null, null, new java.security.SecureRandom());
            conn.setSSLSocketFactory(sc.getSocketFactory());

            // Create payload
            DataOutputStream os = new DataOutputStream(conn.getOutputStream());
            //os.writeBytes(URLEncoder.encode(jsonParam.toString(), "UTF-8"));
            os.writeBytes(jsonData.toString());
            os.flush();
            os.close();

            // Get response
            BufferedReader in = new BufferedReader(
                new InputStreamReader(conn.getInputStream())
            );

            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            in.close();

            System.out.println("Status: " + conn.getResponseCode());
            System.out.println("Message: " + conn.getResponseMessage());

            if (conn.getResponseCode() == 200) {
                jsonResponse = new JSONObject(response.toString());
            } else {
                jsonResponse = new JSONObject();
                jsonResponse.put("responseCode", conn.getResponseCode());
            }

            conn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return jsonResponse;
    }
}
