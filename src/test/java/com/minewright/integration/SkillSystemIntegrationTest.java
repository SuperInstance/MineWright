package com.minewright.integration;

import com.minewright.skill.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Skill System Integration Tests")
class SkillSystemIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("Skills can be added and retrieved from library")
    void testSkillStorage() {
        SkillLibrary library = SkillLibrary.getInstance();

        ExecutableSkill skill = ExecutableSkill.builder("mineIronOre")
            .description("Mine iron ore from nearby deposits")
            .codeTemplate("function mineIronOre() { return findAndMine('iron_ore'); }")
            .category("mining")
            .requiredAction("mine")
            .build();

        assertTrue(library.addSkill(skill), "Skill should be added");

        Skill retrieved = library.getSkill("mineIronOre");
        assertTrue(retrieved != null, "Skill should be retrievable");
        assertEquals("mineIronOre", retrieved.getName(), "Name should match");
        assertEquals("mining", retrieved.getCategory(), "Category should match");
    }
}