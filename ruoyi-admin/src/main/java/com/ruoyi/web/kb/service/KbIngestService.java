package com.ruoyi.web.kb.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import com.knuddels.jtokkit.api.EncodingType;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.web.kb.config.KbAsyncConfig;
import com.ruoyi.web.kb.config.KbVectorStoreConfig;
import com.ruoyi.web.kb.domain.KbChunk;
import com.ruoyi.web.kb.domain.KbDocument;
import com.ruoyi.web.kb.mapper.KbChunkMapper;
import com.ruoyi.web.kb.mapper.KbDocumentMapper;

/**
 * 知识库文档异步入库服务
 * <p>
 * 链路：物理文件 → 解析(PDF/Tika) → Token 切分 → 向量化写入向量库 → 回填切片元数据。
 * 向量写入与 JSON 落盘对 vectorStore 实例加锁串行，避免并发覆盖。
 *
 * @author ruoyi
 */
@Service
public class KbIngestService
{
    private static final Logger log = LoggerFactory.getLogger(KbIngestService.class);

    /** 解析文本上限，防止超大文档/异常内容拖垮内存 */
    private static final int MAX_TOTAL_CHARS = 2_000_000;

    /** 单文档最大切片数 */
    private static final int MAX_CHUNKS = 1500;

    /** 切片目标 token 数（中文约 400~500 字） */
    private static final int CHUNK_SIZE = 600;

    private final KbDocumentMapper documentMapper;

    private final KbChunkMapper chunkMapper;

    private final VectorStore vectorStore;

    private final ThreadPoolTaskExecutor executor;

    public KbIngestService(KbDocumentMapper documentMapper, KbChunkMapper chunkMapper,
            VectorStore vectorStore, @Qualifier(KbAsyncConfig.KB_INGEST_EXECUTOR) ThreadPoolTaskExecutor executor)
    {
        this.documentMapper = documentMapper;
        this.chunkMapper = chunkMapper;
        this.vectorStore = vectorStore;
        this.executor = executor;
    }

    /** 提交异步入库任务 */
    public void submit(Long docId)
    {
        executor.execute(() -> ingest(docId));
    }

