# --- !Ups

CREATE TABLE `Messages`(
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `received_status` VARCHAR(25) NOT NULL,
  `message` BLOB NOT NULL,
  `convid` BIGINT NOT NULL,
  `senderid` BIGINT NOT NULL,
  `recipientid` BIGINT NOT NULL,
  `created_on` TIMESTAMP DEFAULT 0,
  `updated_on` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                       ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT `fk_conversation_message`
  FOREIGN KEY (`convid`) REFERENCES `Conversations`(`id`)
    ON UPDATE CASCADE
    ON DELETE CASCADE
) ENGINE=InnoDB;

# --- !Downs

DROP TABLE `Messages`;