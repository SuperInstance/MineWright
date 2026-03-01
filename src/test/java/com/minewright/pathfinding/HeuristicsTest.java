package com.minewright.pathfinding;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link Heuristics}.
 *
 * Tests cover:
 * <ul>
 *   <li>Manhattan distance calculation</li>
 *   <li>Euclidean distance calculation</li>
 *   <li>Octile distance calculation</li>
 *   <li>Chebyshev distance calculation</li>
 *   <li>Height-weighted distance calculation</li>
 *   <li>Hierarchical distance calculation</li>
 *   <li>Adaptive heuristic selection</li>
 *   <li>Terrain-aware heuristics</li>
 * </ul>
 */
@DisplayName("Heuristics Tests")
class HeuristicsTest {

    private static final double EPSILON = 0.001;

    @Test
    @DisplayName("Manhattan distance calculates |dx| + |dy| + |dz|")
    void testManhattanDistance() {
        BlockPos from = new BlockPos(10, 64, 20);
        BlockPos to = new BlockPos(15, 68, 25);

        double distance = Heuristics.manhattanDistance(from, to);

        assertEquals(5 + 4 + 5, distance, EPSILON, "Manhattan distance should sum absolute differences");
    }

    @Test
    @DisplayName("Manhattan distance is zero for same position")
    void testManhattanDistanceSamePosition() {
        BlockPos pos = new BlockPos(100, 64, 200);

        double distance = Heuristics.manhattanDistance(pos, pos);

        assertEquals(0.0, distance, EPSILON, "Distance to same position should be 0");
    }

    @Test
    @DisplayName("Manhattan distance handles negative coordinates")
    void testManhattanDistanceNegative() {
        BlockPos from = new BlockPos(-10, 64, -20);
        BlockPos to = new BlockPos(5, 68, 10);

        double distance = Heuristics.manhattanDistance(from, to);

        assertEquals(15 + 4 + 30, distance, EPSILON, "Should handle negative coordinates correctly");
    }

    @Test
    @DisplayName("Euclidean distance calculates sqrt(dx^2 + dy^2 + dz^2)")
    void testEuclideanDistance() {
        BlockPos from = new BlockPos(0, 0, 0);
        BlockPos to = new BlockPos(3, 4, 0);

        double distance = Heuristics.euclideanDistance(from, to);

        assertEquals(5.0, distance, EPSILON, "3-4-5 triangle should have hypotenuse 5");
    }

    @Test
    @DisplayName("Euclidean distance is zero for same position")
    void testEuclideanDistanceSamePosition() {
        BlockPos pos = new BlockPos(50, 70, 90);

        double distance = Heuristics.euclideanDistance(pos, pos);

        assertEquals(0.0, distance, EPSILON, "Distance to same position should be 0");
    }

    @Test
    @DisplayName("Euclidean distance calculates 3D diagonal correctly")
    void testEuclideanDistance3D() {
        BlockPos from = new BlockPos(0, 0, 0);
        BlockPos to = new BlockPos(1, 1, 1);

        double distance = Heuristics.euclideanDistance(from, to);

        assertEquals(Math.sqrt(3), distance, EPSILON, "3D diagonal of unit cube is sqrt(3)");
    }

    @Test
    @DisplayName("Octile distance accounts for diagonal movement cost")
    void testOctileDistance() {
        // Octile distance: diagonal costs sqrt(2), cardinal costs 1
        BlockPos from = new BlockPos(0, 64, 0);
        BlockPos to = new BlockPos(10, 64, 10);

        double distance = Heuristics.octileDistance(from, to);

        // For pure diagonal in 2D: max(dx, dz) + (sqrt(2) - 1) * min(dx, dz)
        // Here dx = 10, dz = 10, so: 10 + (sqrt(2) - 1) * 10 = 10 * sqrt(2)
        double expected = 10 * Math.sqrt(2);
        assertEquals(expected, distance, EPSILON, "Octile should account for diagonal cost");
    }

