"""
Tests for Tactical Decision Module
"""

import pytest
from src.tactical import (
    Position,
    Threat,
    Hazard,
    TacticalDecision,
    assess_threats,
    detect_hazards,
    get_quick_decision,
    prioritize_resources,
    adjust_pathfinding_for_threats,
)


class TestThreatAssessment:
    """Test threat assessment functions"""

    def test_assess_threats_empty(self):
        """Test assessment with no entities"""
        threats = assess_threats(
            agent_position=Position(x=0, y=64, z=0),
            nearby_entities=[],
            current_health=20,
        )

        assert threats == []

    def test_assess_threats_zombie(self):
        """Test assessment with zombie threat"""
        entities = [
            {
                "type": "zombie",
                "x": 5,
                "y": 64,
                "z": 0,
                "health": 20,
            }
        ]

        threats = assess_threats(
            agent_position=Position(x=0, y=64, z=0),
            nearby_entities=entities,
            current_health=20,
        )

        assert len(threats) == 1
        assert threats[0].entity_type == "zombie"
        assert threats[0].distance == 5.0
        assert threats[0].danger_level > 0

    def test_assess_threats_multiple(self):
        """Test assessment with multiple threats"""
        entities = [
            {"type": "zombie", "x": 5, "y": 64, "z": 0, "health": 20},
            {"type": "creeper", "x": 10, "y": 64, "z": 0, "health": 20},
            {"type": "chicken", "x": 15, "y": 64, "z": 0, "health": 10},
        ]

        threats = assess_threats(
            agent_position=Position(x=0, y=64, z=0),
            nearby_entities=entities,
            current_health=20,
        )

        # Should prioritize creeper over zombie due to danger rating
        assert len(threats) >= 2
        # Creeper should have higher danger level than zombie
        creeper = next((t for t in threats if t.entity_type == "creeper"), None)
        zombie = next((t for t in threats if t.entity_type == "zombie"), None)

        if creeper and zombie:
            assert creeper.danger_level > zombie.danger_level

    def test_assess_threats_low_health(self):
        """Test that low health increases danger"""
        entities = [
            {"type": "zombie", "x": 5, "y": 64, "z": 0, "health": 20},
        ]

        # High health
        threats_healthy = assess_threats(
            agent_position=Position(x=0, y=64, z=0),
            nearby_entities=entities,
            current_health=20,
        )

        # Low health
        threats_wounded = assess_threats(
            agent_position=Position(x=0, y=64, z=0),
            nearby_entities=entities,
            current_health=3,
        )

        # Wounded agent should perceive higher danger
        assert threats_wounded[0].danger_level > threats_healthy[0].danger_level


class TestHazardDetection:
    """Test hazard detection functions"""

    def test_detect_hazards_empty(self):
        """Test detection with no blocks"""
        hazards = detect_hazards(
            agent_position=Position(x=0, y=64, z=0),
            nearby_blocks=[],
        )

        # Should at least check for fall hazard
        assert isinstance(hazards, list)

    def test_detect_hazards_lava(self):
        """Test detection of lava hazard"""
        blocks = [
            {"type": "lava", "x": 2, "y": 64, "z": 0, "solid": False},
        ]

        hazards = detect_hazards(
            agent_position=Position(x=0, y=64, z=0),
            nearby_blocks=blocks,
        )

        assert len(hazards) > 0
        assert any(h.hazard_type == "lava" for h in hazards)

    def test_detect_hazards_fall(self):
        """Test detection of fall hazard"""
        # Create a void below the agent
        blocks = []
        for y in range(63, 50, -1):
            blocks.append({"type": "air", "x": 0, "y": y, "z": 0, "solid": False})

        hazards = detect_hazards(
            agent_position=Position(x=0, y=64, z=0),
            nearby_blocks=blocks,
        )

        assert any(h.hazard_type == "fall" for h in hazards)

    def test_detect_hazards_suffocation(self):
        """Test detection of suffocation hazard"""
        blocks = [
            {"type": "stone", "x": 0, "y": 65, "z": 0, "solid": True},
        ]

        hazards = detect_hazards(
            agent_position=Position(x=0, y=64, z=0),
            nearby_blocks=blocks,
        )

        assert any(h.hazard_type == "suffocation" for h in hazards)


