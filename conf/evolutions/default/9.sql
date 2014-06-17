# --- !Ups

CREATE TABLE `Billings`(
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `status` VARCHAR(25) NOT NULL,
  `userid` BIGINT NOT NULL,
  `offer_title` VARCHAR(255) NOT NULL,
  `offer_description` BLOB NOT NULL,
  `offer_price` DOUBLE NOT NULL,
  `currency_code` VARCHAR(25) NOT NULL,
  `locale` VARCHAR(25) NOT NULL,
  `transactionid` BIGINT NOT NULL,
  `paid_amount` DOUBLE NULL,
  `googlewallet_id` VARCHAR(255) NULL,
  `created_on` TIMESTAMP DEFAULT 0,
  `updated_on` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                       ON UPDATE CURRENT_TIMESTAMP,
  INDEX `indx_billing_user` (`userid`),
  INDEX `indx_billing_wallet` (`googlewallet_id`)
) ENGINE=InnoDB;

# --- !Downs

DROP TABLE `Billings`;