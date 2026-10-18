-- ----------------------------
-- 扫码点单小程序 相关表结构
-- 支付设计说明：
--   个人主体小程序无法开通微信支付（官方仅对个体工商户/企业等开放），
--   故 pay_type 默认 '1'=到店付款（顾客扫门店收款码付款，店员手动核销）；
--   待办理个体工商户并取得商户号后，新增 '2'=微信支付 实现即可，
--   订单表、状态机、前端页面均无需改动（pay_no/pay_time 字段已预留）。
-- ----------------------------

-- 1、商品分类表
drop table if exists shop_category;
create table shop_category (
  category_id       bigint(20)      not null auto_increment    comment '分类ID',
  category_name     varchar(50)     not null                   comment '分类名称',
  order_num         int(4)          default 0                  comment '显示顺序',
  status            char(1)         default '0'                comment '状态（0正常 1停用）',
  del_flag          char(1)         default '0'                comment '删除标志（0代表存在 2代表删除）',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (category_id),
  key idx_shop_category_order (order_num, status)
) engine=innodb auto_increment=100 comment = '点单商品分类表';

-- 2、商品表
drop table if exists shop_product;
create table shop_product (
  product_id        bigint(20)      not null auto_increment    comment '商品ID',
  category_id       bigint(20)      not null                   comment '所属分类',
  product_name      varchar(100)    not null                   comment '商品名称',
  product_image     varchar(500)    default ''                 comment '商品图片（存相对路径）',
  price             decimal(10,2)   not null default 0.00      comment '售价（元）',
  description       varchar(500)    default ''                 comment '商品描述',
  order_num         int(4)          default 0                  comment '显示顺序',
  is_hot            char(1)         default '0'                comment '是否热销（0否 1是）',
  is_available      char(1)         default '1'                comment '是否可售（0售罄/下架 1可售）',
  status            char(1)         default '0'                comment '状态（0正常 1停用）',
  del_flag          char(1)         default '0'                comment '删除标志（0代表存在 2代表删除）',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (product_id),
  key idx_shop_product_cat (category_id, order_num),
  key idx_shop_product_status (status, del_flag)
) engine=innodb auto_increment=100 comment = '点单商品表';

-- 3、商品口味规格表
-- 一个商品可配多组口味（如"温度""糖度""加料"），每组内多个选项；
-- is_price_add=1 的选项表示加价项（如加珍珠 +2 元），下单时累加。
drop table if exists shop_product_flavor;
create table shop_product_flavor (
  flavor_id         bigint(20)      not null auto_increment    comment '口味ID',
  product_id        bigint(20)      not null                   comment '所属商品',
  group_name        varchar(50)     not null                   comment '口味组名（温度/糖度/加料）',
  flavor_name       varchar(50)     not null                   comment '口味选项名（少冰/三分糖/加珍珠）',
  extra_price       decimal(10,2)   default 0.00               comment '加价金额（0为不加价）',
  is_default        char(1)         default '0'                comment '是否默认选中（0否 1是）',
  order_num         int(4)          default 0                  comment '组内显示顺序',
  group_order_num   int(4)          default 0                  comment '组显示顺序',
  status            char(1)         default '0'                comment '状态（0正常 1停用）',
  primary key (flavor_id),
  key idx_shop_flavor_product (product_id, group_order_num, order_num)
) engine=innodb auto_increment=100 comment = '商品口味规格表';

-- 4、小程序顾客表
-- 与后台 sys_user 完全隔离：顾客无后台账号，仅有 openid 身份标识。
-- 注意：个人主体小程序无法调用 getPhoneNumber，phone 字段仅企业主体可用。
drop table if exists wx_user;
create table wx_user (
  wx_user_id        bigint(20)      not null auto_increment    comment '顾客ID',
  openid            varchar(64)     not null                   comment '微信openid',
  nick_name         varchar(64)     default ''                 comment '微信昵称',
  avatar_url        varchar(500)    default ''                 comment '头像地址',
  phone             varchar(20)     default ''                 comment '手机号（个人主体取不到，预留）',
  order_count       int(11)         default 0                  comment '累计下单数',
  last_login_time   datetime                                   comment '最后登录时间',
  status            char(1)         default '0'                comment '状态（0正常 1停用）',
  create_time       datetime                                   comment '创建时间',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (wx_user_id),
  unique key uk_wx_user_openid (openid)
) engine=innodb auto_increment=100 comment = '小程序顾客表';

