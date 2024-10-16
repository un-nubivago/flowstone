# Flowstone

Flowstone makes ores renewable resources by modifying what blocks Lava turns into whenever it meets Water (or, in some cases, a Blue Ice block).

![Flowstone Showcase](img/Flowstone_Showcase_1.gif)

(In the GIF, I configured Flowstone to always generate some ore instead of Stone, for example purposes)

## Features

Since version 6.3, Flowstone offers various features that can be separately activated and deactivated through the configuration file.

### Deepslate Generators

```json
"allowDeepslateGenerators": true // enabled by default
```

With this feature enabled, trying to generate Stone or Cobblestone when deep enough underground (in most worlds, below y=8) will generate Deepslate and Cobbled Deepslate instead.

Note that this feature depends on the world generation options. Thus, if, for some reason, your world doesn't replace Stone and Cobblestone with Deepslate and Cobble Deepslate or does so at a different y-level, this feature will mirror such configurations.

### Worldly Generators

```json
"allowWorldlyGenerators": true // enabled by default
```

With this feature enabled, whenever Stone, Deepslate (with the previous feature), or Netherrack (with one of the following features) is to be generated, an ore block might be generated instead.

For each ore block (that is, for each block under the `c:ores` tag), the probability of it being generated depends on the world generation configuration, so on a vanilla world with the same distribution [documented on the wiki](https://minecraft.wiki/w/Ore).

This feature should automatically be compatible with every mod that adds new ores under the `c:ores` tag.

Finally, note that only normal ore blocks (like Diamond Ore) can replace Stone, only deepslate ore blocks (like Deepslate Diamond Ore) can replace Deepslate, and only netherrack ore blocks (like Quartz Ore or Ancient Debris) can replace Netherrack.

### Custom Generators

```json
"allowCustomGenerators": false // disabled by default
```

With this feature, one can define custom generators through datapacks.

<details>
<summary>Example</summary>

```tree
.
├── data
│   └── additional_generators
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

The rest of this example is on the project source page on GitHub, under the examples folder.

### Basalt Generation

```json
"enableBasaltGeneration": false // disabled by default
```

I know, I know, Basalt generation is already a vanilla Minecraft feature but since I freaking hate Basalt for its uselessness, I added this feature to disable its generation. Simple as that.

### Netherrack Generation

```json
"enableNetherrackGeneration": true // enabled by default
```

With this feature, whenever Lava meets a Blue Ice block in the Nether, and if no Basalt is to be generated, the Lava turns into a Netherrack block.

### Debug Mode

```json
"debugMode": null // hidden and disabled by default
```

When enabled, this feature forces Flowstone to generate the alternative blocks instead of the default ones, as if setting the chances for those blocks to be generated to 100% (the feature GIF had been generated with this feature enabled, for instance).

One should use this feature only when and if they are testing which blocks Worldly or Custom generators (for it works only for those two features) can generate since using it through a normal playthrough is practically cheating.
