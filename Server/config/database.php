<?php


class database
{
    private $hostname = "localhost";
    private $username = "{USERNAME}";
    private $password = "{PASSWORD}";
    private $db = "bump_chat";
    public $conn;

    public function getConnection()
    {
        if ($this->conn == null)
        {
            $this->conn = mysqli_connect($this->hostname, $this->username, $this->password, $this->db);
        }

        if ($this->conn->connect_error) {
            die("Database connection failed: " . $this->conn->connect_error);
        }

        return $this->conn;
    }
}