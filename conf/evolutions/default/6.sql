# --- !Ups

CREATE TABLE `Transactions`(
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `status` VARCHAR(25) NOT NULL,
  `offer_title` VARCHAR(255) NOT NULL,
  `offer_description` BLOB NOT NULL,
  `offer_price` DOUBLE NOT NULL,
  `currency_code` VARCHAR(25) NOT NULL,
  `locale` VARCHAR(25) NOT NULL,
  `buyerid` BIGINT NOT NULL,
  `sellerid` BIGINT NOT NULL,
  `offerid` BIGINT NOT NULL,
  `created_on` TIMESTAMP DEFAULT 0,
  `updated_on` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                       ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

# --- !Downs

DROP TABLE `Transactions`;