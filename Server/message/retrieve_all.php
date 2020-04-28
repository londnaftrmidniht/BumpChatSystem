<?php
include_once "../config/database.php";
include_once "../helpers/encryptionHelpers.php";
include_once "../objects/inbox.php";
include_once "../objects/message.php";

// Takes raw data from the request and convert JSON to object
$json = file_get_contents('php://input');
$data = json_decode($json);

$responseObj = new stdClass();

if (isset($data->identifier) && isset($data->challenge_response))
{
    // Connect to DB
    $database = new database();
    $dbconnect = $database->getConnection();

    // Get inbox
    $inbox = new inbox($dbconnect);
    if ($inbox->get_by_identifier($data->identifier)) {
        // Make sure inbox is claimed and awaiting verification
        if ($inbox->claimed === 1)
        {
            if ($inbox->verify_challenge($data->challenge_response, false))
            {
//                // Update push token
//                $inbox->firebase_token = $data->firebase_token;
//                $inbox->save();

                // Get all new messages
                $messages = Message::get_all($dbconnect, $inbox->identifier);
                $responseObj->messages = $messages;
                $responseObj->error = false;
            }
            else
            {
                $responseObj->message = "Verification failed. Please try again.";
                $responseObj->error = true;
            }
        }
        else
        {
            $responseObj->message = "Inbox not verified.";
            $responseObj->error = true;
        }
    } else {
        $responseObj->message = "Inbox not found.";
        $responseObj->error = true;
    }

    echo json_encode($responseObj);
} else {
    $responseObj->error = true;
    $responseObj->message = "Malformed request.";
}
