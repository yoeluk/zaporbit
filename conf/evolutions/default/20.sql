# --- !Ups

CREATE TABLE `PaypalContants`(
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `merchantid` BIGINT NOT NULL,
  `name` VARCHAR(50) NOT NULL,
  `surname` VARCHAR(50) NOT NULL,
  `email` VARCHAR(50) NOT NULL,
  `businessName` VARCHAR(200) NULL,
  `country` VARCHAR(50) NOT NULL,
  `paypalid` VARCHAR(50) NOT NULL,
  CONSTRAINT `fk_contact_paypalmerchant`
  FOREIGN KEY (`merchantid`) REFERENCES `PaypalMerchants`(`id`)
    ON UPDATE CASCADE
    ON DELETE CASCADE
) ENGINE=InnoDB;

# --- !Downs

DROP TABLE `PaypalContants`;