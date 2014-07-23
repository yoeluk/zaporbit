# --- !Ups

CREATE TABLE `Merchants`(
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `userid` BIGINT NOT NULL,
  `identifier` VARCHAR(255) NOT NULL,
  `secret` VARCHAR(255) NOT NULL,
  `created_on` TIMESTAMP DEFAULT 0,
  `updated_on` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                       ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT `fk_merchant_user`
  FOREIGN KEY (`userid`) REFERENCES `Users`(`id`)
    ON UPDATE CASCADE
    ON DELETE CASCADE
) ENGINE=InnoDB;

# --- !Downs

DROP TABLE `Merchants`;