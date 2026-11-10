# Touhou Little Maid: Spell — 1.18.2 移植版

[Touhou Little Maid: Spell](https://github.com/yimeng261/Touhou-Little-Maid-Spell)（作者 **DreamCat**，MIT）的 **Minecraft 1.18.2 / Forge 40.2.0** 移植版。

原模组面向 1.20.1，本工程把其中**可脱离魔法前置运行**的两部分移植到 1.18.2：

1. **女仆饰品（bauble）**——18 件本模组饰品 + 云梦水晶 / 春华归返 / 发簪等去魔法改造后的效果；
2. **建筑结构**——13 个 `StructureFeature` / 72 个结构模板 / 39 张战利品表，含 `the_retreat` 退隐维度与寻风之铃。

> **范围声明**：本移植**不包含任何魔法适配**。铁魔法（Iron's Spellbooks）、Goety 等前置模组不引入、不启用、不移植。原模组中纯魔法内容（阴阳祭坛、星之魔女全链路、`fairy_maid_cafe`）**整体放弃**。

---

## 许可与来源（License & Attribution）

- **本移植**：MIT，见 [LICENSE](LICENSE)。
- **上游原模组 [Touhou Little Maid: Spell](https://github.com/yimeng261/Touhou-Little-Maid-Spell)**：作者 **DreamCat**，声明 **MIT**（见其构建产物 `META-INF/mods.toml`）。本项目是其演绎作品，保留原作者署名与 MIT 许可声明。
- **依赖的 [Touhou Little Maid](https://github.com/TartaricAcid/TouhouLittleMaid) 本体**：**CC-BY-NC-SA**（署名-非商业性使用-相同方式共享）。本移植**只调用其运行时 API，不内嵌其任何代码或美术资源**，因此不构成对该本体的演绎；但：

> ⚠️ **本移植仅供非商业用途（Non-Commercial）。**
> ⚠️ 使用本模组**必须自行安装** Touhou Little Maid 本体，并遵循其 CC-BY-NC-SA 许可。
> ⚠️ 东方 Project 及其角色归 **ZUN / 上海爱丽丝幻乐团** 所有，请遵循其二次创作规约。

本仓库内**不含**上游的音效 BGM、星之魔女模型包、旧版贴图包（`legacy_pack`）或 Patchouli 手册章节。

---

## 安装

| 依赖 | 版本 |
| :--- | :--- |
| Minecraft | 1.18.2 |
| Forge | 40.2.0+ |
| Touhou Little Maid | 1.2.2+（**必需**） |

1. 安装 Forge 1.18.2；
2. 把 **Touhou Little Maid** 放入 `mods/`；
3. 把本模组的 jar 放入 `mods/`；
4. 启动游戏。

> 本模组**不依赖** Iron's Spellbooks / Goety / Curios 之外的任何魔法模组。

---

## 从源码构建

```bash
./gradlew build
```

产物体：`build/libs/touhou_little_maid_spell-1.9.2.1-forge+mc1.18.2-PORT.jar`

构建需要 JDK 17。Touhou Little Maid 的 1.18.2 jar 需放在本地 `libs/`（该目录不入库，见 `.gitignore`）。

---

## 移植说明

与原模组相比的行为差异（均为 1.18.2 平台限制所致，非设计选择）：

- Jigsaw 结构的 `size` 上限为 7（1.18.2 `JigsawConfiguration` 限制），原模组为 20；
- 1.18.2 无 `minecraft:jigsaw` 结构类型，拼图结构需自建 `StructureFeature`；
- 版本新增方块（樱花木/叶、泥砖、青蛙灯等）按语义降级映射到 1.18.2 同类方块；
- 「个人退隐维度」（按玩家分配私有世界 + 结构搜索配额，上游约 1900 行）**未移植**；改为：共享退隐维度 + 生成器结构集白名单 + 原版 `findNearestMapFeature` 寻路 + 5 秒临时区块票据。

---

## 致谢

- **DreamCat** — 原模组 Touhou Little Maid: Spell 的作者
- **TartaricAcid** 与 Touhou Little Maid 全体贡献者 — 本体模组与饰品 API
- **ZUN / 上海爱丽丝幻乐团** — 东方 Project
