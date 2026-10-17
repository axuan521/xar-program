-- ----------------------------
-- AI 知识库（RAG）相关表结构
-- 依赖：Spring AI 2.0.1 向量检索 + 文档入库链路
-- ----------------------------

-- 1、知识库文档主表
drop table if exists kb_document;
create table kb_document (
  doc_id            bigint(20)      not null auto_increment    comment '文档ID',
  doc_name          varchar(200)    not null                   comment '原始文件名',
  store_path        varchar(500)    not null                   comment '存储相对路径（相对 ruoyi.profile）',
  file_type         varchar(20)     default ''                 comment '文件扩展名（pdf/docx/xlsx/pptx/txt/md）',
  file_size         bigint(20)      default 0                  comment '文件大小（字节）',
  status            char(1)         default '0'                comment '状态（0处理中 2就绪 3失败）',
  failure_reason    varchar(500)    default null               comment '失败原因',
  chunk_count       int(11)         default 0                  comment '切片数量',
  content_chars     int(11)         default 0                  comment '解析出的字符数',
  del_flag          char(1)         default '0'                comment '删除标志（0代表存在 2代表删除）',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (doc_id),
  key idx_kb_doc_status (status, create_time)
) engine=innodb auto_increment=100 comment = '知识库文档表';

-- 2、知识库切片表（向量库元数据，vector_id 与 SimpleVectorStore 中的记录一一对应）
drop table if exists kb_chunk;
create table kb_chunk (
  chunk_id          bigint(20)      not null auto_increment    comment '切片ID',
  doc_id            bigint(20)      not null                   comment '所属文档ID',
  chunk_index       int(11)         not null                   comment '文档内切片序号',
  vector_id         varchar(64)     default null               comment '向量库记录ID（删除/对账依据）',
  embedding_status  char(1)         default '0'                comment '向量化状态（0未向量化 1完成 2失败）',
  token_count       int(11)         default 0                  comment '估算token数',
  content           mediumtext                                 comment '切片原文（引用展示用）',
  create_time       datetime                                   comment '创建时间',
  primary key (chunk_id),
  unique key uk_kb_chunk (doc_id, chunk_index),
  key idx_kb_chunk_vector (vector_id),
  key idx_kb_chunk_status (embedding_status)
) engine=innodb auto_increment=100 comment = '知识库切片表';

-- ----------------------------
-- 菜单 SQL（知识库管理，menu_id 使用 2100 段，避开现有数据与 auto_increment=2000）
-- admin 角色自带全部权限；其他角色请在“角色管理”中勾选对应菜单
-- ----------------------------
insert into sys_menu values('2100', '知识库管理', '0', '9', 'kb', null, '', '', 1, 0, 'M', '0', '0', '', 'form', 'admin', sysdate(), '', null, '知识库目录');
insert into sys_menu values('2101', '文档管理', '2100', '1', 'document', 'kb/document/index', '', '', 1, 0, 'C', '0', '0', 'kb:document:list', 'documentation', 'admin', sysdate(), '', null, '知识库文档管理菜单');
insert into sys_menu values('2102', '文档查询', '2101', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'kb:document:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2103', '文档上传', '2101', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'kb:document:upload', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2104', '文档删除', '2101', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'kb:document:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2105', '重新入库', '2101', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'kb:document:retry', '#', 'admin', sysdate(), '', null, '');
