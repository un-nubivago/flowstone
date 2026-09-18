# Flowstone

**Flowstone** makes ores renewable resources by modifying what blocks **Lava** turns into whenever it meets **Water** (or, in some cases, a block of **Blue Ice**).

![Flowstone Showcase](img/Flowstone_Showcase_1.gif)

_(In the GIF, I configured **Flowstone** to always generate some ore instead of **Stone**, for example purposes. See [below](#debug-mode) for details.)_

## Features

Since version 6.3, **Flowstone** offers several features you can enable or disable separately in the configuration file.

You can usually find **Flowstone**'s configuration file at `<your-minecraft-instance>/config/flowstone.json`.

### Deepslate Generators

```json
"allowDeepslateGenerators": true // enabled by default
```

With this feature enabled, trying to generate **Stone** or **Cobblestone** when deep enough underground (in most worlds, below $y=8$) will generate Deepslate and Cobbled Deepslate instead.

> [!NOTE]
>
> This feature depends on the world generation options. So, if your world doesn't generate **Deepslate** (or generates it at a different y-level), this feature will mirror that configuration.

### Worldly Generators

```json
"allowWorldlyGenerators": true // enabled by default
```

With this feature enabled, whenever **Stone**, **Deepslate** (with the previous feature), or **Netherrack** (with one of the following features) is about to be generated, it might generate an appropriate ore block instead.

For each ore block (that is, each block under the `c:ores` tag), the probability of it being generated depends on the world generation configuration, so on a vanilla world it follows the same distribution [documented on the wiki](https://minecraft.wiki/w/Ore).

This feature should automatically be compatible with every mod that adds new ores under the `c:ores` tag.

Finally, note that only normal ore blocks (like **Diamond Ore**) can replace **Stone**, only deepslate ore blocks (like **Deepslate Diamond Ore**) can replace **Deepslate**, and only netherrack ore blocks (like **Quartz Ore** or **Ancient Debris**) can replace **Netherrack**.

### Custom Generators

```json
"allowCustomGenerators": false // disabled by default
```

With this feature enabled, one can define custom generators through datapacks.

<details>
<summary>Example</summary>

```tree
<datapack_name>.zip
├── data
│   └── <datapack_name>
│       └── flowstone
│           └── generators
│               ├── andesite.json
│               ├── diorite.json
│               ├── granite.json
│               └── tuff.json
└── pack.mcmeta
```

```json
{
    // The block to be replaced
    "replace": "minecraft:cobbled_deepslate",
    // The block to replace the previous with
    "with": "minecraft:tuff",
    // The cache of replacement
    "chance": 0.3
}
```

</details>

The rest of this example is on the project source page on GitHub, under the [examples folder](./examples/).

### Basalt Generation

```json
"enableBasaltGeneration": false // disabled by default
```

I know, I know, **Basalt** generation is already a vanilla **Minecraft** feature, but since I freaking hate **Basalt** for its uselessness, I added this feature to disable its generation. Simple as that.

### Netherrack Generation

```json
"enableNetherrackGeneration": true // enabled by default
```

With this feature enabled, whenever **Lava** meets a block of **Blue Ice** in the **Nether**, and no **Basalt** would be generated, the **Lava** turns into a block of **Netherrack**.

### End Stone Generation

```json
"enableEndStoneGeneration": true // enabled by default
```

With this feature enabled, whenever **Stone** or **Cobblestone** is about to generate in **The End**, a block of **End Stone** generates instead.

Alas, this feature makes ores' regeneration impossible in **The End**. Nonetheless, I set it as enabled by default because I like the idea that **The End** is _corrupted_ and, thus, that (almost) every block generated within it gets corrupted as well.

### Debug Mode

```json
"debugMode": null // hidden and disabled by default
```

When enabled, this feature forces **Flowstone** to generate the alternative blocks instead of the default ones, as if setting the chances for those blocks to be generated to 100% (for instance, the GIF at the head of this page was generated with this feature enabled).

Use this feature only when you're testing which blocks _Worldly_ or _Custom_ generators can generate (it works only for those two features), since using it in a normal playthrough is practically cheating.
