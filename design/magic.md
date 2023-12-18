# The Magic System

Main component is a wand that is configured differently to cast different spells.
These spells should be customizable by the player.

Each staff is created by combinng a type of wood and a gem. The wood will determine the power of the staff, the gem will determine the spell, Rings can be used to modify the spell.
Each staff has 1+ slots and 0+ ring slots. The slots types are determined by the wood type.

Each staff can be "forged" with a different spell. The wood used will determine how powerful the spell is.
The spells are not "learned" by the player, but are "forged" into the staff. The player can only use the staff to cast spells.

The power of a staff is specific to each spell. Some attributes can be increased by rings for all staffs, such as casting speed

## Staff

Staff determines how powerful a spell can be combined into.

### Wood types

All staff are made from wood, but can be made from different kind of wood.

| Wood type   | Gem slots | Ring slots | Special modifier                   | drying rate | Description                                           |
|-------------|-----------|------------|------------------------------------|-------------|-------------------------------------------------------|
| Birch       | 1         | 0          | N/A                                | 100%        | The most common wood type                             |
| Aerowode    | 1         | 2          | Spells are not affected by gravity | 100%        | The wood seems to no be affected by gravity           |
| Red wood    | 1         | 0          | Will start fires, at random        | 200%        | The wood crackles and smokes                          |
| driftwood   | 1         | 3          | Works while under water            | 25%         | Even when dried, drops of water forms around its base |
| Wisted wood | 2         | 1          | N/A                                | 100%        | The wood is twisted to fit two gems                   |
| Trekant     | 3         | 2          | N/A                                | 100%        | The wood is twisted to fit three gems                 |

### Wood rating

The time from cut to use will determine the power of the wood. The longer the wood is left to dry, the more powerful it will be.
When crafting to a staff the drying will stop.

The drying rate is also affected by the wood type. Some wood types will dry faster than others.

| Wood rating | Absolute power | Relative power increase | Drying duration (real time) | Drying duration total | Description                                                                   |
|-------------|----------------|-------------------------|-----------------------------|-----------------------|-------------------------------------------------------------------------------|
| Freshly cut | 30%            | N/A                     | 0 seconds                   | 0 seconds             | This wood is still green, it will not be very powerful                        |
| Dried       | 50%            | +20%                    | 1 hour                      | 1 hour                | This wood has been left to dry for a while, it will be more powerful          |
| Aged        | 70%            | +20%                    | 2 hours                     | 3 hours               | This wood has been left to dry for a long time, it will be very powerful      |
| Ancient     | 90%            | +20%                    | 5 hours                     | 8 hours               | This wood has been left to dry for a very long time, it will be very powerful |
| Petrified   | 100%           | +10%                    | 10 hours                    | 18 hours              | This wood has been left to dry for a very very long time, it will be powerful |

## Gems

Each gem will produce a different spell. The power of the spell is determined by the quality of the gem (flaw rating).

### Types of gems

| Gem type   | Power meaning                                     | Effect when power at 100% | Description                                                                                                                                           |
|------------|---------------------------------------------------|---------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------|
| Diamond    | Greater break radius, i.e,. more blocks at a time | 10 block radius           | Allows to break blocks, slow and a small radius, but will always give blocks to the player                                                            |
| Enineer    | Greater build radius, i.e,. more blocks at a time | 10 block radius           | will create blocks in a radius (radius is customizable during use of the staff)                                                                       |
| Trapiche   | Greater teleport radius.                          | 10000 block radius        | Teleports the player to the location of the staff, The staff must be placed there by a player. Needs a "twin" staff tuned to the same tag to teleport |
| Sky gem    | More damage per thunderbolt                       | 100% of players health    | Summons a thunderbolt to the location of the staff, can be used to create a defensive barrier                                                         |
| Blast opal | Greater explosion                                 | 100 blocks                | Destroys blocks in a radius, a low % will be mined, the rest will be destroyed                                                                        |
| Dartle Gem | Dart further                                      | 10 blocks                 | Allow player to teleport short distances (i.e., dart)                                                                                                 |
| Obsidian   | More damage pr projectile                         | 100% of players health    | Destroys blocks in a radius, a low % will be mined, the rest will be destroyed                                                                        |

### Flaw ratings - more flawed decrease the power of the gem

| Flaw rating | Absolute power | Relative Power decrease (this - above) | Description                                               |
|-------------|----------------|----------------------------------------|-----------------------------------------------------------|
| Flawless    | 100%           | N/A                                    | I didn't know this perfection of existed anymore!         |
| Scratched   | 85%            | -15%                                   | A few surface scratches shouldn't be too much of an issue |
| Chipped     | 70%            | -15%                                   | Someone hit this on with a hammer!                        |
| Fractured   | 50%            | -20%                                   | Not bad, not great                                        |
| Shattered   | 25%            | -25%                                   | At least its not broken                                   |
| Ruined      | 0%             | -25%                                   | This is useless                                           |

## Rings

Rings can be used to modify the spell. Each ring will have a different effect on the spell.

### Special rings

Some rings will have a special effect on the spell, these will not have a flaw rating.

| Special Ring type | Effect                                     |
|-------------------|--------------------------------------------|
| palantir          | visualize where the spell will land/travel |
| anti-gravity      | the spell will not be affected by gravity  |

### Normal rings

To give some meat here is the boring list of all the "normal" rings

| Ring type  | Effect                                                                        |
|------------|-------------------------------------------------------------------------------|
| Opal       | Gem power                                                                     |
| Emerald    | spell range                                                                   |
| Ruby       | incarnation speed (decreases delay between each cast)                         |
| Sapphire   | spell speed                                                                   |
| Aeromir    | Allow the player to move faster                                               |
| Bright Gem | Adds a cone of light to each spell and will a halo of light around the player |

| Flaw rating       | +Power% | Relative increase | Description                                                     |
|-------------------|---------|-------------------|-----------------------------------------------------------------|
| Flawless          | 60%     | 10%               | I didn't know this perfection of existed anymore!               |
| Minorly scratched | 50%     | 5%                | A small few surface scratches shouldn't be too much of an issue |
| Majorly scratched | 45%     | 5%                | A large few surface scratches, might be an issue                |
| Minorly chipped   | 40%     | 5%                | Someone got a matching small piece of this                      |
| Majorly chipped   | 35%     | 10%               | Someone got a matching big piece of this                        |
| Large fragment    | 25%     | 10%               | Almost whole, just missing most of it                           |
| Small fragment    | 15%     | 5%                | More than the pieces                                            |
| Some pieces       | 10%     | 5%                | Pieces, nothing more                                            |
| Small pieces      | 5%      | 4%                | At least its not _just_ dust                                    |
| Dust              | 1%      | 1%                | This is useless                                                 |
