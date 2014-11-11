# --- !Ups

CREATE TABLE `PaypalMerchants`(
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `userid` BIGINT NOT NULL,
  `token` VARCHAR(250) NOT NULL,
  `tokenSecret` VARCHAR(250) NOT NULL,
  `scope` VARCHAR(250) NOT NULL,
  CONSTRAINT `fk_paypalmerchant_user`
  FOREIGN KEY (`userid`) REFERENCES `Users`(`id`)
    ON UPDATE CASCADE
    ON DELETE CASCADE
) ENGINE=InnoDB;

# --- !Downs

DROP TABLE `PaypalMerchants`;