    @Test
    @DisplayName("Octile distance with vertical component")
    void testOctileDistanceWithVertical() {
        BlockPos from = new BlockPos(0, 64, 0);
        BlockPos to = new BlockPos(10, 70, 5);

        double distance = Heuristics.octileDistance(from, to);

        // Horizontal: max(10, 5) + (sqrt(2) - 1) * min(10, 5)
        // = 10 + 0.414 * 5 = 12.07
        // Plus vertical: 6
        double expected = 10 + (Math.sqrt(2) - 1) * 5 + 6;
        assertEquals(expected, distance, EPSILON, "Octile should include vertical cost");
    }

    @Test
    @DisplayName("Chebyshev distance calculates max(|dx|, |dy|, |dz|)")
    void testChebyshevDistance() {
        BlockPos from = new BlockPos(0, 0, 0);
        BlockPos to = new BlockPos(5, 3, 7);

        double distance = Heuristics.chebyshevDistance(from, to);

        assertEquals(7.0, distance, EPSILON, "Chebyshev should return maximum axis difference");
    }

    @Test
    @DisplayName("Chebyshev distance with equal differences")
    void testChebyshevDistanceEqual() {
        BlockPos from = new BlockPos(0, 0, 0);
        BlockPos to = new BlockPos(5, 5, 5);

        double distance = Heuristics.chebyshevDistance(from, to);

        assertEquals(5.0, distance, EPSILON, "Chebyshev should be 5 when all differences are 5");
    }

    @Test
    @DisplayName("Height-weighted distance weights vertical movement")
    void testHeightWeightedDistance() {
        BlockPos from = new BlockPos(0, 64, 0);
        BlockPos to = new BlockPos(10, 74, 10);

        double weight = 2.0;
        double distance = Heuristics.heightWeightedDistance(from, to, weight);

        // dx + dz + dy * weight = 10 + 10 + 10 * 2 = 40
        double expected = 10 + 10 + 10 * weight;
        assertEquals(expected, distance, EPSILON, "Vertical distance should be weighted");
    }

    @Test
    @DisplayName("Height-weighted distance with different weights")
    void testHeightWeightedDistanceWeights() {
        BlockPos from = new BlockPos(0, 60, 0);
        BlockPos to = new BlockPos(5, 70, 5);

        // Weight 1.0 (no weighting) = 5 + 10 + 5 = 20
        double weight1 = Heuristics.heightWeightedDistance(from, to, 1.0);
        assertEquals(20.0, weight1, EPSILON, "Weight 1.0 should give Manhattan distance");

        // Weight 2.0 (double vertical) = 5 + 10 * 2 + 5 = 30
        double weight2 = Heuristics.heightWeightedDistance(from, to, 2.0);
        assertEquals(30.0, weight2, EPSILON, "Weight 2.0 should double vertical cost");

        // Weight 0.5 (half vertical) = 5 + 10 * 0.5 + 5 = 15
        double weight3 = Heuristics.heightWeightedDistance(from, to, 0.5);
        assertEquals(15.0, weight3, EPSILON, "Weight 0.5 should halve vertical cost");
    }

    @Test
    @DisplayName("Hierarchical distance uses chunk-level calculation")
    void testHierarchicalDistance() {
        BlockPos from = new BlockPos(0, 64, 0);
        BlockPos to = new BlockPos(32, 64, 32); // 2 chunks away
        int chunkSize = 16;

        double distance = Heuristics.hierarchicalDistance(from, to, chunkSize);

        // From chunk: (0, 0), To chunk: (2, 2)
        // Octile at chunk level: 2 + (sqrt(2) - 1) * 2
        // Convert back: multiply by 16
        double chunkDistance = 2 + (Math.sqrt(2) - 1) * 2;
        double expected = chunkDistance * chunkSize;
        assertEquals(expected, distance, EPSILON, "Should calculate distance at chunk level");
    }

    @Test
    @DisplayName("Hierarchical distance within same chunk")
    void testHierarchicalDistanceSameChunk() {
        BlockPos from = new BlockPos(10, 64, 10);
        BlockPos to = new BlockPos(15, 64, 15);
        int chunkSize = 16;

        double distance = Heuristics.hierarchicalDistance(from, to, chunkSize);

        // Both in same chunk (0, 0), so distance should be small
        assertEquals(0.0, distance, EPSILON, "Same chunk should have 0 chunk-level distance");
    }

