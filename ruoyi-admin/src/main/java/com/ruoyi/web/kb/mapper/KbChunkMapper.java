package com.ruoyi.web.kb.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.web.kb.domain.KbChunk;

/**
 * 知识库切片 数据层
 *
 * @author ruoyi
 */
public interface KbChunkMapper
{
    /**
     * 查询某文档的切片列表
     *
     * @param docId 文档ID
     * @return 切片集合
     */
    public List<KbChunk> selectKbChunkByDocId(@Param("docId") Long docId);

    /**
     * 查询某文档已向量化的切片（重建/对账用）
     *
     * @param docId 文档ID
     * @return 切片集合
     */
    public List<KbChunk> selectEmbeddedChunks(@Param("docId") Long docId);

    /**
     * 统计已向量化切片数
     *
     * @param docId 文档ID
     * @return 切片数
     */
    public int countEmbeddedChunks(@Param("docId") Long docId);

    /**
     * 全库统计已向量化切片数（启动对账用）
     *
     * @return 切片数
     */
    public int countAllEmbeddedChunks();

    /**
     * 批量新增切片
     *
     * @param chunkList 切片集合
     * @return 结果
     */
    public int batchInsertKbChunk(@Param("list") List<KbChunk> chunkList);

    /**
     * 更新切片向量信息
     *
     * @param kbChunk 切片信息
     * @return 结果
     */
    public int updateKbChunk(KbChunk kbChunk);

    /**
     * 删除某文档的全部切片
     *
     * @param docId 文档ID
     * @return 结果
     */
    public int deleteKbChunkByDocId(@Param("docId") Long docId);
}
