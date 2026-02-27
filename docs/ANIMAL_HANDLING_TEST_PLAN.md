# Animal Handling AI - Test Plan

## Test Environment

- **Minecraft Version:** 1.20.1
- **Forge Version:** 47.4.16
- **Java Version:** 17
- **Test World:** Superflat with animals spawned

## Unit Tests

### AnimalClassificationTest

```java
@Test
public void testWolfClassification() {
    Wolf wolf = new Wolf(EntityType.WOLF, level);
    assertEquals(AnimalClassification.TAMABLE_PET,
                 AnimalClassification.classify(wolf));
    assertTrue(AnimalClassification.TAMABLE_PET.isTamable());
    assertTrue(AnimalClassification.TAMABLE_PET.isBreedable());
}

@Test
public void testCowClassification() {
    Cow cow = new Cow(EntityType.COW, level);
    assertEquals(AnimalClassification.BREEDABLE_LIVESTOCK,
                 AnimalClassification.classify(cow));
    assertFalse(AnimalClassification.BREEDABLE_LIVESTOCK.isTamable());
    assertTrue(AnimalClassification.BREEDABLE_LIVESTOCK.isBreedable());
}

@Test
public void testZombieClassification() {
    Zombie zombie = new Zombie(EntityType.ZOMBIE, level);
    assertEquals(AnimalClassification.HOSTILE,
                 AnimalClassification.classify(zombie));
    assertTrue(AnimalClassification.HOSTILE.isDangerous());
    assertFalse(AnimalClassification.HOSTILE.isBreedable());
}
```

### AnimalDetectorTest

```java
@Test
public void testScanRadius() {
    ForemanEntity foreman = spawnForeman();
    AnimalDetector detector = new AnimalDetector(foreman);

    // Spawn animals at different distances
    spawnAnimal(EntityType.COW, foreman.blockPosition(), 5);
    spawnAnimal(EntityType.COW, foreman.blockPosition(), 20);
    spawnAnimal(EntityType.COW, foreman.blockPosition(), 40);

    Map<AnimalClassification, List<Animal>> result = detector.scan(32);

    // Should find 2 cows (within 32 blocks)
    assertEquals(2, result.get(AnimalClassification.BREEDABLE_LIVESTOCK).size());
}

@Test
public void testFindNearest() {
    ForemanEntity foreman = spawnForeman();
    AnimalDetector detector = new AnimalDetector(foreman);

    spawnAnimal(EntityType.WOLF, foreman.blockPosition(), 10);
    spawnAnimal(EntityType.WOLF, foreman.blockPosition(), 5);

    Animal nearest = detector.findNearest(AnimalClassification.TAMABLE_PET);

    assertNotNull(nearest);
    assertEquals(5.0, foreman.distanceTo(nearest), 0.1);
}

@Test
public void testFindBreedingPairs() {
    ForemanEntity foreman = spawnForeman();
    AnimalDetector detector = new AnimalDetector(foreman);

    // Spawn adult cows
    Cow cow1 = spawnAdultCow(foreman.blockPosition(), 5);
    Cow cow2 = spawnAdultCow(foreman.blockPosition(), 6);
    Cow baby = spawnBabyCow(foreman.blockPosition(), 7);

    List<BreedingPair> pairs = detector.findBreedingPairs();

    // Should find 1 pair (cow1 + cow2, baby excluded)
    assertEquals(1, pairs.size());
    assertTrue(pairs.get(0).parent1() == cow1 || pairs.get(0).parent1() == cow2);
}
```

### PetManagerTest

```java
@Test
public void testTameAnimal() {
    ForemanEntity foreman = spawnForeman();
    PetManager petManager = new PetManager(foreman);

    Wolf wolf = spawnWildWolf();
    assertFalse(wolf.isTame());

    petManager.tameAnimal(wolf);

    assertTrue(wolf.isTame());
    assertEquals(1, petManager.getOwnedPets().size());
}

@Test
public void testCommandPetFollow() {
    ForemanEntity foreman = spawnForeman();
    PetManager petManager = new PetManager(foreman);

    Wolf wolf = spawnTamedWolf(foreman);
    petManager.tameAnimal(wolf);

    petManager.commandPet(wolf.getUUID(), PetCommand.FOLLOW);

    PetData pet = petManager.getOwnedPets().iterator().next();
    assertEquals(PetFollowMode.FOLLOW, pet.followMode());
}

@Test
public void testCommandPetStay() {
    ForemanEntity foreman = spawnForeman();
    PetManager petManager = new PetManager(foreman);

    Wolf wolf = spawnTamedWolf(foreman);
    petManager.tameAnimal(wolf);

    petManager.commandPet(wolf.getUUID(), PetCommand.STAY);

    PetData pet = petManager.getOwnedPets().iterator().next();
    assertEquals(PetFollowMode.STAY, pet.followMode());
    assertTrue(wolf.isInSittingPose());
}
```