-- 5、订单主表
-- 取餐码规则：每日从 1 开始自增（pickup_date + pickup_no 联合唯一），
-- 现场叫号只需报 3 位数字，不会与历史订单冲突。
drop table if exists shop_order;
create table shop_order (
  order_id          bigint(20)      not null auto_increment    comment '订单ID',
  order_no          varchar(32)     not null                   comment '订单编号（业务流水号）',
  wx_user_id        bigint(20)      not null                   comment '下单顾客ID',
  openid            varchar(64)     not null                   comment '下单顾客openid（冗余，便于查询）',
  total_amount      decimal(10,2)   not null default 0.00      comment '订单总额（元）',
  pay_type          char(1)         default '1'                comment '支付方式（1到店付款 2微信支付）',
  pay_status        char(1)         default '0'                comment '支付状态（0未支付 1已支付 2已退款）',
  pay_no            varchar(64)     default null               comment '支付流水号（微信支付回填，到店付可为空）',
  pay_time          datetime                                   comment '支付时间',
  order_status      char(1)         default '0'                comment '订单状态（0待付款 1待制作 2制作中 3待取餐 4已完成 5已取消）',
  pickup_date       date                                       comment '取餐日期（配合 pickup_no 生成当日号码）',
  pickup_no         int(11)         default 0                  comment '当日取餐序号（1开始自增）',
  pickup_code       varchar(10)     default ''                 comment '取餐码（冗余存储，便于查询展示）',
  verify_time       datetime                                   comment '核销/收款确认时间',
  verify_by         varchar(64)     default ''                 comment '核销操作人',
  finish_time       datetime                                   comment '完成时间',
  customer_remark   varchar(200)    default ''                 comment '顾客备注（少辣、打包等）',
  cancel_reason     varchar(200)    default ''                 comment '取消原因',
  del_flag          char(1)         default '0'                comment '删除标志（0代表存在 2代表删除）',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '下单时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (order_id),
  unique key uk_shop_order_no (order_no),
  unique key uk_shop_order_pickup (pickup_date, pickup_no),
  key idx_shop_order_user (wx_user_id, create_time),
  key idx_shop_order_status (order_status, create_time),
  key idx_shop_order_pickup_code (pickup_code)
) engine=innodb auto_increment=100 comment = '点单订单表';

-- 6、订单明细表
-- 商品名与单价在此快照存储：商品日后改价或下架，历史订单金额不受影响。
drop table if exists shop_order_item;
create table shop_order_item (
  item_id           bigint(20)      not null auto_increment    comment '明细ID',
  order_id          bigint(20)      not null                   comment '所属订单',
  product_id        bigint(20)      not null                   comment '商品ID',
  product_name      varchar(100)    not null                   comment '商品名称快照',
  product_image     varchar(500)    default ''                 comment '商品图片快照',
  unit_price        decimal(10,2)   not null default 0.00      comment '下单时单价快照（元）',
  flavor_text       varchar(200)    default ''                 comment '口味描述（少冰/三分糖/加珍珠）',
  flavor_amount     decimal(10,2)   default 0.00               comment '口味加价合计（元）',
  quantity          int(11)         not null default 1         comment '数量',
  subtotal          decimal(10,2)   not null default 0.00      comment '小计（单价+加价）×数量',
  create_time       datetime                                   comment '创建时间',
  primary key (item_id),
  key idx_shop_item_order (order_id),
  key idx_shop_item_product (product_id)
) engine=innodb auto_increment=100 comment = '订单明细表';

-- ----------------------------
-- 初始化演示数据（可按需删除）
-- ----------------------------
insert into shop_category values('100', '经典奶茶', '1', '0', '0', 'admin', sysdate(), '', null, '奶茶系列');
insert into shop_category values('101', '手冲咖啡', '2', '0', '0', 'admin', sysdate(), '', null, '咖啡系列');
insert into shop_category values('102', '轻食小点', '3', '0', '0', 'admin', sysdate(), '', null, '搭配小食');

insert into shop_product values('100', '100', '招牌珍珠奶茶', '', '15.00', '锡兰红茶底，手煮黑糖珍珠', 1, '1', '1', '0', '0', 'admin', sysdate(), '', null, '');
insert into shop_product values('101', '100', '茉莉奶绿',     '', '16.00', '茉莉绿茶搭配淡奶，清爽不腻', 2, '0', '1', '0', '0', 'admin', sysdate(), '', null, '');
insert into shop_product values('102', '101', '美式咖啡',     '', '12.00', '深烘豆，无糖',             1, '0', '1', '0', '0', 'admin', sysdate(), '', null, '');
insert into shop_product values('103', '102', '海盐芝士吐司', '', '18.00', '现烤，芝士拉丝',           1, '1', '1', '0', '0', 'admin', sysdate(), '', null, '');

