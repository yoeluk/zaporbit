# --- !Ups

CREATE TABLE `Conversations`(
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `user1_status` VARCHAR(20) NOT NULL,
  `user2_status` VARCHAR(20) NOT NULL,
  `user1id` BIGINT NOT NULL,
  `user2id` BIGINT NOT NULL,
  `title` VARCHAR(256) NOT NULL,
  `offerid` BIGINT NULL,
  `created_on` TIMESTAMP DEFAULT 0,
  `updated_on` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                       ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

# --- !Downs

DROP TABLE `Conversations`;