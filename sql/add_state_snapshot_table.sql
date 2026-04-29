-- ========================================
-- 文章生成阶段状态快照表
-- 用于保存和恢复文章生成过程中的中间状态
-- ========================================

CREATE TABLE IF NOT EXISTS article_state_snapshot (
    -- 主键
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    
    -- 任务标识
    task_id VARCHAR(64) NOT NULL COMMENT '任务ID',
    phase VARCHAR(32) NOT NULL COMMENT '当前阶段',
    checkpoint VARCHAR(32) NOT NULL COMMENT '检查点类型',
    
    -- 输入参数
    topic TEXT COMMENT '选题',
    style VARCHAR(32) COMMENT '文章风格',
    user_description TEXT COMMENT '用户补充描述',
    enabled_image_methods TEXT COMMENT '允许的配图方式列表(JSON)',
    
    -- 阶段1: 标题生成状态
    title_options TEXT COMMENT '标题方案列表(JSON)',
    main_title VARCHAR(500) COMMENT '选中的主标题',
    sub_title VARCHAR(500) COMMENT '选中的副标题',
    
    -- 阶段2: 大纲生成状态
    outline TEXT COMMENT '大纲内容(JSON)',
    
    -- 阶段3: 正文生成状态
    content TEXT COMMENT '正文内容(Markdown)',
    content_with_placeholders TEXT COMMENT '带占位符的正文',
    image_requirements TEXT COMMENT '配图需求列表(JSON)',
    images TEXT COMMENT '配图结果列表(JSON)',
    full_content TEXT COMMENT '完整图文内容',
    
    -- 生成进度
    progress_percent INT DEFAULT 0 COMMENT '生成进度百分比',
    current_agent VARCHAR(32) COMMENT '当前执行的智能体',
    
    -- 元数据
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    -- 索引
    INDEX idx_task_id (task_id),
    INDEX idx_phase (phase),
    INDEX idx_created_at (created_at)
    
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章生成阶段状态快照表';