### BreedingManagerTest

```java
@Test
public void testScheduleBreeding() {
    ForemanEntity foreman = spawnForeman();
    BreedingManager breedingManager = new BreedingManager(foreman);

    // Spawn adult cows
    spawnAdultCow(foreman.blockPosition(), 5);
    spawnAdultCow(foreman.blockPosition(), 6);

    breedingManager.scheduleBreeding(EntityType.COW, 1);

    // Should have scheduled breeding
    assertFalse(breedingManager.getBreedingQueue().isEmpty());
}

@Test
public void testBreedingCooldown() {
    ForemanEntity foreman = spawnForeman();
    BreedingManager breedingManager = new BreedingManager(foreman);

    Cow cow1 = spawnAdultCow(foreman.blockPosition(), 5);
    Cow cow2 = spawnAdultCow(foreman.blockPosition(), 6);

    // Breed once
    breedingManager.breedPair(new BreedingPair(cow1, cow2));

    // Try to breed again immediately
    BreedingStats stats = breedingManager.getStats(EntityType.COW);
    assertEquals(1, stats.bred);

    // Should respect cooldown
    assertTrue(cow1.getAge() >= 0); // Still in cooldown
}
```

### PenManagerTest

```java
@Test
public void testCreatePen() {
    ForemanEntity foreman = spawnForeman();
    PenManager penManager = new PenManager(foreman);

    Pen pen = penManager.createPen(
        new BlockPos(100, 64, 100),
        16, 16, 3,
        "sheep"
    );

    assertNotNull(pen);
    assertEquals("sheep", pen.purpose());
    assertEquals(0, penManager.countInPen(pen.id()));
}

@Test
public void testGeneratePenStructure() {
    ForemanEntity foreman = spawnForeman();
    PenManager penManager = new PenManager(foreman);

    Pen pen = penManager.createPen(
        new BlockPos(100, 64, 100),
        10, 10, 3,
        "test"
    );

    List<BlockPlacement> blocks = penManager.generatePenStructure(pen);

    // Should have blocks for floor, walls, gate
    assertTrue(blocks.size() > 100);
}

@Test
public void testAssignToPen() {
    ForemanEntity foreman = spawnForeman();
    PenManager penManager = new PenManager(foreman);

    Pen pen = penManager.createPen(
        new BlockPos(100, 64, 100),
        16, 16, 3,
        "sheep"
    );

    Sheep sheep = spawnSheep(new BlockPos(100, 64, 100));
    penManager.assignToPen(pen.id(), List.of(sheep));

    assertTrue(pen.animals().contains(sheep.getUUID()));
    assertEquals(1, penManager.countInPen(pen.id()));
}
```

## Integration Tests

### Taming Integration

```java
@Test
public void testTameWolfIntegration() {
    // Setup
    ForemanEntity foreman = spawnForeman();
    Wolf wolf = spawnWildWolf(foreman.blockPosition(), 10);

    // Create taming task
    Task task = new Task("tame wolf", Map.of("target", "wolf"));
    TameAction action = new TameAction(foreman, task);

    // Execute
    action.start();
    while (!action.isComplete()) {
        action.tick();
        tickWorld();
    }

    // Verify
    ActionResult result = action.getResult();
    assertTrue(result.success(), result.message());
    assertTrue(wolf.isTame());
    assertEquals(foreman, wolf.getOwner());
}

@Test
public void testTameCatIntegration() {
    ForemanEntity foreman = spawnForeman();
    Cat cat = spawnWildCat(foreman.blockPosition(), 10);

    Task task = new Task("tame cat", Map.of("target", "cat"));
    TameAction action = new TameAction(foreman, task);

    action.start();
    while (!action.isComplete()) {
        action.tick();
        tickWorld();
    }

    assertTrue(action.getResult().success());
    assertTrue(cat.isTame());
}
```

