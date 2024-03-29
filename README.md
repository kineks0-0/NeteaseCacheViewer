# NeteaseCacheViewer

一个管理网易云音乐缓存的应用[第四次重构中]

[![API](https://img.shields.io/badge/API-21%2B-yellow.svg?style=flat)](https://developer.android.com/about/versions/lollipop)
[![Kotlin Version](https://img.shields.io/badge/Kotlin-1.6.10-blue.svg)](https://kotlinlang.org)
[![GitHub Releases](https://img.shields.io/github/downloads/kineks0-0/NeteaseCacheViewer/latest/total?logo=github)](https://github.com/kineks0-0/NeteaseCacheViewer/releases)

![截图](https://github.com/kineks0-0/NeteaseCacheViewer/raw/dev/screenshot/2022-08-15.webp)

## ✨ 特性

- 查看来自修改版，储存重定向和原版网易云音乐的缓存文件
- 管理缓存(预览,删除,替换,详情)
- 导出缓存(单个或批量,导出时自动识别格式写入媒体标签)
- TODO()

<br>

## ☑️ 目前进度:

- [x] **[完成]** 查看缓存列表
- [x] **[完成]** 获取缓存歌曲信息
- [x] **[完成]** 预览播放缓存
- [x] **[完成]** 导出缓存
- [x] **[完成]** 批量导出
- [x] **[完成]** 检查更新
- [x] **[完成]** 查看缓存具体信息
- [x] **[完成]** 适配 Android Q+ 的存储框架 api

<br>

- [ ] **[正在]** 更新软件UI风格
- [ ] **[正在]** 优化性能
- [ ] **[正在]** 播放界面控制

<br>

- [ ] 批量操作缓存
- [ ] 设置界面控制
- [ ] 等待修改项目架构
- [ ] 完成项目中的TODO

<br>

## ℹ️ 常见错误

### 打开应用出现无数据:

- 请检查下权限以及根目录有无 Netease 文件夹，同时需要网易云开启缓存
- 修改版或者储存重定向一般被重定向到 Android\com.netease.cloudmusic[lite]\ 下，AndroidR 以上的系统需要手动授权SAF权限

### 文件显示 '丢失 IDX!' :

- 一般是因为用了模块或者网易云尚未开始缓存导致的 // 说明下 : 缓存文件后缀是uc!,idx!则是负责声明缓存文件的信息(例如md5和完整长度)

### 文件显示 '缓存损坏' :

- 目前判断损坏是直接对比文件大小的，如果大小对不上则说明文件还没缓存完

### 文件没有显示 '缓存损坏' 但导出后播放不正常 :

- 由于为了性能考虑只是对比文件长度并没有校检md5,因此并不能百分百识别缓存完整性

<br>

## 📜 开源许可

本项目仅供个人学习研究使用，禁止用于商业及非法用途。

基于 [MIT license](https://opensource.org/licenses/MIT) 许可进行开源。

<br>

## 📃 隐私协议

您在使用我们的服务时，我们可能会收集和使用您的相关信息。

我们希望通过本《隐私政策》向您说明，在使用我们的服务时，我们如何收集、使用、储存和分享这些信息，以及我们为您提供的访问、更新、控制和保护这些信息的方式。

本《隐私政策》与您所使用的服务息息相关，希望您仔细阅读，在需要时，按照本《隐私政策》的指引，作出您认为适当的选择。

本《隐私政策》中涉及的相关技术词汇，我们尽量以简明扼要的表述，并提供进一步说明的链接，以便您的理解。

您使用或继续使用我们的服务，即意味着同意我们按照本《隐私政策》收集、使用、储存和分享您的相关信息。

如对本《隐私政策》或相关事宜有任何问题，请通过 Github 上联系。

<h2>我们获取的您的信息</h2>
您使用服务时我们可能收集如下信息：

**日志信息** ，指您使用我们的服务时，系统可能通过cookies、web beacon或其他方式自动采集的技术信息，包括：

**设备或软件信息** ，例如您的移动设备、网页浏览器或用于接入我们服务的其他程序所提供的配置信息、您的IP地址和移动设备所用的版本和设备识别码；

我们可能适时修订本《隐私政策》的条款，该等修订构成本《隐私政策》的一部分。如该等修订造成您在本《隐私政策》下权利的实质减少，我们将在修订生效前通过在主页上显著位置提示或以其他方式通知您。在该种情况下，若您继续使用我们的服务，即表示同意受经修订的本《隐私政策》的约束。

### 权限说明

1. 请求存储读写权限,即允许应用对设备存储读写:用于读取缓存文件和导出媒体文件.
2. 请求访问所有文件权限,AndroidR+的设备中读取第三方应用缓存需要申请该权限.
3. 请求网络权限,用于检查应用情况和更新缓存消息.

### 第三方SDK

- AppCenter:
  用于统计和上报崩溃消息,[《微软隐私策略》](https://privacy.microsoft.com/en-us/privacystatement)

<br>

## 🛠️开源库使用

- Compose
- Kotlin
- Android X
- Retrofit 2
- Permission X
- StarrySky
- Exoplayer
- Jaudiotagger: Ealvatag
