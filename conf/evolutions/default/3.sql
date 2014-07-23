# --- !Ups

CREATE TABLE `Offers`(
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `title` VARCHAR(255) NOT NULL,
  `description` BLOB NOT NULL,
  `price` DOUBLE NOT NULL,
  `locale` VARCHAR(25) NOT NULL,
  `currency_code` VARCHAR(25) NOT NULL,
  `shop` VARCHAR(255) NOT NULL,
  `highlight` BOOLEAN NOT NULL,
  `waggle` BOOLEAN NOT NULL,
  `telephone` VARCHAR(255) NULL,
  `userid` BIGINT NOT NULL,
  `locationid` BIGINT NULL,
  `created_on` TIMESTAMP DEFAULT 0,
  `updated_on` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        ON UPDATE CURRENT_TIMESTAMP,
  INDEX `indx_offers_title` (`title`),
  INDEX `indx_offers_shop` (`shop`),
  CONSTRAINT `fk_offer_user`
  FOREIGN KEY (`userid`) REFERENCES `Users`(`id`)
    ON UPDATE CASCADE
    ON DELETE CASCADE
) ENGINE=InnoDB;

# --- !Downs

DROP TABLE `Offers`;