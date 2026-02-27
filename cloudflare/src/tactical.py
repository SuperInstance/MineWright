"""
Tactical Decision Module for Minecraft Agent Reflex

Provides fast threat assessment, hazard detection, and quick combat decisions.
All functions are optimized for <50ms response time in Cloudflare Workers.
"""

from dataclasses import dataclass
from typing import Optional
import math


@dataclass
class Position:
    """3D position in Minecraft world"""
    x: float
    y: float
    z: float


@dataclass
class Threat:
    """Combat threat assessment"""
    entity_type: str
    position: Position
    distance: float
    danger_level: float  # 0.0 to 1.0
    estimated_health: int


@dataclass
class Hazard:
    """Environmental hazard"""
    hazard_type: str
    position: Position
    severity: float


@dataclass
class TacticalDecision:
    """Quick tactical decision response"""
    action: str
    priority: float
    reasoning: str
    target_position: Optional[Position] = None
    confidence: float = 1.0


# Entity danger ratings based on Minecraft mob difficulty
ENTITY_DANGER_RATINGS = {
    # High danger (5+)
    "warden": 10.0,
    "ender_dragon": 10.0,
    "wither": 10.0,
    "witch": 6.0,
    "vindicator": 5.0,
    "evoker": 5.0,
    "piglin_brute": 5.0,

    # Medium-high danger (3-4)
    "creeper": 4.0,
    "zombie_pigman": 4.0,
    "blaze": 3.5,
    "ghast": 3.5,
    "bogged": 3.0,  # Skeleton variant

    # Medium danger (2)
    "skeleton": 2.5,
    "stray": 2.5,
    "spider": 2.0,
    "cave_spider": 2.5,
    "drowned": 2.0,
    "husk": 2.0,
    "phantom": 2.5,

    # Low-medium danger (1)
    "zombie": 1.5,
    "zombie_villager": 1.5,
    "slime": 1.0,
    "silverfish": 1.0,

    # Low danger (0.5)
    "enderman": 0.5,  # Only dangerous if provoked

    # Neutral (0)
    "piglin": 0.0,
    "villager": 0.0,
    "animal": 0.0,
}


def assess_threats(
    agent_position: Position,
    nearby_entities: list[dict],
    current_health: int,
    combat_score: float = 0.5,
) -> list[Threat]:
    """
    Assess combat threats from nearby entities.

    Args:
        agent_position: Current agent position
        nearby_entities: List of nearby entity data from game
        current_health: Agent's current health (0-20)
        combat_score: Agent's combat capability (0.0 to 1.0)

    Returns:
        List of threats sorted by danger_level (descending)
    """
    threats = []

    for entity in nearby_entities:
        entity_type = entity.get("type", "unknown")
        entity_pos = Position(
            x=entity.get("x", 0),
            y=entity.get("y", 0),
            z=entity.get("z", 0),
        )

        # Calculate distance
        distance = math.sqrt(
            (agent_position.x - entity_pos.x) ** 2 +
            (agent_position.y - entity_pos.y) ** 2 +
            (agent_position.z - entity_pos.z) ** 2
        )

        # Get base danger rating for entity type
        base_danger = ENTITY_DANGER_RATINGS.get(entity_type, 1.0)

        # Calculate danger level based on multiple factors
        danger_level = calculate_danger_level(
            base_danger=base_danger,
            distance=distance,
            agent_health=current_health,
            combat_score=combat_score,
        )

        # Skip low-threat entities (danger < 0.3)
        if danger_level < 0.3:
            continue

        threat = Threat(
            entity_type=entity_type,
            position=entity_pos,
            distance=distance,
            danger_level=danger_level,
            estimated_health=entity.get("health", 20),
        )

        threats.append(threat)

    # Sort by danger level (highest first)
    threats.sort(key=lambda t: t.danger_level, reverse=True)

    # Return top 5 threats
    return threats[:5]


