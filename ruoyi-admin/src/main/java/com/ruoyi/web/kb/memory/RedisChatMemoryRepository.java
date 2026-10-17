package com.ruoyi.web.kb.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

/**
 * 基于 Redis 的对话记忆仓库
 * <p>
 * 实现 Spring AI {@link ChatMemoryRepository}，复用若依已有 Redis（localhost:6379），
 * 不引入额外中间件；按 conversationId 存 JSON 列表，重启后上下文仍在。
 *
 * @author ruoyi
 */
@Component
public class RedisChatMemoryRepository implements ChatMemoryRepository
{
    /** 记忆 key 前缀 */
    private static final String KEY_PREFIX = "chat:memory:";

    /** 记忆保留天数 */
    private static final long EXPIRE_DAYS = 7;

    private final StringRedisTemplate redisTemplate;

    public RedisChatMemoryRepository(StringRedisTemplate redisTemplate)
    {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public List<String> findConversationIds()
    {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        List<String> ids = new ArrayList<>();
        if (keys != null)
        {
            for (String key : keys)
            {
                ids.add(key.substring(KEY_PREFIX.length()));
            }
        }
        return ids;
    }

    @Override
    public List<Message> findByConversationId(String conversationId)
    {
        List<Message> messages = new ArrayList<>();
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + conversationId);
        if (json == null || json.isEmpty())
        {
            return messages;
        }
        JSONArray array = JSON.parseArray(json);
        for (int i = 0; i < array.size(); i++)
        {
            JSONObject item = array.getJSONObject(i);
            String type = item.getString("type");
            String text = item.getString("text");
            if (text == null)
            {
                continue;
            }
            if (MessageType.USER.getValue().equals(type))
            {
                messages.add(new UserMessage(text));
            }
            else if (MessageType.ASSISTANT.getValue().equals(type))
            {
                messages.add(new AssistantMessage(text));
            }
            else if (MessageType.SYSTEM.getValue().equals(type))
            {
                messages.add(new SystemMessage(text));
            }
            // TOOL 等其他类型不恢复（本应用未启用工具调用循环）
        }
        return messages;
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages)
    {
        JSONArray array = new JSONArray();
        for (Message message : messages)
        {
            JSONObject item = new JSONObject();
            item.put("type", message.getMessageType().getValue());
            item.put("text", message.getText());
            array.add(item);
        }
        String key = KEY_PREFIX + conversationId;
        redisTemplate.opsForValue().set(key, array.toJSONString(), EXPIRE_DAYS, TimeUnit.DAYS);
    }

    @Override
    public void deleteByConversationId(String conversationId)
    {
        redisTemplate.delete(KEY_PREFIX + conversationId);
    }
}
