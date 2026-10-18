<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="商品名称" prop="productName">
        <el-input v-model="queryParams.productName" placeholder="请输入商品名称" clearable style="width: 200px"
                  @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="分类" prop="categoryId">
        <el-select v-model="queryParams.categoryId" placeholder="全部分类" clearable style="width: 160px">
          <el-option v-for="c in categoryOptions" :key="c.categoryId" :label="c.categoryName" :value="c.categoryId" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="商品状态" clearable style="width: 130px">
          <el-option label="上架" value="0" />
          <el-option label="下架" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd"
                   v-hasPermi="['shop:product:add']">新增商品</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete"
                   v-hasPermi="['shop:product:remove']">删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="productList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="商品ID" align="center" prop="productId" width="80" />
      <el-table-column label="图片" align="center" width="80">
        <template slot-scope="scope">
          <image-preview :src="scope.row.productImage" :width="44" :height="44" />
        </template>
      </el-table-column>
      <el-table-column label="商品名称" align="center" prop="productName" :show-overflow-tooltip="true" min-width="140" />
      <el-table-column label="分类" align="center" prop="categoryName" width="110" />
      <el-table-column label="售价(元)" align="center" prop="price" width="90">
        <template slot-scope="scope"><span style="color:#f56c6c;font-weight:600">{{ scope.row.price }}</span></template>
      </el-table-column>
      <el-table-column label="热销" align="center" width="70">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.isHot === '1'" type="danger" size="mini">热销</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="可售" align="center" width="90">
        <template slot-scope="scope">
          <el-switch v-model="scope.row.isAvailable" active-value="1" inactive-value="0"
                     @change="toggleAvailable(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="80">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'info'" size="small">
            {{ scope.row.status === "0" ? "上架" : "下架" }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)"
                     v-hasPermi="['shop:product:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)"
                     v-hasPermi="['shop:product:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum"
                :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/修改商品 -->
    <el-dialog :title="title" :visible.sync="open" width="760px" append-to-body :close-on-click-modal="false">
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="商品名称" prop="productName">
              <el-input v-model="form.productName" placeholder="如：招牌珍珠奶茶" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属分类" prop="categoryId">
              <el-select v-model="form.categoryId" placeholder="请选择分类" style="width:100%">
                <el-option v-for="c in categoryOptions" :key="c.categoryId" :label="c.categoryName" :value="c.categoryId" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="售价(元)" prop="price">
              <el-input-number v-model="form.price" :precision="2" :step="1" :min="0.01" :max="99999"
                               controls-position="right" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="显示顺序" prop="orderNum">
              <el-input-number v-model="form.orderNum" controls-position="right" :min="0" :max="9999" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="商品图片">
          <image-upload v-model="form.productImage" :limit="1" />
        </el-form-item>
        <el-form-item label="商品描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="一句话卖点" maxlength="200" show-word-limit />
        </el-form-item>
        <el-row>
          <el-col :span="8">
            <el-form-item label="热销推荐">
              <el-switch v-model="form.isHot" active-value="1" inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="是否可售">
              <el-switch v-model="form.isAvailable" active-value="1" inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="上架状态">
              <el-radio-group v-model="form.status">
                <el-radio label="0">上架</el-radio>
                <el-radio label="1">下架</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 口味规格编辑 -->
        <el-divider content-position="left">
          口味规格
          <el-button type="text" icon="el-icon-plus" @click="addFlavorGroup" style="margin-left:8px">添加口味组</el-button>
        </el-divider>
        <div v-for="(group, gi) in flavorGroups" :key="gi" class="flavor-group">
          <div class="flavor-group-head">
            <el-input v-model="group.groupName" size="mini" placeholder="组名，如：温度" style="width:140px" />
            <span class="flavor-tip">组顺序</span>
            <el-input-number v-model="group.groupOrderNum" size="mini" :min="0" controls-position="right" style="width:110px" />
            <el-button type="text" icon="el-icon-plus" @click="addFlavor(gi)">加选项</el-button>
            <el-button type="text" icon="el-icon-delete" style="color:#f56c6c" @click="removeFlavorGroup(gi)">删除组</el-button>
          </div>
          <el-table :data="group.items" size="mini" border style="margin-top:6px">
            <el-table-column label="选项名" min-width="150">
              <template slot-scope="scope">
                <el-input v-model="scope.row.flavorName" size="mini" placeholder="如：少冰" />
              </template>
            </el-table-column>
            <el-table-column label="加价(元)" width="150">
              <template slot-scope="scope">
                <el-input-number v-model="scope.row.extraPrice" size="mini" :precision="2" :step="1"
                                 :min="0" controls-position="right" style="width:120px" />
              </template>
            </el-table-column>
            <el-table-column label="组内顺序" width="120">
              <template slot-scope="scope">
                <el-input-number v-model="scope.row.orderNum" size="mini" :min="0" controls-position="right" style="width:100px" />
              </template>
            </el-table-column>
            <el-table-column label="默认选中" width="90" align="center">
              <template slot-scope="scope">
                <el-radio v-model="group.defaultName" :label="scope.row.flavorName"><span></span></el-radio>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="70" align="center">
              <template slot-scope="scope">
                <el-button type="text" icon="el-icon-delete" style="color:#f56c6c" @click="removeFlavor(gi, scope.$index)">删</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <el-empty v-if="flavorGroups.length === 0" description="暂无口味规格，可点击「添加口味组」" :image-size="60" />
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listProduct, getProduct, addProduct, updateProduct, delProduct } from "@/api/shop/product";
import { optionselectCategory } from "@/api/shop/category";

