# --- !Ups

CREATE TABLE `Ratings`(
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `rating` INT NOT NULL,
  `userid` BIGINT NOT NULL,
  `by_userid` BIGINT NOT NULL,
  `transid` BIGINT NOT NULL,
  `feedbackid` BIGINT NOT NULL,
  `created_on` TIMESTAMP DEFAULT 0,
  `updated_on` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                       ON UPDATE CURRENT_TIMESTAMP,
  INDEX `indx_transid_rating` (`transid`),
  INDEX `indx_byuser_rating` (`by_userid`),
  INDEX `indx_user_rating` (`userid`),
  CONSTRAINT `fk_feedback_rating`
  FOREIGN KEY (`feedbackid`) REFERENCES `Feedbacks`(`id`)
    ON UPDATE CASCADE
    ON DELETE CASCADE
) ENGINE=InnoDB;

# --- !Downs

DROP TABLE `Ratings`;