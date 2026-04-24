# Gateway 加密通信使用指南

## 概述

Gateway实现了端到端的请求/响应加密，使用AES-256-GCM算法确保数据的机密性和完整性。

## 架构设计

```
前端 <--加密--> Gateway <--明文--> Backend
```

- **前端**: 使用sessionEncKey加密请求，解密响应
- **Gateway**: 解密请求，加密响应，从Redis获取session key
- **Backend**: 处理明文请求，返回明文响应，负责token验证

## 安全特性

1. **AES-256-GCM**: 认证加密，防止篡改
2. **会话密钥隔离**: 每个会话使用独立的32字节随机密钥
3. **随机IV**: 每次加密使用新的12字节随机IV
4. **密钥存储**: Session key存储在Redis，7天过期
5. **路径白名单**: 支持配置跳过加密的接口

## 配置说明

### application.properties

```properties
# 启用加密
gateway.crypto.enabled=true

# Session ID请求头名称
gateway.crypto.session-id-header=X-Session-Id

# 跳过加密的路径（支持通配符）
gateway.crypto.skip-paths[0]=/ymb/auth/login
gateway.crypto.skip-paths[1]=/ymb/health
gateway.crypto.skip-paths[2]=/actuator/**
```

### 路径匹配规则

- 精确匹配: `/ymb/auth/login`
- 单级通配: `/ymb/user/*` (匹配 `/ymb/user/123`)
- 多级通配: `/actuator/**` (匹配 `/actuator/health/liveness`)

## 前端集成

### 1. 登录获取密钥

```javascript
// 登录接口不加密
const response = await fetch('/ymb/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ phone: '13800138000', code: '123456' })
});

const { token, sessionId, sessionEncKey } = await response.json();

// 保存到本地存储
localStorage.setItem('sessionId', sessionId);
localStorage.setItem('sessionEncKey', sessionEncKey);
localStorage.setItem('token', token);
```

### 2. 加密请求

```javascript
// 前端加密工具（需要引入crypto-js或Web Crypto API）
async function encryptRequest(plainJson, base64Key) {
  const keyBytes = Uint8Array.from(atob(base64Key), c => c.charCodeAt(0));
  const key = await crypto.subtle.importKey(
    'raw', keyBytes, { name: 'AES-GCM' }, false, ['encrypt']
  );
  
  const iv = crypto.getRandomValues(new Uint8Array(12));
  const plainBytes = new TextEncoder().encode(JSON.stringify(plainJson));
  
  const ciphertext = await crypto.subtle.encrypt(
    { name: 'AES-GCM', iv: iv },
    key,
    plainBytes
  );
  
  const combined = new Uint8Array(iv.length + ciphertext.byteLength);
  combined.set(iv);
  combined.set(new Uint8Array(ciphertext), iv.length);
  
  return btoa(String.fromCharCode(...combined));
}

// 发送加密请求
const sessionId = localStorage.getItem('sessionId');
const sessionEncKey = localStorage.getItem('sessionEncKey');

const requestData = { userId: 123, action: 'query' };
const encryptedBody = await encryptRequest(requestData, sessionEncKey);

const response = await fetch('/ymb/user/profile', {
  method: 'POST',
  headers: {
    'Content-Type': 'text/plain',
    'X-Session-Id': sessionId
  },
  body: encryptedBody
});

const encryptedResponse = await response.text();
const decryptedData = await decryptRequest(encryptedResponse, sessionEncKey);
console.log(decryptedData);
```

### 3. 解密响应

```javascript
async function decryptRequest(encryptedBase64, base64Key) {
  const keyBytes = Uint8Array.from(atob(base64Key), c => c.charCodeAt(0));
  const key = await crypto.subtle.importKey(
    'raw', keyBytes, { name: 'AES-GCM' }, false, ['decrypt']
  );
  
  const combined = Uint8Array.from(atob(encryptedBase64), c => c.charCodeAt(0));
  const iv = combined.slice(0, 12);
  const ciphertext = combined.slice(12);
  
  const plainBytes = await crypto.subtle.decrypt(
    { name: 'AES-GCM', iv: iv },
    key,
    ciphertext
  );
  
  const plaintext = new TextDecoder().decode(plainBytes);
  return JSON.parse(plaintext);
}
```

## 后端集成

Backend服务无需修改，接收的是Gateway解密后的明文JSON。

### 示例Controller

```java
@RestController
@RequestMapping("/user")
public class UserController {
    
    // Gateway会将 /ymb/user/profile 路由到这里
    // 接收的是解密后的明文JSON
    @PostMapping("/profile")
    public UserProfile getProfile(@RequestBody ProfileRequest request) {
        // 直接处理明文数据
        return userService.getProfile(request.getUserId());
    }
}
```

## 错误处理

### 常见错误码

- **401 Unauthorized**: 
  - 缺少 `X-Session-Id` 请求头
  - Session已过期或无效
  
- **400 Bad Request**:
  - 请求体为空
  - 解密失败（密钥错误或数据被篡改）

- **500 Internal Server Error**:
  - 服务器内部错误

### 错误响应格式

```json
{
  "code": "401",
  "message": "Invalid or expired session",
  "timestamp": 1714723200000
}
```

## 测试

### 单元测试

```bash
./gradlew :yami-gateway:test --tests "AesGcmCryptoTest"
```

### 集成测试

使用curl测试（需要先登录获取sessionId和sessionEncKey）：

```bash
# 1. 登录（不加密）
curl -X POST http://localhost:8080/ymb/auth/login \
  -H "Content-Type: application/json" \
  -d '{"phone":"13800138000","code":"123456"}'

# 2. 使用返回的sessionId和sessionEncKey加密请求
# （需要使用加密工具生成encryptedBody）

# 3. 发送加密请求
curl -X POST http://localhost:8080/ymb/user/profile \
  -H "Content-Type: text/plain" \
  -H "X-Session-Id: your-session-id" \
  -d "base64-encrypted-data"
```

## 性能考虑

1. **Redis连接池**: 确保Redis连接池配置合理
2. **加密开销**: AES-GCM性能优秀，对延迟影响<1ms
3. **缓存策略**: Session key在Redis中缓存，避免重复查询

## 安全建议

1. **HTTPS**: 生产环境必须使用HTTPS
2. **密钥轮换**: 定期更新session key（重新登录）
3. **监控**: 监控解密失败率，检测潜在攻击
4. **日志**: 不要记录敏感数据（密钥、明文）
5. **限流**: 对解密失败的请求进行限流

## 故障排查

### 解密失败

1. 检查sessionId是否正确
2. 检查session是否过期（Redis中是否存在）
3. 检查前端加密实现是否正确（IV长度、算法参数）
4. 检查Base64编码是否正确

### Session过期

- Session默认7天过期
- 过期后需要重新登录获取新的sessionId和sessionEncKey
- 前端应处理401错误，引导用户重新登录
