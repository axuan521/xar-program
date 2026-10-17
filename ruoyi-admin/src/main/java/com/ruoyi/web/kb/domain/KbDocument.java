package com.ruoyi.web.kb.domain;

import java.util.List;
import com.ruoyi.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 知识库文档对象 kb_document
 *
 * @author ruoyi
 */
public class KbDocument extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 文档ID */
    private Long docId;

    /** 原始文件名 */
    private String docName;

    /** 存储相对路径（相对 ruoyi.profile，含 /profile 前缀） */
    private String storePath;

    /** 文件扩展名 */
    private String fileType;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 状态（0处理中 2就绪 3失败） */
    private String status;

    /** 失败原因 */
    private String failureReason;

    /** 切片数量 */
    private Integer chunkCount;

    /** 解析出的字符数 */
    private Integer contentChars;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    /** 切片列表（详情/分块查询时填充） */
    private List<KbChunk> chunks;

    public Long getDocId()
    {
        return docId;
    }

    public void setDocId(Long docId)
    {
        this.docId = docId;
    }

    public String getDocName()
    {
        return docName;
    }

    public void setDocName(String docName)
    {
        this.docName = docName;
    }

    public String getStorePath()
    {
        return storePath;
    }

    public void setStorePath(String storePath)
    {
        this.storePath = storePath;
    }

    public String getFileType()
    {
        return fileType;
    }

    public void setFileType(String fileType)
    {
        this.fileType = fileType;
    }

    public Long getFileSize()
    {
        return fileSize;
    }

    public void setFileSize(Long fileSize)
    {
        this.fileSize = fileSize;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getFailureReason()
    {
        return failureReason;
    }

    public void setFailureReason(String failureReason)
    {
        this.failureReason = failureReason;
    }

    public Integer getChunkCount()
    {
        return chunkCount;
    }

    public void setChunkCount(Integer chunkCount)
    {
        this.chunkCount = chunkCount;
    }

    public Integer getContentChars()
    {
        return contentChars;
    }

    public void setContentChars(Integer contentChars)
    {
        this.contentChars = contentChars;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    public List<KbChunk> getChunks()
    {
        return chunks;
    }

    public void setChunks(List<KbChunk> chunks)
    {
        this.chunks = chunks;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("docId", getDocId())
            .append("docName", getDocName())
            .append("storePath", getStorePath())
            .append("fileType", getFileType())
            .append("fileSize", getFileSize())
            .append("status", getStatus())
            .append("failureReason", getFailureReason())
            .append("chunkCount", getChunkCount())
            .append("contentChars", getContentChars())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
