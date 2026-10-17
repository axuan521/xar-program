package com.ruoyi.web.kb.domain;

import java.util.Date;

/**
 * 知识库切片对象 kb_chunk
 *
 * @author ruoyi
 */
public class KbChunk
{
    /** 切片ID */
    private Long chunkId;

    /** 所属文档ID */
    private Long docId;

    /** 文档内切片序号 */
    private Integer chunkIndex;

    /** 向量库记录ID */
    private String vectorId;

    /** 向量化状态（0未向量化 1完成 2失败） */
    private String embeddingStatus;

    /** 估算token数 */
    private Integer tokenCount;

    /** 切片原文 */
    private String content;

    /** 创建时间 */
    private Date createTime;

    public Long getChunkId()
    {
        return chunkId;
    }

    public void setChunkId(Long chunkId)
    {
        this.chunkId = chunkId;
    }

    public Long getDocId()
    {
        return docId;
    }

    public void setDocId(Long docId)
    {
        this.docId = docId;
    }

    public Integer getChunkIndex()
    {
        return chunkIndex;
    }

    public void setChunkIndex(Integer chunkIndex)
    {
        this.chunkIndex = chunkIndex;
    }

    public String getVectorId()
    {
        return vectorId;
    }

    public void setVectorId(String vectorId)
    {
        this.vectorId = vectorId;
    }

    public String getEmbeddingStatus()
    {
        return embeddingStatus;
    }

    public void setEmbeddingStatus(String embeddingStatus)
    {
        this.embeddingStatus = embeddingStatus;
    }

    public Integer getTokenCount()
    {
        return tokenCount;
    }

    public void setTokenCount(Integer tokenCount)
    {
        this.tokenCount = tokenCount;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }
}
