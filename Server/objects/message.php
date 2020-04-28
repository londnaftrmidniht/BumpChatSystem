<?php


class message
{
    private $conn;

    public $message_id;
    public $inbox_id;
    public $message;
    public $received;

    public function __construct($dbConn)
    {
        $this->conn = $dbConn;
    }

    public function create($inbox_id, $message)
    {
        $this->inbox_id = $inbox_id;
        $this->message = $message;
        $this->received = date('Y-m-d H:i:s');

        $stmt = $this->conn->prepare(
            "INSERT INTO 
            messages(
                     inbox_id, 
                     message, 
                     received
             )
             VALUES(?,?,?)");
        $stmt->bind_param(
            "sss",
            $this->inbox_id,
            $this->message,
            $this->received
        );
        $success = $stmt->execute();
        $this->message_id = $stmt->insert_id;

        $stmt->close();

        return $success;
    }

    // Get all messages for an inbox
    public static function get_all($dbConn, $identifier)
    {
        $stmt = $dbConn->prepare("SELECT
                m.message_id,
                i.identifier,
                m.message,
                m.received
              FROM
                inboxes i
              INNER JOIN
                messages m on i.inbox_id = m.inbox_id
              WHERE
                i.identifier = ?");
        $stmt->bind_param("s", $identifier);
        $stmt->execute();
        $result = $stmt->get_result();
        $stmt->close();

        $messages = [];
        while ($row = $result->fetch_assoc()) {
            // Create new message with only necessary filled
            $message = new stdClass(null);
            $message->message_id = $row["message_id"];
            $message->identifier = $row["identifier"];
            $message->message = $row["message"];
            $message->received = strtotime($row["received"]) * 1000; // Android uses epoch milli not second

            $messages[] = $message;
        }

        return $messages;
    }

    // Clear all messages older and including $messageUpperId
    public static function clear($dbConn, $identifier, $messageUpperId) {
        $stmt = $dbConn->prepare(
            "DELETE
                messages
            FROM
                messages
            INNER JOIN
                inboxes on messages.inbox_id = inboxes.inbox_id
             WHERE
                inboxes.identifier = ?
                AND messages.message_id <= ?");
        $stmt->bind_param("si", $identifier, $messageUpperId);
        $stmt->execute();
        $result = $stmt->get_result();
        $stmt->close();

        return $result->num_rows;
    }
}