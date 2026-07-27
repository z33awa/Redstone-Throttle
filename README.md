# Create: Redstone Throttle

![Create: Redstone Throttle](src/main/resources/icon.png)

[![Minecraft 1.21.1](https://img.shields.io/badge/Minecraft-1.21.1-62B47A)](https://www.minecraft.net/)
[![NeoForge](https://img.shields.io/badge/Loader-NeoForge-EF7B45)](https://neoforged.net/)
[![Create 6.0.10+](https://img.shields.io/badge/Create-6.0.10%2B-CDB894)](https://www.curseforge.com/minecraft/mc-mods/create)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![CurseForge](https://img.shields.io/badge/Download-CurseForge-F16436)](https://www.curseforge.com/minecraft/mc-mods/redstone-throttle)
[![Modrinth](https://img.shields.io/badge/Download-Modrinth-00AF5C)](https://modrinth.com/mod/createredstone-throttle)

A compact kinetic controller for the Create mod. Use redstone signals to adjust an
existing rotation without replacing your entire transmission.

English · [简体中文](#简体中文)

## Features

- Six-axis placement with dedicated kinetic input and output faces.
- Two rotatable redstone control faces: red increases speed and blue decreases it.
- Three control modes for different automation needs.
- Adjustable update interval from 10 to 200 ticks (0.5–10 seconds).
- Stress and overstress behavior integrated with Create's kinetic network.
- Goggle tooltips for live input, output, mode, and configuration values.
- Optional compatibility with Create Aeronautics.

The arrow on the side points from the kinetic input toward the output. The red and
blue control faces can receive independent redstone signals.

## Control modes

### Follow signal strength

The output speed is derived from the difference between the red and blue signals.
This is the simplest mode for proportional redstone control.

### Fixed speed

The block outputs a configured target speed while it has a valid kinetic input.

### Signal strength × multiplier

The output speed is calculated as:

```text
output speed = initial speed + (red signal - blue signal) × multiplier
```

Initial speed can be configured from 0 to 256 RPM. Rotation direction follows the
kinetic input, and the output becomes zero when no valid input is available.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.219 or newer
- Create 6.0.10 or newer

Create Aeronautics is optional. Install the mod on both the client and the server.

## Installation

1. Install NeoForge for Minecraft 1.21.1.
2. Install Create 6.0.10 or newer.
3. Put the Redstone Throttle jar in the `mods` folder on both client and server.
4. Optionally install Create Aeronautics.

## Usage

1. Connect Create power to the input face and your machinery to the output face.
2. Feed redstone signals into the red and blue control faces.
3. Hold Shift and right-click the block to open its configuration screen.
4. Scroll over a value to adjust it; hold Shift while scrolling for ×10 steps.
5. Use a wrench on a shaft end to rotate the redstone control faces around the
   transmission axis.

## Building from source

This project uses the Gradle wrapper and Java 21:

```powershell
.\gradlew.bat build
```

The built jar is written to `build/libs`.

## Links

- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/redstone-throttle)
- [Modrinth](https://modrinth.com/mod/createredstone-throttle)
- [Issue tracker](https://github.com/z33awa/Redstone-Throttle/issues)

## License

Released under the [MIT License](LICENSE). Copyright © 2026 z33awa.

---

## 简体中文

**Create: Redstone Throttle（机械动力：红石调速器）** 是一个紧凑的动力控制方块，
可以使用红石信号调节已有传动系统的转速。

### 功能

- 支持六个方向放置，拥有独立的动力输入面和输出面。
- 红色与蓝色红石控制面可绕传动轴旋转；红色加速，蓝色减速。
- 提供三种控制模式，适合不同的自动化场景。
- 更新间隔可在 10–200 tick（0.5–10 秒）之间调整。
- 接入机械动力原生动力网络，正确响应应力与过载状态。
- 佩戴工程师护目镜时显示输入、输出、模式和配置参数。
- 可选兼容 Create Aeronautics。

方块侧面的箭头由动力输入指向动力输出。红色和蓝色控制面可以分别接收红石信号。

### 控制模式

- **跟随红石强度**：根据红、蓝两侧的信号差计算输出转速。
- **固定转速**：存在有效动力输入时，输出设置的目标转速。
- **红石强度 × 倍率**：

```text
输出转速 = 初始转速 +（红色信号 - 蓝色信号）× 倍率
```

倍率模式的初始转速可在 0–256 RPM 之间调整。旋转方向跟随动力输入；没有有效输入时，
输出转速为 0。

### 依赖

- Minecraft 1.21.1
- NeoForge 21.1.219 或更高版本
- Create 6.0.10 或更高版本

Create Aeronautics 为可选依赖。本模组需要同时安装在客户端与服务端。

### 安装与使用

1. 安装 Minecraft 1.21.1 对应的 NeoForge。
2. 安装 Create 6.0.10 或更高版本。
3. 将本模组 jar 放入客户端与服务端的 `mods` 文件夹。
4. 将动力接入输入面，将机械连接到输出面。
5. 给红色、蓝色控制面输入红石信号。
6. 按住 Shift 右键方块打开设置界面。
7. 将鼠标移到数值上滚动滚轮进行调整；按住 Shift 滚动时步长为 ×10。
8. 使用扳手点击传动轴端面，可绕传动轴旋转红石控制面。

### 从源码构建

项目使用 Gradle Wrapper 和 Java 21：

```powershell
.\gradlew.bat build
```

构建产物位于 `build/libs`。

### 许可

本项目采用 [MIT 许可证](LICENSE)。Copyright © 2026 z33awa。
