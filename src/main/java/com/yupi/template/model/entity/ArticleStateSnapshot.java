package com.yupi.template.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文章生成阶段状态快照实体
 * 用于保存和恢复文章生成过程中的中间状态
 *
 * @author AI Passage Creator
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(value = "article_state_snapshot", camelToUnderline = true)
public class ArticleStateSnapshot implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    private String taskId;

    private String phase;

    private String checkpoint;

    private String topic;

    private String style;

    private String userDescription;

    private String enabledImageMethods;

    private String titleOptions;

    private String mainTitle;

    private String subTitle;

    private String outline;

    private String content;

    private String contentWithPlaceholders;

    private String imageRequirements;

    private String images;

    private String fullContent;

    private Integer progressPercent;

    private String currentAgent;

    private LocalDateTime createdAt;

    @Column(onInsertValue = "now()", onUpdateValue = "now()")
    private LocalDateTime updatedAt;
}
