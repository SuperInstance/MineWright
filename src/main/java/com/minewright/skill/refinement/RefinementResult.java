package com.minewright.skill.refinement;

import com.minewright.skill.Skill;

public class RefinementResult {
    private final Skill refinedSkill;
    private final String reasoning;
    private final int iteration;
    private final boolean success;

    public RefinementResult(Skill refinedSkill, String reasoning, int iteration, boolean success) {
        this.refinedSkill = refinedSkill;
        this.reasoning = reasoning != null ? reasoning : "";
        this.iteration = Math.max(0, iteration);
        this.success = success;
    }

    public Skill getRefinedSkill() { return refinedSkill; }
    public String getReasoning() { return reasoning; }
    public int getIteration() { return iteration; }
    public boolean isSuccess() { return success; }

    public static RefinementResult success(Skill refinedSkill, String reasoning, int iteration) {
        return new RefinementResult(refinedSkill, reasoning, iteration, true);
    }

    public static RefinementResult failure(String reason, int iteration) {
        return new RefinementResult(null, reason, iteration, false);
    }
}
