# 0.2.4-beta · Broader Iron's Spells compatibility

The optional Iron's Spells 'n Spellbooks integration now accepts **1.21-3.4.4 and later**, lowering the previous 1.21.1-3.15.5 requirement. Minecraft stays at **1.21.1**, and the NeoForge minimum stays at **21.1.181**.

The existing skill adapter already handles the older spell API. Gameplay code, artwork, settings, and network protocol are unchanged from 0.2.3-beta. Spells follow the behavior available in the installed Iron's version.

## Requirements

- Minecraft Java Edition **1.21.1**
- **NeoForge 21.1.181 or later** for Minecraft 1.21.1
- Install Fingertip on both the client and server for multiplayer
- Iron's Spells is optional; if installed, use **1.21-3.4.4 or later** and satisfy its own dependency requirements

## Validation

The minimum combination was checked with Iron's 1.21-3.4.4, NeoForge 21.1.181, Curios 9.5.1, GeckoLib 4.9.3, Player Animation Library 2.0.4, and Caelus 7.0.1. Skill actions, key takeover, the radial menu, and dedicated-server loading passed. All runtime classes and game resources are unchanged from the tested 0.2.3-beta code.

## Updating

Close the game, remove the previous Fingertip JAR, and install `poris-elegies-fingertip-incantations-0.2.4-beta.jar`. Keep only one Fingertip version in each `mods` folder. The `-sources.jar` is for development and does not belong in `mods`.

Source code uses MIT; logo and artwork have a separate All Rights Reserved statement.

---

# 0.2.4-beta · 扩大 Iron's Spells 版本支持

可选联动 Iron's Spells 'n Spellbooks 的最低要求从 1.21.1-3.15.5 下调至 **1.21-3.4.4**。Minecraft 仍为 **1.21.1**，NeoForge 最低仍为 **21.1.181**。

现有技能适配已能处理旧版法术接口。本次只调整版本声明和说明，玩法代码、美术、配置与网络协议沿用 0.2.3-beta。法术行为以实际安装的 Iron's 版本为准。

多人游戏需客户端与服务端均安装指尖咒文。Iron's 并非必装；安装时须满足它自己的前置依赖。最低组合使用 Curios 9.5.1、GeckoLib 4.9.3、Player Animation Library 2.0.4、Caelus 7.0.1，已通过技能、按键接管、轮盘和独立服务端检查。

更新时关闭游戏，移出旧指尖 JAR，再放入 `poris-elegies-fingertip-incantations-0.2.4-beta.jar`，同一个 `mods` 文件夹只保留一版。`-sources.jar` 为开发用源码包，不放进 `mods`。

源码采用 MIT；Logo 与美术素材按独立声明保留所有权利。
