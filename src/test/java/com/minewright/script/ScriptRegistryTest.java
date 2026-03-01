package com.minewright.script;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Test suite for ScriptRegistry class.
 */
public class ScriptRegistryTest {

    @AfterEach
    public void cleanup() {
        ScriptRegistry.reset();
    }

    @Test
    public void testSingletonInstance() {
        ScriptRegistry instance1 = ScriptRegistry.getInstance();
        ScriptRegistry instance2 = ScriptRegistry.getInstance();

        assertSame(instance1, instance2);
    }

    @Test
    public void testRegisterValidScript() {
        ScriptRegistry registry = ScriptRegistry.getInstance();

        Script script = createValidScript("test_script", "Test Script");

        assertTrue(registry.register(script));
        assertTrue(registry.contains("test_script"));
        assertEquals(1, registry.size());
    }

    @Test
    public void testRegisterNullScript() {
        ScriptRegistry registry = ScriptRegistry.getInstance();

        assertFalse(registry.register(null));
        assertEquals(0, registry.size());
    }

    @Test
    public void testRegisterScriptWithoutId() {
        ScriptRegistry registry = ScriptRegistry.getInstance();

        Script script = Script.builder()
            .metadata(Script.ScriptMetadata.builder()
                .name("No ID Script")
                .build())
            .scriptNode(ScriptNode.builder()
                .type(ScriptNode.NodeType.SEQUENCE)
                .build())
            .build();

        assertFalse(registry.register(script));
        assertEquals(0, registry.size());
    }

    @Test
    public void testGetScript() {
        ScriptRegistry registry = ScriptRegistry.getInstance();

        Script script = createValidScript("get_test", "Get Test");
        registry.register(script);

        Script retrieved = registry.get("get_test");

        assertNotNull(retrieved);
        assertEquals("get_test", retrieved.getId());
        assertEquals("Get Test", retrieved.getName());
    }

    @Test
    public void testGetNonexistentScript() {
        ScriptRegistry registry = ScriptRegistry.getInstance();

        Script retrieved = registry.get("nonexistent");

        assertNull(retrieved);
    }

    @Test
    public void testUnregisterScript() {
        ScriptRegistry registry = ScriptRegistry.getInstance();

        Script script = createValidScript("unreg_test", "Unregister Test");
        registry.register(script);

        assertTrue(registry.contains("unreg_test"));

        Script unregistered = registry.unregister("unreg_test");

        assertNotNull(unregistered);
        assertEquals("unreg_test", unregistered.getId());
        assertFalse(registry.contains("unreg_test"));
        assertEquals(0, registry.size());
    }

    @Test
    public void testUnregisterNonexistentScript() {
        ScriptRegistry registry = ScriptRegistry.getInstance();

        Script unregistered = registry.unregister("nonexistent");

        assertNull(unregistered);
    }

    @Test
    public void testFindByName() {
        ScriptRegistry registry = ScriptRegistry.getInstance();

        Script script1 = createValidScript("script1", "Mining Script");
        Script script2 = createValidScript("script2", "Mining Script");
        Script script3 = createValidScript("script3", "Building Script");

        registry.register(script1);
        registry.register(script2);
        registry.register(script3);

        List<Script> miningScripts = registry.findByName("Mining Script");

        assertEquals(2, miningScripts.size());
        assertTrue(miningScripts.stream().allMatch(s -> "Mining Script".equals(s.getName())));
    }

    @Test
    public void testFindByNameCaseInsensitive() {
        ScriptRegistry registry = ScriptRegistry.getInstance();

        Script script = createValidScript("test", "Test Script");
        registry.register(script);

        List<Script> result1 = registry.findByName("test script");
        List<Script> result2 = registry.findByName("TEST SCRIPT");
        List<Script> result3 = registry.findByName("Test Script");

        assertEquals(1, result1.size());
        assertEquals(1, result2.size());
        assertEquals(1, result3.size());
    }

    @Test
    public void testSearchByNamePattern() {
        ScriptRegistry registry = ScriptRegistry.getInstance();

        registry.register(createValidScript("s1", "Auto Mining"));
        registry.register(createValidScript("s2", "Auto Building"));
        registry.register(createValidScript("s3", "Manual Mining"));
        registry.register(createValidScript("s4", "Combat Script"));

        List<Script> autoScripts = registry.searchByName("auto");
        List<Script> miningScripts = registry.searchByName("mining");

        assertEquals(2, autoScripts.size());
        assertEquals(2, miningScripts.size());
    }

