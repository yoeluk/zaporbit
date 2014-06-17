# --- !Ups

CREATE TABLE `Pictures`(
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(255) NOT NULL,
  `offerid` BIGINT NOT NULL,
  CONSTRAINT `fk_picture_offer`
  FOREIGN KEY (`offerid`) REFERENCES `Offers`(`id`)
    ON UPDATE CASCADE
    ON DELETE CASCADE
) ENGINE=InnoDB;

# --- !Downs

DROP TABLE `Pictures`;