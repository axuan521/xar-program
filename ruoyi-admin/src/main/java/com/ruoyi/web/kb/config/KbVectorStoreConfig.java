package com.ruoyi.web.kb.config;

import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.ruoyi.common.config.RuoYiConfig;

/**
 * 知识库向量库配置
 * <p>
 * 采用 Spring AI 自带的 SimpleVectorStore（内存 + JSON 文件持久化），
 * 零新增中间件；数据落盘在 {ruoyi.profile}/kb/vector-store.json，重启自动加载，
 * 不重复计算 embedding。文档量上万级后建议替换为专业向量库（只需换本类的 Bean 实现）。
 *
 * @author ruoyi
 */
@Configuration
public class KbVectorStoreConfig
{
    private static final Logger log = LoggerFactory.getLogger(KbVectorStoreConfig.class);

    /** 向量库落盘目录名（相对 ruoyi.profile） */
    public static final String KB_DIR = "kb";

    public static final String VECTOR_FILE = "vector-store.json";

    @Value("${spring.ai.openai.embedding.dimensions:1024}")
    private int embeddingDimensions;

    /**
     * 向量库文件（绝对路径），入库/删除后 save，启动时 load
     */
    public static File vectorStoreFile()
    {
        File dir = new File(RuoYiConfig.getProfile() + File.separator + KB_DIR);
        if (!dir.exists())
        {
            dir.mkdirs();
        }
        return new File(dir, VECTOR_FILE);
    }

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel)
    {
        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
        File file = vectorStoreFile();
        if (file.exists())
        {
            try
            {
                store.load(file);
                log.info("知识库向量库已从 {} 加载, 维度={}", file.getAbsolutePath(), embeddingDimensions);
            }
            catch (Exception e)
            {
                log.error("知识库向量库加载失败（将以空库启动，可重新上传文档重建）", e);
            }
        }
        else
        {
            log.info("知识库向量库文件不存在，以空库启动: {}", file.getAbsolutePath());
        }
        return store;
    }
}
