package com.ruoyi.web.kb.service;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.web.kb.domain.KbChunk;
import com.ruoyi.web.kb.domain.KbDocument;

/**
 * 知识库文档 服务层
 *
 * @author ruoyi
 */
public interface IKbDocumentService
{
    /**
     * 查询文档列表
     */
    public List<KbDocument> selectKbDocumentList(KbDocument kbDocument);

    /**
     * 查询文档详情（含切片）
     */
    public KbDocument selectKbDocumentById(Long docId);

    /**
     * 查询文档切片列表
     */
    public List<KbChunk> selectKbChunksByDocId(Long docId);

    /**
     * 上传文档并异步入库
     *
     * @return 新文档ID
     */
    public Long uploadDocument(MultipartFile file);

    /**
     * 删除文档（同步删除向量与切片）
     */
    public void deleteKbDocumentByIds(Long[] docIds);

    /**
     * 重新入库（清理旧向量后重新解析）
     */
    public void retryKbDocument(Long docId);
}
