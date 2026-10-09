# Design System Master — 绿意鲜生 / CommunityFreshMarket

> 构建页面前：先查 `design-system/communityfreshmarket/pages/[page].md`；有则覆盖本文件，无则严格遵循本文件。

**依据**：ui-ux-pro-max `--design-system` + 产品定位覆盖  
**日期**：2026-10-07  
**可点击 UI 稿**：`docs/prototypes/index.html`

---

## 产品语境（覆盖理由）

- 社区生鲜**私域预约**工具，非社交社区、非综合电商大促  
- 不做在线支付；主 CTA 文案为「提交预约 / 加入商品篮」  
- 小程序触控优先：44px 触控、8px 间距、`touch-action: manipulation`  
- **覆盖工具默认紫色社区色**（#7C3AED 等）——与生鲜气质冲突，且落 AI 常见紫系套路  
- **不用** Nature Distilled 的 cream + terracotta 组合（项目前端禁忌）  
- 字体以 **Noto Sans SC** 为主（大陆简体）；数字可用字重区分，不引入无中文的 Rubik 作正文

---

## 风格合成

| 来源 | 采纳 |
|------|------|
| **Flat Design**（design-system 主推） | 少阴影、清晰色块、150–200ms 过渡、图标化 |
| **Organic Biophilic**（style 检索） | 圆角 12–16px、叶绿主色、轻氛围，不用厚重拟物 |
| **触控 UX** | 44×44 最小热区、相邻 8px+、加载禁用按钮 |

**一句话**：扁平清爽的晨市工具感 —— 好扫价、好点预约、像熟客店而不是 App Store 落地页。

---

## Color Palette

一条青绿做品牌，外加价格 / 待处理 / 危险三组语义色。不再使用第二套草绿、荧光青、青色或微信绿。细则见 `docs/plans/2026-10-08-color-system.md`。

| Role | Hex | CSS Variable | 用途 |
|------|-----|--------------|------|
| Primary | `#0F766E` | `--color-primary` / `--color-cta` | 选中、链接、主按钮（预约/加购） |
| Primary pressed | `#115E59` | `--color-primary-pressed` | 按下 |
| Secondary | `#0D9488` | `--color-secondary` | 进度、描边强调（同色相、浅一档） |
| Background | `#F0FDFA` | `--color-background` | 页底 |
| Surface | `#FFFFFF` | `--color-surface` | 卡片、按钮上的白 |
| Mist | `#CCFBF1` | `--color-mist` | 有货、选中、正向浅底 |
| Fill | `#F4F7F6` | `--color-fill` | 输入、禁用、凹槽 |
| Text | `#134E4A` | `--color-text` | 正文 |
| Muted | `#5B7C78` | `--color-muted` | 次要说明 |
| Line | `#CCE5E1` | `--color-line` | 分割 |
| Ink | `#0F3D38` | `--color-ink` | 深色头图、次级实心块 |
| Price | `#D97706` | `--color-price` | 价格数字 |
| Price on dark | `#FDE68A` | `--color-price-on-dark` | 深色封面上的价格 |
| Warn | `#B45309` | `--color-warn` | 待确认文字 |
| Warn soft / line | `#FEF6E8` / `#F3D7A8` | `--color-warn-soft` / `--color-warn-line` | 待确认底与边 |
| Danger | `#DC2626` | `--color-danger` | 售罄、取消 |
| Danger soft / line | `#FEF2F2` / `#FECACA` | `--color-danger-soft` / `--color-danger-line` | 危险底与边 |
| Placeholder | `#C5E8E1` / `#F4E0B0` / `#E4EAE8` | `--color-ph-veg` / `--color-ph-fruit` / `--color-ph-sold` | 无图：菜、果、售罄 |

对比：正文与页底 ≥ 4.5:1；白字于 `#0F766E` 可读。有货与「已到店」都走青绿（浅底或实心），不另起青色。

---

## Typography

- **Heading / Body**：Noto Sans SC（300 / 400 / 500 / 700）  
- 店名可加粗或略大字号，不另用展示字体以免小程序体积膨胀  
- 行高正文 1.5–1.6；列表标题 15–16px；辅助 12–13px

```css
@import url('https://fonts.googleapis.com/css2?family=Noto+Sans+SC:wght@300;400;500;700&display=swap');
```

---

## Spacing & Radius

| Token | Value |
|-------|--------|
| `--space-xs` | 4px |
| `--space-sm` | 8px |
| `--space-md` | 16px |
| `--space-lg` | 24px |
| `--space-xl` | 32px |
| `--radius` | 12px（Flat + 轻有机） |
| `--radius-pill` | 999px |

---

## Shadows（Flat 约束）

默认**无投影**；仅弹层可用：

`--shadow-float: 0 8px 24px rgba(15, 118, 110, 0.12)`

---

## Components

### Buttons

- 高度 ≥ 44px；主按钮实心 CTA 绿；次按钮 mist 底 + primary 字  
- `:active` 用 opacity / 背景色，**不**用会挤布局的 scale  
- 异步中 `disabled` + 文案「提交中…」

### List rows（交互容器）

- 白底 + 底部分割线，或 1px `line` 描边；**不是**厚卡片堆叠  
- 整行可点；右侧步进器热区独立 ≥ 44px

### Status chips

- 有货：浅绿底 + primary 字  
- 售罄：浅红底 + danger  
- 待确认：浅琥珀底 + warn  

### Icons

- **禁止 emoji 作图标**；统一 SVG（24 viewBox，描边 1.75–2）  
- Tab / 导航图标线宽一致

### Motion

- 150–200ms ease；`prefers-reduced-motion: reduce` 时关闭位移动画  
- `touch-action: manipulation`

---

## 页面要点（第一波）

| 页面 | 规则 |
|------|------|
| 首页 | 导航店名 + 头图可配主标题（可空）；履约文案可配；商品区 1/2/3 列可配；点商品默认半屏 |
| 分类 | 左类目右列表；价色用 `--color-price`；有货底栏胶囊去结算 |
| 详情 | 售罄时主按钮改「到货提醒我」；完整页保留；多图用轮播（≤3，单图无点） |
| 商品篮 | 左右留边紧凑行（非通栏）：小封面、右侧步进器、「移除」贴名价下；底栏胶囊「去结算」与分类页同款 |
| 结算 | 配送/自提分段；备注短语 chips；CTA「提交预约」 |
| 订单 | 状态色+文案双通道；再来一单 / 我到店了 |
| 商家订单台 | 状态导航切换列表，默认马上处理；「顾客已到店」用 secondary/busy 徽章 |
| 商家商品 | 两列卡片：封面蒙层底 + 名（两行省略）/价叠字，卡底半透明上架/有货/首页推荐三开关；搜索+分类/推荐筛选；入口进分类管理；编辑页最多 3 图小方格、单位=枚举、分类=本店数据、首页推荐开关 |
| 分类管理 | 本店分类 CRUD；与顾客类目同源 |

---

## Anti-patterns（本项目）

- 紫色主色 / 紫底社区风  
- cream + terracotta 温馨杂志风  
- 「去支付」文案  
- emoji Tab 图标  
- 大阴影拟物卡片墙  
- 首屏堆统计、活动条、多段营销

---

## Pre-Delivery Checklist

- [ ] SVG 图标，无 emoji 图标  
- [ ] 可点元素 `cursor: pointer`  
- [ ] 过渡 150–300ms  
- [ ] 正文对比度 ≥ 4.5:1  
- [ ] focus 可见  
- [ ] `prefers-reduced-motion`  
- [ ] 375 宽手机框可读（原型）  
- [ ] CTA 为预约语义，非支付  
