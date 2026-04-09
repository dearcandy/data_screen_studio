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

-- ---------------------------------------------------------------------------
-- 演示用业务表：与本库同一 schema，供「MYSQL」数据源 + 数据集 LIVE / Pipeline fetch 节点测试
-- （查询仅允许 SELECT，与运行时 JdbcFetchConstants 一致）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ds_demo_sales` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `region` VARCHAR(64) NOT NULL COMMENT '区域',
  `product` VARCHAR(128) NOT NULL COMMENT '商品',
  `qty` INT NOT NULL DEFAULT 0 COMMENT '数量',
  `amount` DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '金额',
  `biz_date` DATE NULL COMMENT '业务日期',
  PRIMARY KEY (`id`),
  KEY `idx_ds_demo_sales_region` (`region`),
  KEY `idx_ds_demo_sales_biz_date` (`biz_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='预置测试销售明细，供 MySQL 取数演示';

-- ---------------------------------------------------------------------------
-- 预置测试数据（数据源 / 数据集 / 流程）
-- 主键使用 801+ 段，减少与手工新建记录冲突；若冲突可先删除对应行再执行本节。
-- MySQL 连接与 application.yml 默认一致：127.0.0.1:3308，库名 data_screen_studio，用户/密码 root/root
-- 若本机端口或密码不同，请改 ds_data_source id=801 的 config_json。
-- ---------------------------------------------------------------------------

INSERT INTO `ds_demo_sales` (`id`, `region`, `product`, `qty`, `amount`, `biz_date`) VALUES
  (1, '华东', '笔记本电脑', 3, 15499.00, '2025-03-01'),
  (2, '华东', '无线鼠标', 20, 1998.00, '2025-03-02'),
  (3, '华北', '显示器', 5, 8995.00, '2025-03-02'),
  (4, '华北', '键盘', 12, 3588.00, '2025-03-05'),
  (5, '华南', '笔记本电脑', 1, 4999.00, '2025-03-06'),
  (6, '华南', '扩展坞', 8, 2392.00, '2025-03-07')
ON DUPLICATE KEY UPDATE
  `region` = VALUES(`region`),
  `product` = VALUES(`product`),
  `qty` = VALUES(`qty`),
  `amount` = VALUES(`amount`),
  `biz_date` = VALUES(`biz_date`);

INSERT INTO `ds_data_source` (`id`, `name`, `type`, `config_json`, `remark`, `created_at`, `updated_at`) VALUES
  (801, '【预置】本库 MySQL（演示表 ds_demo_sales）', 'MYSQL',
   '{"host":"127.0.0.1","port":3308,"database":"data_screen_studio","username":"root","password":"root"}',
   '与 backend/src/main/resources/application.yml 默认 MYSQL_* 一致；改端口/密码请同步改此 JSON', NOW(6), NOW(6)),
  (802, '【预置】Mock 数据源', 'MOCK',
   '{"mock":{"service":"MOCK","status":"ok","hint":"用于数据源类型连通性/占位"}}',
   'MOCK 类型数据集取数不读此字段，见数据集 mock_json', NOW(6), NOW(6))
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `type` = VALUES(`type`),
  `config_json` = VALUES(`config_json`),
  `remark` = VALUES(`remark`),
  `updated_at` = VALUES(`updated_at`);

