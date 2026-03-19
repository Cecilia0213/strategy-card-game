package systems;

import abilities.Keyword;
import structures.GameState;
import structures.GameUnit;
import structures.basic.Tile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Handles movement range calculation and summon tile calculation.
 * Uses 1-indexed board coordinates (x: 1-9, y: 1-5).
 */
public final class MovementSystem {

    private static final int[][] CARDINAL  = {{1,0},{-1,0},{0,1},{0,-1}};
    private static final int[][] EIGHT_DIR = {{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};

    private MovementSystem() {}

    /**
     * Valid move tiles.
     * - Standard units: cardinal BFS up to 2 steps.
     * - Flying units: any unoccupied tile.
     * - Units adjacent to enemy Provoke cannot move away.
     */
    public static List<Tile> getValidMoveTiles(GameState state, GameUnit unit) {
        if (unit == null) return new ArrayList<>();
        if (unit.hasKeyword(Keyword.PROVOKE) || unit.hasKeyword(Keyword.FLYING) || unit.hasKeyword(Keyword.RUSH)) {
            // no-op, only keeps enum imports meaningful for readability
        }

        if (hasAdjacentEnemyProvoke(state, unit)) {
            return new ArrayList<>();
        }

        if (unit.hasKeyword(Keyword.FLYING)) {
            return getAllEmptyTiles(state);
        }

        List<Tile> result = new ArrayList<>();
        int startX = unit.getTileX();
        int startY = unit.getTileY();

        Queue<int[]> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();

        queue.add(new int[]{startX, startY, 0});
        visited.add(startX + "," + startY);

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int x = curr[0], y = curr[1], steps = curr[2];

            if (steps > 0 && state.getUnitOnTile(x, y) == null) {
                Tile tile = state.getTile(x, y);
                if (tile != null) result.add(tile);
            }

            if (steps >= 2) continue;

            for (int[] dir : CARDINAL) {
                int nx = x + dir[0];
                int ny = y + dir[1];
                String key = nx + "," + ny;
                if (visited.contains(key)) continue;
                if (nx < 1 || nx > 9 || ny < 1 || ny > 5) continue;
                if (state.getUnitOnTile(nx, ny) != null) {
                    visited.add(key);
                    continue;
                }
                visited.add(key);
                queue.add(new int[]{nx, ny, steps + 1});
            }
        }

        return result;
    }

    /**
     * Returns tiles adjacent (8-directional) to position (x,y) that contain an enemy unit.
     * If the attacker is adjacent to an enemy Provoke unit, only those Provoke units may be targeted.
     */
    public static List<Tile> getAttackableTilesFromPos(GameState state, GameUnit unit, int x, int y) {
        List<Tile> result = new ArrayList<>();
        boolean mustAttackProvoke = hasAdjacentEnemyProvokeAt(state, unit, x, y);

        for (int[] dir : EIGHT_DIR) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            if (nx < 1 || nx > 9 || ny < 1 || ny > 5) continue;
            GameUnit target = state.getUnitOnTile(nx, ny);
            if (target == null || target.getOwner() == unit.getOwner()) continue;
            if (mustAttackProvoke && !target.hasKeyword(Keyword.PROVOKE)) continue;
            Tile tile = state.getTile(nx, ny);
            if (tile != null) result.add(tile);
        }
        return result;
    }

    public static List<Tile> getAttackableTiles(GameState state, GameUnit unit) {
        return getAttackableTilesFromPos(state, unit, unit.getTileX(), unit.getTileY());
    }

    public static List<Tile> getAllAttackableTiles(GameState state, GameUnit unit) {
        Set<String> seen = new HashSet<>();
        List<Tile> result = new ArrayList<>();

        for (Tile t : getAttackableTiles(state, unit)) {
            if (seen.add(t.getTilex() + "," + t.getTiley())) result.add(t);
        }

        if (!unit.hasMoved()) {
            for (Tile moveTile : getValidMoveTiles(state, unit)) {
                for (Tile atkTile : getAttackableTilesFromPos(state, unit, moveTile.getTilex(), moveTile.getTiley())) {
                    if (seen.add(atkTile.getTilex() + "," + atkTile.getTiley())) result.add(atkTile);
                }
            }
        }

        return result;
    }

    public static List<Tile> getValidSummonTiles(GameState state, int playerId) {
        Set<String> seen = new HashSet<>();
        List<Tile> result = new ArrayList<>();

        for (int x = 1; x <= 9; x++) {
            for (int y = 1; y <= 5; y++) {
                GameUnit unit = state.getUnitOnTile(x, y);
                if (unit != null && unit.getOwner() == playerId) {
                    for (int[] dir : EIGHT_DIR) {
                        int nx = x + dir[0];
                        int ny = y + dir[1];
                        if (nx < 1 || nx > 9 || ny < 1 || ny > 5) continue;
                        if (state.getUnitOnTile(nx, ny) != null) continue;
                        if (seen.add(nx + "," + ny)) {
                            Tile tile = state.getTile(nx, ny);
                            if (tile != null) result.add(tile);
                        }
                    }
                }
            }
        }

        return result;
    }

    public static boolean hasAdjacentEnemyProvoke(GameState state, GameUnit unit) {
        return hasAdjacentEnemyProvokeAt(state, unit, unit.getTileX(), unit.getTileY());
    }

    private static boolean hasAdjacentEnemyProvokeAt(GameState state, GameUnit unit, int x, int y) {
        for (int[] dir : EIGHT_DIR) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            if (nx < 1 || nx > 9 || ny < 1 || ny > 5) continue;
            GameUnit adjacent = state.getUnitOnTile(nx, ny);
            if (adjacent == null) continue;
            if (adjacent.getOwner() == unit.getOwner()) continue;
            if (adjacent.hasKeyword(Keyword.PROVOKE)) return true;
        }
        return false;
    }

    private static List<Tile> getAllEmptyTiles(GameState state) {
        List<Tile> result = new ArrayList<>();
        for (int x = 1; x <= 9; x++) {
            for (int y = 1; y <= 5; y++) {
                if (state.getUnitOnTile(x, y) != null) continue;
                Tile tile = state.getTile(x, y);
                if (tile != null) result.add(tile);
            }
        }
        return result;
    }
}
