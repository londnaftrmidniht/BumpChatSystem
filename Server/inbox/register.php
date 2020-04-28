<?php
include_once "../config/database.php";
include_once "../helpers/encryptionHelpers.php";
include_once "../objects/inbox.php";

// Takes raw data from the request
$json = file_get_contents('php://input');

// Converts it into a PHP object
$data = json_decode($json);

$responseObj = new stdClass();

//if (isset($data->public_key_pem) && isset($data->firebase_token)) {
if (isset($data->public_key_pem)) {
    $database = new database();
    $dbconnect = $database->getConnection();

    // Identifier (calculate independently so we know inbox is for that public key)
    // Sha-256 is used here to simplify the inbox id and make it easier to index
    // The probability that two VALID public keys collide is basically zero
    $inbox = new inbox($dbconnect);

    // Check if inbox has already been registered
    $claimed = false;
    if ($inbox->get_by_identifier(hash('sha256', $data->public_key_pem))) {
        // Allow registering again if inbox is not claimed
        if ($inbox->claimed === 0) {
//            $inbox->firebase_token = $data->firebase_token;
            $inbox->firebase_token = "";
            $inbox->challenge = bin2hex(openssl_random_pseudo_bytes(32));
            $inbox->challenge_expiration = strtotime('5 minutes');
            $inbox->save();
        } else {
            $claimed = true;
        }
    } else {
        // Inbox is new
        $inbox->identifier = hash('sha256', $data->public_key_pem);
        $inbox->public_key = $data->public_key_pem;
//        $inbox->firebase_token = $data->firebase_token;
        $inbox->firebase_token = "";
        $inbox->challenge = bin2hex(openssl_random_pseudo_bytes(32));
        $inbox->challenge_expiration = strtotime('5 minutes');
        $inbox->save();
    }

    if (!$claimed) {
        $responseObj->error = false;
        $responseObj->challenge = encryptionHelpers::encryptRsa($data->public_key_pem, $inbox->challenge);
    } else {
        $responseObj->error = true;
        $responseObj->message = "Inbox already claimed";
    }

    echo json_encode($responseObj);
} else {
    $responseObj->error = true;
    $responseObj->message = "Malformed request.";
}
