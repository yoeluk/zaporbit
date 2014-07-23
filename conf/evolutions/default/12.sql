# --- !Ups

CREATE TABLE `Feedbacks`(
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `feedback` VARCHAR(255) NOT NULL,
  `userid` BIGINT NOT NULL,
  `by_userid` BIGINT NOT NULL,
  `transid` BIGINT NOT NULL,
  `created_on` TIMESTAMP DEFAULT 0,
  `updated_on` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                       ON UPDATE CURRENT_TIMESTAMP,
  INDEX `indx_byuser_feedback` (`by_userid`),
  INDEX `indx_user_feedback` (`userid`),
  CONSTRAINT `fk_transaction_feedback`
  FOREIGN KEY (`transid`) REFERENCES `Transactions`(`id`)
    ON UPDATE CASCADE
    ON DELETE CASCADE
) ENGINE=InnoDB;


# --- !Downs

DROP TABLE `Feedbacks`;