### Breeding Integration

```java
@Test
public void testBreedCowsIntegration() {
    // Setup
    ForemanEntity foreman = spawnForeman();
    Cow cow1 = spawnAdultCow(foreman.blockPosition(), 10);
    Cow cow2 = spawnAdultCow(foreman.blockPosition(), 12);

    // Give foreman wheat
    giveItem(foreman, Items.WHEAT, 2);

    // Create breeding task
    Task task = new Task("breed cows", Map.of(
        "animal", "cow",
        "count", 1
    ));

    BreedAction action = new BreedAction(foreman, task);

    // Execute
    action.start();
    while (!action.isComplete()) {
        action.tick();
        tickWorld();
    }

    // Verify
    ActionResult result = action.getResult();
    assertTrue(result.success(), result.message());

    // Check for baby cow
    List<Entity> babies = getEntitiesNearby(foreman, 20, EntityType.COW);
    assertTrue(babies.stream().anyMatch(e -> ((Cow)e).isBaby()));
}

@Test
public void testBreedChickensIntegration() {
    ForemanEntity foreman = spawnForeman();
    Chicken chicken1 = spawnAdultChicken(foreman.blockPosition(), 5);
    Chicken chicken2 = spawnAdultChicken(foreman.blockPosition(), 6);

    giveItem(foreman, Items.WHEAT_SEEDS, 2);

    Task task = new Task("breed chickens", Map.of(
        "animal", "chicken",
        "count", 1
    ));

    BreedAction action = new BreedAction(foreman, task);

    action.start();
    while (!action.isComplete()) {
        action.tick();
        tickWorld();
    }

    assertTrue(action.getResult().success());
}
```

### Pen Building Integration

```java
@Test
public void testBuildPenIntegration() {
    // Setup
    ForemanEntity foreman = spawnForeman();
    BlockPos buildLocation = new BlockPos(100, 64, 100);

    // Create pen building task
    Task task = new Task("build sheep pen", Map.of(
        "type", "sheep",
        "width", 16,
        "depth", 16,
        "height", 3,
        "x", buildLocation.getX(),
        "y", buildLocation.getY(),
        "z", buildLocation.getZ()
    ));

    BuildPenAction action = new BuildPenAction(foreman, task);

    // Execute
    action.start();
    while (!action.isComplete()) {
        action.tick();
        tickWorld();
    }

    // Verify pen structure
    ActionResult result = action.getResult();
    assertTrue(result.success(), result.message());

    // Check for fence walls
    assertTrue(hasBlockAt(buildLocation.offset(0, 0, -8), Blocks.OAK_FENCE));
    assertTrue(hasBlockAt(buildLocation.offset(0, 0, 8), Blocks.OAK_FENCE));

    // Check for gate
    assertTrue(hasBlockAt(buildLocation.offset(0, 0, 8), Blocks.OAK_FENCE_GATE));

    // Check for feeder
    assertTrue(hasBlockAt(buildLocation, Blocks.HAY_BLOCK));
}
```

### Shearing Integration

```java
@Test
public void testShearSheepIntegration() {
    // Setup
    ForemanEntity foreman = spawnForeman();
    Sheep sheep = spawnSheep(foreman.blockPosition(), 10);
    assertFalse(sheep.isSheared());

    // Create shearing task
    Task task = new Task("shear sheep", Map.of("target", "sheep"));
    ShearAction action = new ShearAction(foreman, task);

    // Execute
    action.start();
    while (!action.isComplete()) {
        action.tick();
        tickWorld();
    }

    // Verify
    ActionResult result = action.getResult();
    assertTrue(result.success(), result.message());
    assertTrue(sheep.isSheared());

    // Check for wool drops
    List<ItemEntity> items = getItemsNearby(foreman, 10);
    assertTrue(items.stream().anyMatch(i -> i.getItem().getItem() == Items.WHITE_WOOL));
}

@Test
public void testShearMultipleSheepIntegration() {
    ForemanEntity foreman = spawnForeman();

    // Spawn 5 sheep
    for (int i = 0; i < 5; i++) {
        spawnSheep(foreman.blockPosition(), 5 + i * 3);
    }

    Task task = new Task("shear sheep", Map.of("target", "sheep"));
    ShearAction action = new ShearAction(foreman, task);

    action.start();
    while (!action.isComplete()) {
        action.tick();
        tickWorld();
    }

    ActionResult result = action.getResult();
    assertTrue(result.success());
    assertTrue(result.message().contains("5"));
}
```

