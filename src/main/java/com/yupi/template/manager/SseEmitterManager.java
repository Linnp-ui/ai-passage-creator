package com.yupi.template.manager;

import com.yupi.template.model.enums.SseMessageTypeEnum;
import com.yupi.template.utils.GsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.yupi.template.constant.ArticleConstant.SSE_RECONNECT_TIME_MS;
import static com.yupi.template.constant.ArticleConstant.SSE_TIMEOUT_MS;

/**
 * SSE Emitter 管理器
 *
 * @author <a href="https://codefather.cn">编程导航学习圈</a>
 */
@Component
@Slf4j
public class SseEmitterManager {
    private static final int MAX_PENDING_MESSAGES_PER_TASK = 200;

    /**
     * 存储所有的 SseEmitter
     */
    private final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();
    /**
     * 暂存未建立 SSE 连接期间的消息，避免“先执行后连接”导致前端无感知。
     */
    private final Map<String, List<String>> pendingMessageMap = new ConcurrentHashMap<>();

    /**
     * 创建 SseEmitter
     *
     * @param taskId 任务ID
     * @return SseEmitter
     */
    public SseEmitter createEmitter(String taskId) {
        // 若存在旧连接，先关闭并清理，避免同一 taskId 多连接导致消息错发
        SseEmitter oldEmitter = emitterMap.remove(taskId);
        if (oldEmitter != null) {
            try {
                oldEmitter.complete();
            } catch (Exception e) {
                log.warn("关闭旧 SSE 连接失败, taskId={}", taskId, e);
            }
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        
        // 设置超时回调
        emitter.onTimeout(() -> {
            log.warn("SSE 连接超时, taskId={}", taskId);
            emitterMap.remove(taskId);
        });
        
        // 设置完成回调
        emitter.onCompletion(() -> {
            log.info("SSE 连接完成, taskId={}", taskId);
            emitterMap.remove(taskId);
        });
        
        // 设置错误回调
        emitter.onError((e) -> {
            log.error("SSE 连接错误, taskId={}", taskId, e);
            emitterMap.remove(taskId);
        });
        
        emitterMap.put(taskId, emitter);
        log.info("SSE 连接已创建, taskId={}", taskId);

        // 新连接建立后补发缓存消息
        flushPendingMessages(taskId, emitter);
        
        return emitter;
    }

    /**
     * 发送消息
     *
     * @param taskId  任务ID
     * @param message 消息内容
     */
    public void send(String taskId, String message) {
        SseEmitter emitter = emitterMap.get(taskId);
        if (emitter == null) {
            cachePendingMessage(taskId, message);
            log.debug("SSE Emitter 不存在，消息已暂存, taskId={}", taskId);
            return;
        }
        
        try {
            emitter.send(SseEmitter.event()
                    .data(message)
                    .reconnectTime(SSE_RECONNECT_TIME_MS));
            log.debug("SSE 消息发送成功, taskId={}, message={}", taskId, message);
        } catch (IOException e) {
            log.error("SSE 消息发送失败, taskId={}", taskId, e);
            emitterMap.remove(taskId);
            cachePendingMessage(taskId, message);
        }
    }

    /**
     * 发送结构化消息
     *
     * @param taskId         任务ID
     * @param type           消息类型
     * @param additionalData 附加数据
     */
    public void sendMessage(String taskId, SseMessageTypeEnum type, Map<String, Object> additionalData) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", type.getValue());
        data.putAll(additionalData);
        send(taskId, GsonUtils.toJson(data));
    }

    /**
     * 完成连接
     *
     * @param taskId 任务ID
     */
    public void complete(String taskId) {
        SseEmitter emitter = emitterMap.get(taskId);
        if (emitter == null) {
            log.warn("SSE Emitter 不存在, taskId={}", taskId);
            return;
        }
        
        try {
            emitter.complete();
            log.info("SSE 连接已完成, taskId={}", taskId);
        } catch (Exception e) {
            log.error("SSE 连接完成失败, taskId={}", taskId, e);
        } finally {
            emitterMap.remove(taskId);
        }
    }

    /**
     * 检查 Emitter 是否存在
     *
     * @param taskId 任务ID
     * @return 是否存在
     */
    public boolean exists(String taskId) {
        return emitterMap.containsKey(taskId);
    }

    private void cachePendingMessage(String taskId, String message) {
        pendingMessageMap.compute(taskId, (key, oldList) -> {
            List<String> newList = oldList == null ? new ArrayList<>() : new ArrayList<>(oldList);
            newList.add(message);
            if (newList.size() > MAX_PENDING_MESSAGES_PER_TASK) {
                newList.remove(0);
            }
            return newList;
        });
    }

    private void flushPendingMessages(String taskId, SseEmitter emitter) {
        List<String> pendingMessages = pendingMessageMap.remove(taskId);
        if (pendingMessages == null || pendingMessages.isEmpty()) {
            return;
        }
        for (String pendingMessage : pendingMessages) {
            try {
                emitter.send(SseEmitter.event()
                        .data(pendingMessage)
                        .reconnectTime(SSE_RECONNECT_TIME_MS));
            } catch (IOException e) {
                log.error("补发 SSE 缓存消息失败, taskId={}", taskId, e);
                cachePendingMessage(taskId, pendingMessage);
                break;
            }
        }
        log.info("SSE 缓存消息补发完成, taskId={}, count={}", taskId, pendingMessages.size());
    }
}