def calculate_danger_level(
    base_danger: float,
    distance: float,
    agent_health: int,
    combat_score: float,
) -> float:
    """
    Calculate overall danger level for a threat.

    Factors:
    - Base entity danger rating
    - Distance (closer = more dangerous)
    - Agent health (lower health = more dangerous)
    - Agent combat capability (higher = less dangerous)

    Returns:
        Danger level from 0.0 to 1.0
    """
    # Distance factor: <5 blocks = very dangerous, >20 blocks = minimal
    distance_factor = max(0.0, 1.0 - (distance / 20.0))

    # Health factor: <5 hearts = double danger, >15 hearts = normal
    health_factor = 1.0
    if agent_health < 5:
        health_factor = 2.0
    elif agent_health < 10:
        health_factor = 1.5
    elif agent_health > 15:
        health_factor = 0.8

    # Combat factor: Higher combat score reduces danger
    combat_factor = max(0.5, 1.0 - combat_score * 0.5)

    # Calculate final danger (normalize to 0.0 - 1.0)
    raw_danger = (
        base_danger *
        distance_factor *
        health_factor *
        combat_factor
    )

    # Clamp to 0.0 - 1.0
    return min(1.0, max(0.0, raw_danger / 10.0))


def detect_hazards(
    agent_position: Position,
    nearby_blocks: list[dict],
) -> list[Hazard]:
    """
    Detect environmental hazards in nearby blocks.

    Args:
        agent_position: Current agent position
        nearby_blocks: List of nearby block data from game

    Returns:
        List of hazards sorted by severity (descending)
    """
    hazards = []

    # Define dangerous block types
    dangerous_blocks = {
        "lava": {"type": "lava", "severity": 1.0, "radius": 3},
        "fire": {"type": "fire", "severity": 0.7, "radius": 2},
        "magma_block": {"type": "fire", "severity": 0.5, "radius": 1},
        "campfire": {"type": "fire", "severity": 0.3, "radius": 1},
        "sweet_berry_bush": {"type": "damage", "severity": 0.2, "radius": 0},
        "wither_rose": {"type": "damage", "severity": 0.4, "radius": 0},
        "cactus": {"type": "damage", "severity": 0.3, "radius": 0},
    }

    # Check for fall hazard (empty space below)
    blocks_below = [b for b in nearby_blocks if b.get("y", 0) < agent_position.y - 1]
    solid_blocks_below = [b for b in blocks_below if b.get("solid", False)]

    if len(blocks_below) > 5 and len(solid_blocks_below) == 0:
        # Potential fall hazard
        drop_distance = len(blocks_below)
        severity = min(1.0, drop_distance / 10.0)  # 10+ blocks = fatal

        hazards.append(Hazard(
            hazard_type="fall",
            position=Position(agent_position.x, agent_position.y - 1, agent_position.z),
            severity=severity,
        ))

    # Check for suffocation hazard (solid blocks at head level)
    head_level_blocks = [
        b for b in nearby_blocks
        if abs(b.get("y", 0) - agent_position.y - 1) < 0.5
        and b.get("solid", False)
        and abs(b.get("x", 0) - agent_position.x) < 0.5
        and abs(b.get("z", 0) - agent_position.z) < 0.5
    ]

    if head_level_blocks:
        hazards.append(Hazard(
            hazard_type="suffocation",
            position=Position(agent_position.x, agent_position.y + 1, agent_position.z),
            severity=0.9,
        ))

    # Check for dangerous blocks
    for block in nearby_blocks:
        block_type = block.get("type", "")
        block_pos = Position(
            x=block.get("x", 0),
            y=block.get("y", 0),
            z=block.get("z", 0),
        )

        if block_type in dangerous_blocks:
            info = dangerous_blocks[block_type]
            distance = math.sqrt(
                (agent_position.x - block_pos.x) ** 2 +
                (agent_position.y - block_pos.y) ** 2 +
                (agent_position.z - block_pos.z) ** 2
            )

            if distance <= info["radius"]:
                # Reduce severity based on distance
                adjusted_severity = info["severity"] * (1.0 - distance / info["radius"])

                hazards.append(Hazard(
                    hazard_type=info["type"],
                    position=block_pos,
                    severity=adjusted_severity,
                ))

    # Sort by severity (highest first)
    hazards.sort(key=lambda h: h.severity, reverse=True)

    return hazards[:5]


