package com.yupi.template.service;

import com.yupi.template.model.entity.User;

/**
 * 文章续生成服务
 * 处理从断点恢复文章生成的逻辑
 *
 * @author AI Passage Creator
 */
public interface ArticleResumeService {

    /**
     * 检查文章是否可以续生成
     *
     * @param taskId   任务ID
     * @param loginUser 当前用户
     * @return 是否可以续生成
     */
    boolean canResume(String taskId, User loginUser);

    /**
     * 获取文章续生成信息
     *
     * @param taskId   任务ID
     * @param loginUser 当前用户
     * @return 续生成信息，包含当前阶段、进度等
     */
    ResumeInfo getResumeInfo(String taskId, User loginUser);

    /**
     * 续生成文章
     * 从上次中断的阶段继续生成
     *
     * @param taskId   任务ID
     * @param loginUser 当前用户
     * @return 是否成功启动续生成
     */
    boolean resumeArticle(String taskId, User loginUser);

    /**
     * 续生成信息DTO
     */
    class ResumeInfo {
        private boolean canResume;
        private String currentPhase;
        private String phaseDescription;
        private int progressPercent;
        private String lastCheckpoint;
        private String message;

        public boolean isCanResume() { return canResume; }
        public void setCanResume(boolean canResume) { this.canResume = canResume; }
        public String getCurrentPhase() { return currentPhase; }
        public void setCurrentPhase(String currentPhase) { this.currentPhase = currentPhase; }
        public String getPhaseDescription() { return phaseDescription; }
        public void setPhaseDescription(String phaseDescription) { this.phaseDescription = phaseDescription; }
        public int getProgressPercent() { return progressPercent; }
        public void setProgressPercent(int progressPercent) { this.progressPercent = progressPercent; }
        public String getLastCheckpoint() { return lastCheckpoint; }
        public void setLastCheckpoint(String lastCheckpoint) { this.lastCheckpoint = lastCheckpoint; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
