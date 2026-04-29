package com.yupi.template.mapper;

import com.mybatisflex.core.BaseMapper;
import com.yupi.template.model.entity.ArticleStateSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 文章状态快照 Mapper
 *
 * @author AI Passage Creator
 */
@Mapper
public interface ArticleStateSnapshotMapper extends BaseMapper<ArticleStateSnapshot> {

    /**
     * 根据任务ID查询最新的快照
     */
    @Select("SELECT * FROM article_state_snapshot WHERE task_id = #{taskId} ORDER BY created_at DESC LIMIT 1")
    ArticleStateSnapshot selectLatestByTaskId(@Param("taskId") String taskId);

    /**
     * 根据任务ID查询所有快照
     */
    @Select("SELECT * FROM article_state_snapshot WHERE task_id = #{taskId} ORDER BY created_at DESC")
    List<ArticleStateSnapshot> selectByTaskId(@Param("taskId") String taskId);

    /**
     * 根据任务ID和阶段查询快照
     */
    @Select("SELECT * FROM article_state_snapshot WHERE task_id = #{taskId} AND phase = #{phase} ORDER BY created_at DESC LIMIT 1")
    ArticleStateSnapshot selectByTaskIdAndPhase(@Param("taskId") String taskId, @Param("phase") String phase);

    /**
     * 删除任务的所有快照
     */
    @Select("DELETE FROM article_state_snapshot WHERE task_id = #{taskId}")
    int deleteByTaskId(@Param("taskId") String taskId);
}