def get_quick_decision(
    threats: list[Threat],
    hazards: list[Hazard],
    combat_score: float = 0.5,
    current_mission: Optional[str] = None,
) -> TacticalDecision:
    """
    Generate quick tactical decision based on threats and hazards.

    Decision priority:
    1. Fatal hazards (severity > 0.8) -> Flee immediately
    2. High danger threats (danger > 0.7) -> Flee or prepare combat
    3. Combat situation -> Attack or shield
    4. Low danger -> Continue mission

    Args:
        threats: List of assessed threats
        hazards: List of detected hazards
        combat_score: Agent's combat capability (0.0 to 1.0)
        current_mission: Current mission task

    Returns:
        Tactical decision with action and reasoning
    """
    # Check for fatal hazards first
    fatal_hazards = [h for h in hazards if h.severity > 0.8]
    if fatal_hazards:
        return flee_from_hazard(fatal_hazards[0])

    # Check for high danger threats
    critical_threats = [t for t in threats if t.danger_level > 0.7]
    if critical_threats:
        if combat_score > 0.6:
            return combat_decision(critical_threats[0])
        else:
            return flee_from_threat(critical_threats[0])

    # Check for medium danger threats
    if threats and threats[0].danger_level > 0.4:
        if combat_score > 0.7:
            return combat_decision(threats[0])
        else:
            return hold_position(threats[0])

    # Check for medium hazards
    medium_hazards = [h for h in hazards if h.severity > 0.4]
    if medium_hazards:
        return avoid_hazard(medium_hazards[0])

    # No immediate threats - continue mission
    return continue_mission(current_mission)


def flee_from_hazard(hazard: Hazard) -> TacticalDecision:
    """Generate decision to flee from a hazard"""
    # Calculate flee position (move away from hazard)
    dx = 0 if hazard.position.x == 0 else (-1 if hazard.position.x > 0 else 1)
    dz = 0 if hazard.position.z == 0 else (-1 if hazard.position.z > 0 else 1)

    return TacticalDecision(
        action="flee",
        priority=1.0,
        reasoning=f"FATAL HAZARD: {hazard.hazard_type} at {hazard.position}",
        target_position=Position(
            x=hazard.position.x + dx * 5,
            y=hazard.position.y,
            z=hazard.position.z + dz * 5,
        ),
        confidence=1.0,
    )


def flee_from_threat(threat: Threat) -> TacticalDecision:
    """Generate decision to flee from a threat"""
    dx = 0 if threat.position.x == 0 else (-1 if threat.position.x > 0 else 1)
    dz = 0 if threat.position.z == 0 else (-1 if threat.position.z > 0 else 1)

    return TacticalDecision(
        action="flee",
        priority=0.9,
        reasoning=f"DANGER: {threat.entity_type} ({threat.distance:.1f} blocks)",
        target_position=Position(
            x=threat.position.x + dx * 10,
            y=threat.position.y,
            z=threat.position.z + dz * 10,
        ),
        confidence=0.95,
    )


def combat_decision(threat: Threat) -> TacticalDecision:
    """Generate combat decision"""
    # Close distance to engage
    dx = 0 if threat.position.x == 0 else (-1 if threat.position.x > 0 else 1)
    dz = 0 if threat.position.z == 0 else (-1 if threat.position.z > 0 else 1)

    return TacticalDecision(
        action="attack",
        priority=threat.danger_level,
        reasoning=f"ENGAGE: {threat.entity_type} at {threat.distance:.1f} blocks",
        target_position=Position(
            x=threat.position.x + dx * 2,
            y=threat.position.y,
            z=threat.position.z + dz * 2,
        ),
        confidence=0.8,
    )


def hold_position(threat: Threat) -> TacticalDecision:
    """Generate decision to hold position and prepare"""
    return TacticalDecision(
        action="shield",
        priority=threat.danger_level,
        reasoning=f"CAUTION: {threat.entity_type} detected - hold and observe",
        confidence=0.7,
    )


def avoid_hazard(hazard: Hazard) -> TacticalDecision:
    """Generate decision to avoid a hazard"""
    dx = 0 if hazard.position.x == 0 else (-1 if hazard.position.x > 0 else 1)
    dz = 0 if hazard.position.z == 0 else (-1 if hazard.position.z > 0 else 1)

    return TacticalDecision(
        action="dodge",
        priority=hazard.severity,
        reasoning=f"AVOID: {hazard.hazard_type} nearby",
        target_position=Position(
            x=hazard.position.x + dx * 3,
            y=hazard.position.y,
            z=hazard.position.z + dz * 3,
        ),
        confidence=0.8,
    )


