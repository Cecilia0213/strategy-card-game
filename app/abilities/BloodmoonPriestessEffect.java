package abilities;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.GameUnit;
import structures.basic.Tile;
import utils.BasicObjectBuilders;
import utils.StaticConfFiles;

/**
 * Sprint 4 - Member A
 * Bloodmoon Priestess：Deathwatch
 * Summon a Wraithling on a randomly selected unoccupied adjacent tile
 */
public class BloodmoonPriestessEffect implements Effect {

    @Override
    public void execute(ActorRef out, GameState state, GameUnit source) {
        if (source == null || source.isDead()) return;

        int px = source.getTileX();
        int py = source.getTileY();
        Tile spawnTile = null;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                int nx = px + dx;
                int ny = py + dy;

                if (nx >= 1 && nx <= 9 && ny >= 1 && ny <= 5) {
                    if (state.getUnitOnTile(nx, ny) == null) {
                        spawnTile = state.getTile(nx, ny);
                        break;
                    }
                }
            }
            if (spawnTile != null) break;
        }

        if (spawnTile != null) {
            try {
                int wraithId = state.getAndIncrementUnitId();
                structures.basic.Unit wraithlingTemplate = BasicObjectBuilders.loadUnit(
                        StaticConfFiles.wraithling, wraithId, structures.basic.Unit.class);
                wraithlingTemplate.setPositionByTile(spawnTile);

                GameUnit wraithling = new GameUnit(wraithlingTemplate, source.getOwner(), 1, 1, false);
                wraithling.setCardName("Wraithling");
                wraithling.setHasMoved(true);
                wraithling.setHasAttacked(true);

                state.placeUnit(spawnTile.getTilex(), spawnTile.getTiley(), wraithling);

                BasicCommands.drawUnit(out, wraithlingTemplate, spawnTile);
                try { Thread.sleep(200); } catch (InterruptedException e) { e.printStackTrace(); }
                BasicCommands.setUnitAttack(out, wraithlingTemplate, 1);
                try { Thread.sleep(50); } catch (InterruptedException e) { e.printStackTrace(); }
                BasicCommands.setUnitHealth(out, wraithlingTemplate, 1);
                try { Thread.sleep(50); } catch (InterruptedException e) { e.printStackTrace(); }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
