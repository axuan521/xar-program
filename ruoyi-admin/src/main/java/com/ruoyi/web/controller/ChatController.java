package com.ruoyi.web.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.utils.StringUtils;
import reactor.core.publisher.Flux;

/**
 * AI 对话控制器
 * <p>
 * 两种模式（由请求体 useKb 控制）：
 * <ul>
 * <li>useKb=false/null：普通日常对话（与旧行为一致），附带多轮记忆</li>
 * <li>useKb=true：先检索知识库向量库，命中则把参考资料注入系统提示词，
 * 并在回答前推送 __REFS__ 引用帧；未命中推送 __NOHIT__ 并降级为普通回答</li>
 * </ul>
 * SSE 协议保持既有约定：若干 data 帧 + 结束哨兵 [DONE]。
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/chat")
public class ChatController
{
    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    /** 检索命中引用帧前缀（后接 JSON 数组） */
    public static final String REFS_PREFIX = "__REFS__";

    /** 知识库未命中提示帧 */
    public static final String NO_HIT_FLAG = "__NOHIT__";

    /** 对话记忆窗口：保留最近 N 条消息 */
    private static final int MEMORY_WINDOW = 20;

    /** 检索返回切片数 */
    private static final int TOP_K = 4;

    /** 相似度阈值（低于则视为未命中） */
    private static final double SIMILARITY_THRESHOLD = 0.5D;

    private final ChatClient chatClient;

    private final VectorStore vectorStore;

    public ChatController(ChatClient.Builder builder, VectorStore vectorStore,
            ChatMemoryRepository chatMemoryRepository)
    {
        this.vectorStore = vectorStore;
        this.chatClient = builder
                .defaultAdvisors(MessageChatMemoryAdvisor
                        .builder(MessageWindowChatMemory.builder()
                                .chatMemoryRepository(chatMemoryRepository)
                                .maxMessages(MEMORY_WINDOW)
                                .build())
                        .build())
                .build();
    }

    /**
     * 对话请求
     *
     * @param message        用户消息
     * @param useKb          是否基于知识库回答（false/缺省 = 日常对话）
     * @param conversationId 会话ID（同 ID 共享上下文；缺省时本条独立）
     */
    public record ChatRequest(String message, Boolean useKb, String conversationId)
    {
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestBody ChatRequest req)
    {
        String message = req.message();
        if (StringUtils.isBlank(message))
        {
            return Flux.just("消息不能为空", "[DONE]");
        }
        String cid = StringUtils.isNotBlank(req.conversationId())
                ? req.conversationId()
                : UUID.randomUUID().toString();

        ChatClient.ChatClientRequestSpec spec = chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, cid));

        // 无特殊控制帧；知识库命中时为引用 JSON，未命中时为 __NOHIT__
        String leadFrame = "";
        if (Boolean.TRUE.equals(req.useKb()))
        {
            List<Document> hits = null;
            try
            {
                hits = vectorStore.similaritySearch(SearchRequest.builder()
                        .query(message)
                        .topK(TOP_K)
                        .similarityThreshold(SIMILARITY_THRESHOLD)
                        .build());
            }
            catch (Exception e)
            {
                log.error("知识库检索失败，本次降级为普通对话", e);
            }
            if (hits != null && !hits.isEmpty())
            {
                leadFrame = buildRefsFrame(hits);
                spec = spec.system(buildRagPrompt(message, hits));
            }
            else
            {
                leadFrame = NO_HIT_FLAG;
            }
        }

        Flux<String> body = spec.stream().content();
        if (leadFrame.isEmpty())
        {
            return body.concatWith(Flux.just("[DONE]"));
        }
        return Flux.concat(Flux.just(leadFrame), body, Flux.just("[DONE]"));
    }

    /** 引用帧：[{"docName":"xx.pdf","snippet":"前120字","score":0.71}, ...] */
    private static String buildRefsFrame(List<Document> hits)
    {
        JSONArray refs = new JSONArray();
        for (Document hit : hits)
        {
            JSONObject ref = new JSONObject();
            Object docName = hit.getMetadata().get("docName");
            ref.put("docName", docName == null ? "未知文档" : docName.toString());
            String text = hit.getText() == null ? "" : hit.getText();
            ref.put("snippet", text.length() > 120 ? text.substring(0, 120) + "…" : text);
            ref.put("score", hit.getScore());
            refs.add(ref);
        }
        return REFS_PREFIX + JSON.toJSONString(refs);
    }

    /** 把检索结果拼装进系统提示词 */
    private static String buildRagPrompt(String question, List<Document> hits)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("你是企业知识库助手。请依据下方【参考资料】回答用户问题。")
          .append("回答必须忠实于资料内容；资料未覆盖的部分，请明确说明档案中没有相关信息，不得编造。")
          .append("适当注明来源文件名。用中文回答。\n\n【参考资料】\n");
        int i = 1;
        for (Document hit : hits)
        {
            Object docName = hit.getMetadata().get("docName");
            sb.append("〔").append(i++).append("〕来源：").append(docName == null ? "未知文档" : docName).append("\n")
              .append(hit.getText()).append("\n\n");
        }
        sb.append("【用户问题】\n").append(question);
        return sb.toString();
    }
}
