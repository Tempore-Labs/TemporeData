-- =============================================================================
-- TemporeData 平台功能测试数据种子（幂等）
-- 目标库 temporedata_dev。重新执行安全（INSERT IGNORE，已存在则跳过）。
-- 密码统一复用 admin 的 BCrypt 哈希 => 测试用户登录密码与 admin 相同。
-- =============================================================================

-- ---------- 系统 / 权限 ----------
INSERT IGNORE INTO zy_role (id, code, name, remark, protected_role, data_scope, create_time)
VALUES
 ('r-analyst',   'analyst',   '数据分析师', '查看分析权限', 0, 'self', NOW()),
 ('r-developer', 'developer', '数据开发',   '开发与调度权限', 0, 'dept', NOW()),
 ('r-viewer',    'viewer',    '只读用户',    '只读访问', 0, 'self', NOW());

INSERT IGNORE INTO zy_tenant (id, name, description, status, created_at, updated_at)
VALUES
 ('t-retail',  '零售事业部', '零售业务数据域', '1', NOW(), NOW()),
 ('t-finance', '财务部',     '财务数据域',     '1', NOW(), NOW());

-- 测试用户（密码 = admin 密码），用 admin 行的哈希保证可登录
INSERT IGNORE INTO zy_user (id, username, password, real_name, nickname, email, phone, status, tenant_id, created_at, updated_at)
SELECT 'u-test1', 'test1', password, '测试员一', 'Test One', 'test1@local.dev', '13800000001', '1', 't-retail', NOW(), NOW() FROM zy_user WHERE username='admin';
INSERT IGNORE INTO zy_user (id, username, password, real_name, nickname, email, phone, status, tenant_id, created_at, updated_at)
SELECT 'u-test2', 'test2', password, '测试员二', 'Test Two', 'test2@local.dev', '13800000002', '1', 't-finance', NOW(), NOW() FROM zy_user WHERE username='admin';
INSERT IGNORE INTO zy_user (id, username, password, real_name, nickname, email, phone, status, tenant_id, created_at, updated_at)
SELECT 'u-test3', 'viewer3', password, '查看员', 'Viewer',  'viewer3@local.dev','13800000003','1','default', NOW(), NOW() FROM zy_user WHERE username='admin';

INSERT IGNORE INTO zy_user_role (user_id, role_id) VALUES ('u-test1','r-analyst'),('u-test1','r-developer'),('u-test2','r-viewer'),('u-test3','r-viewer');

INSERT IGNORE INTO zy_secret (id, tenant_id, zy_secret.`key`, zy_secret.`value`, description, scope, create_date_time, create_by)
VALUES ('sec-001','default','app.secret.demo','demo-secret-value','演示密钥','global', NOW(6),'admin');

-- ---------- 数据源 ----------
INSERT IGNORE INTO zy_datasource (id, name, type, host, port, database_name, username, password, params, status, tenant_id, created_at, updated_at)
VALUES
 ('ds-mysql-1','MySQL-业务库','MySQL','mysql-prod.internal',3306,'salesdb','ruser','ENC(demo)', '{"charset":"utf8mb4","maxPool":8}', '1','default',NOW(),NOW()),
 ('ds-hive-1','Hive-数仓','Hive','hive.internal',10000,'default','huser','ENC(demo)', '{"auth":"LDAP"}', '1','default',NOW(),NOW()),
 ('ds-bq-1','BigQuery-分析','BigQuery','my-gcp-project',0,'eu',NULL,NULL, '{"location":"EU"}', '1','default',NOW(),NOW());

-- ---------- 元数据 / 列级 ----------
INSERT IGNORE INTO zy_meta_column (id, table_id, datasource_id, table_name, column_name, column_type, column_size, nullable, default_value, comment, primary_key, ordinal_position, sensitive_flag, data_level_code, status)
VALUES
 ('mcol-01','t1','ds-hive-1','finance.ods_orders','order_id','bigint',20,0,'NULL','订单ID',1,1,0,'L1','1'),
 ('mcol-02','t1','ds-hive-1','finance.ods_orders','user_id','bigint',20,0,'NULL','用户ID',0,2,0,'L1','1'),
 ('mcol-03','t1','ds-hive-1','finance.ods_orders','phone','varchar',20,1,'NULL','联系电话',0,3,1,'L3','1'),
 ('mcol-04','t2','ds-bq-1','bigquery_marts.dws_sales_daily','gmv','decimal',18,0,'0','成交额',0,1,0,'L2','1');

