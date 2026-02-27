package com.minewright.execution;

import com.minewright.action.actions.BaseAction;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for common action-related operations.
 *
 * <p>Provides shared functionality for action execution to reduce
 * code duplication across interceptors and other components.</p>
 *
 * @since 1.1.0
 */
public final class ActionUtils {

    private ActionUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Extracts action type name from action class.
     * <p>
     * Removes "Action" suffix and converts to lowercase.
     * For example, "MineBlockAction" becomes "mineblock".
     *
     * @param action The action to extract name from
     * @return Lowercase action type name
     */
    public static String extractActionType(BaseAction action) {
        if (action == null) {
            return "unknown";
        }
        String className = action.getClass().getSimpleName();
        if (className.endsWith("Action")) {
            return className.substring(0, className.length() - 6).toLowerCase();
        }
        return className.toLowerCase();
    }

    /**
     * Tracks start times for actions using identity hash codes.
     * <p>
     * Thread-safe implementation using ConcurrentHashMap.
     * Uses System.identityHashCode() to track actions by reference.
     */
    public static class ActionTimer {
        private final ConcurrentHashMap<Integer, Long> startTimes = new ConcurrentHashMap<>();

        /**
         * Records the start time for an action.
         *
         * @param action The action to track
         */
        public void recordStart(BaseAction action) {
            startTimes.put(System.identityHashCode(action), System.currentTimeMillis());
        }

        /**
         * Gets and removes the elapsed time for an action.
         *
         * @param action The action to get elapsed time for
         * @return Elapsed milliseconds, or 0 if no start time was recorded
         */
        public long getElapsedAndRemove(BaseAction action) {
            Long startTime = startTimes.remove(System.identityHashCode(action));
            return startTime != null ? System.currentTimeMillis() - startTime : 0;
        }

        /**
         * Removes the start time for an action without calculating elapsed time.
         *
         * @param action The action to clean up
         */
        public void remove(BaseAction action) {
            startTimes.remove(System.identityHashCode(action));
        }

        /**
         * Clears all tracked start times.
         */
        public void clear() {
            startTimes.clear();
        }
    }
}
