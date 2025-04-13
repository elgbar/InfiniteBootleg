# World generation

## Biomes

Biomes are split into three categories:

* super-surface biomes, unused, always air
* surface biomes, dictates the terrain height,
* sub-surface biomes, everything below ground,

### surface biomes

The surface biomes are have a couple of parameters

* height, when air becomes ground
* depth, when ground becomes sub-sub surface
* cloud-base, how far up the super-surface biomes start.

These are created with only the world-x-coordinate. so they are simple 1D lines in the world

#### biome types

The biome type is defined by thge

##### plains

##### mountains

##### desert

### super-surface biomes

Currently, not prioritized, but can be used to create interesting terrain in the sky.

For now these are just air

### sub-surface biomes

The most complex biomes, taking both the x and y world coordinate as input.
These are used to create caves, ores, and other underground features.