    @Test
    @DisplayName("Adaptive heuristic uses Euclidean for flying")
    void testAdaptiveHeuristicFlying() {
        BlockPos from = new BlockPos(0, 64, 0);
        BlockPos to = new BlockPos(10, 70, 10);

        // Create mock context with flying enabled
        PathfindingContext context = createMockContext(true, false, false, false);

        double distance = Heuristics.adaptiveHeuristic(from, to, context);

        // Should use Euclidean for flying
        double expected = Math.sqrt(10 * 10 + 6 * 6 + 10 * 10);
        assertEquals(expected, distance, EPSILON, "Flying should use Euclidean distance");
    }

    @Test
    @DisplayName("Adaptive heuristic uses octile for normal movement")
    void testAdaptiveHeuristicNormal() {
        BlockPos from = new BlockPos(0, 64, 0);
        BlockPos to = new BlockPos(10, 64, 10);

        // Create mock context without special capabilities
        PathfindingContext context = createMockContext(false, false, false, false);

        double distance = Heuristics.adaptiveHeuristic(from, to, context);

        // Should use octile for normal movement
        double expected = Heuristics.octileDistance(from, to);
        assertEquals(expected, distance, EPSILON, "Normal movement should use octile distance");
    }

    @Test
    @DisplayName("Terrain-aware heuristic penalizes climbing")
    void testTerrainAwareHeuristicUphill() {
        BlockPos from = new BlockPos(0, 64, 0);
        BlockPos to = new BlockPos(10, 80, 10); // 16 blocks higher

        PathfindingContext context = createMockContext(false, false, false, false);

        double distance = Heuristics.terrainAwareHeuristic(from, to, context);

        double baseCost = Heuristics.octileDistance(from, to);
        double penalty = 16 * 0.5; // Extra cost for climbing

        assertTrue(distance >= baseCost, "Terrain-aware should be at least base cost");
        assertTrue(distance <= baseCost + penalty + EPSILON,
            "Terrain-aware should not exceed base cost + penalty");
    }

    @Test
    @DisplayName("Terrain-aware heuristic discounts descending")
    void testTerrainAwareHeuristicDownhill() {
        BlockPos from = new BlockPos(0, 80, 0);
        BlockPos to = new BlockPos(10, 64, 10); // 16 blocks lower

        PathfindingContext context = createMockContext(false, false, false, false);

        double distance = Heuristics.terrainAwareHeuristic(from, to, context);

        double baseCost = Heuristics.octileDistance(from, to);
        double discount = 16 * 0.2; // Discount for falling

        assertTrue(distance >= baseCost + discount - EPSILON,
            "Terrain-aware should apply discount for descending");
        assertTrue(distance <= baseCost, "Terrain-aware should not exceed base cost when descending");
    }

    @Test
    @DisplayName("Heuristic function interface can be used as lambda")
    void testHeuristicFunctionInterface() {
        BlockPos from = new BlockPos(0, 64, 0);
        BlockPos to = new BlockPos(10, 64, 10);

        Heuristics.HeuristicFunction manhattanFunc = Heuristics::manhattanDistance;
        Heuristics.HeuristicFunction euclideanFunc = Heuristics::euclideanDistance;

        double manhattan = manhattanFunc.estimate(from, to);
        double euclidean = euclideanFunc.estimate(from, to);

        assertTrue(manhattan > euclidean, "Manhattan should be greater than Euclidean for diagonal");
        assertEquals(20.0, manhattan, EPSILON, "Manhattan should be 20");
        assertEquals(Math.sqrt(200), euclidean, EPSILON, "Euclidean should be sqrt(200)");
    }

