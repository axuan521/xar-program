package com.ruoyi.web.kb.service.impl;

import java.io.File;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.web.kb.domain.KbChunk;
import com.ruoyi.web.kb.domain.KbDocument;
import com.ruoyi.web.kb.mapper.KbChunkMapper;
import com.ruoyi.web.kb.mapper.KbDocumentMapper;
import com.ruoyi.web.kb.service.IKbDocumentService;
import com.ruoyi.web.kb.service.KbIngestService;

/**
 * 知识库文档 服务实现
 *
 * @author ruoyi
 */
@Service
public class KbDocumentServiceImpl implements IKbDocumentService
{
    private static final Logger log = LoggerFactory.getLogger(KbDocumentServiceImpl.class);

    /** 知识库允许的文件类型 */
    private static final String[] KB_ALLOWED_EXTENSION = { "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md" };

    private final KbDocumentMapper documentMapper;

    private final KbChunkMapper chunkMapper;

    private final KbIngestService ingestService;

    public KbDocumentServiceImpl(KbDocumentMapper documentMapper, KbChunkMapper chunkMapper, KbIngestService ingestService)
    {
        this.documentMapper = documentMapper;
        this.chunkMapper = chunkMapper;
        this.ingestService = ingestService;
    }

    @Override
    public List<KbDocument> selectKbDocumentList(KbDocument kbDocument)
    {
        return documentMapper.selectKbDocumentList(kbDocument);
    }

    @Override
    public KbDocument selectKbDocumentById(Long docId)
    {
        KbDocument doc = documentMapper.selectKbDocumentById(docId);
        if (doc != null)
        {
            doc.setChunks(chunkMapper.selectKbChunkByDocId(docId));
        }
        return doc;
    }

    @Override
    public List<KbChunk> selectKbChunksByDocId(Long docId)
    {
        return chunkMapper.selectKbChunkByDocId(docId);
    }

    @Override
    public Long uploadDocument(MultipartFile file)
    {
        try
        {
            String baseDir = RuoYiConfig.getUploadPath();
            String storePath = FileUploadUtils.upload(baseDir, file, KB_ALLOWED_EXTENSION);

            KbDocument doc = new KbDocument();
            doc.setDocName(file.getOriginalFilename());
            doc.setStorePath(storePath);
            doc.setFileType(FileUploadUtils.getExtension(file));
            doc.setFileSize(file.getSize());
            doc.setStatus("0");
            doc.setChunkCount(0);
            doc.setContentChars(0);
            doc.setCreateBy(SecurityUtils.getUsername());
            documentMapper.insertKbDocument(doc);

            ingestService.submit(doc.getDocId());
            return doc.getDocId();
        }
        catch (Exception e)
        {
            log.error("知识库文档上传失败", e);
            throw new ServiceException("文档上传失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteKbDocumentByIds(Long[] docIds)
    {
        for (Long docId : docIds)
        {
            KbDocument doc = documentMapper.selectKbDocumentById(docId);
            if (doc == null)
            {
                continue;
            }
            try
            {
                ingestService.removeVectors(docId);
            }
            catch (Exception e)
            {
                log.error("删除文档向量失败 docId=" + docId, e);
            }
            documentMapper.deleteKbDocumentById(docId);
            File file = KbIngestService.resolvePhysicalFile(doc.getStorePath());
            if (file.exists())
            {
                file.delete();
            }
        }
    }

    @Override
    public void retryKbDocument(Long docId)
    {
        KbDocument doc = documentMapper.selectKbDocumentById(docId);
        if (doc == null)
        {
            throw new ServiceException("文档不存在");
        }
        // 清理旧向量与切片后重新走入库链路
        ingestService.removeVectors(docId);
        KbDocument update = new KbDocument();
        update.setDocId(docId);
        update.setStatus("0");
        update.setChunkCount(0);
        update.setFailureReason("");
        documentMapper.updateKbDocument(update);
        ingestService.submit(docId);
    }
}