## Performance Tests

```java
@Test
public void testScanManyAnimals() {
    ForemanEntity foreman = spawnForeman();
    AnimalDetector detector = new AnimalDetector(foreman);

    // Spawn 100 animals
    for (int i = 0; i < 100; i++) {
        spawnAnimal(EntityType.COW, foreman.blockPosition(), 10 + i);
    }

    long startTime = System.nanoTime();
    Map<AnimalClassification, List<Animal>> result = detector.scan(64);
    long duration = System.nanoTime() - startTime;

    // Should complete in under 100ms
    assertTrue(duration < 100_000_000);
    assertEquals(100, result.get(AnimalClassification.BREEDABLE_LIVESTOCK).size());
}

@Test
public void testLargeBreedingQueue() {
    ForemanEntity foreman = spawnForeman();
    BreedingManager breedingManager = new BreedingManager(foreman);

    // Spawn 50 pairs (100 animals)
    for (int i = 0; i < 50; i++) {
        spawnAdultCow(foreman.blockPosition(), 5 + i * 2);
        spawnAdultCow(foreman.blockPosition(), 6 + i * 2);
    }

    breedingManager.scheduleBreeding(EntityType.COW, 50);

    // Queue should handle 50 pairs
    assertFalse(breedingManager.getBreedingQueue().isEmpty());
}

@Test
public void testManyPetsTracking() {
    ForemanEntity foreman = spawnForeman();
    PetManager petManager = new PetManager(foreman);

    // Tame 20 wolves
    for (int i = 0; i < 20; i++) {
        Wolf wolf = spawnTamedWolf(foreman);
        petManager.tameAnimal(wolf);
    }

    assertEquals(20, petManager.getOwnedPets().size());

    // Test tick performance
    long startTime = System.nanoTime();
    petManager.tick();
    long duration = System.nanoTime() - startTime;

    // Should tick quickly
    assertTrue(duration < 10_000_000); // Under 10ms
}
```

## End-to-End Scenarios

### Scenario 1: Start a Sheep Farm

```java
@Test
public void testSheepFarmScenario() {
    ForemanEntity foreman = spawnForeman();

    // Step 1: Build pen
    Task buildTask = new Task("build sheep pen", Map.of(
        "type", "sheep",
        "width", 20,
        "depth", 20,
        "height", 3
    ));

    BuildPenAction buildAction = new BuildPenAction(foreman, buildTask);
    executeAction(buildAction);
    assertTrue(buildAction.getResult().success());

    // Step 2: Spawn sheep
    for (int i = 0; i < 4; i++) {
        spawnSheep(foreman.blockPosition(), 5 + i * 2);
    }

    // Step 3: Breed sheep
    Task breedTask = new Task("breed sheep", Map.of("animal", "sheep", "count", 2));
    BreedAction breedAction = new BreedAction(foreman, breedTask);
    executeAction(breedAction);
    assertTrue(breedAction.getResult().success());

    // Step 4: Shear sheep
    waitMinutes(5); // Wait for babies to grow

    Task shearTask = new Task("shear sheep", Map.of("target", "sheep"));
    ShearAction shearAction = new ShearAction(foreman, shearTask);
    executeAction(shearAction);
    assertTrue(shearAction.getResult().success());

    // Verify: All sheep sheared, wool collected
    List<Entity> sheep = getEntitiesNearby(foreman, 20, EntityType.SHEEP);
    assertTrue(sheep.stream().allMatch(s -> ((Sheep)s).isSheared()));
}
```

### Scenario 2: Tame and Command Pets

