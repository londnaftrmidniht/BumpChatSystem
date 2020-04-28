<?php
include_once "../config/database.php";
include_once "../helpers/encryptionHelpers.php";
include_once "../objects/inbox.php";

// Takes raw data from the request
$json = file_get_contents('php://input');

// Converts it into a PHP object
$data = json_decode($json);

$responseObj = new stdClass();

if (isset($data->identifier)) {
    $database = new database();
    $dbconnect = $database->getConnection();

    $inbox = new inbox($dbconnect);

    if ($inbox->get_by_identifier($data->identifier)) {
        // Make sure inbox is claimed before messages can be retrieved
        if ($inbox->claimed === 1) {
            // Set challenge for user to answer before messages can be downloaded
            $inbox->challenge = bin2hex(openssl_random_pseudo_bytes(32));
            $inbox->challenge_expiration = strtotime('5 minutes');
            $inbox->save();

            $responseObj->error = false;
            $responseObj->challenge = encryptionHelpers::encryptRsa($inbox->public_key, $inbox->challenge);
        } else {
            $responseObj->error = true;
            $responseObj->message = "Inbox has not been verified yet.";
        }
    } else {
        $responseObj->error = true;
        $responseObj->message = "Inbox not found.";
    }

    echo json_encode($responseObj);
} else {
    $responseObj->error = true;
    $responseObj->message = "Malformed request.";
}
