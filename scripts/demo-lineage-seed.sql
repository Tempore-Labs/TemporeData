-- Demo seed for the lineage walkthrough / local boot (idempotent).
-- Creates a demo tenant + admin user (admin/admin123) + role link, and the
-- source data that `POST /api/lineage/rebuild` consumes:
--   temporedata_workflow_lineage  -> task-level READ/WRITE edges
--   temporedata_meta_table        -> standalone table nodes

INSERT IGNORE INTO zy_tenant (id, name) VALUES ('t-demo', 'Demo Tenant');

-- Password hash is BCrypt of "admin123"
INSERT IGNORE INTO zy_user (id, username, password, real_name, status, tenant_id, nickname)
  VALUES ('u-admin', 'admin', '$2a$10$8Qbm7a7KgmrMUUcHG5uU2ecxSi9ADlsAYRdVBHQQ6rxvRr340fSVq', 'Admin', '1', 't-demo', 'Admin');
INSERT IGNORE INTO zy_user_role (user_id, role_id) VALUES ('u-admin', 'role-admin');
-- ADMIN role gets lineage write (frontend also short-circuits via user.admin)
INSERT IGNORE INTO res_permission (id, role_id, resource_type, resource_key, action, scope)
  VALUES ('perm-lineage-admin', 'role-admin', 'LINEAGE', '*', 'WRITE', 'ALLOW');

INSERT INTO temporedata_workflow_lineage (id, workflow_id, node_id, node_name, source_table, target_table, sql_type, dialect) VALUES
  ('wl-001', 'wf-001', 'task-001', 'Load Sales Daily',   'ods_sales',       'dws_sales_daily',  'INSERT', 'MYSQL'),
  ('wl-002', 'wf-002', 'task-002', 'Aggregate Customer 360', 'dws_sales_daily', 'ads_customer_360', 'INSERT', 'MYSQL'),
  ('wl-003', 'wf-003', 'task-003', 'Feed Product Dim',   'dws_sales_daily', 'dim_product',      'INSERT', 'MYSQL')
ON DUPLICATE KEY UPDATE node_name = VALUES(node_name);

INSERT INTO temporedata_meta_table (id, datasource_id, schema_name, table_name, table_comment, tags, status, lineage_count) VALUES
  ('mt-001', 'ds-sales', 'ods', 'ods_sales',             'sales raw',    'PII,Critical', 'ACTIVE', 1),
  ('mt-002', 'ds-sales', 'dws', 'dws_sales_daily',       'daily fact',   'SLA-99.9',     'ACTIVE', 3),
  ('mt-003', 'ds-mkt',   'dim', 'dim_promotion',          'promotion dim','',            'ACTIVE', 0),
  ('mt-004', 'ds-mkt',   'ads', 'ads_marketing_report',   'mkt report',   '',            'ACTIVE', 0)
ON DUPLICATE KEY UPDATE table_name = VALUES(table_name);