```java
@Test
public void testPetCommandScenario() {
    ForemanEntity foreman = spawnForeman();
    PetManager petManager = new PetManager(foreman);

    // Step 1: Tame 3 wolves
    for (int i = 0; i < 3; i++) {
        Wolf wolf = spawnWildWolf(foreman.blockPosition(), 10 + i * 3);

        Task tameTask = new Task("tame wolf", Map.of("target", "wolf"));
        TameAction tameAction = new TameAction(foreman, tameTask);
        executeAction(tameAction);

        assertTrue(wolf.isTame());
        petManager.tameAnimal(wolf);
    }

    assertEquals(3, petManager.getOwnedPets().size());

    // Step 2: Command to follow
    petManager.setFollowMode(PetFollowMode.FOLLOW);

    // Move foreman
    foreman.teleportTo(foreman.getX() + 20, foreman.getY(), foreman.getZ());

    // Tick until pets follow
    for (int i = 0; i < 100; i++) {
        petManager.tick();
        tickWorld();
    }

    // Verify: Pets are following
    for (PetData pet : petManager.getOwnedPets()) {
        Animal animal = findAnimal(pet.uuid());
        assertNotNull(animal);
        assertTrue(animal.distanceTo(foreman) < 10);
    }

    // Step 3: Command to protect
    petManager.setFollowMode(PetFollowMode.PROTECT);

    // Spawn zombie near foreman
    Zombie zombie = spawnZombie(foreman.blockPosition(), 5);

    for (int i = 0; i < 100; i++) {
        petManager.tick();
        tickWorld();
    }

    // Verify: Wolves are attacking zombie
    for (PetData pet : petManager.getOwnedPets()) {
        Animal animal = findAnimal(pet.uuid());
        if (animal instanceof Wolf wolf) {
            assertEquals(zombie, wolf.getTarget());
        }
    }
}
```

## Manual Testing Checklist

### In-Game Tests

- [ ] Spawn foreman with `/foreman spawn Rancher`
- [ ] Command: "tame a wolf" - verify wolf becomes tamed
- [ ] Command: "tame a cat" - verify cat becomes tamed
- [ ] Command: "breed cows" - verify baby cow spawns
- [ ] Command: "build a sheep pen" - verify pen structure
- [ ] Command: "shear sheep" - verify sheep sheared, wool drops
- [ ] Command: "make my pets follow" - verify pets follow
- [ ] Command: "make my pets stay" - verify pets sit/stay
- [ ] Spawn hostile mob near pets in protect mode - verify attack

### Edge Cases

- [ ] Try to tame already tamed animal - should skip
- [ ] Try to breed baby animals - should fail
- [ ] Try to breed without food - should notify
- [ ] Try to build pen in invalid location - should find alternative
- [ ] Try to shear already sheared sheep - should skip
- [ ] Kill owned pet - should remove from tracking
- [ ] Reload world with pets - should persist
- [ ] Breed animals in pen - should stay in pen

### Performance

- [ ] Scan area with 200+ animals - should be fast
- [ ] Track 50+ pets - should not lag
- [ ] Process breeding queue of 20+ pairs - should handle
- [ ] Build multiple pens - should not conflict
- [ ] Tick all managers every tick - should be efficient

## Bug Reporting Template

```
Bug Title: [Brief description]

**Steps to Reproduce:**
1.
2.
3.

**Expected Behavior:**
[What should happen]

**Actual Behavior:**
[What actually happens]

**Environment:**
- Minecraft Version: 1.20.1
- Forge Version: 47.4.16
- MineWright Version: [version]

**Logs:**
[Paste relevant log sections]

**Screenshots/Videos:**
[If applicable]
```

## Test Execution

Run tests with:
```bash
./gradlew test --tests AnimalClassificationTest
./gradlew test --tests AnimalDetectorTest
./gradlew test --tests PetManagerTest
./gradlew test --tests BreedingManagerTest
./gradlew test --tests PenManagerTest
```

Run all animal tests:
```bash
./gradlew test --tests "*Animal*Test"
```

## Coverage Goals

- **Unit Tests:** 80% code coverage
- **Integration Tests:** All major use cases
- **Performance Tests:** Handle 200+ animals
- **Manual Tests:** All in-game commands

## Sign-Off

- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] Manual testing complete
- [ ] Performance acceptable
- [ ] Documentation updated
- [ ] Code reviewed
