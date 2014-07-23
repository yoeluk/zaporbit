# --- !Ups

CREATE TABLE `Buyings`(
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `status` VARCHAR(25) NOT NULL,
  `userid` BIGINT NOT NULL,
  `transactionid` BIGINT NOT NULL,
  `created_on` TIMESTAMP DEFAULT 0,
  `updated_on` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                       ON UPDATE CURRENT_TIMESTAMP,
  INDEX `indx_buying_user` (`userid`),
  CONSTRAINT `fk_buying_transaction`
  FOREIGN KEY (`transactionid`) REFERENCES `Transactions`(`id`)
    ON UPDATE CASCADE
    ON DELETE CASCADE
) ENGINE=InnoDB;

# --- !Downs

DROP TABLE `Buyings`;