def continue_mission(current_mission: Optional[str]) -> TacticalDecision:
    """Generate decision to continue current mission"""
    return TacticalDecision(
        action="hold",
        priority=0.1,
        reasoning=f"Area clear - continue mission: {current_mission or 'idle'}",
        confidence=0.9,
    )


def prioritize_resources(
    nearby_resources: list[dict],
    agent_position: Position,
    current_inventory: dict,
    mission_needs: list[str],
) -> list[dict]:
    """
    Prioritize nearby resources based on mission needs and current inventory.

    Args:
        nearby_resources: List of nearby resource blocks
        agent_position: Current agent position
        current_inventory: Current inventory counts
        mission_needs: Required resources for current mission

    Returns:
        List of resources sorted by priority (descending)
    """
    prioritized = []

    for resource in nearby_resources:
        resource_type = resource.get("type", "")
        resource_pos = Position(
            x=resource.get("x", 0),
            y=resource.get("y", 0),
            z=resource.get("z", 0),
        )

        # Calculate distance
        distance = math.sqrt(
            (agent_position.x - resource_pos.x) ** 2 +
            (agent_position.y - resource_pos.y) ** 2 +
            (agent_position.z - resource_pos.z) ** 2
        )

        # Calculate priority score
        priority = calculate_resource_priority(
            resource_type=resource_type,
            distance=distance,
            current_inventory=current_inventory,
            mission_needs=mission_needs,
        )

        prioritized.append({
            **resource,
            "priority": priority,
            "distance": distance,
        })

    # Sort by priority (highest first)
    prioritized.sort(key=lambda r: r["priority"], reverse=True)

    return prioritized[:10]


def calculate_resource_priority(
    resource_type: str,
    distance: float,
    current_inventory: dict,
    mission_needs: list[str],
) -> float:
    """
    Calculate priority score for a resource.

    Higher priority for:
    - Resources needed for current mission
    - Resources the agent doesn't have much of
    - Resources that are close by
    """
    priority = 0.0

    # Mission need bonus
    if resource_type in mission_needs:
        priority += 10.0

    # Inventory scarcity bonus
    current_count = current_inventory.get(resource_type, 0)
    if current_count < 10:
        priority += (10 - current_count) * 0.5

    # Distance penalty (closer is better)
    priority -= distance * 0.1

    return max(0.0, priority)


def adjust_pathfinding_for_threats(
    current_path: list[Position],
    threats: list[Threat],
    hazards: list[Hazard],
) -> list[Position]:
    """
    Adjust a planned path to avoid threats and hazards.

    Args:
        current_path: List of positions in current path
        threats: List of threats to avoid
        hazards: List of hazards to avoid

    Returns:
        Adjusted path that avoids dangers
    """
    if not current_path:
        return current_path

    # Simple path adjustment: check each waypoint
    adjusted_path = []
    avoidance_distance = 5.0

    for waypoint in current_path:
        # Check if waypoint is too close to threats
        too_dangerous = False

        for threat in threats:
            distance = math.sqrt(
                (waypoint.x - threat.position.x) ** 2 +
                (waypoint.y - threat.position.y) ** 2 +
                (waypoint.z - threat.position.z) ** 2
            )

            if distance < avoidance_distance and threat.danger_level > 0.5:
                too_dangerous = True
                break

        # Check if waypoint is too close to hazards
        if not too_dangerous:
            for hazard in hazards:
                distance = math.sqrt(
                    (waypoint.x - hazard.position.x) ** 2 +
                    (waypoint.y - hazard.position.y) ** 2 +
                    (waypoint.z - hazard.position.z) ** 2
                )

                if distance < avoidance_distance and hazard.severity > 0.4:
                    too_dangerous = True
                    break

        # Keep safe waypoints, skip dangerous ones
        if not too_dangerous:
            adjusted_path.append(waypoint)

    # Ensure we still have a path
    if not adjusted_path and current_path:
        # Fallback: keep the destination
        adjusted_path.append(current_path[-1])

    return adjusted_path
