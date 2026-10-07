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

| Role | Hex | CSS Variable | 用途 |
|------|-----|--------------|------|
| Primary | `#0F766E` | `--color-primary` | Tab 选中、链接、主结构 |
| Secondary | `#2DD4BF` | `--color-secondary` | 浅强调、进度点 |
| CTA | `#16A34A` | `--color-cta` | 主按钮（预约/加购） |
| Background | `#F0FDFA` | `--color-background` | 页底 |
| Surface | `#FFFFFF` | `--color-surface` | 列表行底 |
| Text | `#134E4A` | `--color-text` | 正文 |
| Muted | `#5B7C78` | `--color-muted` | 次要说明 |
| Line | `#CCE5E1` | `--color-line` | 分割 |
| Price | `#D97706` | `--color-price` | 价格 |
| Danger | `#DC2626` | `--color-danger` | 售罄、取消 |
| Warn | `#B45309` | `--color-warn` | 待确认徽章 |

对比：正文与背景满足 ≥ 4.5:1；主按钮白字于 `#16A34A` 可读。

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
| 首页 | 店名英雄级；一句履约信任文案；快捷进分类；推荐可空 |
| 分类 | 左类目右列表；价色用 `--color-price` |
| 详情 | 售罄时主按钮改「到货提醒我」 |
| 结算 | 配送/自提分段；备注短语 chips；CTA「提交预约」 |
| 订单 | 状态色+文案双通道；再来一单 / 我到店了 |
| 商家订单台 | 待处理优先；「顾客已到店」用 secondary/busy 徽章 |
| 商家商品 | 上架 / 有货 双开关；入口进分类管理；编辑页单位=枚举、分类=本店数据 |
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
