<?php
include_once "../vendor/autoload.php";


ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);


use Kreait\Firebase;
use Kreait\Firebase\Messaging;
use Kreait\Firebase\Messaging\CloudMessage;
use Kreait\Firebase\Messaging\Notification;


$firebase = (new Firebase\Factory())
    ->withServiceAccount('../bumpchat-service-account.json')
    ->create();

$messaging = $firebase->getMessaging();

$deviceToken = 'eW3Gd51kLbM:APA91bF6fE4FTdFOkK643gP9PXuyCkMxz0RdxSNXDZNSS8y4l-xojRA1bC4KfA6ESMpGChnEBF4LgH-GjLe3aCQov0uTee-NeZKkYN2Bf6kU1SFDbHyrrsuCyx6jXD3tjhUQeAinfKCb';
$notification = Notification::create('New message', 'Tap to open');
$data = [
    'inbox_identifier' => '5d361a1c5297e2ef25ad7adcfc89e77b527355cd'
];

$message = CloudMessage::withTarget('token', $deviceToken)
    ->withNotification($notification)
    ->withData($data);

$result = $messaging->send($message);

var_dump($result);