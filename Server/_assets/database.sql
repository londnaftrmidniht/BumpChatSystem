-- Create database
CREATE DATABASE bump_chat;

-- Create tables
DROP TABLE IF EXISTS messages;
DROP TABLE IF EXISTS inboxes;

CREATE TABLE inboxes (
  inbox_id int(11) NOT NULL AUTO_INCREMENT,
  identifier VARCHAR(255) NOT NULL,
  public_key VARCHAR(4096) NOT NULL,
  firebase_token VARCHAR(255) NOT NULL,
  challenge VARCHAR(255) NULL,
  challenge_expiration DATETIME NULL,
  claimed TINYINT DEFAULT 0,
  PRIMARY KEY (inbox_id),
  UNIQUE INDEX inboxes_identifier_uindex (identifier)
) ENGINE=InnoDB ENCRYPTED=YES DEFAULT CHARSET=latin1;


CREATE TABLE messages (
  message_id int(11) NOT NULL AUTO_INCREMENT,
  inbox_id int(11) NOT NULL,
  message text,
  received TIMESTAMP,
  PRIMARY KEY (message_id),
  KEY messages_inboxes_inbox_id_fk (inbox_id),
  CONSTRAINT messages_inboxes_inbox_id_fk FOREIGN KEY (inbox_id) REFERENCES inboxes (inbox_id)
) ENGINE=InnoDB ENCRYPTED=YES DEFAULT CHARSET=latin1;