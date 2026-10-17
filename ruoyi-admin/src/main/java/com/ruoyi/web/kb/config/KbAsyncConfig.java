package com.ruoyi.web.kb.config;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 知识库文档入库线程池
 * <p>
 * 解析 + 向量化为耗时操作，与 HTTP 线程隔离；
 * 队列满时用 CallerRunsPolicy 在提交线程执行，形成自然背压，防止内存堆积。
 *
 * @author ruoyi
 */
@Configuration
public class KbAsyncConfig
{
    public static final String KB_INGEST_EXECUTOR = "kbIngestExecutor";

    @Bean(name = KB_INGEST_EXECUTOR)
    public ThreadPoolTaskExecutor kbIngestExecutor()
    {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("kb-ingest-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
