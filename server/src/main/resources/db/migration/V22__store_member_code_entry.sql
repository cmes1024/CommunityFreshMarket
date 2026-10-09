ALTER TABLE store
  ADD COLUMN member_code_entry VARCHAR(16) NOT NULL DEFAULT 'PROFILE'
    COMMENT '顾客会员码入口：PROFILE 我的 | TAB 底栏正中' AFTER product_detail_mode;
