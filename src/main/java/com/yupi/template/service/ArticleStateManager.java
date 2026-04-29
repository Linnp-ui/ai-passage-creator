package com.yupi.template.service;

import com.yupi.template.model.dto.article.ArticleState;
import com.yupi.template.model.entity.ArticleStateSnapshot;
import com.yupi.template.model.enums.ArticleGenerationCheckpoint;

/**
 * 文章状态管理服务
 * 负责保存、恢复和清理文章生成过程中的状态快照
 *
 * @author AI Passage Creator
 */
public interface ArticleStateManager {

    /**
     * 保存状态快照
     *
     * @param taskId     任务ID
     * @param state      当前状态
     * @param checkpoint 检查点类型
     * @return 保存的快照ID
     */
    Long saveSnapshot(String taskId, ArticleState state, ArticleGenerationCheckpoint checkpoint);

    /**
     * 保存状态快照（带进度）
     *
     * @param taskId          任务ID
     * @param state           当前状态
     * @param checkpoint      检查点类型
     * @param progressPercent 进度百分比
     * @param currentAgent    当前执行的智能体
     * @return 保存的快照ID
     */
    Long saveSnapshot(String taskId, ArticleState state, ArticleGenerationCheckpoint checkpoint,
                      int progressPercent, String currentAgent);

    /**
     * 加载最新状态快照
     *
     * @param taskId 任务ID
     * @return 最新的状态快照，如果不存在返回null
     */
    ArticleStateSnapshot loadLatestSnapshot(String taskId);

    /**
     * 从快照恢复ArticleState
     *
     * @param snapshot 状态快照
     * @return 恢复的ArticleState
     */
    ArticleState restoreFromSnapshot(ArticleStateSnapshot snapshot);

    /**
     * 从最新快照恢复状态
     *
     * @param taskId 任务ID
     * @return 恢复的ArticleState，如果不存在返回null
     */
    ArticleState restoreFromLatestSnapshot(String taskId);

    /**
     * 检查是否存在可恢复的状态
     *
     * @param taskId 任务ID
     * @return 是否存在可恢复的状态
     */
    boolean hasRecoverableState(String taskId);

    /**
     * 清理任务的所有快照
     *
     * @param taskId 任务ID
     */
    void clearSnapshots(String taskId);

    /**
     * 获取任务当前进度
     *
     * @param taskId 任务ID
     * @return 进度百分比，如果没有快照返回0
     */
    int getProgressPercent(String taskId);
}
