# --- !Ups

CREATE TABLE `Billings`(
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `status` VARCHAR(40) NOT NULL,
  `userid` BIGINT NOT NULL,
  `offer_title` VARCHAR(255) NOT NULL,
  `offer_description` BLOB NOT NULL,
  `offer_price` DOUBLE NOT NULL,
  `currency_code` VARCHAR(50) NOT NULL,
  `locale` VARCHAR(50) NOT NULL,
  `transactionid` BIGINT NOT NULL,
  `amount` DOUBLE NOT NULL,
  `payment_provider` VARCHAR(255) NULL,
  `pay_id` VARCHAR(255) NULL,
  `created_on` TIMESTAMP DEFAULT 0,
  `updated_on` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                       ON UPDATE CURRENT_TIMESTAMP,
  INDEX `indx_billing_user` (`userid`)
) ENGINE=InnoDB;

# --- !Downs

DROP TABLE `Billings`;