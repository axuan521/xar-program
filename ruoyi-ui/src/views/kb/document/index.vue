<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="文件名" prop="docName">
        <el-input v-model="queryParams.docName" placeholder="请输入文件名" clearable style="width: 220px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="文档状态" clearable style="width: 160px">
          <el-option label="处理中" value="0" />
          <el-option label="就绪" value="2" />
          <el-option label="失败" value="3" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-upload2" size="mini" @click="uploadOpen = true" v-hasPermi="['kb:document:upload']">上传档案</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete()" v-hasPermi="['kb:document:remove']">删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="docList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="文档ID" align="center" prop="docId" width="80" />
      <el-table-column label="文件名" align="center" prop="docName" :show-overflow-tooltip="true" min-width="200" />
      <el-table-column label="类型" align="center" prop="fileType" width="80" />
      <el-table-column label="大小" align="center" width="100">
        <template slot-scope="scope">{{ formatSize(scope.row.fileSize) }}</template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="110">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.status === '0'" type="warning" size="small">处理中</el-tag>
          <el-tag v-else-if="scope.row.status === '2'" type="success" size="small">就绪</el-tag>
          <el-tag v-else-if="scope.row.status === '3'" type="danger" size="small">失败</el-tag>
          <el-tag v-else size="small">{{ scope.row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="切片数" align="center" prop="chunkCount" width="80" />
      <el-table-column label="字符数" align="center" prop="contentChars" width="90" />
      <el-table-column label="失败原因" align="center" prop="failureReason" :show-overflow-tooltip="true" min-width="160" />
      <el-table-column label="上传时间" align="center" prop="createTime" width="160">
        <template slot-scope="scope"><span>{{ parseTime(scope.row.createTime) }}</span></template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="210" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-view" @click="handleDetail(scope.row)" v-hasPermi="['kb:document:query']">切片</el-button>
          <el-button size="mini" type="text" icon="el-icon-refresh" :disabled="scope.row.status === '0'" @click="handleRetry(scope.row)" v-hasPermi="['kb:document:retry']">重入库</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['kb:document:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 上传对话框 -->
    <el-dialog title="上传档案" :visible.sync="uploadOpen" width="480px" append-to-body>
      <el-upload
        ref="kbUpload"
        drag
        multiple
        :limit="uploadLimit"
        :action="uploadUrl"
        :headers="uploadHeaders"
        :data="{}"
        :file-list="uploadFileList"
        :before-upload="beforeUpload"
        :on-success="handleUploadSuccess"
        :on-error="handleUploadError"
        :on-remove="handleUploadRemove"
        accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.md"
      >
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">将档案拖到此处，或<em>点击上传</em></div>
        <div class="el-upload__tip" slot="tip">
          支持 pdf / doc / docx / xls / xlsx / ppt / pptx / txt / md，单个不超过 10MB。
          上传后自动解析入库，稍候刷新状态。纯图片扫描件无法提取文字。
        </div>
      </el-upload>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="uploadOpen = false">关 闭</el-button>
      </div>
    </el-dialog>

    <!-- 切片详情对话框 -->
    <el-dialog :title="'切片详情 - ' + detailTitle" :visible.sync="detailOpen" width="720px" append-to-body>
      <el-table v-loading="detailLoading" :data="chunkList" max-height="460">
        <el-table-column label="#" align="center" prop="chunkIndex" width="50" />
        <el-table-column label="向量ID" align="center" prop="vectorId" width="150" :show-overflow-tooltip="true" />
        <el-table-column label="Token" align="center" prop="tokenCount" width="70" />
        <el-table-column label="内容" align="left" prop="content" :show-overflow-tooltip="true" min-width="260" />
      </el-table>
      <div slot="footer" class="dialog-footer">
        <el-button @click="detailOpen = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listDocument, delDocument, retryDocument, listChunks } from "@/api/kb/document";
import { getToken } from "@/utils/auth";

export default {
  name: "KbDocument",
  data() {
    return {
      loading: false,
      showSearch: true,
      total: 0,
      docList: [],
      ids: [],
      multiple: true,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        docName: null,
        status: null,
      },
      // 上传
      uploadOpen: false,
      uploadLimit: 10,
      uploadFileList: [],
      pollTimer: null,
      // 详情
      detailOpen: false,
      detailLoading: false,
      detailTitle: "",
      chunkList: [],
    };
  },
  computed: {
    uploadUrl() {
      return process.env.VUE_APP_BASE_API + "/kb/document/upload";
    },
    uploadHeaders() {
      return { Authorization: "Bearer " + getToken() };
    },
  },
  created() {
    this.getList();
  },
  beforeDestroy() {
    this.stopPolling();
  },
  methods: {
    getList() {
      this.loading = true;
      listDocument(this.queryParams).then((response) => {
        this.docList = response.rows;
        this.total = response.total;
        this.loading = false;
        // 存在处理中的文档则轮询刷新状态
        if (this.docList.some((d) => d.status === "0")) {
          this.startPolling();
        } else {
          this.stopPolling();
        }
      }).catch(() => {
        this.loading = false;
      });
    },
    startPolling() {
      if (this.pollTimer) return;
      this.pollTimer = setInterval(() => {
        listDocument(this.queryParams).then((response) => {
          this.docList = response.rows;
          this.total = response.total;
          if (!this.docList.some((d) => d.status === "0")) {
            this.stopPolling();
          }
        });
      }, 3000);
    },
    stopPolling() {
      if (this.pollTimer) {
        clearInterval(this.pollTimer);
        this.pollTimer = null;
      }
    },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    handleSelectionChange(selection) {
      this.ids = selection.map((item) => item.docId);
      this.multiple = !selection.length;
    },
    beforeUpload(file) {
      const allow = ["pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md"];
      const ext = file.name.lastIndexOf(".") > -1 ? file.name.slice(file.name.lastIndexOf(".") + 1).toLowerCase() : "";
      if (!allow.includes(ext)) {
        this.$modal.msgError("不支持的文件类型: " + ext);
        return false;
      }
      if (file.size / 1024 / 1024 > 10) {
        this.$modal.msgError("文件大小不能超过 10MB");
        return false;
      }
      return true;
    },
    handleUploadSuccess(res, file, fileList) {
      if (res && res.code === 200) {
        this.$modal.msgSuccess("《" + file.name + "》已提交入库");
      } else {
        this.$modal.msgError("《" + file.name + "》" + ((res && res.msg) || "上传失败"));
      }
      // 从受控列表移除该文件（成功即消失，失败也移除由提示兜底），保留其他仍在上传的项
      this.uploadFileList = fileList.filter((f) => f.uid !== file.uid);
      this.getList();
    },
    handleUploadError(err, file, fileList) {
      this.$modal.msgError("《" + file.name + "》上传失败，请检查登录状态或文件大小");
      this.uploadFileList = fileList.filter((f) => f.uid !== file.uid);
    },
    handleUploadRemove(file, fileList) {
      this.uploadFileList = fileList;
    },
    handleRetry(row) {
      const vm = this;
      this.$modal
        .confirm('确认对文档"' + row.docName + '"重新解析入库吗？')
        .then(function () {
          return retryDocument(row.docId);
        })
        .then(() => {
          vm.$modal.msgSuccess("已重新提交入库");
          vm.getList();
        })
        .catch(() => {});
    },
    handleDelete(row) {
      const docIds = row && row.docId ? [row.docId] : this.ids;
      const vm = this;
      this.$modal
        .confirm("是否确认删除选中的知识库文档？将同步删除已入库的向量数据。")
        .then(function () {
          return delDocument(docIds);
        })
        .then(() => {
          vm.getList();
          vm.$modal.msgSuccess("删除成功");
        })
        .catch(() => {});
    },
    handleDetail(row) {
      this.detailTitle = row.docName;
      this.chunkList = [];
      this.detailOpen = true;
      this.detailLoading = true;
      listChunks(row.docId)
        .then((response) => {
          this.chunkList = response.data || [];
          this.detailLoading = false;
        })
        .catch(() => {
          this.detailLoading = false;
        });
    },
    formatSize(size) {
      if (size == null) return "-";
      if (size < 1024) return size + " B";
      if (size < 1024 * 1024) return (size / 1024).toFixed(1) + " KB";
      return (size / 1024 / 1024).toFixed(2) + " MB";
    },
  },
};
</script>
