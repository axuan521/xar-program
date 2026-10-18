/**
 * 小程序全局配置
 *
 * 后端地址优先级：
 *   1. 环境变量 VITE_API_BASE_URL（.env.development / .env.production）
 *   2. 默认本机 http://localhost:8080
 *
 * 微信开发者工具需勾选「不校验合法域名」才能访问 localhost；
 * 真机预览请改成局域网 IP 或已备案的 HTTPS 域名。
 */
const DEFAULT_BASE_URL = 'http://localhost:8080'

function readEnvBaseUrl() {
  try {
    // Vite 会在编译期把 import.meta.env 替换成静态对象
    return (import.meta.env && import.meta.env.VITE_API_BASE_URL) || ''
  } catch (e) {
    return ''
  }
}

export const BASE_URL = (readEnvBaseUrl() || DEFAULT_BASE_URL).replace(/\/+$/, '')

/** 本地缓存 key，统一管理避免各页面拼错 */
export const STORAGE_KEYS = {
  CUSTOMER_TOKEN: 'shop_customer_token',
  CUSTOMER_INFO: 'shop_customer_info',
  STAFF_TOKEN: 'shop_staff_token',
  STAFF_INFO: 'shop_staff_info',
  CART: 'shop_cart',
  TABLE_NO: 'shop_table_no',
  MOCK_CODE: 'shop_mock_login_code',
  RECENT_VERIFY: 'shop_recent_verify'
}

/** 门店信息（可按实际门店修改） */
export const SHOP_INFO = {
  name: '若依饮品店',
  slogan: '扫码点单 · 现做现取',
  notice: '下单后请留意取餐码，叫号后到取餐台取餐',
  businessHours: '09:00 - 22:00',
  address: '门店地址请在 config/index.js 中修改',
  servicePhone: ''
}

/** 订单状态字典 */
export const ORDER_STATUS = {
  UNPAID: '0',
  WAIT_MAKE: '1',
  MAKING: '2',
  WAIT_PICKUP: '3',
  FINISHED: '4',
  CANCELED: '5'
}

export const ORDER_STATUS_TEXT = {
  '0': '待付款',
  '1': '待制作',
  '2': '制作中',
  '3': '待取餐',
  '4': '已完成',
  '5': '已取消'
}

export const PAY_STATUS_TEXT = {
  '0': '未支付',
  '1': '已支付',
  '2': '已退款'
}

export const PAY_TYPE_TEXT = {
  '1': '到店付款',
  '2': '微信支付'
}

export default {
  baseUrl: BASE_URL,
  timeout: 20000,
  shop: SHOP_INFO,
  storageKeys: STORAGE_KEYS
}

export const getBaseUrl = () => BASE_URL
export const getTimeout = () => 20000