-- ---------- 数据开发 / SQL 查询 ----------
INSERT IGNORE INTO zy_query (id, tenant_id, datasource_name, sql_text, status, duration_ms, create_by, create_time, row_count, datasource_id)
VALUES
 ('q-001','default','MySQL-业务库','SELECT order_id, user_id, amount FROM finance.ods_orders LIMIT 10','SUCCESS',120,'test1','2026-09-01 10:00:00',10,'ds-mysql-1'),
 ('q-002','default','Hive-数仓','SELECT gmv FROM bigquery_marts.dws_sales_daily','SUCCESS',340,'test1','2026-09-01 10:05:00',64,'ds-hive-1'),
 ('q-003','default','BigQuery-分析','SELECT * FROM missing_table','FAILED',60,'test1','2026-09-01 10:06:00',0,'ds-bq-1');

-- ---------- 运行中心 / 调度任务 ----------
INSERT IGNORE INTO zy_task_define (id, name, task_type, target_ref, cron_expression, biz_date_mode, timezone, enabled, params_json, owner, status, version, create_by, created_at, updated_at)
VALUES
 ('task-001','订单同步','SQL','finance.ods_orders','0 0 1 * * ?','TODAY','Asia/Shanghai',1,'{"sql":"INSERT INTO finance.ods_orders ..."}','admin','enabled',1,'admin',NOW(),NOW()),
 ('task-002','GMV日汇总','SQL','bigquery_marts.dws_sales_daily','0 30 2 * * ?','TODAY','Asia/Shanghai',1,'{"sql":"CREATE TABLE bigquery_marts.dws_sales_daily ..."}','admin','enabled',1,'admin',NOW(),NOW());

INSERT IGNORE INTO zy_task_instance (id, task_id, instance_no, trigger_time, start_time, finish_time, biz_date, status, result_msg, version, created_at)
VALUES
 ('ti-001','task-001',1,NOW(),NOW(),NOW(),'2026-09-01','SUCCESS','ok',1,NOW()),
 ('ti-002','task-002',1,NOW(),NOW(),NOW(),'2026-09-01','RUNNING','running',1,NOW());

INSERT IGNORE INTO zy_task_log (id, instance_id, level, message, created_at)
VALUES ('tl-001','ti-001','INFO','task started',NOW()),('tl-002','ti-001','INFO','task finished',NOW());

-- ---------- 数据开发 / 工作流 ----------
INSERT IGNORE INTO zy_workflow (id, name, description, status, cron_expression, schedule_enabled, tenant_id, created_at, updated_at)
VALUES ('wf-001','订单数仓流程','ods->dws->ads 跨库血缘流程','RUNNING','0 0 2 * * ?',1,'t-retail',NOW(),NOW());

INSERT IGNORE INTO zy_workflow_node (id, workflow_id, name, type, script, config, position_x, position_y, sort_order, created_at, updated_at)
VALUES
 ('wfn-01','wf-001','同步ODS','sync',   'INSERT INTO finance.ods_orders ...','{}',100,100,1,NOW(),NOW()),
 ('wfn-02','wf-001','构建DWS','sql',    'INSERT INTO bigquery_marts.dws_sales_daily ...','{}',200,100,2,NOW(),NOW()),
 ('wfn-03','wf-001','发布报表','report', 'SELECT * FROM bigquery_marts.ads_gmv_top','{}',300,100,3,NOW(),NOW());

INSERT IGNORE INTO zy_workflow_edge (id, workflow_id, source_node_id, target_node_id, created_at)
VALUES
 ('wfe-01','wf-001','wfn-01','wfn-02',NOW()),
 ('wfe-02','wf-001','wfn-02','wfn-03',NOW());

-- ---------- 数据服务 / API ----------
INSERT IGNORE INTO zy_data_api (id, tenant_id, name, description, datasource_id, `sql`, method, path, status, api_key, create_by, create_time)
VALUES
 ('api-001','default','订单查询API','按ID查订单', 'ds-mysql-1','SELECT * FROM finance.ods_orders WHERE order_id = #{id}','GET','/api/orders/detail','ONLINE','ak-order-demo','admin','2026-09-01 12:00:00'),
 ('api-002','default','GMV报表API','查询成交额', 'ds-bq-1','SELECT report_date, gmv FROM bigquery_marts.ads_gmv_top','GET','/api/report/gmv','ONLINE','ak-gmv-demo','admin','2026-09-01 12:00:00');

-- ---------- 治理 / 数据脱敏 ----------
INSERT IGNORE INTO zy_mask_rule (id, name, rule_type, mask_pattern, description, datasource_id, table_name, column_name, status, create_time)
VALUES
 ('mask-001','手机号脱敏','HIDE_MIDDLE','138****0001','手机号中段加掩码','ds-mysql-1','finance.ods_orders','phone',1,NOW()),
 ('mask-002','身份证脱敏','HIDE_ALL','********','全掩码','ds-mysql-1','finance.ods_users','id_card',1,NOW());