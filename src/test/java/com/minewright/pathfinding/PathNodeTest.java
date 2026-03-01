package com.minewright.pathfinding;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PathNode}.
 *
 * Tests cover:
 * <ul>
 *   <li>Node creation and initialization</li>
 *   <li>Cost calculations (gCost, hCost, fCost)</li>
 *   <li>Parent-child relationships</li>
 *   <li>Comparison and ordering for priority queues</li>
 *   <li>Equality and hashing</li>
 *   <li>Movement type handling</li>
 *   <li>Utility methods (copy, distance calculations)</li>
 * </ul>
 */
@DisplayName("PathNode Tests")
class PathNodeTest {

    @Test
    @DisplayName("Simple constructor creates node with default values")
    void testSimpleConstructor() {
        BlockPos pos = new BlockPos(10, 64, 20);
        PathNode node = new PathNode(pos);

        assertEquals(pos, node.pos, "Position should match");
        assertEquals(0.0, node.gCost, 0.001, "Default gCost should be 0");
        assertEquals(0.0, node.hCost, 0.001, "Default hCost should be 0");
        assertEquals(0.0, node.fCost(), 0.001, "fCost should be gCost + hCost");
        assertEquals(MovementType.WALK, node.movement, "Default movement type should be WALK");
        assertNull(node.parent, "Parent should be null");
        assertEquals(1.0, node.costMultiplier, 0.001, "Default cost multiplier should be 1.0");
    }

    @Test
    @DisplayName("Full constructor initializes all fields")
    void testFullConstructor() {
        BlockPos pos = new BlockPos(5, 70, 15);
        PathNode parent = new PathNode(new BlockPos(0, 64, 0));
        double gCost = 10.5;
        double hCost = 5.2;
        MovementType movement = MovementType.JUMP;

        PathNode node = new PathNode(pos, parent, gCost, hCost, movement);

        assertEquals(pos, node.pos);
        assertEquals(parent, node.parent);
        assertEquals(gCost, node.gCost, 0.001);
        assertEquals(hCost, node.hCost, 0.001);
        assertEquals(gCost + hCost, node.fCost(), 0.001);
        assertEquals(movement, node.movement);
    }

    @Test
    @DisplayName("fCost returns sum of gCost and hCost")
    void testFCostCalculation() {
        PathNode node = new PathNode(new BlockPos(0, 64, 0));

        node.gCost = 10.0;
        node.hCost = 5.0;

        assertEquals(15.0, node.fCost(), 0.001, "fCost should be gCost + hCost");

        node.gCost = 7.5;
        node.hCost = 3.2;

        assertEquals(10.7, node.fCost(), 0.001, "fCost should update with new costs");
    }

    @Test
    @DisplayName("compareTo orders by fCost (lower is better)")
    void testCompareTo() {
        PathNode node1 = new PathNode(new BlockPos(0, 64, 0));
        node1.gCost = 5.0;
        node1.hCost = 5.0; // fCost = 10.0

        PathNode node2 = new PathNode(new BlockPos(1, 64, 0));
        node2.gCost = 3.0;
        node2.hCost = 4.0; // fCost = 7.0

        PathNode node3 = new PathNode(new BlockPos(2, 64, 0));
        node3.gCost = 8.0;
        node3.hCost = 5.0; // fCost = 13.0

        assertTrue(node2.compareTo(node1) < 0, "node2 (fCost=7) should be less than node1 (fCost=10)");
        assertTrue(node1.compareTo(node3) < 0, "node1 (fCost=10) should be less than node3 (fCost=13)");
        assertTrue(node3.compareTo(node2) > 0, "node3 (fCost=13) should be greater than node2 (fCost=7)");

        // Equal fCosts
        PathNode node4 = new PathNode(new BlockPos(3, 64, 0));
        node4.gCost = 6.0;
        node4.hCost = 4.0; // fCost = 10.0

        assertEquals(0, node1.compareTo(node4), "Nodes with equal fCost should compare as equal");
    }

    @Test
    @DisplayName("Equality based on position only")
    void testEquals() {
        BlockPos pos = new BlockPos(10, 64, 20);
        PathNode node1 = new PathNode(pos);
        PathNode node2 = new PathNode(pos);
        PathNode node3 = new PathNode(new BlockPos(10, 65, 20));

        // Same position = equal
        assertEquals(node1, node2, "Nodes with same position should be equal");
        assertEquals(node1.hashCode(), node2.hashCode(), "Equal nodes should have same hashCode");

        // Different position = not equal
        assertNotEquals(node1, node3, "Nodes with different positions should not be equal");

        // Not equal to null
        assertNotEquals(node1, null, "Node should not equal null");

        // Not equal to different type
        assertNotEquals(node1, pos, "Node should not equal BlockPos");
    }

