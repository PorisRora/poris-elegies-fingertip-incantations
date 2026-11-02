# 1.2.1 — Fabric 1.21.1

First stable Fabric release, based on Fingertip Incantations 1.2.1.

- Customizable slots, Mixed and Fingertip modes, group placement, the action wheel, three themes, and English/Chinese interfaces.
- Equipment identity, supply matching and refill, client/server synchronization, and standard mod keybind support.
- Public skill and inventory-source integration APIs. This edition does **not** include an Iron's Spells adapter.
- Fabric API's minimum is **0.106.0+1.21.1**. Earlier 1.21.1 releases lack the item-storage lookup used by this mod.

## Installation

Minecraft **1.21.1**, Fabric Loader **0.16.14+**, Java **21**.

**Required mod: Fabric API 0.106.0+1.21.1 or later for Minecraft 1.21.1.**

Install `poris-elegies-fingertip-incantations-fabric-1.21.1-1.2.1.jar` on both client and server. Use matching versions and the same loader. Close the game and remove the previous Fingertip JAR before updating. Do not put `-sources.jar` in `mods`.

Inventory Profiles Next's automatic refill can interfere with offhand returns in multiplayer, especially with latency. Disable its automatic refill if this occurs; this interaction is not fixed in this release.

The existing NeoForge 1.2.1 download remains available separately. Source code is MIT; logos and artwork are All Rights Reserved.

---

## 指尖咒文 1.2.1 · Fabric 1.21.1

首个 Fabric 正式版，以指尖咒文 1.2.1 为基线。

- 保留自由编排指尖格、混合／指尖两种模式、分组定位、操作轮盘、三套主题与中英文界面。
- 保留装备身份、补给匹配与接续、双端同步，以及标准模组按键支持。
- 保留技能与物品来源接口，暂不包含 Iron's Spells 适配。
- Fabric API 最低要求为 **0.106.0+1.21.1**；更早的 1.21.1 版缺少当前使用的物品储物查询接口。

运行环境：Minecraft **1.21.1**、Fabric Loader **0.16.14+**、Java **21**。

**必需前置：Fabric API 0.106.0+1.21.1 或更新的 1.21.1 版。**

客户端和服务端都需安装对应 Fabric JAR，并使用相同加载器与指尖版本。更新前关闭游戏、移出旧指尖包；源码包 `-sources.jar` 不放进 mods。

Inventory Profiles Next 的自动补货可能干扰多人环境中的副手物品归还，尤其是延迟较高时。遇到时请关闭它的自动补货；本版尚未修复该兼容问题。

NeoForge 1.2.1 仍单独提供。源码 MIT；Logo 与美术素材保留权利。