-- 珍珠奶茶的口味：温度 / 糖度 / 加料（加料为加价项）
insert into shop_product_flavor values('100', '100', '温度', '常温',   '0.00', '1', '1', '1', '0');
insert into shop_product_flavor values('101', '100', '温度', '少冰',   '0.00', '0', '2', '1', '0');
insert into shop_product_flavor values('102', '100', '温度', '去冰',   '0.00', '0', '3', '1', '0');
insert into shop_product_flavor values('103', '100', '温度', '热',     '0.00', '0', '4', '1', '0');
insert into shop_product_flavor values('104', '100', '糖度', '全糖',   '0.00', '1', '1', '2', '0');
insert into shop_product_flavor values('105', '100', '糖度', '七分糖', '0.00', '0', '2', '2', '0');
insert into shop_product_flavor values('106', '100', '糖度', '半糖',   '0.00', '0', '3', '2', '0');
insert into shop_product_flavor values('107', '100', '糖度', '三分糖', '0.00', '0', '4', '2', '0');
insert into shop_product_flavor values('108', '100', '糖度', '无糖',   '0.00', '0', '5', '2', '0');
insert into shop_product_flavor values('109', '100', '加料', '加珍珠', '2.00', '0', '1', '3', '0');
insert into shop_product_flavor values('110', '100', '加料', '加椰果', '2.00', '0', '2', '3', '0');
insert into shop_product_flavor values('111', '100', '加料', '加布丁', '3.00', '0', '3', '3', '0');

-- 美式的口味：温度（咖啡无糖度选项）
insert into shop_product_flavor values('112', '102', '温度', '冰',   '0.00', '1', '1', '1', '0');
insert into shop_product_flavor values('113', '102', '温度', '热',   '0.00', '0', '2', '1', '0');
insert into shop_product_flavor values('114', '102', '加料', '加奶', '1.00', '0', '1', '2', '0');

-- ----------------------------
-- 菜单 SQL（点单管理，menu_id 使用 2200 段，避开知识库 2100 段）
-- ----------------------------
insert into sys_menu values('2200', '点单管理', '0', '10', 'shop', null, '', '', 1, 0, 'M', '0', '0', '', 'shopping', 'admin', sysdate(), '', null, '扫码点单管理目录');

insert into sys_menu values('2201', '商品分类', '2200', '1', 'category', 'shop/category/index', '', '', 1, 0, 'C', '0', '0', 'shop:category:list', 'tree-table', 'admin', sysdate(), '', null, '商品分类菜单');
insert into sys_menu values('2202', '分类查询', '2201', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'shop:category:query',  '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2203', '分类新增', '2201', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'shop:category:add',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2204', '分类修改', '2201', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'shop:category:edit',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2205', '分类删除', '2201', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'shop:category:remove', '#', 'admin', sysdate(), '', null, '');

insert into sys_menu values('2210', '商品管理', '2200', '2', 'product', 'shop/product/index', '', '', 1, 0, 'C', '0', '0', 'shop:product:list', 'documentation', 'admin', sysdate(), '', null, '商品管理菜单');
insert into sys_menu values('2211', '商品查询', '2210', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'shop:product:query',  '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2212', '商品新增', '2210', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'shop:product:add',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2213', '商品修改', '2210', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'shop:product:edit',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2214', '商品删除', '2210', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'shop:product:remove', '#', 'admin', sysdate(), '', null, '');

insert into sys_menu values('2220', '订单管理', '2200', '3', 'order', 'shop/order/index', '', '', 1, 0, 'C', '0', '0', 'shop:order:list', 'form', 'admin', sysdate(), '', null, '订单管理菜单');
insert into sys_menu values('2221', '订单查询', '2220', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'shop:order:query',  '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2222', '订单核销', '2220', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'shop:order:verify', '#', 'admin', sysdate(), '', null, '确认收款/核销取餐码');
insert into sys_menu values('2223', '状态流转', '2220', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'shop:order:status', '#', 'admin', sysdate(), '', null, '制作中/待取餐/完成');
insert into sys_menu values('2224', '订单取消', '2220', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'shop:order:cancel', '#', 'admin', sysdate(), '', null, '');

insert into sys_menu values('2230', '小程序顾客', '2200', '4', 'wxuser', 'shop/wxuser/index', '', '', 1, 0, 'C', '0', '0', 'shop:wxuser:list', 'peoples', 'admin', sysdate(), '', null, '小程序顾客菜单');
insert into sys_menu values('2231', '顾客查询', '2230', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'shop:wxuser:query', '#', 'admin', sysdate(), '', null, '');
