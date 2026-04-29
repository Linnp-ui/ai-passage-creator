package com.yupi.template.service.impl;

import com.google.gson.reflect.TypeToken;
import com.yupi.template.mapper.ArticleStateSnapshotMapper;
import com.yupi.template.model.dto.article.ArticleState;
import com.yupi.template.model.entity.ArticleStateSnapshot;
import com.yupi.template.model.enums.ArticleGenerationCheckpoint;
import com.yupi.template.service.ArticleStateManager;
import com.yupi.template.utils.GsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 文章状态管理服务实现
 *
 * @author AI Passage Creator
 */
@Service
@Slf4j
public class ArticleStateManagerImpl implements ArticleStateManager {

    @Resource
    private ArticleStateSnapshotMapper snapshotMapper;

    @Override
    public Long saveSnapshot(String taskId, ArticleState state, ArticleGenerationCheckpoint checkpoint) {
        return saveSnapshot(taskId, state, checkpoint, checkpoint.getProgressPercent(), null);
    }

    @Override
    public Long saveSnapshot(String taskId, ArticleState state, ArticleGenerationCheckpoint checkpoint,
                            int progressPercent, String currentAgent) {
        try {
            ArticleStateSnapshot snapshot = convertToSnapshot(taskId, state, checkpoint, progressPercent, currentAgent);
            snapshotMapper.insert(snapshot);
            log.info("状态快照已保存, taskId={}, checkpoint={}, progress={}%", 
                    taskId, checkpoint.getValue(), progressPercent);
            return snapshot.getId();
        } catch (Exception e) {
            log.error("保存状态快照失败, taskId={}, checkpoint={}", taskId, checkpoint.getValue(), e);
            return null;
        }
    }

    @Override
    public ArticleStateSnapshot loadLatestSnapshot(String taskId) {
        return snapshotMapper.selectLatestByTaskId(taskId);
    }

    @Override
    public ArticleState restoreFromSnapshot(ArticleStateSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }

        ArticleState state = new ArticleState();
        state.setTaskId(snapshot.getTaskId());
        state.setTopic(snapshot.getTopic());
        state.setStyle(snapshot.getStyle());
        state.setUserDescription(snapshot.getUserDescription());
        state.setPhase(snapshot.getPhase());

        // 恢复配图方式
        if (snapshot.getEnabledImageMethods() != null) {
            state.setEnabledImageMethods(GsonUtils.fromJson(
                    snapshot.getEnabledImageMethods(),
                    new TypeToken<List<String>>() {}
            ));
        }

        // 恢复标题方案
        if (snapshot.getTitleOptions() != null) {
            state.setTitleOptions(GsonUtils.fromJson(
                    snapshot.getTitleOptions(),
                    new TypeToken<List<ArticleState.TitleOption>>() {}
            ));
        }

        // 恢复标题
        if (snapshot.getMainTitle() != null) {
            ArticleState.TitleResult title = new ArticleState.TitleResult();
            title.setMainTitle(snapshot.getMainTitle());
            title.setSubTitle(snapshot.getSubTitle());
            state.setTitle(title);
        }

        // 恢复大纲
        if (snapshot.getOutline() != null) {
            ArticleState.OutlineResult outline = new ArticleState.OutlineResult();
            List<ArticleState.OutlineSection> sections = GsonUtils.fromJson(
                    snapshot.getOutline(),
                    new TypeToken<List<ArticleState.OutlineSection>>() {}
            );
            outline.setSections(sections != null ? sections : Collections.emptyList());
            state.setOutline(outline);
        }

        // 恢复正文
        state.setContent(snapshot.getContent());

        // 恢复配图需求
        if (snapshot.getImageRequirements() != null) {
            state.setImageRequirements(GsonUtils.fromJson(
                    snapshot.getImageRequirements(),
                    new TypeToken<List<ArticleState.ImageRequirement>>() {}
            ));
        }

        // 恢复配图结果
        if (snapshot.getImages() != null) {
            state.setImages(GsonUtils.fromJson(
                    snapshot.getImages(),
                    new TypeToken<List<ArticleState.ImageResult>>() {}
            ));
        }

        // 恢复完整内容
        state.setFullContent(snapshot.getFullContent());

        log.info("状态已从快照恢复, taskId={}, phase={}, checkpoint={}",
                snapshot.getTaskId(), snapshot.getPhase(), snapshot.getCheckpoint());

        return state;
    }

    @Override
    public ArticleState restoreFromLatestSnapshot(String taskId) {
        ArticleStateSnapshot snapshot = loadLatestSnapshot(taskId);
        return restoreFromSnapshot(snapshot);
    }

    @Override
    public boolean hasRecoverableState(String taskId) {
        ArticleStateSnapshot snapshot = loadLatestSnapshot(taskId);
        return snapshot != null && !ArticleGenerationCheckpoint.GENERATION_COMPLETE.getValue()
                .equals(snapshot.getCheckpoint());
    }

    @Override
    public void clearSnapshots(String taskId) {
        int count = snapshotMapper.deleteByTaskId(taskId);
        log.info("已清理任务的所有快照, taskId={}, count={}", taskId, count);
    }

    @Override
    public int getProgressPercent(String taskId) {
        ArticleStateSnapshot snapshot = loadLatestSnapshot(taskId);
        return snapshot != null && snapshot.getProgressPercent() != null 
                ? snapshot.getProgressPercent() : 0;
    }

    /**
     * 将ArticleState转换为快照实体
     */
    private ArticleStateSnapshot convertToSnapshot(String taskId, ArticleState state,
                                                    ArticleGenerationCheckpoint checkpoint,
                                                    int progressPercent, String currentAgent) {
        // 如果 phase 为 null，使用 checkpoint 对应的阶段
        String phase = state.getPhase();
        if (phase == null || phase.isEmpty()) {
            phase = checkpoint.getValue();
        }
        return ArticleStateSnapshot.builder()
                .taskId(taskId)
                .phase(phase)
                .checkpoint(checkpoint.getValue())
                .topic(state.getTopic())
                .style(state.getStyle())
                .userDescription(state.getUserDescription())
                .enabledImageMethods(state.getEnabledImageMethods() != null 
                        ? GsonUtils.toJson(state.getEnabledImageMethods()) : null)
                .titleOptions(state.getTitleOptions() != null 
                        ? GsonUtils.toJson(state.getTitleOptions()) : null)
                .mainTitle(state.getTitle() != null ? state.getTitle().getMainTitle() : null)
                .subTitle(state.getTitle() != null ? state.getTitle().getSubTitle() : null)
                .outline(state.getOutline() != null && state.getOutline().getSections() != null
                        ? GsonUtils.toJson(state.getOutline().getSections()) : null)
                .content(state.getContent())
                .imageRequirements(state.getImageRequirements() != null
                        ? GsonUtils.toJson(state.getImageRequirements()) : null)
                .images(state.getImages() != null ? GsonUtils.toJson(state.getImages()) : null)
                .fullContent(state.getFullContent())
                .progressPercent(progressPercent)
                .currentAgent(currentAgent)
                .build();
    }
}
