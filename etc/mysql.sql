SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL';

DROP SCHEMA IF EXISTS `cloauth` ;
CREATE SCHEMA IF NOT EXISTS `cloauth` DEFAULT CHARACTER SET latin1 ;
USE `cloauth` ;

-- -----------------------------------------------------
-- Table `cloauth`.`users`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `cloauth`.`users` (
  `id` BIGINT NOT NULL AUTO_INCREMENT ,
  `verifiedEmail` VARCHAR(50) CHARACTER SET 'utf8' NOT NULL ,
  `userName` VARCHAR(50) NOT NULL ,
  `firstName` VARCHAR(45) NULL ,
  `lastName` VARCHAR(45) NULL ,
  `displayName` VARCHAR(45) NULL ,
  `roles` VARCHAR(45) NULL COMMENT 'space delim listof roles \ntodo: make this a relation\n\n' ,
  `language` VARCHAR(10) NULL DEFAULT 'en-GB' ,
  UNIQUE INDEX `verifiedemail_UNIQUE` (`verifiedEmail` ASC) ,
  PRIMARY KEY (`id`) ,
  UNIQUE INDEX `userName_UNIQUE` (`userName` ASC) )
ENGINE = InnoDB
AUTO_INCREMENT = 5
DEFAULT CHARACTER SET = latin1;


-- -----------------------------------------------------
-- Table `cloauth`.`clients`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `cloauth`.`clients` (
  `id` VARCHAR(32) NOT NULL ,
  `clientSecret` VARCHAR(32) NOT NULL ,
  `orgName` VARCHAR(45) NULL ,
  `description` VARCHAR(128) NULL ,
  `redirectUri` VARCHAR(256) NULL COMMENT 'Space delimited list of registered URIs\n' ,
  `users_id` BIGINT NOT NULL ,
  PRIMARY KEY (`id`) ,
  UNIQUE INDEX `clientid_UNIQUE` (`id` ASC) ,
  CONSTRAINT `fk1`
    FOREIGN KEY (`users_id` )
    REFERENCES `cloauth`.`users` (`id` )
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `cloauth`.`grant`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `cloauth`.`grant` (
  `id` BIGINT NOT NULL AUTO_INCREMENT ,
  `clients_id` VARCHAR(32) NOT NULL ,
  `users_id` BIGINT NOT NULL ,
  `expiry` BIGINT UNSIGNED NULL ,
  `refreshToken` VARCHAR(32) NULL ,
  PRIMARY KEY (`id`) ,
  INDEX `fk_grant_2` (`users_id` ASC) ,
  UNIQUE INDEX `refreshToken_UNIQUE` (`refreshToken` ASC) ,
  INDEX `fk_grant_1` (`clients_id` ASC) ,
  CONSTRAINT `fk_grant_2`
    FOREIGN KEY (`users_id` )
    REFERENCES `cloauth`.`users` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_grant_1`
    FOREIGN KEY (`clients_id` )
    REFERENCES `cloauth`.`clients` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `cloauth`.`scope`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `cloauth`.`scope` (
  `id` INT NOT NULL AUTO_INCREMENT ,
  `uri` VARCHAR(128) NOT NULL ,
  `description` VARCHAR(128) NULL ,
  PRIMARY KEY (`id`) ,
  UNIQUE INDEX `uri_UNIQUE` (`uri` ASC) ,
  UNIQUE INDEX `id_UNIQUE` (`id` ASC) )
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `cloauth`.`grant_scope`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `cloauth`.`grant_scope` (
  `grant_id` BIGINT NOT NULL ,
  `scope_id` INT NOT NULL ,
  PRIMARY KEY (`grant_id`, `scope_id`) ,
  INDEX `fk_grant_scope_1` (`scope_id` ASC) ,
  INDEX `fk_grant_scope_2` (`grant_id` ASC) ,
  CONSTRAINT `fk_grant_scope_1`
    FOREIGN KEY (`scope_id` )
    REFERENCES `cloauth`.`scope` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_grant_scope_2`
    FOREIGN KEY (`grant_id` )
    REFERENCES `cloauth`.`grant` (`id` )
    ON DELETE CASCADE
    ON UPDATE NO ACTION)
ENGINE = InnoDB
COMMENT = 'join table for finding out which scopes are granted\n\n';



SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
