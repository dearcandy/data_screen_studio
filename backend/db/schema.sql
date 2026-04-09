-- Data Screen Studio MySQL DDL
-- Recommended charset/collation: utf8mb4 / utf8mb4_unicode_ci

CREATE DATABASE IF NOT EXISTS `data_screen_studio`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `data_screen_studio`;

CREATE TABLE IF NOT EXISTS `ds_data_source` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255) NOT NULL,
  `type` VARCHAR(32) NOT NULL,
  `config_json` TEXT NOT NULL,
  `remark` VARCHAR(512) NULL,
  `created_at` DATETIME(6) NULL,
  `updated_at` DATETIME(6) NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ds_data_source_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `ds_data_set` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255) NOT NULL,
  `data_source_id` BIGINT NULL,
  `fetch_mode` VARCHAR(16) NOT NULL,
  `fetch_spec` TEXT NULL,
  `mock_json` TEXT NULL,
  `script_text` TEXT NULL,
  `public_token` VARCHAR(64) NOT NULL,
  `enabled` BIT(1) NOT NULL,
  `created_at` DATETIME(6) NULL,
  `updated_at` DATETIME(6) NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ds_data_set_public_token` (`public_token`),
  KEY `idx_ds_data_set_data_source_id` (`data_source_id`),
  KEY `idx_ds_data_set_fetch_mode` (`fetch_mode`),
  CONSTRAINT `fk_ds_data_set_data_source`
    FOREIGN KEY (`data_source_id`) REFERENCES `ds_data_source` (`id`)
    ON DELETE SET NULL
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `ds_pipeline` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255) NOT NULL,
  `description` VARCHAR(1024) NULL,
  `definition_json` LONGTEXT NOT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'draft',
  `creator` VARCHAR(128) NULL,
  `public_token` VARCHAR(64) NOT NULL,
  `external_enabled` BIT(1) NOT NULL DEFAULT b'0',
  `created_at` DATETIME(6) NULL,
  `updated_at` DATETIME(6) NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ds_pipeline_public_token` (`public_token`),
  KEY `idx_ds_pipeline_status` (`status`),
  KEY `idx_ds_pipeline_external_enabled` (`external_enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `ds_pipeline_execution` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `pipeline_id` BIGINT NOT NULL,
  `execution_id` VARCHAR(64) NOT NULL,
  `trigger_type` VARCHAR(32) NULL,
  `input_params` LONGTEXT NULL,
  `status` VARCHAR(32) NOT NULL,
  `output_result` LONGTEXT NULL,
  `error_msg` TEXT NULL,
  `start_time` DATETIME(6) NULL,
  `end_time` DATETIME(6) NULL,
  `node_details_json` LONGTEXT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ds_pipeline_execution_execution_id` (`execution_id`),
  KEY `idx_ds_pipeline_execution_pipeline_id` (`pipeline_id`),
  KEY `idx_ds_pipeline_execution_status` (`status`),
  CONSTRAINT `fk_ds_pipeline_execution_pipeline`
    FOREIGN KEY (`pipeline_id`) REFERENCES `ds_pipeline` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
