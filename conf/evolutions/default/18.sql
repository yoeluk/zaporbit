# --- !Ups

CREATE TABLE `UserOptions`(
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `userid` BIGINT NOT NULL,
  `about` TEXT NULL,
  `background` VARCHAR(250) NULL,
  `picture` VARCHAR(250) NULL,
  CONSTRAINT `fk_options_user`
  FOREIGN KEY (`userid`) REFERENCES `Users`(`id`)
    ON UPDATE CASCADE
    ON DELETE CASCADE
) ENGINE=InnoDB;

# --- !Downs

DROP TABLE `UserOptions`;