    @Test
    @DisplayName("Reflexive, symmetric, and transitive equality")
    void testEqualsContract() {
        BlockPos pos = new BlockPos(5, 64, 10);
        PathNode node1 = new PathNode(pos);
        PathNode node2 = new PathNode(pos);
        PathNode node3 = new PathNode(pos);

        // Reflexive
        assertEquals(node1, node1, "Node should equal itself");

        // Symmetric
        assertEquals(node1, node2, "node1 should equal node2");
        assertEquals(node2, node1, "node2 should equal node1");

        // Transitive
        assertEquals(node1, node2, "node1 should equal node2");
        assertEquals(node2, node3, "node2 should equal node3");
        assertEquals(node1, node3, "node1 should equal node3");
    }

    @Test
    @DisplayName("HashCode is consistent with equals")
    void testHashCode() {
        BlockPos pos = new BlockPos(15, 70, 25);
        PathNode node1 = new PathNode(pos);
        PathNode node2 = new PathNode(pos);

        // Equal nodes have equal hash codes
        assertEquals(node1.hashCode(), node2.hashCode(), "Equal nodes should have equal hash codes");

        // Hash code is consistent
        int hash1 = node1.hashCode();
        int hash2 = node1.hashCode();
        assertEquals(hash1, hash2, "Hash code should be consistent across calls");
    }

    @Test
    @DisplayName("toString contains all relevant information")
    void testToString() {
        BlockPos pos = new BlockPos(10, 64, 20);
        PathNode node = new PathNode(pos);
        node.gCost = 12.5;
        node.hCost = 7.3;
        node.movement = MovementType.JUMP;

        String str = node.toString();

        assertTrue(str.contains("PathNode"), "toString should contain class name");
        assertTrue(str.contains(pos.toString()), "toString should contain position");
        assertTrue(str.contains("12.5"), "toString should contain gCost");
        assertTrue(str.contains("7.3"), "toString should contain hCost");
        assertTrue(str.contains(String.format("%.1f", node.fCost())), "toString should contain fCost");
        assertTrue(str.contains("JUMP"), "toString should contain movement type");
    }

    @Test
    @DisplayName("Copy creates independent node without parent")
    void testCopy() {
        BlockPos pos = new BlockPos(8, 66, 18);
        PathNode parent = new PathNode(new BlockPos(0, 64, 0));
        PathNode original = new PathNode(pos, parent, 15.0, 8.0, MovementType.CLIMB);
        original.costMultiplier = 2.0;

        PathNode copy = original.copy();

        assertEquals(original.pos, copy.pos, "Copy should have same position");
        assertEquals(original.gCost, copy.gCost, 0.001, "Copy should have same gCost");
        assertEquals(original.hCost, copy.hCost, 0.001, "Copy should have same hCost");
        assertEquals(original.movement, copy.movement, "Copy should have same movement type");
        assertEquals(original.costMultiplier, copy.costMultiplier, 0.001, "Copy should have same cost multiplier");
        assertNull(copy.parent, "Copy should not have parent");
        assertNotSame(original, copy, "Copy should be a different object");
    }

    @Test
    @DisplayName("Copy is independent of original")
    void testCopyIndependence() {
        PathNode original = new PathNode(new BlockPos(10, 64, 10));
        original.gCost = 10.0;
        original.hCost = 5.0;
        original.movement = MovementType.WALK;
        original.costMultiplier = 1.5;

        PathNode copy = original.copy();

        // Modify original
        original.gCost = 20.0;
        original.hCost = 10.0;
        original.movement = MovementType.JUMP;
        original.costMultiplier = 3.0;

        // Copy should be unchanged
        assertEquals(10.0, copy.gCost, 0.001, "Copy gCost should not change");
        assertEquals(5.0, copy.hCost, 0.001, "Copy hCost should not change");
        assertEquals(MovementType.WALK, copy.movement, "Copy movement should not change");
        assertEquals(1.5, copy.costMultiplier, 0.001, "Copy cost multiplier should not change");
    }

    @Test
    @DisplayName("isGoal checks against goal position")
    void testIsGoal() {
        BlockPos goal = new BlockPos(100, 64, 200);
        PathNode node = new PathNode(goal);
        PathNode otherNode = new PathNode(new BlockPos(50, 64, 100));

        assertTrue(node.isGoal(goal), "Node at goal position should return true");
        assertFalse(otherNode.isGoal(goal), "Node not at goal position should return false");
    }

    @Test
    @DisplayName("manhattanDistanceTo calculates correct Manhattan distance")
    void testManhattanDistanceTo() {
        PathNode node = new PathNode(new BlockPos(10, 64, 20));

        // Same position
        assertEquals(0, node.manhattanDistanceTo(new BlockPos(10, 64, 20)),
            "Distance to same position should be 0");

        // Only X difference
        assertEquals(5, node.manhattanDistanceTo(new BlockPos(15, 64, 20)),
            "Should calculate X distance correctly");

        // Only Y difference
        assertEquals(10, node.manhattanDistanceTo(new BlockPos(10, 74, 20)),
            "Should calculate Y distance correctly");

        // Only Z difference
        assertEquals(8, node.manhattanDistanceTo(new BlockPos(10, 64, 28)),
            "Should calculate Z distance correctly");

        // All axes different
        assertEquals(3 + 7 + 5, node.manhattanDistanceTo(new BlockPos(13, 71, 25)),
            "Should sum all axis distances");

        // Negative differences (absolute value)
        assertEquals(15, node.manhattanDistanceTo(new BlockPos(-5, 64, 20)),
            "Should handle negative X difference");
    }

