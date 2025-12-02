# FeedClient

一个完整的Android Feed流应用，支持多种卡片类型、预加载、缓存和曝光检测。

## 功能特性
- 🎯 MVP架构设计
- 📱 三种卡片类型（图文、增强、视频）
- ⚡ 预加载和缓存优化
- 👁️ 曝光事件检测系统
- 🎥 视频卡片自动播放
- 📊 混合排版布局
- 💾 本地缓存管理

## 技术栈
- Android SDK
- RecyclerView + Adapter
- Glide图片加载
- Gson序列化
- Material Design

## 项目结构
- `adapter/` - 适配器层
- `contract/` - MVP合约接口
- `model/` - 数据模型层
- `presenter/` - 业务逻辑层
- `utils/` - 工具类
- `view/` - 视图层
- `viewholder/` - ViewHolder

## 构建和运行
```bash
./gradlew assembleDebug
./gradlew installDebug