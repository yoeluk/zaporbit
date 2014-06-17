# --- !Ups

CREATE TABLE `Friends`(
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `userid` BIGINT NOT NULL,
  `friendid` BIGINT NOT NULL,
  INDEX `indx_friend_friendid` (`friendid`),
  CONSTRAINT `fk_friend_user`
  FOREIGN KEY (`userid`) REFERENCES `Users`(`id`)
    ON UPDATE CASCADE
    ON DELETE CASCADE
) ENGINE=InnoDB;

# --- !Downs

DROP TABLE `Friends`;