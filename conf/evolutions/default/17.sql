# --- !Ups

CREATE TABLE `ListingStatuses`(
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `status` VARCHAR(25) NOT NULL,
  `offerid` BIGINT NOT NULL,
  `created_on` TIMESTAMP DEFAULT 0,
  `updated_on` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                       ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

# --- !Downs

DROP TABLE `ListingStatuses`;