<?php

class inbox
{
    private $conn;

    public $inbox_id;
    public $identifier;
    public $public_key;
    public $firebase_token;
    public $challenge;
    public $challenge_expiration;
    public $claimed;

    public function __construct($dbConn)
    {
        $this->conn = $dbConn;
    }

    public function get_by_identifier($identifier)
    {
        $found = false;

        $stmt = $this->conn->prepare(
            "SELECT
                *
              FROM
                inboxes
              WHERE
                identifier = ?");
        $stmt->bind_param("s", $identifier);
        $stmt->execute();
        $result = $stmt->get_result();
        $stmt->close();

        if ($result->num_rows === 1)
        {
            $row = $result->fetch_assoc();
            $this->inbox_id = $row["inbox_id"];
            $this->identifier = $row["identifier"];
            $this->public_key = $row["public_key"];
            $this->firebase_token = $row["firebase_token"];
            $this->challenge = $row["challenge"];
            $this->challenge_expiration = strtotime($row["challenge_expiration"]);
            $this->claimed = $row["claimed"];

            $found = true;
        }

        return $found;
    }

    public function save()
    {
        $success = false;
        if (!empty($this->inbox_id))
        {
            $challenge_expiration = !empty($this->challenge_expiration) ? date('Y-m-d H:i:s', $this->challenge_expiration) : null;

            $stmt = $this->conn->prepare(
                "UPDATE
                inboxes
                SET
                    identifier = ?, 
                    public_key = ?, 
                    firebase_token = ?, 
                    challenge = ?,
                    challenge_expiration = ?,
                    claimed = ?
                WHERE 
                    inbox_id = ?
                ");
            $stmt->bind_param(
                "sssssss",
                $this->identifier,
                $this->public_key,
                $this->firebase_token,
                $this->challenge,
                $challenge_expiration,
                $this->claimed,
                $this->inbox_id);
            $stmt->execute();
        }
        else
        {
            $challenge_expiration = !empty($this->challenge_expiration) ? date('Y-m-d H:i:s', $this->challenge_expiration) : null;

            $stmt = $this->conn->prepare(
            "INSERT INTO 
                inboxes(identifier, 
                        public_key, 
                        firebase_token, 
                        challenge,
                        challenge_expiration
                )
                VALUES(
                       ?,?,?,?,?
                )");
            $stmt->bind_param(
                "sssss",
                $this->identifier,
                $this->public_key,
                $this->firebase_token,
                $this->challenge,
                $challenge_expiration
            );
            $success = $stmt->execute();
            $this->inbox_id = $stmt->insert_id;
        }

        $stmt->close();

        return $success;
    }

    // Challenge verification gets one attempt
    public function verify_challenge($challenge_response, $registering)
    {
        // Don't accept an empty challenge
        if (!empty($this->inbox_id) || !empty($challenge_response)) {
            $now = date('Y-m-d H:i:s');

            $stmt = $this->conn->prepare("SELECT
                *
              FROM
                inboxes
              WHERE
                identifier = ?
                AND challenge = ?
                AND challenge_expiration >= ?");
            $stmt->bind_param("sss",
                $this->identifier,
                $challenge_response,
                $now
            );
            $stmt->execute();

            $result = $stmt->get_result();
            $stmt->close();

            // Clear challenge after 1 attempt for security
            $this->challenge = null;
            $this->challenge_expiration = null;

            if ($registering)
            {
                $this->claimed = 1;
            }

            $this->save();

            return $registering ? $result->num_rows === 1 : $this->claimed === $result->num_rows;
        } else {
            return false;
        }
    }
}