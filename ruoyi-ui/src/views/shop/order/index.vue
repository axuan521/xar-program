<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="订单编号" prop="orderNo">
        <el-input v-model="queryParams.orderNo" placeholder="请输入订单编号" clearable style="width: 200px"
                  @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="订单状态" prop="orderStatus">
        <el-select v-model="queryParams.orderStatus" placeholder="全部状态" clearable style="width: 160px">
          <el-option label="待付款" value="0" />
          <el-option label="待制作" value="1" />
          <el-option label="制作中" value="2" />
          <el-option label="待取餐" value="3" />
          <el-option label="已完成" value="4" />
          <el-option label="已取消" value="5" />
        </el-select>
      </el-form-item>
      <el-form-item label="支付方式" prop="payType">
        <el-select v-model="queryParams.payType" placeholder="全部方式" clearable style="width: 130px">
          <el-option label="到店付款" value="1" />
          <el-option label="微信支付" value="2" />
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
                   v-hasPermi="['shop:order:remove']">删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="orderList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="订单ID" align="center" prop="orderId" width="80" />
      <el-table-column label="订单编号" align="center" prop="orderNo" :show-overflow-tooltip="true" min-width="160" />
      <el-table-column label="取餐码" align="center" prop="pickupCode" width="100">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.pickupCode" type="success" size="medium" effect="dark">
            {{ scope.row.pickupCode }}
          </el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="顾客" align="center" prop="nickName" width="120" :show-overflow-tooltip="true" />
      <el-table-column label="总金额(元)" align="center" prop="totalAmount" width="100">
        <template slot-scope="scope">
          <span style="color:#f56c6c;font-weight:600">¥{{ scope.row.totalAmount }}</span>
        </template>
      </el-table-column>
      <el-table-column label="支付方式" align="center" width="100">
        <template slot-scope="scope">
          <el-tag :type="scope.row.payType === '2' ? 'primary' : 'info'" size="small">
            {{ scope.row.payType === "2" ? "微信支付" : "到店付款" }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="支付状态" align="center" width="100">
        <template slot-scope="scope">
          <el-tag :type="scope.row.payStatus === '1' ? 'success' : 'warning'" size="small">
            {{ getPayStatusText(scope.row.payStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="订单状态" align="center" width="100">
        <template slot-scope="scope">
          <el-tag :type="getStatusType(scope.row.orderStatus)" size="small">
            {{ getStatusText(scope.row.orderStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="200" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-view" @click="handleDetail(scope.row)"
                     v-hasPermi="['shop:order:query']">详情</el-button>
          <el-button v-if="scope.row.orderStatus === '0' && scope.row.payType === '1'" size="mini" type="text"
                     icon="el-icon-money" @click="handleCollect(scope.row)"
                     v-hasPermi="['shop:order:verify']">确认收款</el-button>
          <el-button v-if="scope.row.orderStatus === '1'" size="mini" type="text" icon="el-icon-video-play"
                     @click="handleStart(scope.row)" v-hasPermi="['shop:order:status']">开始制作</el-button>
          <el-button v-if="scope.row.orderStatus === '2'" size="mini" type="text" icon="el-icon-circle-check"
                     @click="handleFinish(scope.row)" v-hasPermi="['shop:order:status']">制作完成</el-button>
          <el-button v-if="scope.row.orderStatus === '3'" size="mini" type="text" icon="el-icon-check"
                     @click="handleVerify(scope.row)" v-hasPermi="['shop:order:verify']">确认取餐</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum"
                :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 订单详情 -->
    <el-dialog title="订单详情" :visible.sync="detailOpen" width="700px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="订单编号">{{ detailData.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="取餐码">
          <el-tag v-if="detailData.pickupCode" type="success" size="medium" effect="dark">
            {{ detailData.pickupCode }}
          </el-tag>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="顾客昵称">{{ detailData.nickName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="顾客电话">{{ detailData.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="总金额">
          <span style="color:#f56c6c;font-weight:600">¥{{ detailData.totalAmount }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="支付方式">
          {{ detailData.payType === "2" ? "微信支付" : "到店付款" }}
        </el-descriptions-item>
        <el-descriptions-item label="支付状态">
          <el-tag :type="detailData.payStatus === '1' ? 'success' : 'warning'" size="small">
            {{ getPayStatusText(detailData.payStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="订单状态">
          <el-tag :type="getStatusType(detailData.orderStatus)" size="small">
            {{ getStatusText(detailData.orderStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ parseTime(detailData.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detailData.customerRemark || '无' }}</el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">订单明细</el-divider>
      <el-table :data="detailData.items" border>
        <el-table-column label="商品名称" align="center" prop="productName" :show-overflow-tooltip="true" />
        <el-table-column label="口味" align="center" prop="flavorText" width="150" :show-overflow-tooltip="true">
          <template slot-scope="scope">
            {{ scope.row.flavorText || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="单价(元)" align="center" prop="unitPrice" width="100" />
        <el-table-column label="数量" align="center" prop="quantity" width="80" />
        <el-table-column label="小计(元)" align="center" prop="subtotal" width="100">
          <template slot-scope="scope">
            <span style="color:#f56c6c;font-weight:600">¥{{ scope.row.subtotal }}</span>
          </template>
        </el-table-column>
      </el-table>

      <div slot="footer" class="dialog-footer">
        <el-button @click="detailOpen = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listOrder, getOrder, verifyOrder, updateOrderStatus, delOrder } from "@/api/shop/order";

export default {
  name: "ShopOrder",
  data() {
    return {
      loading: false,
      showSearch: true,
      ids: [],
      multiple: true,
      total: 0,
      orderList: [],
      detailOpen: false,
      detailData: {},
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        orderNo: null,
        orderStatus: null,
        payType: null,
      },
    };
  },
  created() {
    this.getList();
  },
  methods: {
    getList() {
      this.loading = true;
      listOrder(this.queryParams).then(response => {
        this.orderList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    getStatusText(status) {
      const statusMap = {
        '0': '待付款',
        '1': '待制作',
        '2': '制作中',
        '3': '待取餐',
        '4': '已完成',
        '5': '已取消'
      };
      return statusMap[status] || '未知';
    },
    getPayStatusText(status) {
      const map = { '0': '未支付', '1': '已支付', '2': '已退款' };
      return map[status] || '未支付';
    },
    getStatusType(status) {
      const typeMap = {
        '0': 'info',
        '1': 'warning',
        '2': 'primary',
        '3': 'success',
        '4': 'success',
        '5': 'danger'
      };
      return typeMap[status] || 'info';
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
      this.ids = selection.map(item => item.orderId);
      this.multiple = !selection.length;
    },
    handleDetail(row) {
      getOrder(row.orderId).then(response => {
        this.detailData = response.data;
        this.detailOpen = true;
      });
    },
    handleStart(row) {
      this.$modal.confirm('确认开始制作该订单？').then(() => {
        return updateOrderStatus({ orderId: row.orderId, orderStatus: '2' });
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("已开始制作");
      }).catch(() => {});
    },
    handleFinish(row) {
      this.$modal.confirm('确认制作完成？完成后顾客会看到取餐码。').then(() => {
        return updateOrderStatus({ orderId: row.orderId, orderStatus: '3' });
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("制作完成，已通知顾客取餐");
      }).catch(() => {});
    },
    handleCollect(row) {
      this.$modal.confirm('确认已收到该订单款项？确认后订单进入待制作。').then(() => {
        return verifyOrder(row.orderId);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("收款成功，订单已进入待制作");
      }).catch(() => {});
    },
    handleVerify(row) {
      this.$modal.confirm('确认顾客已取餐？').then(() => {
        return verifyOrder(row.orderId);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("取餐核销成功");
      }).catch(() => {});
    },
    handleDelete(row) {
      const orderIds = row.orderId ? [row.orderId] : this.ids;
      this.$modal.confirm('是否确认删除选中的订单？').then(() => {
        return delOrder(orderIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
  }
};
</script>
