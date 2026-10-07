ALTER TABLE store
  ADD COLUMN home_title VARCHAR(64) NULL COMMENT '顾客首页头图主标题，空则不展示以免与导航店名重复' AFTER home_kicker;
