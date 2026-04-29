package com.yupi.template.service.impl;

import com.yupi.template.exception.BusinessException;
import com.yupi.template.exception.ErrorCode;
import com.yupi.template.model.dto.article.ArticleState;
import com.yupi.template.model.entity.Article;
import com.yupi.template.model.entity.ArticleStateSnapshot;
import com.yupi.template.model.entity.User;
import com.yupi.template.model.enums.ArticleGenerationCheckpoint;
import com.yupi.template.model.enums.ArticlePhaseEnum;
import com.yupi.template.model.enums.ArticleStatusEnum;
import com.yupi.template.service.ArticleAsyncService;
import com.yupi.template.service.ArticleResumeService;
import com.yupi.template.service.ArticleService;
import com.yupi.template.service.ArticleStateManager;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 文章续生成服务实现
 *
 * @author AI Passage Creator
 */
@Service
@Slf4j
public class ArticleResumeServiceImpl implements ArticleResumeService {

    @Resource
    private ArticleService articleService;

    @Resource
    private ArticleStateManager articleStateManager;

    @Resource
    private ArticleAsyncService articleAsyncService;

    @Override
    public boolean canResume(String taskId, User loginUser) {
        Article article = articleService.getByTaskId(taskId);
        if (article == null) {
            return false;
        }

        // 校验权限
        if (!article.getUserId().equals(loginUser.getId())) {
            return false;
        }

        // 已完成的文章不能续生成
        if (ArticleStatusEnum.COMPLETED.getValue().equals(article.getStatus())) {
            return false;
        }

        // 检查是否有可恢复的状态
        return articleStateManager.hasRecoverableState(taskId);
    }

    @Override
    public ResumeInfo getResumeInfo(String taskId, User loginUser) {
        ResumeInfo info = new ResumeInfo();

        Article article = articleService.getByTaskId(taskId);
        if (article == null) {
            info.setCanResume(false);
            info.setMessage("文章不存在");
            return info;
        }

        // 校验权限
        if (!article.getUserId().equals(loginUser.getId())) {
            info.setCanResume(false);
            info.setMessage("无权访问该文章");
            return info;
        }

        // 已完成
        if (ArticleStatusEnum.COMPLETED.getValue().equals(article.getStatus())) {
            info.setCanResume(false);
            info.setMessage("文章已生成完成");
            return info;
        }

        ArticleStateSnapshot snapshot = articleStateManager.loadLatestSnapshot(taskId);
        if (snapshot == null) {
            info.setCanResume(false);
            info.setMessage("没有找到可恢复的状态");
            return info;
        }

        info.setCanResume(true);
        info.setCurrentPhase(article.getPhase());

        ArticlePhaseEnum phaseEnum = ArticlePhaseEnum.getByValue(article.getPhase());
        info.setPhaseDescription(phaseEnum != null ? phaseEnum.getDescription() : "未知阶段");

        info.setProgressPercent(snapshot.getProgressPercent() != null ? snapshot.getProgressPercent() : 0);
        info.setLastCheckpoint(snapshot.getCheckpoint());

        ArticleGenerationCheckpoint checkpoint = ArticleGenerationCheckpoint.getByValue(snapshot.getCheckpoint());
        info.setMessage(checkpoint != null ? checkpoint.getDescription() : "准备续生成");

        return info;
    }

    @Override
    public boolean resumeArticle(String taskId, User loginUser) {
        Article article = articleService.getByTaskId(taskId);
        if (article == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文章不存在");
        }

        // 校验权限
        if (!article.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 已完成的文章不能续生成
        if (ArticleStatusEnum.COMPLETED.getValue().equals(article.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "文章已生成完成，无需续生成");
        }

        // 加载最新状态
        ArticleState state = articleStateManager.restoreFromLatestSnapshot(taskId);
        if (state == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "没有找到可恢复的状态");
        }

        String currentPhase = article.getPhase();
        log.info("开始续生成文章, taskId={}, currentPhase={}", taskId, currentPhase);

        // 根据当前阶段决定从哪个阶段继续
        ArticlePhaseEnum phase = ArticlePhaseEnum.getByValue(currentPhase);
        if (phase == null) {
            phase = ArticlePhaseEnum.PENDING;
        }

        switch (phase) {
            case PENDING:
            case TITLE_GENERATING:
                // 从阶段1开始
                articleAsyncService.executePhase1(taskId, article.getTopic(), article.getStyle());
                break;

            case TITLE_SELECTING:
                // 等待用户选择标题，不需要续生成
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "请先选择标题");

            case OUTLINE_GENERATING:
                // 从阶段2开始
                if (article.getMainTitle() == null) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "标题信息缺失，无法续生成");
                }
                articleAsyncService.executePhase2(taskId);
                break;

            case OUTLINE_EDITING:
                // 等待用户编辑大纲
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "请先编辑并确认大纲");

            case CONTENT_GENERATING:
                // 从阶段3开始
                if (article.getOutline() == null || article.getOutline().isEmpty()) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "大纲信息缺失，无法续生成");
                }
                articleAsyncService.executePhase3(taskId);
                break;

            default:
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "未知阶段，无法续生成");
        }

        return true;
    }
}
