package com.minewright.skill.refinement;

import java.util.ArrayList;
import java.util.List;

public class SkillFeedback {
    private final String analysis;
    private final List<Issue> issues;
    private final List<String> suggestions;
    private final List<String> validationRequirements;

    public SkillFeedback(String analysis, List<Issue> issues, List<String> suggestions, List<String> validationRequirements) {
        this.analysis = analysis != null ? analysis : "";
        this.issues = issues != null ? List.copyOf(issues) : List.of();
        this.suggestions = suggestions != null ? List.copyOf(suggestions) : List.of();
        this.validationRequirements = validationRequirements != null ? List.copyOf(validationRequirements) : List.of();
    }

    public String getAnalysis() { return analysis; }
    public List<Issue> getIssues() { return issues; }
    public List<String> getSuggestions() { return suggestions; }
    public List<String> getValidationRequirements() { return validationRequirements; }
    public boolean hasHighSeverityIssues() {
        return issues.stream().anyMatch(i -> "high".equalsIgnoreCase(i.getSeverity()));
    }

    public static Builder builder() { return new Builder(); }

    public static class Issue {
        private final String description;
        private final String severity;
        private final String location;

        public Issue(String description, String severity, String location) {
            this.description = description != null ? description : "";
            this.severity = severity != null ? severity.toLowerCase() : "medium";
            this.location = location;
        }

        public String getDescription() { return description; }
        public String getSeverity() { return severity; }
        public String getLocation() { return location; }
    }

    public static class Builder {
        private String analysis;
        private final List<Issue> issues = new ArrayList<>();
        private final List<String> suggestions = new ArrayList<>();
        private final List<String> validationRequirements = new ArrayList<>();

        public Builder analysis(String a) { this.analysis = a; return this; }
        public Builder issue(String d, String s) { issues.add(new Issue(d, s, null)); return this; }
        public Builder issue(String d, String s, String l) { issues.add(new Issue(d, s, l)); return this; }
        public Builder suggestion(String s) { suggestions.add(s); return this; }
        public Builder validationRequirement(String r) { validationRequirements.add(r); return this; }
        public SkillFeedback build() { return new SkillFeedback(analysis, issues, suggestions, validationRequirements); }
    }
}