    @Test
    @DisplayName("Parent chain can be traversed")
    void testParentChain() {
        PathNode root = new PathNode(new BlockPos(0, 64, 0));
        PathNode child1 = new PathNode(new BlockPos(1, 64, 0), root, 1.0, 9.0, MovementType.WALK);
        PathNode child2 = new PathNode(new BlockPos(2, 64, 0), child1, 2.0, 8.0, MovementType.WALK);
        PathNode goal = new PathNode(new BlockPos(3, 64, 0), child2, 3.0, 7.0, MovementType.WALK);

        assertNull(root.parent, "Root should have no parent");
        assertEquals(root, child1.parent, "child1 parent should be root");
        assertEquals(child1, child2.parent, "child2 parent should be child1");
        assertEquals(child2, goal.parent, "goal parent should be child2");
    }

    @Test
    @DisplayName("Movement types are correctly assigned")
    void testMovementTypes() {
        BlockPos pos = new BlockPos(10, 64, 10);

        PathNode walkNode = new PathNode(pos, null, 0, 0, MovementType.WALK);
        assertEquals(MovementType.WALK, walkNode.movement);

        PathNode jumpNode = new PathNode(pos, null, 0, 0, MovementType.JUMP);
        assertEquals(MovementType.JUMP, jumpNode.movement);

        PathNode climbNode = new PathNode(pos, null, 0, 0, MovementType.CLIMB);
        assertEquals(MovementType.CLIMB, climbNode.movement);

        PathNode swimNode = new PathNode(pos, null, 0, 0, MovementType.SWIM);
        assertEquals(MovementType.SWIM, swimNode.movement);

        PathNode fallNode = new PathNode(pos, null, 0, 0, MovementType.FALL);
        assertEquals(MovementType.FALL, fallNode.movement);

        PathNode parkourNode = new PathNode(pos, null, 0, 0, MovementType.PARKOUR);
        assertEquals(MovementType.PARKOUR, parkourNode.movement);

        PathNode flyNode = new PathNode(pos, null, 0, 0, MovementType.FLY);
        assertEquals(MovementType.FLY, flyNode.movement);
    }

    @Test
    @DisplayName("Cost multiplier affects effective cost")
    void testCostMultiplier() {
        PathNode node = new PathNode(new BlockPos(10, 64, 10));
        node.gCost = 10.0;
        node.hCost = 5.0;
        node.costMultiplier = 2.0;

        assertEquals(10.0, node.gCost, 0.001, "gCost should be stored value");
        assertEquals(5.0, node.hCost, 0.001, "hCost should be stored value");
        assertEquals(15.0, node.fCost(), 0.001, "fCost is sum of g and h (not affected by multiplier)");
        assertEquals(2.0, node.costMultiplier, 0.001, "Cost multiplier should be stored");
        // Note: The actual multiplication is done by the pathfinder, not the node
    }

    @Test
    @DisplayName("Edge case: Negative costs")
    void testNegativeCosts() {
        PathNode node = new PathNode(new BlockPos(0, 64, 0));
        node.gCost = -5.0;
        node.hCost = -3.0;

        assertEquals(-8.0, node.fCost(), 0.001, "fCost should handle negative values");
        // Negative costs might not make sense in practice, but the node should handle them
    }

    @Test
    @DisplayName("Edge case: Very large costs")
    void testLargeCosts() {
        PathNode node = new PathNode(new BlockPos(0, 64, 0));
        node.gCost = Double.MAX_VALUE / 2;
        node.hCost = Double.MAX_VALUE / 2;

        assertTrue(Double.isInfinite(node.fCost()) || node.fCost() > 0,
            "Should handle large costs");
    }

    @Test
    @DisplayName("Multiple nodes can be compared by position")
    void testPositionComparison() {
        BlockPos pos1 = new BlockPos(10, 64, 20);
        BlockPos pos2 = new BlockPos(10, 64, 20);
        BlockPos pos3 = new BlockPos(10, 65, 20);

        PathNode node1 = new PathNode(pos1);
        PathNode node2 = new PathNode(pos2);
        PathNode node3 = new PathNode(pos3);

        // Same position = equal
        assertTrue(node1.equals(node2), "Nodes with same position should be equal");

        // Different position = not equal
        assertFalse(node1.equals(node3), "Nodes with different positions should not be equal");
    }
}
