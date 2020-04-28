<?php
//include_once "../vendor/autoload.php";
include_once "../config/database.php";
include_once "../helpers/encryptionHelpers.php";
include_once "../objects/inbox.php";
include_once "../objects/message.php";

// Takes raw data from the request and convert JSON to object
$json = file_get_contents('php://input');
$data = json_decode($json);

$responseObj = new stdClass();

if (isset($data->identifier) && isset($data->challenge_response) && isset($data->recipient_identifier) && isset($data->message))
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
                $recipientInbox = new inbox($dbconnect);
                if ($recipientInbox->get_by_identifier($data->recipient_identifier)) {
                    $message = new message($dbconnect);
                    $message->create($recipientInbox->inbox_id, $data->message);
                    $responseObj->message = "Message sent successfully.";
                    $responseObj->error = false;
                } else {
                    $responseObj->message = "Partner inbox not created yet.";
                    $responseObj->error = true;
                }
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
