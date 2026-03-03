/**
 * Navigation goal composition system for intelligent pathfinding objectives.
 *
 * <p>This package provides a flexible system for defining navigation goals
 * that can be combined using AND/OR logic. Inspired by RTS game AI
 * techniques for objective-based unit behavior.</p>
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link com.minewright.goal.NavigationGoal} - Interface for pathfinding objectives</li>
 *   <li>{@link com.minewright.goal.CompositeNavigationGoal} - Combines goals with ANY/ALL logic</li>
 *   <li>{@link com.minewright.goal.GetToBlockGoal} - Navigate to nearest block of type</li>
 *   <li>{@link com.minewright.goal.GetToEntityGoal} - Track and reach moving entities</li>
 *   <li>{@link com.minewright.goal.RunAwayGoal} - Escape from danger/threats</li>
 *   <li>{@link com.minewright.goal.Goals} - Factory for common goal patterns</li>
 * </ul>
 *
 * <h2>Design Philosophy</h2>
 * <p>Goals can be combined to create complex navigation behaviors:</p>
 * <ul>
 *   <li><b>ANY</b> - Succeeds when ANY child goal succeeds (e.g., "find water OR lava")</li>
 *   <li><b>ALL</b> - Succeeds when ALL children succeed (e.g., "reach chest AND return home")</li>
 * </ul>
 *
 * <p>This enables sophisticated behaviors like:</p>
 * <pre>
 * Goals.AND(
 *     Goals.findBlock("oak_log"),
 *     Goals.findBlock("stone"),
 *     Goals.NOT(Goals.findBlock("lava"))
 * )
 * </pre>
 *
 * <h2>Integration with Pathfinding</h2>
 * <p>Goals integrate with the A* pathfinder to provide
 * dynamic, multi-objective navigation. The pathfinder evaluates
 * goals during search to find paths that satisfy objectives.</p>
 *
 * @since 1.7.0
 * @see com.minewright.goal.NavigationGoal
 * @see com.minewright.goal.CompositeNavigationGoal
 */
package com.minewright.goal;