    /** 执行入库（在 kb-ingest 线程内） */
    public void ingest(Long docId)
    {
        KbDocument doc = documentMapper.selectKbDocumentById(docId);
        if (doc == null)
        {
            return;
        }
        try
        {
            File file = resolvePhysicalFile(doc.getStorePath());
            if (!file.exists())
            {
                markFailed(doc, "物理文件不存在: " + file.getAbsolutePath());
                return;
            }

            // 1. 解析
            FileSystemResource resource = new FileSystemResource(file);
            List<Document> rawDocs;
            if ("pdf".equalsIgnoreCase(doc.getFileType()))
            {
                rawDocs = new PagePdfDocumentReader(resource).get();
            }
            else
            {
                rawDocs = new TikaDocumentReader(resource).get();
            }
            int totalChars = rawDocs.stream().mapToInt(d -> d.getText() == null ? 0 : d.getText().length()).sum();
            if (totalChars > MAX_TOTAL_CHARS)
            {
                markFailed(doc, "解析文本超过 " + MAX_TOTAL_CHARS + " 字符，文档过大");
                return;
            }

            // 2. 切分
            TokenTextSplitter splitter = TokenTextSplitter.builder()
                    .withEncodingType(EncodingType.CL100K_BASE)
                    .withChunkSize(CHUNK_SIZE)
                    .withMaxNumChunks(MAX_CHUNKS)
                    .withKeepSeparator(true)
                    .build();
            List<Document> pieces = splitter.transform(rawDocs);
            if (pieces.isEmpty())
            {
                markFailed(doc, "未提取到文本内容（可能是纯图片扫描件或空文档）");
                return;
            }

            // 3. 组装带元数据的待入库文档（metadata 供检索过滤与引用展示）
            List<Document> toStore = new ArrayList<>(pieces.size());
            for (int i = 0; i < pieces.size(); i++)
            {
                Map<String, Object> meta = new HashMap<>();
                meta.put("docId", String.valueOf(doc.getDocId()));
                meta.put("docName", doc.getDocName());
                meta.put("chunkIndex", i);
                toStore.add(new Document(pieces.get(i).getText(), meta));
            }

            // 4. 向量化 + 写向量库 + 落盘（对 store 实例串行，避免并发覆盖与重复 save）
            synchronized (vectorStore)
            {
                vectorStore.add(toStore);
                if (vectorStore instanceof SimpleVectorStore simple)
                {
                    simple.save(KbVectorStoreConfig.vectorStoreFile());
                }
            }

            // 5. 回填切片元数据（vector_id 是删除与重启对账的依据）
            List<KbChunk> rows = new ArrayList<>(toStore.size());
            for (int i = 0; i < toStore.size(); i++)
            {
                Document d = toStore.get(i);
                KbChunk chunk = new KbChunk();
                chunk.setDocId(doc.getDocId());
                chunk.setChunkIndex(i);
                chunk.setVectorId(d.getId());
                chunk.setEmbeddingStatus("1");
                chunk.setTokenCount(estimateTokens(d.getText()));
                chunk.setContent(d.getText());
                chunk.setCreateTime(new Date());
                rows.add(chunk);
            }
            for (int from = 0; from < rows.size(); from += 100)
            {
                chunkMapper.batchInsertKbChunk(rows.subList(from, Math.min(from + 100, rows.size())));
            }

            // 6. 文档置为就绪
            KbDocument update = new KbDocument();
            update.setDocId(doc.getDocId());
            update.setStatus("2");
            update.setChunkCount(rows.size());
            update.setContentChars(totalChars);
            update.setFailureReason("");
            documentMapper.updateKbDocument(update);
            log.info("知识库文档入库完成 docId={} 切片数={}", doc.getDocId(), rows.size());
        }
        catch (Exception e)
        {
            log.error("知识库文档入库失败 docId=" + docId, e);
            markFailed(doc, summarize(e));
        }
    }

    /** 删除文档对应的向量与切片记录（同步执行，向量删除失败不阻塞） */
    public void removeVectors(Long docId)
    {
        List<KbChunk> chunks = chunkMapper.selectKbChunkByDocId(docId);
        List<String> vectorIds = new ArrayList<>();
        for (KbChunk chunk : chunks)
        {
            if (StringUtils.isNotEmpty(chunk.getVectorId()))
            {
                vectorIds.add(chunk.getVectorId());
            }
        }
        if (!vectorIds.isEmpty())
        {
            synchronized (vectorStore)
            {
                vectorStore.delete(vectorIds);
                if (vectorStore instanceof SimpleVectorStore simple)
                {
                    simple.save(KbVectorStoreConfig.vectorStoreFile());
                }
            }
        }
        chunkMapper.deleteKbChunkByDocId(docId);
    }

    /**
     * 由存储相对路径（/profile/xxx）还原物理绝对路径
     */
    public static File resolvePhysicalFile(String storePath)
    {
        String relative = StringUtils.substring(storePath, Constants.RESOURCE_PREFIX.length());
        return new File(RuoYiConfig.getProfile() + relative);
    }

    private void markFailed(KbDocument doc, String reason)
    {
        try
        {
            KbDocument update = new KbDocument();
            update.setDocId(doc.getDocId());
            update.setStatus("3");
            update.setFailureReason(StringUtils.substring(reason, 0, 480));
            documentMapper.updateKbDocument(update);
        }
        catch (Exception e)
        {
            log.error("知识库文档失败状态回写异常 docId=" + doc.getDocId(), e);
        }
    }

    private static String summarize(Exception e)
    {
        String msg = e.getMessage();
        return e.getClass().getSimpleName() + (msg == null ? "" : ": " + msg);
    }

    /** 粗略估算 token（中文 1 字 ≈ 1 token，仅作展示） */
    private static int estimateTokens(String text)
    {
        return text == null ? 0 : (int) Math.ceil(text.length() / 1.2);
    }
}
