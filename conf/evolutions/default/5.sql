# --- !Ups

CREATE TABLE `Locations`(
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `street` VARCHAR(255) NOT NULL,
  `locality` VARCHAR(255) NOT NULL,
  `adminArea` VARCHAR(255) NOT NULL,
  `latitude` DOUBLE NOT NULL,
  `longitude` DOUBLE NOT NULL,
  `offerid` BIGINT NOT NULL,
  `created_on` TIMESTAMP DEFAULT 0,
  `updated_on` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                      ON UPDATE CURRENT_TIMESTAMP,
  INDEX `indx_locations_city` (`locality`),
  INDEX `indx_locations_area` (`adminArea`),
  CONSTRAINT `fk_location_offer`
  FOREIGN KEY (`offerid`) REFERENCES `Offers`(`id`)
    ON UPDATE CASCADE
    ON DELETE CASCADE
) ENGINE=InnoDB;

# --- !Downs

DROP TABLE `Locations`;
