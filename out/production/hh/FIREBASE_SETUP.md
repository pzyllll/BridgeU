# Firebase 手机验证码配置说明

## 🔧 Firebase 控制台配置步骤

### 1. 启用 Phone Authentication

1. 访问 [Firebase Console](https://console.firebase.google.com/)
2. 选择项目 `bridgeu-87fbe`
3. 进入 **Authentication** > **Sign-in method**
4. 找到 **Phone** 提供商，点击启用
5. 保存配置

### 2. 添加授权域名（重要！）

在 Firebase Console 的 **Authentication** > **Settings** > **Authorized domains** 中添加：

- `localhost`（用于本地开发，必须添加）
- `127.0.0.1`（用于本地开发，可选）
- `localhost:5175`（如果使用特定端口，建议也添加）
- 你的生产域名（部署时添加）

**注意**：如果不添加 `localhost`，会出现 `auth/internal-error` 错误。

### 3. 配置 reCAPTCHA（如果需要）

如果使用 reCAPTCHA v2，确保：
- 在 Firebase Console 的 **Authentication** > **Settings** > **reCAPTCHA** 中配置
- 或者使用 Firebase 默认的 reCAPTCHA（推荐）

## 🐛 常见错误解决

### ERR_CONNECTION_TIMED_OUT / ERR_CERT_COMMON_NAME_INVALID

这个错误通常是因为：
1. **域名未授权**：确保 `localhost` 已添加到 Firebase 授权域名列表（Authentication > Settings > Authorized domains）
2. **网络问题**：检查是否能访问 `https://www.google.com/recaptcha/api.js`，可能需要科学上网
3. **Firebase 配置错误**：检查 `firebaseConfig` 是否正确
4. **防火墙/代理**：检查是否有防火墙或代理阻止了 reCAPTCHA 请求

### auth/internal-error

这个错误最常见的原因是**域名未授权**。请按以下步骤检查：

1. **检查 Phone Authentication 是否已启用**
   - 进入 Firebase Console > Authentication > Sign-in method
   - 确保 Phone 提供商已启用

2. **检查授权域名配置（最重要！）**
   - 进入 Firebase Console > Authentication > Settings > Authorized domains
   - 确保 `localhost` 在列表中
   - 如果没有，点击 "Add domain" 添加 `localhost`
   - 保存后等待几分钟让配置生效

3. **检查 Firebase 项目配置**
   - 确认项目 ID 正确：`bridgeu-87fbe`
   - 确认 API Key 正确

4. **清除浏览器缓存并重试**
   - 清除浏览器缓存和 Cookie
   - 刷新页面重试

### 验证码发送失败

1. 确保手机号格式正确：`+国家代码手机号`（如 `+66123456789`）
2. 检查 Firebase 配额是否用完
3. 检查网络连接

## 📝 测试步骤

1. 确保 Firebase 控制台配置完成
2. 重启前端开发服务器
3. 尝试注册流程，选择"手机号"
4. 输入带国家区号的手机号（如 `+66123456789`）
5. 点击"发送验证码"
6. 检查浏览器控制台是否有错误

## 🔍 调试技巧

如果遇到问题，检查浏览器控制台：
- 查看是否有 Firebase 相关错误
- 查看网络请求，确认 reCAPTCHA 是否正常加载
- 检查 Firebase SDK 版本是否兼容

## 📞 需要帮助？

如果问题持续存在：
1. 检查 Firebase Console 的 **Authentication** > **Usage** 查看是否有错误日志
2. 确认 Firebase 项目配额未用完
3. 尝试在 Firebase Console 手动测试 Phone Authentication

