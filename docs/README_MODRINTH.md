# Create: Redstone Throttle

![Create: Redstone Throttle](https://raw.githubusercontent.com/z33awa/Redstone-Throttle/main/src/main/resources/icon.png)

A compact Create kinetic controller driven by redstone signals.

## What it does

Redstone Throttle sits in a Create transmission and controls the output speed from
two independent redstone inputs. The red face increases speed, the blue face
decreases it, and the side arrow shows the direction from kinetic input to output.

- Place it on any of the six axes.
- Rotate the redstone control faces around the shaft axis with a wrench.
- Configure it in-game by holding Shift and right-clicking.
- Inspect live input, output, and settings with Engineer's Goggles.
- Use an adjustable update interval from 10 to 200 ticks (0.5–10 seconds).
- Keep Create's native stress and overstress behavior.

## Control modes

### Follow signal strength

Output speed follows the difference between the red and blue signals.

### Fixed speed

Outputs a configured target speed whenever a valid kinetic input is present.

### Signal strength × multiplier

```text
output speed = initial speed + (red signal - blue signal) × multiplier
```

Initial speed is configurable from 0 to 256 RPM. Rotation direction follows the
input, and output becomes zero without a valid kinetic input.

## Compatibility

| Component | Requirement |
| --- | --- |
| Minecraft | 1.21.1 |
| Mod loader | NeoForge 21.1.219+ |
| Create | 6.0.10+ |
| Create Aeronautics | Optional |

Install the mod on both client and server.

## Using the block

1. Connect Create power to the input face and machinery to the output face.
2. Connect redstone to the red and blue control faces.
3. Hold Shift and right-click to open the configuration screen.
4. Scroll over a value to change it; hold Shift for ×10 steps.
5. Use a wrench on either shaft end to rotate the control faces.

## Links

- [GitHub and issue tracker](https://github.com/z33awa/Redstone-Throttle)
- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/redstone-throttle)

## License

All Rights Reserved.

---

# 简体中文

**机械动力：红石节流阀** 是一个由红石信号控制的紧凑型机械动力调速方块。

## 功能说明

红石节流阀安装在机械动力传动系统中，通过两个独立的红石输入控制输出转速。
红色控制面加速，蓝色控制面减速，侧面箭头由动力输入指向动力输出。

- 支持六方向放置。
- 使用扳手可绕传动轴旋转红石控制面。
- 按住 Shift 右键方块打开游戏内设置界面。
- 佩戴工程师护目镜可查看输入、输出和设置参数。
- 更新间隔可在 10–200 tick（0.5–10 秒）之间调整。
- 正确保留机械动力原生的应力与过载行为。

## 控制模式

- **跟随红石强度**：输出转速跟随红、蓝两侧信号差。
- **固定转速**：存在有效动力输入时输出设置的目标转速。
- **红石强度 × 倍率**：

```text
输出转速 = 初始转速 +（红色信号 - 蓝色信号）× 倍率
```

初始转速可在 0–256 RPM 之间设置。旋转方向跟随输入；没有有效动力输入时输出为 0。

## 兼容要求

| 组件 | 要求 |
| --- | --- |
| Minecraft | 1.21.1 |
| 模组加载器 | NeoForge 21.1.219+ |
| Create | 6.0.10+ |
| Create Aeronautics | 可选 |

客户端和服务端都需要安装本模组。

## 使用方法

1. 将机械动力连接到输入面，将机械连接到输出面。
2. 给红色、蓝色控制面输入红石信号。
3. 按住 Shift 右键打开设置界面。
4. 在数值上滚动滚轮调整；按住 Shift 时步长为 ×10。
5. 使用扳手点击任意传动轴端面，旋转红石控制面。

## 链接

- [GitHub 与问题反馈](https://github.com/z33awa/Redstone-Throttle)
- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/redstone-throttle)

## 许可

保留所有权利（All Rights Reserved）。
