/**
 * 小程序展示文案唯一来源。
 * - 门店展示名：线上以接口 store.name 为准；此处为缺省回退（与种子数据一致）。
 * - app.json 的 navigationBarTitleText 无法引用本文件；改店名时请同步改 app.json（原生标题备用）。
 */
const copy = {
  /** 缺省店名；商家在「门店设置」改名后顾客端读接口 */
  storeName: "绿意鲜生",

  launch: {
    entering: "正在进入{storeName}…",
  },

  home: {
    kicker: "今日营业中 · 当面收款",
    heroDesc: "预约配送或到店自提，店员备好再联系你",
    delivery: "配送",
    deliverySub: "尽快出发",
    pickup: "自提",
    pickupSubFallback: "选时段到店",
    sectionWant: "想买点什么",
    all: "全部",
    weighHint: "当面称重结",
    soldOutToday: "今日已售完",
    emptyReco: "暂无推荐，去分类逛逛",
  },

  stock: {
    inStock: "有货",
    soldOut: "售罄",
  },

  pay: {
    offlineHint: "线下当面收款，本单不在线支付",
  },

  product: {
    noteTitle: "购买须知",
    restockTitle: "想买可以先预约提醒",
    restockBody: "店员重新标「有货」时，用微信订阅消息通知你。",
    unitWeigh: "当面称重为准",
    soldOutToday: "今日已售完",
    addCart: "加入商品篮",
    watchMe: "到货提醒我",
    watching: "已订阅到货提醒",
    addedToast: "已加入商品篮",
    subscribedToast: "已订阅",
    restockToast: "到货后会提醒你",
  },

  category: {
    watchMe: "提醒我",
    empty: "该类目暂无商品",
    selectedSuffix: "件已选",
    goCheckout: "去结算",
  },

  cart: {
    emptyTitle: "商品篮还是空的",
    emptyHint: "去分类挑点新鲜的吧",
    goCategory: "去分类看看",
    total: "合计",
    goCheckout: "去结算",
    remove: "移除",
    noBookableToast: "没有可预约的商品",
  },

  checkout: {
    steps: "选履约 · 填信息 · 提交预约",
    delivery: "配送",
    pickup: "自提",
    pickAddress: "请选择收货地址",
    pickSlot: "请选择自提时段",
    asap: "尽快送达",
    remarkLabel: "备注快捷短语",
    remarkPlaceholder: "点短语或手写补充",
    goods: "商品",
    submit: "提交预约",
    submittedToast: "预约已提交",
  },

  profile: {
    defaultNickname: "微信用户",
    memberHint: "店铺成员 · 可进工作台",
    guestHint: "默认身份：顾客",
    orders: "我的订单",
    addressBook: "地址簿",
    contact: "联系门店",
    contactSheetTitle: "联系门店",
    storePhone: "门店电话",
    storePhoneEmpty: "门店暂未填写电话",
    onlineService: "在线客服",
    merchantEntry: "商家工作台",
    merchantEntrySub: "仅店铺成员可见",
  },

  settings: {
    storeSec: "店铺资料",
    storeHint: "店名显示在顾客端导航左上角",
    nameLabel: "店名",
    phoneLabel: "电话",
    noteLabel: "购买须知",
    namePlaceholder: "店名",
    phonePlaceholder: "联系电话",
    notePlaceholder: "如配送范围、当面收款；留空则商详不展示",
    saveStore: "保存设置",
    homeSec: "顾客首页",
    homeHint: "左上角固定店名；头图主标题另配，留空则不显示以免重复",
    heroUpload: "上传首页头图（建议横图）",
    heroClear: "清除头图",
    heroSaved: "头图已更新",
    heroCleared: "已清除头图",
    kickerLabel: "营业状态",
    kickerPlaceholder: "今日营业中 · 当面收款",
    titleLabel: "主标题",
    titlePlaceholder: "头图大字，如时令口号；留空不显示",
    subtitleLabel: "副标题",
    subtitlePlaceholder: "预约配送或到店自提…",
    colsLabel: "商品列数",
    cols1: "1 列",
    cols2: "2 列",
    cols3: "3 列",
    detailModeLabel: "点商品打开",
    detailModeSheet: "半屏",
    detailModePage: "整页",
    slotsSec: "自提时段",
    slotsHint: "顾客结算自提时可选",
    slotPlaceholder: "如 今天 17:00-18:00",
    addSlot: "添加时段",
    confirmAdd: "确认添加",
    remove: "删除",
    disabled: "(停用)",
    saved: "设置已保存",
    // 兼容旧键（若别处引用）
    contactSec: "店铺资料",
    heroSec: "顾客首页",
    heroHint: "头图、文案与商品列数",
    homeDisplaySec: "顾客首页",
    homeDisplayHint: "留空文案用默认",
  },

  merchant: {
    urgentTitle: "马上处理",
    othersTitle: "其他进行中",
    urgentEmpty: "暂无待处理订单",
    othersEmpty: "暂无进行中订单",
    arrivedChip: "顾客已到店",
    cancel: "取消",
    cancelOrder: "取消订单",
    offlinePay: "线下收款",
    catManage: "分类管理",
    addProduct: "＋ 新增商品",
    edit: "编辑",
    shelfOn: "上架",
    shelfOff: "下架",
    shelfHint: "关则顾客不可见",
    stockOn: "有货",
    stockOff: "售罄",
    stockHintOn: "关 = 售罄",
    stockHintOff: "打开即补货可下单",
    homeReco: "首页推荐",
    searchPlaceholder: "按名称搜索",
    filterAll: "全部",
    filterRecoOn: "仅推荐",
    filterRecoOff: "未推荐",
    filterEmpty: "没有匹配的商品",
    catHint: "本店分类供顾客左侧类目与「编辑商品」下拉使用。单位不在此管理。",
    catEnabled: "启用",
    catDisabled: "停用",
    catAdd: "＋ 新增分类",
    catNamePlaceholder: "新分类名称",
    productHint: "图片最多 3 张（第一张=封面）；单位 = 系统枚举；规格说明可选；分类 = 本店数据。",
    fieldName: "名称",
    fieldUnit: "单位",
    fieldSpec: "规格说明（可选）",
    fieldSpecPlaceholderJin: "如 约 1 斤",
    fieldSpecPlaceholderPiece: "如 约 10 枚、500g/份",
    fieldPrice: "价格（元）",
    fieldCategory: "分类（本店）",
    fieldHomeReco: "首页推荐",
    fieldHomeRecoHint: "开启后出现在顾客首页；须同时上架",
    fieldImage: "商品图",
    imageUpload: "添加图片",
    goCats: "管理分类",
    save: "保存",
    inviteStaff: "邀请店员",
    inviteHint: "第一波通过冷启动配置；邀请能力后补",
    backCustomer: "回顾客端",
    backCustomerSub: "继续逛店下单",
    roleHint: "店主 / 店员能力相同（预留角色）",
    ownerPrefix: "店主",
    staffPrefix: "店员",
  },

  titles: {
    home: "", // 首页用店名
    category: "分类",
    product: "商品详情",
    cart: "商品篮",
    checkout: "提交预约",
    addressList: "地址簿",
    addressEdit: "编辑地址",
    addressNew: "新增地址",
    orders: "我的订单",
    orderDetail: "订单详情",
    profile: "我的",
    merchantOrders: "订单台",
    merchantProducts: "商品",
    merchantProductEdit: "编辑商品",
    merchantProductNew: "新增商品",
    merchantCategories: "分类管理",
    merchantSettings: "门店设置",
    merchantProfile: "我的",
  },
};

/** 优先接口店名，否则用缺省 */
function storeDisplayName(store) {
  const name = store && store.name;
  return (name && String(name).trim()) || copy.storeName;
}

/** 替换 {storeName} 占位 */
function format(template, vars) {
  const map = { storeName: copy.storeName, ...(vars || {}) };
  return String(template || "").replace(/\{(\w+)\}/g, (_, k) =>
    map[k] != null ? String(map[k]) : ""
  );
}

module.exports = copy;
module.exports.storeDisplayName = storeDisplayName;
module.exports.format = format;