export default {
  name: "ShopProduct",
  data() {
    return {
      loading: false,
      showSearch: true,
      ids: [],
      multiple: true,
      total: 0,
      productList: [],
      categoryOptions: [],
      title: "",
      open: false,
      // 口味组编辑态：[{ groupName, groupOrderNum, defaultName, items:[{flavorName,extraPrice,orderNum}] }]
      flavorGroups: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        productName: null,
        categoryId: null,
        status: null,
      },
      form: {},
      rules: {
        productName: [{ required: true, message: "商品名称不能为空", trigger: "blur" }],
        categoryId: [{ required: true, message: "请选择所属分类", trigger: "change" }],
        price: [{ required: true, message: "售价不能为空", trigger: "blur" }],
      },
    };
  },
  created() {
    this.getList();
    this.getCategoryOptions();
  },
  methods: {
    getList() {
      this.loading = true;
      listProduct(this.queryParams).then((response) => {
        this.productList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    getCategoryOptions() {
      optionselectCategory().then((response) => {
        this.categoryOptions = response.data || [];
      });
    },
    cancel() {
      this.open = false;
      this.reset();
    },
    reset() {
      this.form = {
        productId: null,
        categoryId: null,
        productName: null,
        productImage: "",
        price: 10,
        description: null,
        orderNum: 0,
        isHot: "0",
        isAvailable: "1",
        status: "0",
      };
      this.flavorGroups = [];
      this.resetForm("form");
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
      this.ids = selection.map((item) => item.productId);
      this.multiple = !selection.length;
    },
    toggleAvailable(row) {
      const text = row.isAvailable === "1" ? "可售" : "售罄";
      updateProduct({ productId: row.productId, isAvailable: row.isAvailable })
        .then(() => this.$modal.msgSuccess("已设为" + text))
        .catch(() => {
          row.isAvailable = row.isAvailable === "1" ? "0" : "1";
        });
    },
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加商品";
    },
    handleUpdate(row) {
      this.reset();
      getProduct(row.productId).then((response) => {
        this.form = response.data;
        this.flavorGroups = this.toGroups(this.form.flavors || []);
        this.open = true;
        this.title = "修改商品";
      });
    },
    // 扁平的 flavors 列表 → 按 groupName 聚合成组（用于编辑）
    toGroups(flavors) {
      const map = {};
      const groups = [];
      flavors.forEach((f) => {
        if (!map[f.groupName]) {
          map[f.groupName] = {
            groupName: f.groupName,
            groupOrderNum: f.groupOrderNum || 0,
            defaultName: "",
            items: [],
          };
          groups.push(map[f.groupName]);
        }
        map[f.groupName].items.push({
          flavorName: f.flavorName,
          extraPrice: Number(f.extraPrice || 0),
          orderNum: f.orderNum || 0,
        });
        if (f.isDefault === "1") map[f.groupName].defaultName = f.flavorName;
      });
      groups.forEach((g) => g.items.sort((a, b) => a.orderNum - b.orderNum));
      return groups.sort((a, b) => a.groupOrderNum - b.groupOrderNum);
    },
    // 编辑态的组 → 扁平 flavors（提交后端）
    toFlavors() {
      const list = [];
      this.flavorGroups.forEach((g) => {
        if (!g.groupName || g.items.length === 0) return;
        g.items.forEach((it) => {
          if (!it.flavorName) return;
          list.push({
            groupName: g.groupName,
            groupOrderNum: g.groupOrderNum || 0,
            flavorName: it.flavorName,
            extraPrice: it.extraPrice || 0,
            orderNum: it.orderNum || 0,
            isDefault: it.flavorName === g.defaultName ? "1" : "0",
            status: "0",
          });
        });
      });
      return list;
    },
    addFlavorGroup() {
      this.flavorGroups.push({
        groupName: "",
        groupOrderNum: this.flavorGroups.length + 1,
        defaultName: "",
        items: [{ flavorName: "", extraPrice: 0, orderNum: 1 }],
      });
    },
    removeFlavorGroup(gi) {
      this.flavorGroups.splice(gi, 1);
    },
    addFlavor(gi) {
      const items = this.flavorGroups[gi].items;
      items.push({ flavorName: "", extraPrice: 0, orderNum: items.length + 1 });
    },
    removeFlavor(gi, ii) {
      this.flavorGroups[gi].items.splice(ii, 1);
    },
    submitForm() {
      this.$refs["form"].validate((valid) => {
        if (!valid) return;
        this.form.flavors = this.toFlavors();
        const req = this.form.productId != null ? updateProduct(this.form) : addProduct(this.form);
        req.then(() => {
          this.$modal.msgSuccess(this.form.productId != null ? "修改成功" : "新增成功");
          this.open = false;
          this.getList();
        });
      });
    },
    handleDelete(row) {
      const productIds = row.productId ? [row.productId] : this.ids;
      this.$modal
        .confirm("是否确认删除选中的商品？")
        .then(() => delProduct(productIds))
        .then(() => {
          this.getList();
          this.$modal.msgSuccess("删除成功");
        })
        .catch(() => {});
    },
  },
};
</script>

<style scoped>
.flavor-group {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 10px 12px;
  margin-bottom: 12px;
  background: #fafafa;
}
.flavor-group-head {
  display: flex;
  align-items: center;
  gap: 8px;
}
.flavor-tip {
  color: #909399;
  font-size: 12px;
}
</style>
