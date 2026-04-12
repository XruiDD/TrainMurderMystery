# Wathe 权限配置

Wathe 模组通过 [fabric-permissions-api](https://github.com/lucko/fabric-permissions-api) 提供权限支持。

## 工作原理

fabric-permissions-api 是一个轻量级的权限桥接层（~12KB），为模组提供标准化的权限检查接口：

- **未安装权限管理模组**：所有权限检查自动回退到原版 OP 等级判断，行为与之前完全一致
- **安装了 LuckPerms 等权限管理模组**：可以通过权限节点精细控制每个命令和功能的访问权限

## 权限节点列表

### 命令权限（默认需要 OP 等级 2）

| 权限节点 | 对应命令 | 说明 |
|---------|---------|------|
| `wathe.command.config` | `/wathe:config` | 重载/修改模组配置 |
| `wathe.command.giveroomkey` | `/wathe:giveRoomKey` | 给予玩家房间钥匙 |
| `wathe.command.forcerole` | `/wathe:forceRole` | 强制指定玩家角色 |
| `wathe.command.mapvote` | `/wathe:mapvote` | 发起地图投票 |
| `wathe.command.mapvariables` | `/wathe:mapVariables` | 设置地图变量（出生点、游玩区域等） |
| `wathe.command.gamesettings` | `/wathe:gameSettings` | 游戏设置（自动开始、角色启用等） |
| `wathe.command.setmoney` | `/wathe:setMoney` | 设置玩家金币 |
| `wathe.command.settimer` | `/wathe:setTimer` | 设置游戏计时器 |
| `wathe.command.setvisual` | `/wathe:setVisual` | 设置视觉效果（雪、雾、HUD 等） |
| `wathe.command.start` | `/wathe:start` | 开始游戏 |
| `wathe.command.stop` | `/wathe:stop` | 停止游戏 |
| `wathe.command.updatedoors` | `/wathe:updateDoors` | 更新门状态 |

### 管理权限（默认需要 OP 等级 2）

| 权限节点 | 说明 |
|---------|------|
| `wathe.admin.horn_cooldown` | 按喇叭时跳过冷却时间 |
| `wathe.admin.horn_start` | 按喇叭直接开始游戏 |
| `wathe.admin.spectator_bypass` | 非游戏期间旁观模式不会被自动重置为冒险模式 |

### 其他权限

| 权限节点 | 默认 OP 等级 | 说明 |
|---------|-------------|------|
| `wathe.spectate` | 1 | 游戏开始时非参与玩家进入旁观模式（而非被踢出服务器） |

## LuckPerms 配置示例

安装 [LuckPerms](https://luckperms.net/) 后，可以通过以下命令配置权限：

```bash
# 允许某个用户组使用开始游戏命令
/lp group helper permission set wathe.command.start true

# 允许某个用户组使用所有 wathe 命令
/lp group admin permission set wathe.command.* true

# 允许普通玩家在游戏开始时旁观（而非被踢出）
/lp group default permission set wathe.spectate true

# 允许 helper 组通过按喇叭开始游戏
/lp group helper permission set wathe.admin.horn_start true

# 给予管理员所有管理权限（喇叭、旁观bypass等）
/lp group admin permission set wathe.admin.* true

# 给予管理员完整的 wathe 权限
/lp group admin permission set wathe.* true
```

## 依赖说明

fabric-permissions-api 已内嵌在 wathe 模组 JAR 中（通过 `include`），无需单独安装。只需要安装 LuckPerms 即可使用权限节点功能。
