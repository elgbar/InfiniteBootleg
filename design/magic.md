# The Magic System

Main component is a wand that is configured differently to cast different spells.
These spells should be customizable by the player.

Each staff is created by combining a type of wood and a gem. The wood will determine the power of the staff, the gem will determine the spell, Rings can be used to modify the
spell.
Each staff has 1+ slots and 0+ ring slots. The slots types are determined by the wood type.

Each staff can be "forged" with a different spell. The wood used will determine how powerful the spell is with the number of slots available.
The spells are not "learned" by the player, but are "forged" into the staff. The player can only use the staff to cast spells.

The power of a staff is specific to each spell. Some attributes can be increased by rings for all staffs, such as casting speed

## Staff

---

Staff determines how powerful a spell can be combined into.

### Removing gems and rings (not implemented)

#### Rings

Rings can be removed at any time without any cost or degradation.

#### Gems

There are two ways to remove gems from a staff.

1. Dragging gems out

* The wood is recovered intact, but the gem will be scratched and its gem rating will be decreased.

2. Destroy the wood

* The gem can be recovered intact, but the wood will be destroyed in the process.

### Wood types

All staff are made from wood, but can be made from different kind of wood.

Cast delay will be affected by wood rating's absolute power.

The cast delay is calculated is split into two components, to try and prevent "machine-gunning".
They are called the fixed and the variable components.
The fixed delay is 40% of the base delay, and the variable delay is 60% of the base delay.
Only the variable delay can be affected by rings

**Note:** There is a hard minimum cast delay of 150ms to prevent "machine-gunning" (game-breaking speeds.)

| Wood type   | Gem slots | Ring slots | drying rate | Cast delay at 100% | Special modifier                                                                  | Description                                                                                   |
|-------------|-----------|------------|-------------|--------------------|-----------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------|
| Birch       | 1         | 0          | 100%        | 1000 ms            | N/A                                                                               | A weakly magical, but common, wood type known for its light color.                            |
| Birch       | 1         | 1          | 100%        | 1000 ms            | N/A                                                                               | A slightly more magical wood to the common birch                                              |
| Aerowode    | 1         | 1          | 100%        | 750 ms             | Player gravity is lowered by up to 20% from normal. Imcompatible with `Lead` ring | A rare and lightweight wood that seems to almost float in the air                             |
| Red wood    | 2         | 2          | 200%        | 500 ms             | Will start fires, at random, where the spell lands                                | The wood crackles and smokes, making it dry very quickly                                      |
| driftwood   | 1         | 1          | 25%         | 900 ms             | Works while under water                                                           | Even when dried, drops of water forms around its base                                         |
| Wisted wood | 2         | 1          | 100%        | 300 ms             | N/A                                                                               | A magical twisted and gnarled wood, known for its ring capacity and speed                     |
| Trekant     | 3         | 2          | 100%        | 300 ms             | N/A                                                                               | A legendary triangular cross-section wood, prized for its unique shape and magical properties |

### Wood rating

The time from cut to use will determine the power of the wood. The longer the wood is left to dry, the more powerful it will be.
When crafting to a staff the drying will stop.

The drying rate is also affected by the wood type. Some wood types will dry faster than others.

**Note:** The absolute power starts at 100% so that spells are balanced when freshly cut.

| Wood rating | Absolute power | Relative power increase | Drying duration (real time) | Drying duration total | Description                                                                                         |
|-------------|----------------|-------------------------|-----------------------------|-----------------------|-----------------------------------------------------------------------------------------------------|
| Freshly cut | 100%           | N/A                     | 0 seconds                   | 0 seconds             | This wood is still green, it will not be very powerful                                              |
| Dried       | 120%           | +20%                    | 1 hour                      | 1 hour                | This wood has been left to dry for a while, it will be more powerful                                |
| Aged        | 140%           | +20%                    | 2 hours                     | 3 hours               | This wood has been left to dry for a long time, it will be powerful                                 |
| Ancient     | 160%           | +20%                    | 5 hours                     | 8 hours               | This wood has been left to dry for a very long time, it will be very powerful                       |
| Petrified   | 170%           | +10%                    | 4 hours                     | 12 hours              | This wood has been left to dry for a extremely long time, there is no point in drying it any longer |

## Gems

---

Each gem will produce a different spell. The power of the spell is determined by the quality of the gem (flaw rating). Gems have names after precious stones

### Types of gems

