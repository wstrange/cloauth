SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL';

CREATE SCHEMA IF NOT EXISTS `cloauth` DEFAULT CHARACTER SET latin1 ;
USE `cloauth` ;

-- -----------------------------------------------------
-- Table `cloauth`.`users`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `cloauth`.`users` (
  `id` BIGINT NOT NULL AUTO_INCREMENT ,
  `verifiedemail` VARCHAR(50) CHARACTER SET 'utf8' NOT NULL ,
  `registrationDate` TIMESTAMP NULL DEFAULT NULL ,
  `username` VARCHAR(50) NULL ,
  `firstname` VARCHAR(45) NULL ,
  `lastname` VARCHAR(45) NULL ,
  `displayname` VARCHAR(45) NULL ,
  `roles` VARCHAR(45) NULL COMMENT 'space delim listof roles \ntodo: make this a relation\n\n' ,
  UNIQUE INDEX `verifiedemail_UNIQUE` (`verifiedemail` ASC) ,
  PRIMARY KEY (`id`) )
ENGINE = InnoDB
AUTO_INCREMENT = 5
DEFAULT CHARACTER SET = latin1;


-- -----------------------------------------------------
-- Table `cloauth`.`clients`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `cloauth`.`clients` (
  `id` BIGINT NOT NULL AUTO_INCREMENT ,
  `clientid` VARCHAR(32) NOT NULL ,
  `clientsecret` VARCHAR(45) NOT NULL ,
  `orgname` VARCHAR(45) NULL ,
  `description` VARCHAR(128) NULL ,
  `returnuris` VARCHAR(256) NULL COMMENT 'Space delimited list of registered URIs\n' ,
  `userid` BIGINT NULL ,
  PRIMARY KEY (`id`) ,
  UNIQUE INDEX `clientid_UNIQUE` (`clientid` ASC) ,
  UNIQUE INDEX `id_UNIQUE` (`id` ASC) ,
  CONSTRAINT `fk1`
    FOREIGN KEY (`userid` )
    REFERENCES `cloauth`.`users` (`id` )
    ON DELETE NO ACTION
    ON UPDATE CASCADE)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `cloauth`.`grant`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `cloauth`.`grant` (
  `id` BIGINT NOT NULL ,
  `clientid` BIGINT NULL ,
  `userid` BIGINT NULL ,
  PRIMARY KEY (`id`) ,
  INDEX `fk_grant_1` (`clientid` ASC) ,
  INDEX `fk_grant_2` (`userid` ASC) ,
  CONSTRAINT `fk_grant_1`
    FOREIGN KEY (`clientid` )
    REFERENCES `cloauth`.`clients` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_grant_2`
    FOREIGN KEY (`userid` )
    REFERENCES `cloauth`.`users` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `cloauth`.`tokens`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `cloauth`.`tokens` (
  `token` VARCHAR(32) NOT NULL ,
  `grantid` BIGINT NULL ,
  `expiry` TIMESTAMP NULL ,
  `tokentype` CHAR(1) NULL ,
  PRIMARY KEY (`token`) ,
  INDEX `fk_tokens_1` (`grantid` ASC) ,
  CONSTRAINT `fk_tokens_1`
    FOREIGN KEY (`grantid` )
    REFERENCES `cloauth`.`grant` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `cloauth`.`scopes`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `cloauth`.`scopes` (
  `id` INT NOT NULL AUTO_INCREMENT ,
  `uri` VARCHAR(128) NULL ,
  `description` VARCHAR(45) NULL ,
  PRIMARY KEY (`id`) ,
  UNIQUE INDEX `uri_UNIQUE` (`uri` ASC) ,
  UNIQUE INDEX `id_UNIQUE` (`id` ASC) )
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `cloauth`.`grant_scopes`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `cloauth`.`grant_scopes` (
  `grantid` BIGINT NOT NULL ,
  `scopeid` INT NOT NULL ,
  PRIMARY KEY (`grantid`, `scopeid`) ,
  INDEX `fk_grant_scopes_1` (`grantid` ASC) ,
  INDEX `fk_grant_scopes_2` (`scopeid` ASC) ,
  CONSTRAINT `fk_grant_scopes_1`
    FOREIGN KEY (`grantid` )
    REFERENCES `cloauth`.`grant` (`id` )
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_grant_scopes_2`
    FOREIGN KEY (`scopeid` )
    REFERENCES `cloauth`.`scopes` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB
COMMENT = 'join table for finding out which scopes are granted\n\n';



SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;

-- -----------------------------------------------------
-- Data for table `cloauth`.`users`
-- -----------------------------------------------------
START TRANSACTION;
USE `cloauth`;
INSERT INTO `cloauth`.`users` (`id`, `verifiedemail`, `registrationDate`, `username`, `firstname`, `lastname`, `displayname`, `roles`) VALUES (NULL, 'test@test.com', '', 'test@test.com', 'test', 'tester', 'testy tester', NULL);

COMMIT;
