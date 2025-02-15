# Network protocol

## Technologies

InfiniteBootleg uses protobuf over http for communication between the client and the (netty) server.

The definition is found in the [packets.proto](../core/src/main/proto/packets.proto) file.

Each packet has a defined `type` which specified how the packet should be handled. The types are split up into three categories

1. SB - Serverbound packets, sent from the client to the server
2. CB - Clientbound packets, sent from the server to the client
3. DX - Duplex packets, can be sent either from the player or the server

## Login

todo

## Movement

Movement is communicated between the server and clients with
the [DX_MOVE_ENTITY](https://github.com/elgbar/InfiniteBootleg/blob/b5c995287a49fff6e84d726a7b2b4eb0d6bb52f2/core/src/main/proto/packets.proto#L18) packet with
a [MoveEntity](https://github.com/elgbar/InfiniteBootleg/blob/b5c995287a49fff6e84d726a7b2b4eb0d6bb52f2/core/src/main/proto/packets.proto#L86-L92) message.

The message contains the position, velocity, and which way the entity looks.

### Client moving authorized entity

Clients are allowed to move authorized entities. This is currently only the player entity.

The client send the server its position whenever the velocity of the entity is non-zero.

When the server receives a move entity packet, it checks the difference between the packets position and the last entity position.
If this is greater than the maximum allowed distance, the server will teleport the client back to the last allowed position.
The max distance is multiplied by the time since the last move packet was received, capping at 1 second.

If the player is falling the maximum allowed distance is increased.

* Falling is defined as having a negative vertical velocity less than -10f
* Maximum normal blocks per second is 4 blocks per second.
* Maximum falling blocks per second is 8 blocks per second.

### Server broadcasting movement

The server will periodically broadcast the position of entities to nearby clients.

A move packet is sent every 1s to all clients which can see an entity if the entity is stationary.

If the entity is moving, then the server will send a move packet every server tick, except the entity itself.