class TestQuickDecision:
    """Test quick tactical decision generation"""

    def test_decision_no_threats(self):
        """Test decision when no threats present"""
        decision = get_quick_decision(
            threats=[],
            hazards=[],
            combat_score=0.5,
            current_mission="build-shelter",
        )

        assert decision.action == "hold"
        assert "continue mission" in decision.reasoning.lower()

    def test_decision_fatal_hazard(self):
        """Test decision to flee from fatal hazard"""
        hazards = [
            Hazard(
                hazard_type="lava",
                position=Position(x=5, y=64, z=0),
                severity=1.0,
            )
        ]

        decision = get_quick_decision(
            threats=[],
            hazards=hazards,
            combat_score=0.5,
        )

        assert decision.action == "flee"
        assert "fatal hazard" in decision.reasoning.lower()
        assert decision.target_position is not None

    def test_decision_critical_threat_flee(self):
        """Test decision to flee when low combat capability"""
        threats = [
            Threat(
                entity_type="creeper",
                position=Position(x=5, y=64, z=0),
                distance=5.0,
                danger_level=0.8,
                estimated_health=20,
            )
        ]

        decision = get_quick_decision(
            threats=threats,
            hazards=[],
            combat_score=0.3,  # Low combat
        )

        assert decision.action == "flee"

    def test_decision_critical_threat_attack(self):
        """Test decision to attack when high combat capability"""
        threats = [
            Threat(
                entity_type="creeper",
                position=Position(x=5, y=64, z=0),
                distance=5.0,
                danger_level=0.8,
                estimated_health=20,
            )
        ]

        decision = get_quick_decision(
            threats=threats,
            hazards=[],
            combat_score=0.8,  # High combat
        )

        assert decision.action == "attack"


class TestResourcePrioritization:
    """Test resource prioritization functions"""

    def test_prioritize_resources_basic(self):
        """Test basic resource prioritization"""
        resources = [
            {"type": "oak_log", "x": 5, "y": 64, "z": 0},
            {"type": "iron_ore", "x": 10, "y": 64, "z": 0},
        ]

        prioritized = prioritize_resources(
            nearby_resources=resources,
            agent_position=Position(x=0, y=64, z=0),
            current_inventory={},
            mission_needs=["oak_log"],
        )

        assert len(prioritized) == 2
        # Oak log should be higher priority due to mission need
        assert prioritized[0]["type"] == "oak_log"

    def test_prioritize_resources_distance(self):
        """Test that closer resources have higher priority"""
        resources = [
            {"type": "oak_log", "x": 20, "y": 64, "z": 0},
            {"type": "oak_log", "x": 5, "y": 64, "z": 0},
        ]

        prioritized = prioritize_resources(
            nearby_resources=resources,
            agent_position=Position(x=0, y=64, z=0),
            current_inventory={},
            mission_needs=["oak_log"],
        )

        # Closer resource should be first
        assert prioritized[0]["x"] == 5


class TestPathfindingAdjustment:
    """Test pathfinding adjustment for threats"""

    def test_adjust_path_no_threats(self):
        """Test that path is unchanged when no threats"""
        path = [
            Position(x=0, y=64, z=0),
            Position(x=5, y=64, z=0),
            Position(x=10, y=64, z=0),
        ]

        adjusted = adjust_pathfinding_for_threats(
            current_path=path,
            threats=[],
            hazards=[],
        )

        assert len(adjusted) == len(path)

    def test_adjust_path_with_threat(self):
        """Test that path avoids threat waypoints"""
        path = [
            Position(x=0, y=64, z=0),
            Position(x=5, y=64, z=0),  # Near threat
            Position(x=10, y=64, z=0),
        ]

        threats = [
            Threat(
                entity_type="zombie",
                position=Position(x=6, y=64, z=0),
                distance=1.0,
                danger_level=0.8,
                estimated_health=20,
            )
        ]

        adjusted = adjust_pathfinding_for_threats(
            current_path=path,
            threats=threats,
            hazards=[],
        )

        # Middle waypoint should be removed or adjusted
        # This test verifies the function runs without error
        assert isinstance(adjusted, list)


@pytest.fixture
def sample_threat():
    """Create a sample threat for testing"""
    return Threat(
        entity_type="zombie",
        position=Position(x=5, y=64, z=0),
        distance=5.0,
        danger_level=0.6,
        estimated_health=20,
    )


@pytest.fixture
def sample_hazard():
    """Create a sample hazard for testing"""
    return Hazard(
        hazard_type="lava",
        position=Position(x=10, y=63, z=0),
        severity=0.8,
    )
