package com.ruoyi.web.kb.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.web.kb.domain.KbDocument;

/**
 * 知识库文档 数据层
 *
 * @author ruoyi
 */
public interface KbDocumentMapper
{
    /**
     * 查询文档列表
     *
     * @param kbDocument 文档信息
     * @return 文档集合
     */
    public List<KbDocument> selectKbDocumentList(KbDocument kbDocument);

    /**
     * 查询文档详情
     *
     * @param docId 文档ID
     * @return 文档信息
     */
    public KbDocument selectKbDocumentById(Long docId);

    /**
     * 按状态查询文档（启动对账/就绪文档加载用）
     *
     * @param status 状态
     * @return 文档集合
     */
    public List<KbDocument> selectKbDocumentByStatus(@Param("status") String status);

    /**
     * 新增文档
     *
     * @param kbDocument 文档信息
     * @return 结果
     */
    public int insertKbDocument(KbDocument kbDocument);

    /**
     * 修改文档
     *
     * @param kbDocument 文档信息
     * @return 结果
     */
    public int updateKbDocument(KbDocument kbDocument);

    /**
     * 删除文档
     *
     * @param docId 文档ID
     * @return 结果
     */
    public int deleteKbDocumentById(Long docId);

    /**
     * 批量删除文档
     *
     * @param docIds 需要删除的文档ID
     * @return 结果
     */
    public int deleteKbDocumentByIds(Long[] docIds);
}