INSERT INTO `ds_data_set` (`id`, `name`, `data_source_id`, `fetch_mode`, `fetch_spec`, `mock_json`, `script_text`, `public_token`, `enabled`, `created_at`, `updated_at`) VALUES
  (801, '【预置】Mock 城市指标', NULL, 'MOCK', NULL,
   '[{"city":"北京","metric":"访客","value":120},{"city":"上海","metric":"访客","value":98},{"city":"广州","metric":"订单","value":45}]',
   'return { items: input, sum: input.reduce((a, x) => a + (x.value || 0), 0) };',
   '11111111111111111111111111111111', 1, NOW(6), NOW(6)),
  (802, '【预置】LIVE 销售表明细', 801, 'LIVE',
   'SELECT id, region, product, qty, amount, biz_date FROM ds_demo_sales ORDER BY id',
   NULL,
   'return { source: "ds_demo_sales", rowCount: input.length, rows: input };',
   '22222222222222222222222222222222', 1, NOW(6), NOW(6)),
  (803, '【预置】LIVE 按区域汇总', 801, 'LIVE',
   'SELECT region, SUM(qty) AS total_qty, SUM(amount) AS total_amount FROM ds_demo_sales GROUP BY region ORDER BY region',
   NULL,
   'return { aggregates: input };',
   'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 1, NOW(6), NOW(6))
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `data_source_id` = VALUES(`data_source_id`),
  `fetch_mode` = VALUES(`fetch_mode`),
  `fetch_spec` = VALUES(`fetch_spec`),
  `mock_json` = VALUES(`mock_json`),
  `script_text` = VALUES(`script_text`),
  `public_token` = VALUES(`public_token`),
  `enabled` = VALUES(`enabled`),
  `updated_at` = VALUES(`updated_at`);

INSERT INTO `ds_pipeline` (`id`, `name`, `description`, `definition_json`, `status`, `creator`, `public_token`, `external_enabled`, `created_at`, `updated_at`) VALUES
  (801, '【预置】MySQL 取数 → 脚本 → 输出',
   '测试 fetch 节点 + script + output；依赖数据源 801 与表 ds_demo_sales',
   '{"nodes":[{"id":"n_fetch","type":"fetch","dependsOn":[],"config":{"dataSourceId":801,"fetchSpec":"SELECT id, region, product, qty, amount, biz_date FROM ds_demo_sales ORDER BY id"}},{"id":"n_script","type":"script","dependsOn":["n_fetch"],"config":{"language":"javascript","source":"return { rows: input, rowCount: (input && input.length) ? input.length : 0 };"}},{"id":"n_out","type":"output","dependsOn":["n_script"],"config":{"format":"json"}}]}',
   'published', 'seed',
   '33333333333333333333333333333333', 1, NOW(6), NOW(6)),
  (802, '【预置】数据集节点（Mock）',
   '测试 dataSet 节点执行预置数据集 801',
   '{"nodes":[{"id":"n_ds","type":"dataSet","dependsOn":[],"config":{"dataSetId":801}},{"id":"n_sc","type":"script","dependsOn":["n_ds"],"config":{"language":"javascript","source":"return { echo: true, payload: input };"}},{"id":"n_ou","type":"output","dependsOn":["n_sc"],"config":{"format":"json"}}]}',
   'draft', 'seed',
   '44444444444444444444444444444444', 0, NOW(6), NOW(6)),
  (803, '【预置】并行分支（parallel）',
   'fetch 后 parallel 两个 script 分支，结果 mergeToList；分支内无 dependsOn，脚本不依赖 input',
   '{"nodes":[{"id":"nf","type":"fetch","dependsOn":[],"config":{"dataSourceId":801,"fetchSpec":"SELECT COUNT(*) AS cnt FROM ds_demo_sales"}},{"id":"np","type":"parallel","dependsOn":["nf"],"config":{"mergeStrategy":"mergeToList","branches":[{"id":"b1","type":"script","config":{"language":"javascript","source":"return {branch:1};"}},{"id":"b2","type":"script","config":{"language":"javascript","source":"return {branch:2};"}}]}},{"id":"no","type":"output","dependsOn":["np"],"config":{"format":"json"}}]}',
   'published', 'seed',
   '55555555555555555555555555555555', 1, NOW(6), NOW(6))
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `description` = VALUES(`description`),
  `definition_json` = VALUES(`definition_json`),
  `status` = VALUES(`status`),
  `creator` = VALUES(`creator`),
  `public_token` = VALUES(`public_token`),
  `external_enabled` = VALUES(`external_enabled`),
  `updated_at` = VALUES(`updated_at`);