| Gem type   | Power meaning                                     | Effect when power at 100% | Description                                                                                                                                           |
|------------|---------------------------------------------------|---------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------|
| Diamond    | Greater break radius, i.e,. more blocks at a time | 10 block radius           | Allows to break blocks, slow and a small radius, but will always give blocks to the player                                                            |
| Quartz     | Greater build radius, i.e,. more blocks at a time | 10 block radius           | will create blocks in a radius (radius is customizable during use of the staff)                                                                       |
| Trapiche   | Greater teleport radius.                          | 10000 block radius        | Teleports the player to the location of the staff, The staff must be placed there by a player. Needs a "twin" staff tuned to the same tag to teleport |
| Sky gem    | More damage per thunderbolt                       | 100% of players health    | Summons a thunderbolt to the location of the staff, can be used to create a defensive barrier                                                         |
| Blast opal | Greater explosion                                 | 100 blocks                | Destroys blocks in a radius, a low % will be mined, the rest will be destroyed                                                                        |
| Dartle Gem | Dart further                                      | 10 blocks                 | Allow player to teleport short distances (i.e., dart)                                                                                                 |
| Obsidian   | More damage pr projectile                         | 100% of players health    | Destroys blocks in a radius, a low % will be mined, the rest will be destroyed                                                                        |

### Flaw ratings - more flawed decrease the power of the gem

Note that the flaw rating will only affect the power of the gem, not the whole staff

| Flaw rating | Absolute power | Relative Power decrease (this - above) | Description                                               |
|-------------|----------------|----------------------------------------|-----------------------------------------------------------|
| Flawless    | 100%           | N/A                                    | I didn't know this perfection of existed anymore!         |
| Scratched   | 85%            | -15%                                   | A few surface scratches shouldn't be too much of an issue |
| Chipped     | 70%            | -15%                                   | It has a sharp edge for the missing part                  |
| Cracked     | 50%            | -20%                                   | Someone hit this with a hammer!                           |
| Fractured   | 25%            | -25%                                   | At least its not ruined                                   |
| Ruined      | 1%             | -24%                                   | This is useless                                           |

## Rings

---

Rings can be used to modify the spell-entity and/or the holder of the staff.

Each ring will have a different effect on the spell.
Rings are named after elements of the periodic table, and should somewhat relate to what they are used for in the real world.

### Special rings

Some rings will have a special effect on the spell, these will not have a flaw rating.

| Special Ring type | Effect                                                                        | Name Reason                               |
|-------------------|-------------------------------------------------------------------------------|-------------------------------------------|
| Scandium          | visualize where the spell will land/travel                                    | Name like it "scans" the surrounding area |
| Lead              | the spell will be affected by gravity                                         | Is known to be heavy                      |
| Phosphorus        | Adds a cone of light to each spell and will a halo of light around the player | Used in flares                            |

### Normal rings

To give some meat here is the boring list of all the "normal" rings

| Ring type | Effect                                                          | Name Reason           |
|-----------|-----------------------------------------------------------------|-----------------------|
| Iron      | Increases gem power                                             | Iron symbolises power |
| Aluminium | Increases spell range                                           | Lightweight material  |
| Copper    | Increases incarnation speed (decreases delay between each cast) |                       |
| Tin       | Increases spell speed (Speed of spell-entity)                   |                       |
| Titanium  | Increases player movement speed                                 | Lightweight material  |

Each normal ring have a flaw rating which indicate how much they linearly increase the gem effect.
Even a ring of the lowest quality is still better than no ring at all.
If a ring had a negative effect, then no player would have equipped it.

| Ring flaw rating | +Gem Effect% | Relative increase | Description                                                     |
|------------------|--------------|-------------------|-----------------------------------------------------------------|
| Perfect          | 175%         | 15%               | I didn't know this perfection of existed!                       |
| Nearly Perfect   | 160%         | 10%               | Too bad someone dropped it, it should've been perfect           |
| Minor scratch    | 150%         | 5%                | A small few surface scratches shouldn't be too much of an issue |
| Major scratch    | 145%         | 5%                | A large few surface scratches, might be an issue                |
| Minor chip       | 140%         | 5%                | Someone got a matching small piece of this                      |
| Major chip     | 135%     | 10%               | Someone got a matching big piece of this                        |
| Large fragment | 125%     | 10%               | Almost whole, just missing most of it                           |
| Small fragment | 115%     | 5%                | More than the pieces                                            |
| Some pieces    | 110%     | 5%                | Pieces, nothing more                                            |
| Small pieces   | 105%     | 4%                | At least its not _just_ dust                                    |
| Dust           | 101%     | 1%                | This is almost useless                                          |
