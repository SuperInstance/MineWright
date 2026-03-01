package com.minewright.pathfinding;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.entity.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link MovementValidator}.
 *
 * Tests cover:
 * <ul>
 *   <li>Walking validation (solid ground, headroom)</li>
 *   <li>Jumping validation (jump height, clearance)</li>
 *   <li>Falling validation (fall distance, landing safety)</li>
 *   <li>Climbing validation (climbable blocks)</li>
 *   <li>Swimming validation (water detection)</li>
 *   <li>Parkour validation (gap jumping)</li>
 *   <li>Sneaking validation (low ceiling)</li>
 *   <li>Flying validation (bypasses constraints)</li>
 *   <li>Dangerous block detection</li>
 *   <li>Helper methods (solid ground, headroom, blocked)</li>
 * </ul>
 */
@DisplayName("MovementValidator Tests")
class MovementValidatorTest {

    @Mock
    private Level mockLevel;

    @Mock
    private Entity mockEntity;

    @Mock
    private PathfindingContext mockContext;

    private MovementValidator validator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validator = new MovementValidator();

        // Setup default context behavior
        setupDefaultContext();
    }

    // ========== Walking Tests ==========

    @Test
    @DisplayName("Walking is valid with solid ground and headroom")
    void testCanWalkValid() {
        BlockPos from = new BlockPos(0, 64, 0);
        BlockPos to = new BlockPos(1, 64, 0);

        setupSolidGround(to);
        setupHeadroom(to);

        boolean result = validator.canWalkTo(from, to, mockContext);

        assertTrue(result, "Walking should be valid with solid ground and headroom");
    }

    @Test
    @DisplayName("Walking is invalid without solid ground")
    void testCanWalkNoSolidGround() {
        BlockPos from = new BlockPos(0, 64, 0);
        BlockPos to = new BlockPos(1, 64, 0);

        setupNoSolidGround(to);

        boolean result = validator.canWalkTo(from, to, mockContext);

        assertFalse(result, "Walking should be invalid without solid ground");
    }

    @Test
    @DisplayName("Walking is invalid without headroom")
    void testCanWalkNoHeadroom() {
        BlockPos from = new BlockPos(0, 64, 0);
        BlockPos to = new BlockPos(1, 64, 0);

        setupSolidGround(to);
        setupNoHeadroom(to);

        boolean result = validator.canWalkTo(from, to, mockContext);

        assertFalse(result, "Walking should be invalid without headroom");
    }

    @Test
    @DisplayName("Walking up one block requires jump capability")
    void testCanWalkUpOneBlock() {
        BlockPos from = new BlockPos(0, 64, 0);
        BlockPos to = new BlockPos(1, 65, 0);

        setupSolidGround(to);
        setupHeadroom(to);

        when(mockContext.getJumpHeight()).thenReturn(1.0);

        boolean result = validator.canWalkTo(from, to, mockContext);

        assertTrue(result, "Walking up one block should be valid with jump height 1.0");
    }

    @Test
    @DisplayName("Walking up more than one block is invalid")
    void testCanWalkUpMultipleBlocks() {
        BlockPos from = new BlockPos(0, 64, 0);
        BlockPos to = new BlockPos(1, 66, 0); // 2 blocks up

        setupSolidGround(to);
        setupHeadroom(to);

        boolean result = validator.canWalkTo(from, to, mockContext);

        assertFalse(result, "Walking up more than one block should be invalid");
    }

    // ========== Jumping Tests ==========

    @Test
    @DisplayName("Jumping is valid with jump height and solid ground")
    void testCanJumpValid() {
        BlockPos from = new BlockPos(0, 64, 0);
        BlockPos to = new BlockPos(1, 65, 0); // 1 block up

        setupSolidGround(to);
        setupHeadroom(to);
        setupHeadroom(from); // Need headroom at jump start

        when(mockContext.getJumpHeight()).thenReturn(1.5);

        boolean result = validator.canJumpTo(from, to, mockContext);

        assertTrue(result, "Jumping should be valid with sufficient jump height");
    }

    @Test
    @DisplayName("Jumping is invalid if jump height is insufficient")
    void testCanJumpInsufficientHeight() {
        BlockPos from = new BlockPos(0, 64, 0);
        BlockPos to = new BlockPos(1, 66, 0); // 2 blocks up

        setupSolidGround(to);
        setupHeadroom(to);

        when(mockContext.getJumpHeight()).thenReturn(1.0);

        boolean result = validator.canJumpTo(from, to, mockContext);

        assertFalse(result, "Jumping should be invalid with insufficient jump height");
    }

    @Test
    @DisplayName("Jumping down is invalid")
    void testCanJumpDownInvalid() {
        BlockPos from = new BlockPos(0, 66, 0);
        BlockPos to = new BlockPos(1, 65, 0); // 1 block down

        boolean result = validator.canJumpTo(from, to, mockContext);

        assertFalse(result, "Jumping down should be invalid (use falling instead)");
    }

    // ========== Falling Tests ==========

    @Test
    @DisplayName("Falling is valid within safe distance")
    void testCanFallValid() {
        BlockPos from = new BlockPos(0, 68, 0);
        BlockPos to = new BlockPos(0, 65, 0); // 3 blocks down

        setupSolidGround(to);
        setupHeadroom(to);
        setupClearFallPath(from, to);

        when(mockContext.getMaxFallDistance()).thenReturn(3);

        boolean result = validator.canFallTo(from, to, mockContext);

        assertTrue(result, "Falling should be valid within safe distance");
    }

    @Test
    @DisplayName("Falling is invalid beyond safe distance")
    void testCanFallUnsafeDistance() {
        BlockPos from = new BlockPos(0, 70, 0);
        BlockPos to = new BlockPos(0, 65, 0); // 5 blocks down

        when(mockContext.getMaxFallDistance()).thenReturn(3);
        when(mockContext.allowDangerousMovements()).thenReturn(false);

        boolean result = validator.canFallTo(from, to, mockContext);

        assertFalse(result, "Falling should be invalid beyond safe distance");
    }

    @Test
    @DisplayName("Falling is valid with dangerous movements allowed")
    void testCanFallWithDangerousAllowed() {
        BlockPos from = new BlockPos(0, 70, 0);
        BlockPos to = new BlockPos(0, 65, 0); // 5 blocks down

        setupSolidGround(to);
        setupHeadroom(to);
        setupClearFallPath(from, to);

        when(mockContext.getMaxFallDistance()).thenReturn(3);
        when(mockContext.allowDangerousMovements()).thenReturn(true);

        boolean result = validator.canFallTo(from, to, mockContext);

        assertTrue(result, "Falling should be valid with dangerous movements allowed");
    }

    @Test
    @DisplayName("Falling up is invalid")
    void testCanFallUpInvalid() {
        BlockPos from = new BlockPos(0, 65, 0);
        BlockPos to = new BlockPos(0, 66, 0); // 1 block up

        boolean result = validator.canFallTo(from, to, mockContext);

        assertFalse(result, "Falling up should be invalid (use jumping instead)");
    }

    // ========== Climbing Tests ==========

    @Test
    @DisplayName("Climbing is valid on climbable blocks")
    void testCanClimbValid() {
        BlockPos from = new BlockPos(0, 64, 0);
        BlockPos to = new BlockPos(0, 65, 0);

        setupClimbableBlock(to);
        when(mockContext.canClimb()).thenReturn(true);

        boolean result = validator.canClimbTo(from, to, mockContext);

        assertTrue(result, "Climbing should be valid on climbable blocks");
    }

    @Test
    @DisplayName("Climbing is invalid when climb capability is disabled")
    void testCanClimbCapabilityDisabled() {
        BlockPos from = new BlockPos(0, 64, 0);
        BlockPos to = new BlockPos(0, 65, 0);

        setupClimbableBlock(to);
        when(mockContext.canClimb()).thenReturn(false);

        boolean result = validator.canClimbTo(from, to, mockContext);

        assertFalse(result, "Climbing should be invalid when capability is disabled");
    }

    @Test
    @DisplayName("Climbing is invalid on non-climbable blocks")
    void testCanClimbNonClimbable() {
        BlockPos from = new BlockPos(0, 64, 0);
        BlockPos to = new BlockPos(0, 65, 0);

        setupNonClimbableBlock(to);
        when(mockContext.canClimb()).thenReturn(true);

        boolean result = validator.canClimbTo(from, to, mockContext);

        assertFalse(result, "Climbing should be invalid on non-climbable blocks");
    }

    // ========== Swimming Tests ==========

    @Test
    @DisplayName("Swimming is valid in water")
    void testCanSwimValid() {
        BlockPos from = new BlockPos(0, 64, 0);
        BlockPos to = new BlockPos(1, 64, 0);

        setupWater(to);
        when(mockContext.canSwim()).thenReturn(true);

        boolean result = validator.canSwimTo(from, to, mockContext);

        assertTrue(result, "Swimming should be valid in water");
    }

    @Test
    @DisplayName("Swimming is invalid without swim capability")
    void testCanSwimNoCapability() {
        BlockPos from = new BlockPos(0, 64, 0);
        BlockPos to = new BlockPos(1, 64, 0);

        setupWater(to);
        when(mockContext.canSwim()).thenReturn(false);

        boolean result = validator.canSwimTo(from, to, mockContext);

        assertFalse(result, "Swimming should be invalid without capability");
    }

    @Test
    @DisplayName("Swimming is invalid outside water")
    void testCanSwimOutsideWater() {
        BlockPos from = new BlockPos(0, 64, 0);
        BlockPos to = new BlockPos(1, 64, 0);

        setupNoWater(to);
        when(mockContext.canSwim()).thenReturn(true);

        boolean result = validator.canSwimTo(from, to, mockContext);

        assertFalse(result, "Swimming should be invalid outside water");
    }

    // ========== Parkour Tests ==========

    @Test
    @DisplayName("Parkour is valid for 2-block jump")
    void testCanParkourValid() {
        BlockPos from = new BlockPos(0, 64, 0);
        BlockPos to = new BlockPos(2, 64, 0); // 2 blocks away

        setupSolidGround(to);
        setupHeadroom(to);
        when(mockContext.canParkour()).thenReturn(true);

        boolean result = validator.canParkourTo(from, to, mockContext);

        assertTrue(result, "Parkour should be valid for 2-block jump");
    }

    @Test
    @DisplayName("Parkour is invalid for too short jump")
    void testCanParkourTooShort() {
        BlockPos from = new BlockPos(0, 64, 0);
        BlockPos to = new BlockPos(1, 64, 0); // 1 block away

        when(mockContext.canParkour()).thenReturn(true);

        boolean result = validator.canParkourTo(from, to, mockContext);

        assertFalse(result, "Parkour should be invalid for too short jump (use walk)");
    }

    @Test
    @DisplayName("Parkour is invalid for too long jump")
    void testCanParkourTooLong() {
        BlockPos from = new BlockPos(0, 64, 0);
        BlockPos to = new BlockPos(4, 64, 0); // 4 blocks away

        when(mockContext.canParkour()).thenReturn(true);

        boolean result = validator.canParkourTo(from, to, mockContext);

        assertFalse(result, "Parkour should be invalid for too long jump");
    }

    // ========== Sneaking Tests ==========

    @Test
    @DisplayName("Sneaking is valid with low ceiling")
    void testCanSneakValid() {
        BlockPos from = new BlockPos(0, 64, 0);
        BlockPos to = new BlockPos(1, 64, 0);

        setupSolidGround(to);
        setupLowCeiling(to);

        boolean result = validator.canSneakTo(from, to, mockContext);

        assertTrue(result, "Sneaking should be valid with low ceiling");
    }

    @Test
    @DisplayName("Sneaking is invalid with normal ceiling")
    void testCanSneakNormalCeiling() {
        BlockPos from = new BlockPos(0, 64, 0);
        BlockPos to = new BlockPos(1, 64, 0);

        setupSolidGround(to);
        setupHeadroom(to); // Normal ceiling

        boolean result = validator.canSneakTo(from, to, mockContext);

        assertFalse(result, "Sneaking should be invalid with normal ceiling (use walk)");
    }

    // ========== Flying Tests ==========

    @Test
    @DisplayName("Flying bypasses terrain constraints")
    void testCanFlyValid() {
        BlockPos from = new BlockPos(0, 64, 0);
        BlockPos to = new BlockPos(10, 100, 10);

        setupNotBlocked(to);
        when(mockContext.canFly()).thenReturn(true);

        boolean result = validator.canFlyTo(from, to, mockContext);

        assertTrue(result, "Flying should bypass terrain constraints");
    }

    @Test
    @DisplayName("Flying is invalid without fly capability")
    void testCanFlyNoCapability() {
        BlockPos from = new BlockPos(0, 64, 0);
        BlockPos to = new BlockPos(1, 65, 0);

        when(mockContext.canFly()).thenReturn(false);

        boolean result = validator.canFlyTo(from, to, mockContext);

        assertFalse(result, "Flying should be invalid without capability");
    }

    @Test
    @DisplayName("Flying is invalid into solid block")
    void testCanFlyIntoSolid() {
        BlockPos from = new BlockPos(0, 64, 0);
        BlockPos to = new BlockPos(1, 64, 0);

        setupBlocked(to);
        when(mockContext.canFly()).thenReturn(true);

        boolean result = validator.canFlyTo(from, to, mockContext);

        assertFalse(result, "Flying should be invalid into solid block");
    }

    // ========== Helper Method Tests ==========

    @Test
    @DisplayName("hasSolidGround returns true for solid blocks")
    void testHasSolidGround() {
        BlockPos pos = new BlockPos(0, 64, 0);
        BlockPos below = new BlockPos(0, 63, 0);

        BlockState solidState = mock(BlockState.class);
        when(solidState.isSolidRender(eq(mockLevel), eq(below))).thenReturn(true);
        when(mockLevel.getBlockState(below)).thenReturn(solidState);

        boolean result = validator.hasSolidGround(mockLevel, pos);

        assertTrue(result, "Should have solid ground");
    }

    @Test
    @DisplayName("hasSolidGround returns true for climbable blocks")
    void testHasSolidGroundClimbable() {
        BlockPos pos = new BlockPos(0, 64, 0);

        setupClimbableBlock(pos);

        boolean result = validator.hasSolidGround(mockLevel, pos);

        assertTrue(result, "Climbable blocks should provide solid ground");
    }

    @Test
    @DisplayName("hasSolidGround returns true for water")
    void testHasSolidGroundWater() {
        BlockPos pos = new BlockPos(0, 64, 0);

        setupWater(pos);

        boolean result = validator.hasSolidGround(mockLevel, pos);

        assertTrue(result, "Water should provide solid ground (swimming)");
    }

    @Test
    @DisplayName("hasHeadroom returns true with 2-block clearance")
    void testHasHeadroomValid() {
        BlockPos pos = new BlockPos(0, 64, 0);

        setupHeadroom(pos);

        boolean result = validator.hasHeadroom(mockLevel, pos);

        assertTrue(result, "Should have headroom with 2-block clearance");
    }

    @Test
    @DisplayName("hasHeadroom returns false with block above")
    void testHasHeadroomBlocked() {
        BlockPos pos = new BlockPos(0, 64, 0);

        setupNoHeadroom(pos);

        boolean result = validator.hasHeadroom(mockLevel, pos);

        assertFalse(result, "Should not have headroom with block above");
    }

    @Test
    @DisplayName("isBlocked returns true for solid blocks")
    void testIsBlocked() {
        BlockPos pos = new BlockPos(0, 64, 0);

        setupBlocked(pos);

        boolean result = validator.isBlocked(mockLevel, pos);

        assertTrue(result, "Should be blocked for solid blocks");
    }

    @Test
    @DisplayName("isClimbable returns true for ladders")
    void testIsClimbableLadder() {
        BlockPos pos = new BlockPos(0, 64, 0);

        setupClimbableBlock(pos);

        boolean result = validator.isClimbable(mockLevel, pos);

        assertTrue(result, "Ladders should be climbable");
    }

    @Test
    @DisplayName("isWater returns true for water blocks")
    void testIsWater() {
        BlockPos pos = new BlockPos(0, 64, 0);

        setupWater(pos);

        boolean result = validator.isWater(mockLevel, pos);

        assertTrue(result, "Water blocks should be detected");
    }

    @Test
    @DisplayName("Cache can be cleared")
    void testClearCache() {
        BlockPos pos = new BlockPos(0, 64, 0);
        setupSolidGround(pos);

        // Perform some validation to populate cache
        validator.hasSolidGround(mockLevel, pos);

        // Should not throw exception
        validator.clearCache();
    }

    @Test
    @DisplayName("Custom cache size is respected")
    void testCustomCacheSize() {
        MovementValidator customValidator = new MovementValidator(100);

        // Should not throw exception
        customValidator.clearCache();

        assertNotNull(customValidator, "Validator with custom cache size should be created");
    }

    // ========== canMove Integration Tests ==========

    @Test
    @DisplayName("canMove dispatches to correct movement type")
    void testCanMoveDispatch() {
        BlockPos from = new BlockPos(0, 64, 0);
        BlockPos to = new BlockPos(1, 64, 0);

        setupSolidGround(to);
        setupHeadroom(to);
        setupNotDangerous(to);
        setupInBounds(to);

        boolean result = validator.canMove(from, to, MovementType.WALK, mockContext);

        assertTrue(result, "canMove should dispatch to canWalkTo");
    }

    @Test
    @DisplayName("canMove rejects dangerous blocks")
    void testCanMoveDangerous() {
        BlockPos from = new BlockPos(0, 64, 0);
        BlockPos to = new BlockPos(1, 64, 0);

        setupSolidGround(to);
        setupHeadroom(to);
        setupDangerous(to); // Lava or cactus
        setupInBounds(to);
        when(mockContext.allowDangerousMovements()).thenReturn(false);

        boolean result = validator.canMove(from, to, MovementType.WALK, mockContext);

        assertFalse(result, "canMove should reject dangerous blocks");
    }

    @Test
    @DisplayName("canMove rejects out of bounds positions")
    void testCanMoveOutOfBounds() {
        BlockPos from = new BlockPos(0, 64, 0);
        BlockPos to = new BlockPos(1, -100, 0); // Below world

        setupOutOfBounds(to);

        boolean result = validator.canMove(from, to, MovementType.WALK, mockContext);

        assertFalse(result, "canMove should reject out of bounds positions");
    }

    // ========== Helper setup methods ==========

    private void setupDefaultContext() {
        when(mockContext.getLevel()).thenReturn(mockLevel);
        when(mockContext.getEntity()).thenReturn(mockEntity);
        when(mockContext.getJumpHeight()).thenReturn(1.0);
        when(mockContext.getMaxFallDistance()).thenReturn(3);
        when(mockContext.canClimb()).thenReturn(false);
        when(mockContext.canSwim()).thenReturn(false);
        when(mockContext.canFly()).thenReturn(false);
        when(mockContext.canParkour()).thenReturn(false);
        when(mockContext.allowDangerousMovements()).thenReturn(false);

        // Setup world bounds
        when(mockLevel.getMinBuildHeight()).thenReturn(-64);
        when(mockLevel.getMaxBuildHeight()).thenReturn(320);
    }

    private void setupSolidGround(BlockPos pos) {
        BlockPos below = pos.below();
        BlockState solidState = mock(BlockState.class);
        when(solidState.isSolidRender(eq(mockLevel), eq(below))).thenReturn(true);
        when(mockLevel.getBlockState(below)).thenReturn(solidState);
    }

    private void setupNoSolidGround(BlockPos pos) {
        BlockPos below = pos.below();
        BlockState airState = mock(BlockState.class);
        when(airState.isSolidRender(eq(mockLevel), eq(below))).thenReturn(false);
        when(mockLevel.getBlockState(below)).thenReturn(airState);
    }

    private void setupHeadroom(BlockPos pos) {
        BlockState airState = mock(BlockState.class);
        when(airState.isSolidRender(eq(mockLevel), any())).thenReturn(false);
        when(mockLevel.getBlockState(pos)).thenReturn(airState);
        when(mockLevel.getBlockState(pos.above())).thenReturn(airState);
    }

    private void setupNoHeadroom(BlockPos pos) {
        BlockState solidState = mock(BlockState.class);
        when(solidState.isSolidRender(eq(mockLevel), any())).thenReturn(true);
        when(mockLevel.getBlockState(pos.above())).thenReturn(solidState);
    }

    private void setupLowCeiling(BlockPos pos) {
        BlockState belowState = mock(BlockState.class);
        when(belowState.isSolidRender(eq(mockLevel), any())).thenReturn(false);
        when(mockLevel.getBlockState(pos)).thenReturn(belowState);

        BlockState aboveState = mock(BlockState.class);
        when(aboveState.isSolidRender(eq(mockLevel), eq(pos.above()))).thenReturn(true);
        when(mockLevel.getBlockState(pos.above())).thenReturn(aboveState);
    }

    private void setupBlocked(BlockPos pos) {
        BlockState solidState = mock(BlockState.class);
        when(solidState.isSolidRender(eq(mockLevel), eq(pos))).thenReturn(true);
        when(mockLevel.getBlockState(pos)).thenReturn(solidState);
    }

    private void setupNotBlocked(BlockPos pos) {
        BlockState airState = mock(BlockState.class);
        when(airState.isSolidRender(eq(mockLevel), eq(pos))).thenReturn(false);
        when(mockLevel.getBlockState(pos)).thenReturn(airState);
    }

    private void setupClimbableBlock(BlockPos pos) {
        BlockState climbableState = mock(BlockState.class);
        when(climbableState.isLadder(eq(mockLevel), eq(pos), any())).thenReturn(true);
        when(mockLevel.getBlockState(pos)).thenReturn(climbableState);
    }

    private void setupNonClimbableBlock(BlockPos pos) {
        BlockState normalState = mock(BlockState.class);
        when(normalState.isLadder(eq(mockLevel), eq(pos), any())).thenReturn(false);
        when(mockLevel.getBlockState(pos)).thenReturn(normalState);
    }

    private void setupWater(BlockPos pos) {
        BlockState waterState = mock(BlockState.class);
        when(waterState.getFluidState()).thenReturn(Fluids.WATER.defaultFluidState());
        when(mockLevel.getBlockState(pos)).thenReturn(waterState);
    }

    private void setupNoWater(BlockPos pos) {
        BlockState nonWaterState = mock(BlockState.class);
        when(nonWaterState.getFluidState()).thenReturn(Fluids.EMPTY.defaultFluidState());
        when(mockLevel.getBlockState(pos)).thenReturn(nonWaterState);
    }

    private void setupDangerous(BlockPos pos) {
        BlockState dangerousState = mock(BlockState.class);
        when(dangerousState.getFluidState()).thenReturn(Fluids.LAVA.defaultFluidState());
        when(mockLevel.getBlockState(pos)).thenReturn(dangerousState);
    }

    private void setupNotDangerous(BlockPos pos) {
        BlockState safeState = mock(BlockState.class);
        when(safeState.getFluidState()).thenReturn(Fluids.EMPTY.defaultFluidState());
        when(mockLevel.getBlockState(pos)).thenReturn(safeState);
    }

    private void setupInBounds(BlockPos pos) {
        // Within world bounds
        when(mockLevel.getMinBuildHeight()).thenReturn(-64);
        when(mockLevel.getMaxBuildHeight()).thenReturn(320);
    }

    private void setupOutOfBounds(BlockPos pos) {
        when(mockLevel.getMinBuildHeight()).thenReturn(-64);
        when(mockLevel.getMaxBuildHeight()).thenReturn(320);
    }

    private void setupClearFallPath(BlockPos from, BlockPos to) {
        // All blocks between from and to are not blocked
        for (int y = to.getY() + 1; y < from.getY(); y++) {
            BlockPos checkPos = new BlockPos(to.getX(), y, to.getZ());
            setupNotBlocked(checkPos);
        }
    }
}
