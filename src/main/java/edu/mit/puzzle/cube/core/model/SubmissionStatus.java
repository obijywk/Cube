package edu.mit.puzzle.cube.core.model;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

public enum SubmissionStatus {

    SUBMITTED,
    ASSIGNED,
    INCORRECT,
    CORRECT;

    private static Set<SubmissionStatus> TERMINAL_STATUSES = ImmutableSet.of(INCORRECT, CORRECT);

    public static SubmissionStatus getDefault() {
        return SUBMITTED;
    }

    public boolean isTerminal() {
        return TERMINAL_STATUSES.contains(this);
    }
}
