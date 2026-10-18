<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="昵称" prop="nickName">
        <el-input v-model="queryParams.nickName" placeholder="请输入昵称" clearable style="width: 200px"
                  @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input v-model="queryParams.phone" placeholder="请输入手机号" clearable style="width: 200px"
                  @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 130px">
          <el-option label="正常" value="0" />
          <el-option label="停用" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete"
                   v-hasPermi="['shop:wxuser:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport"
                   v-hasPermi="['shop:wxuser:export']">导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="wxUserList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="顾客ID" align="center" prop="wxUserId" width="80" />
      <el-table-column label="头像" align="center" width="80">
        <template slot-scope="scope">
          <image-preview :src="scope.row.avatarUrl" :width="44" :height="44" />
        </template>
      </el-table-column>
      <el-table-column label="昵称" align="center" prop="nickName" :show-overflow-tooltip="true" min-width="120" />
      <el-table-column label="手机号" align="center" prop="phone" width="130" />
      <el-table-column label="累计下单" align="center" prop="orderCount" width="100">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.orderCount > 0" type="success" size="small">{{ scope.row.orderCount }}次</el-tag>
          <span v-else style="color:#999">0次</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="90">
        <template slot-scope="scope">
          <el-switch v-model="scope.row.status" active-value="0" inactive-value="1"
                     @change="handleStatusChange(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="首次进入" align="center" prop="createTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="最近登录" align="center" prop="lastLoginTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.lastLoginTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-view" @click="handleDetail(scope.row)"
                     v-hasPermi="['shop:wxuser:query']">详情</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)"
                     v-hasPermi="['shop:wxuser:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum"
                :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 顾客详情 -->
    <el-dialog title="顾客详情" :visible.sync="detailOpen" width="600px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="顾客ID">{{ detailData.wxUserId }}</el-descriptions-item>
        <el-descriptions-item label="OpenID">
          <el-tooltip :content="detailData.openid" placement="top">
            <span>{{ detailData.openid ? detailData.openid.substring(0, 10) + '...' : '-' }}</span>
          </el-tooltip>
        </el-descriptions-item>
        <el-descriptions-item label="昵称">{{ detailData.nickName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ detailData.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="头像" :span="2">
          <image-preview v-if="detailData.avatarUrl" :src="detailData.avatarUrl" :width="60" :height="60" />
          <span v-else>无</span>
        </el-descriptions-item>
        <el-descriptions-item label="累计下单">{{ detailData.orderCount || 0 }}次</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detailData.status === '0' ? 'success' : 'danger'" size="small">
            {{ detailData.status === "0" ? "正常" : "停用" }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="首次进入">{{ parseTime(detailData.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="最近登录">{{ parseTime(detailData.lastLoginTime) || '从未登录' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detailData.remark || '无' }}</el-descriptions-item>
      </el-descriptions>
      <div slot="footer" class="dialog-footer">
        <el-button @click="detailOpen = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listWxUser, getWxUser, changeWxUserStatus, delWxUser } from "@/api/shop/wxuser";

export default {
  name: "ShopWxUser",
  data() {
    return {
      loading: false,
      showSearch: true,
      ids: [],
      multiple: true,
      total: 0,
      wxUserList: [],
      detailOpen: false,
      detailData: {},
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        nickName: null,
        phone: null,
        status: null,
      },
    };
  },
  created() {
    this.getList();
  },
  methods: {
    getList() {
      this.loading = true;
      listWxUser(this.queryParams).then(response => {
        this.wxUserList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
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
      this.ids = selection.map(item => item.wxUserId);
      this.multiple = !selection.length;
    },
    handleStatusChange(row) {
      let text = row.status === "0" ? "启用" : "停用";
      this.$modal.confirm('确认要' + text + '该顾客吗？').then(() => {
        return changeWxUserStatus(row.wxUserId, row.status);
      }).then(() => {
        this.$modal.msgSuccess(text + "成功");
      }).catch(() => {
        row.status = row.status === "0" ? "1" : "0";
      });
    },
    handleDetail(row) {
      getWxUser(row.wxUserId).then(response => {
        this.detailData = response.data;
        this.detailOpen = true;
      });
    },
    handleDelete(row) {
      const wxUserIds = row.wxUserId ? [row.wxUserId] : this.ids;
      this.$modal.confirm('是否确认删除选中的顾客？').then(() => {
        return delWxUser(wxUserIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    handleExport() {
      this.download('/shop/wxuser/export', {
        ...this.queryParams
      }, `wxuser_${new Date().getTime()}.xlsx`);
    },
  }
};
</script>
