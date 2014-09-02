# --- !Ups

CREATE TABLE `OAuth2s`(
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `userid` BIGINT NOT NULL,
  `accessToken` VARCHAR(255) NOT NULL,
  `tokenType` VARCHAR(255) NULL,
  `expiresIn` INT NULL,
  `refreshToken` VARCHAR(255) NULL,
  CONSTRAINT `fk_OAuth2_user`
  FOREIGN KEY (`userid`) REFERENCES `Users`(`fbuserid`)
    ON UPDATE CASCADE
    ON DELETE CASCADE
) ENGINE=InnoDB;

# --- !Downs

DROP TABLE `OAuth2s`;