    @Test
    public void testFindByTag() {
        ScriptRegistry registry = ScriptRegistry.getInstance();

        Script script1 = Script.builder()
            .metadata(Script.ScriptMetadata.builder()
                .id("tagged1")
                .name("Script 1")
                .addTag("mining")
                .addTag("automation")
                .build())
            .scriptNode(ScriptNode.builder()
                .type(ScriptNode.NodeType.SEQUENCE)
                .build())
            .build();

        Script script2 = Script.builder()
            .metadata(Script.ScriptMetadata.builder()
                .id("tagged2")
                .name("Script 2")
                .addTag("building")
                .build())
            .scriptNode(ScriptNode.builder()
                .type(ScriptNode.NodeType.SEQUENCE)
                .build())
            .build();

        registry.register(script1);
        registry.register(script2);

        List<Script> miningScripts = registry.findByTag("mining");
        List<Script> buildingScripts = registry.findByTag("building");

        assertEquals(1, miningScripts.size());
        assertEquals("tagged1", miningScripts.get(0).getId());

        assertEquals(1, buildingScripts.size());
        assertEquals("tagged2", buildingScripts.get(0).getId());
    }

    @Test
    public void testGetAll() {
        ScriptRegistry registry = ScriptRegistry.getInstance();

        registry.register(createValidScript("s1", "Script 1"));
        registry.register(createValidScript("s2", "Script 2"));
        registry.register(createValidScript("s3", "Script 3"));

        Collection<Script> all = registry.getAll();

        assertEquals(3, all.size());
    }

    @Test
    public void testIsEmpty() {
        ScriptRegistry registry = ScriptRegistry.getInstance();

        assertTrue(registry.isEmpty());

        registry.register(createValidScript("test", "Test"));

        assertFalse(registry.isEmpty());
    }

    @Test
    public void testClear() {
        ScriptRegistry registry = ScriptRegistry.getInstance();

        registry.register(createValidScript("s1", "Script 1"));
        registry.register(createValidScript("s2", "Script 2"));

        assertEquals(2, registry.size());

        registry.clear();

        assertEquals(0, registry.size());
        assertTrue(registry.isEmpty());
    }

    @Test
    public void testGetIds() {
        ScriptRegistry registry = ScriptRegistry.getInstance();

        registry.register(createValidScript("id1", "Script 1"));
        registry.register(createValidScript("id2", "Script 2"));
        registry.register(createValidScript("id3", "Script 3"));

        Set<String> ids = registry.getIds();

        assertEquals(3, ids.size());
        assertTrue(ids.contains("id1"));
        assertTrue(ids.contains("id2"));
        assertTrue(ids.contains("id3"));
    }

    @Test
    public void testGetNames() {
        ScriptRegistry registry = ScriptRegistry.getInstance();

        registry.register(createValidScript("s1", "Mining"));
        registry.register(createValidScript("s2", "Building"));
        registry.register(createValidScript("s3", "Mining"));

        Set<String> names = registry.getNames();

        assertEquals(2, names.size());
        assertTrue(names.contains("mining"));
        assertTrue(names.contains("building"));
    }

    @Test
    public void testRegisterAll() {
        ScriptRegistry registry = ScriptRegistry.getInstance();

        List<Script> scripts = List.of(
            createValidScript("s1", "Script 1"),
            createValidScript("s2", "Script 2"),
            createValidScript("s3", "Script 3")
        );

        int count = registry.registerAll(scripts);

        assertEquals(3, count);
        assertEquals(3, registry.size());
    }

    @Test
    public void testGetStats() {
        ScriptRegistry registry = ScriptRegistry.getInstance();

        Script script1 = Script.builder()
            .metadata(Script.ScriptMetadata.builder()
                .id("stats1")
                .name("Mining Script")
                .addTag("automation")
                .addTag("mining")
                .build())
            .scriptNode(ScriptNode.builder()
                .type(ScriptNode.NodeType.SEQUENCE)
                .build())
            .build();

        Script script2 = Script.builder()
            .metadata(Script.ScriptMetadata.builder()
                .id("stats2")
                .name("Building Script")
                .addTag("automation")
                .build())
            .scriptNode(ScriptNode.builder()
                .type(ScriptNode.NodeType.SEQUENCE)
                .build())
            .build();

        registry.register(script1);
        registry.register(script2);

        ScriptRegistry.RegistryStats stats = registry.getStats();

        assertEquals(2, stats.totalScripts());
        assertEquals(2, stats.uniqueNames());
        assertEquals(2, stats.tagCounts().size());
        assertEquals(2, stats.tagCounts().get("automation"));
        assertEquals(1, stats.tagCounts().get("mining"));
    }

    @Test
    public void testReset() {
        ScriptRegistry registry = ScriptRegistry.getInstance();

        registry.register(createValidScript("test", "Test"));

        assertEquals(1, registry.size());

        ScriptRegistry.reset();

        assertEquals(0, registry.size());
    }

    // Helper methods

    private Script createValidScript(String id, String name) {
        return Script.builder()
            .metadata(Script.ScriptMetadata.builder()
                .id(id)
                .name(name)
                .description("Test script for " + name)
                .build())
            .scriptNode(ScriptNode.builder()
                .type(ScriptNode.NodeType.SEQUENCE)
                .build())
            .version("1.0.0")
            .build();
    }
}
