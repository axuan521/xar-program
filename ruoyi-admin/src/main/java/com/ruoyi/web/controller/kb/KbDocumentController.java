package com.ruoyi.web.controller.kb;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.web.kb.domain.KbChunk;
import com.ruoyi.web.kb.domain.KbDocument;
import com.ruoyi.web.kb.service.IKbDocumentService;

/**
 * 知识库文档管理
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/kb/document")
public class KbDocumentController extends BaseController
{
    private final IKbDocumentService kbDocumentService;

    public KbDocumentController(IKbDocumentService kbDocumentService)
    {
        this.kbDocumentService = kbDocumentService;
    }

    /**
     * 查询文档列表
     */
    @PreAuthorize("@ss.hasPermi('kb:document:list')")
    @GetMapping("/list")
    public TableDataInfo list(KbDocument kbDocument)
    {
        startPage();
        List<KbDocument> list = kbDocumentService.selectKbDocumentList(kbDocument);
        return getDataTable(list);
    }

    /**
     * 获取文档详细信息（含切片）
     */
    @PreAuthorize("@ss.hasPermi('kb:document:query')")
    @GetMapping("/{docId}")
    public AjaxResult getInfo(@PathVariable Long docId)
    {
        return success(kbDocumentService.selectKbDocumentById(docId));
    }

    /**
     * 查询文档切片列表
     */
    @PreAuthorize("@ss.hasPermi('kb:document:query')")
    @GetMapping("/{docId}/chunks")
    public AjaxResult chunks(@PathVariable Long docId)
    {
        List<KbChunk> chunks = kbDocumentService.selectKbChunksByDocId(docId);
        return success(chunks);
    }

    /**
     * 上传文档（异步入库，前端轮询状态）
     */
    @PreAuthorize("@ss.hasPermi('kb:document:upload')")
    @Log(title = "知识库文档", businessType = BusinessType.INSERT)
    @PostMapping("/upload")
    public AjaxResult upload(@RequestPart("file") MultipartFile file)
    {
        Long docId = kbDocumentService.uploadDocument(file);
        return AjaxResult.success("上传成功，正在解析入库", java.util.Map.of("docId", docId));
    }

    /**
     * 删除文档（同步删除向量与切片）
     */
    @PreAuthorize("@ss.hasPermi('kb:document:remove')")
    @Log(title = "知识库文档", businessType = BusinessType.DELETE)
    @DeleteMapping("/{docIds}")
    public AjaxResult remove(@PathVariable Long[] docIds)
    {
        kbDocumentService.deleteKbDocumentByIds(docIds);
        return success();
    }

    /**
     * 重新入库
     */
    @PreAuthorize("@ss.hasPermi('kb:document:retry')")
    @Log(title = "知识库文档", businessType = BusinessType.UPDATE)
    @PostMapping("/retry/{docId}")
    public AjaxResult retry(@PathVariable Long docId)
    {
        kbDocumentService.retryKbDocument(docId);
        return success();
    }
}
