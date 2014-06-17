# --- !Ups

CREATE TABLE `Users`(
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(255) NOT NULL,
  `surname` VARCHAR(255) NOT NULL,
  `fbuserid` BIGINT NOT NULL,
  `email` VARCHAR(255) NOT NULL,
  `isMerchant` BOOL NOT NULL,
  `created_on` TIMESTAMP DEFAULT 0,
  UNIQUE KEY `uk_fbuserid` (`fbuserid`)
) ENGINE=InnoDB;

INSERT INTO `Users` VALUES (21435, 'Chris', 'X', 111111, 'email@example.com', false, NOW());
INSERT INTO `Users` VALUES (21436, 'Mark', 'Y', 111113, 'email2@example.com', false, NOW());

# --- !Downs

DROP TABLE `Users`;