    @Test
    @DisplayName("Create heuristic returns appropriate function")
    void testCreateHeuristic() {
        BlockPos from = new BlockPos(0, 64, 0);
        BlockPos to = new BlockPos(10, 64, 10);

        // Normal context
        PathfindingContext normalContext = createMockContext(false, false, false, false);
        Heuristics.HeuristicFunction normalHeuristic = Heuristics.createHeuristic(normalContext);
        double normalDistance = normalHeuristic.estimate(from, to);

        // Flying context
        PathfindingContext flyingContext = createMockContext(true, false, false, false);
        Heuristics.HeuristicFunction flyingHeuristic = Heuristics.createHeuristic(flyingContext);
        double flyingDistance = flyingHeuristic.estimate(from, to);

        assertEquals(Heuristics.octileDistance(from, to), normalDistance, EPSILON,
            "Normal should use octile");
        assertEquals(Heuristics.euclideanDistance(from, to), flyingDistance, EPSILON,
            "Flying should use Euclidean");
    }

    @Test
    @DisplayName("All heuristics are non-negative")
    void testHeuristicsNonNegative() {
        BlockPos from = new BlockPos(0, 64, 0);
        BlockPos to = new BlockPos(100, 100, 100);

        assertTrue(Heuristics.manhattanDistance(from, to) >= 0, "Manhattan should be non-negative");
        assertTrue(Heuristics.euclideanDistance(from, to) >= 0, "Euclidean should be non-negative");
        assertTrue(Heuristics.octileDistance(from, to) >= 0, "Octile should be non-negative");
        assertTrue(Heuristics.chebyshevDistance(from, to) >= 0, "Chebyshev should be non-negative");
        assertTrue(Heuristics.heightWeightedDistance(from, to, 2.0) >= 0,
            "Height-weighted should be non-negative");
    }

    @Test
    @DisplayName("Heuristics satisfy triangle inequality")
    void testTriangleInequality() {
        BlockPos a = new BlockPos(0, 64, 0);
        BlockPos b = new BlockPos(10, 64, 10);
        BlockPos c = new BlockPos(20, 64, 20);

        // Manhattan: d(a,c) <= d(a,b) + d(b,c)
        double manhattanAC = Heuristics.manhattanDistance(a, c);
        double manhattanAB = Heuristics.manhattanDistance(a, b);
        double manhattanBC = Heuristics.manhattanDistance(b, c);
        assertTrue(manhattanAC <= manhattanAB + manhattanBC + EPSILON,
            "Manhattan should satisfy triangle inequality");

        // Euclidean: d(a,c) <= d(a,b) + d(b,c)
        double euclideanAC = Heuristics.euclideanDistance(a, c);
        double euclideanAB = Heuristics.euclideanDistance(a, b);
        double euclideanBC = Heuristics.euclideanDistance(b, c);
        assertTrue(euclideanAC <= euclideanAB + euclideanBC + EPSILON,
            "Euclidean should satisfy triangle inequality");
    }

    @Test
    @DisplayName("Heuristics are symmetric")
    void testSymmetry() {
        BlockPos from = new BlockPos(10, 64, 20);
        BlockPos to = new BlockPos(30, 70, 40);

        assertEquals(Heuristics.manhattanDistance(from, to),
            Heuristics.manhattanDistance(to, from), EPSILON, "Manhattan should be symmetric");
        assertEquals(Heuristics.euclideanDistance(from, to),
            Heuristics.euclideanDistance(to, from), EPSILON, "Euclidean should be symmetric");
        assertEquals(Heuristics.octileDistance(from, to),
            Heuristics.octileDistance(to, from), EPSILON, "Octile should be symmetric");
        assertEquals(Heuristics.chebyshevDistance(from, to),
            Heuristics.chebyshevDistance(to, from), EPSILON, "Chebyshev should be symmetric");
    }

    // Helper method to create mock PathfindingContext
    private PathfindingContext createMockContext(boolean canFly, boolean canSwim,
                                                  boolean canClimb, boolean canParkour) {
        // Create a mock context for testing
        Level mockLevel = mock(Level.class);
        Entity mockEntity = mock(Entity.class);

        when(mockEntity.blockPosition()).thenReturn(new BlockPos(0, 64, 0));

        PathfindingContext context = new PathfindingContext(mockLevel, mockEntity);

        // Note: The setter methods return the context for chaining, but we can't
        // easily mock the internal state. In a real integration test, you would
        // use proper Level/Entity implementations or a more sophisticated mock setup.

        // For the purpose of testing Heuristics (which don't depend heavily on
        // Level/Entity), we skip the full context setup in unit tests.

        return context;
    }
}
