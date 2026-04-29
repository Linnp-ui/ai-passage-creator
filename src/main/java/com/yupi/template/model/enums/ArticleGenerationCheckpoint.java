package com.yupi.template.model.enums;

import lombok.Getter;

/**
 * 文章生成检查点枚举
 * 定义生成过程中需要保存状态的关键节点
 *
 * @author AI Passage Creator
 */
@Getter
public enum ArticleGenerationCheckpoint {

    // 阶段1: 标题生成检查点
    TITLE_OPTIONS_GENERATED("TITLE_OPTIONS_GENERATED", "标题方案已生成", 10),
    TITLE_CONFIRMED("TITLE_CONFIRMED", "标题已确认", 15),

    // 阶段2: 大纲生成检查点
    OUTLINE_GENERATED("OUTLINE_GENERATED", "大纲已生成", 30),
    OUTLINE_CONFIRMED("OUTLINE_CONFIRMED", "大纲已确认", 35),

    // 阶段3: 正文生成检查点
    CONTENT_GENERATING("CONTENT_GENERATING", "正文生成中", 40),
    CONTENT_STREAM_PROGRESS("CONTENT_STREAM_PROGRESS", "正文流式生成进度", 50),
    CONTENT_GENERATED("CONTENT_GENERATED", "正文已生成", 60),

    // 阶段3: 配图生成检查点
    IMAGE_REQUIREMENTS_ANALYZED("IMAGE_REQUIREMENTS_ANALYZED", "配图需求已分析", 65),
    IMAGES_GENERATING("IMAGES_GENERATING", "配图生成中", 70),
    IMAGE_PARTIAL_COMPLETE("IMAGE_PARTIAL_COMPLETE", "部分配图完成", 80),
    IMAGES_ALL_COMPLETE("IMAGES_ALL_COMPLETE", "所有配图完成", 90),

    // 阶段3: 图文合成检查点
    CONTENT_MERGED("CONTENT_MERGED", "图文已合成", 95),

    // 完成
    GENERATION_COMPLETE("GENERATION_COMPLETE", "生成完成", 100);

    private final String value;
    private final String description;
    private final int progressPercent;

    ArticleGenerationCheckpoint(String value, String description, int progressPercent) {
        this.value = value;
        this.description = description;
        this.progressPercent = progressPercent;
    }

    public static ArticleGenerationCheckpoint getByValue(String value) {
        if (value == null) {
            return null;
        }
        for (ArticleGenerationCheckpoint checkpoint : values()) {
            if (checkpoint.getValue().equals(value)) {
                return checkpoint;
            }
        }
        return null;
    }
}
