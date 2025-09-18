package org.ammck.games

import org.ammck.game.WaypointType

data class Waypoint(
    val type: WaypointType,
    val index: Int
)