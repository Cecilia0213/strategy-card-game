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
 * Gloom Chaser：Opening Gambit (whenever a unit is summoned onto the battlefield, trigger the following effect)
 * Summon a Wraithling directly behind this unit (to its left for the human player)
 */
public class GloomChaserEffect implements Effect {

    @Override
    public void execute(ActorRef out, GameState state, GameUnit source) {
        if (source == null || source.isDead()) return;

        int spawnX = (source.getOwner() == 1) ? source.getTileX() - 1 : source.getTileX() + 1;
        int spawnY = source.getTileY();

        if (spawnX >= 1 && spawnX <= 9 && spawnY >= 1 && spawnY <= 5) {
            if (state.getUnitOnTile(spawnX, spawnY) == null) {
                Tile spawnTile = state.getTile(spawnX, spawnY);
                try {
                    int wraithId = state.getAndIncrementUnitId();
                    structures.basic.Unit wraithTemplate = BasicObjectBuilders.loadUnit(
                            StaticConfFiles.wraithling, wraithId, structures.basic.Unit.class);
                    wraithTemplate.setPositionByTile(spawnTile);

                    GameUnit wraithling = new GameUnit(wraithTemplate, source.getOwner(), 1, 1, false);
                    wraithling.setCardName("Wraithling");

                    wraithling.setHasMoved(true);
                    wraithling.setHasAttacked(true);

                    state.placeUnit(spawnX, spawnY, wraithling);

                    BasicCommands.drawUnit(out, wraithTemplate, spawnTile);
                    try { Thread.sleep(200); } catch (InterruptedException e) { e.printStackTrace(); }
                    BasicCommands.setUnitAttack(out, wraithTemplate, 1);
                    try { Thread.sleep(50); } catch (InterruptedException e) { e.printStackTrace(); }
                    BasicCommands.setUnitHealth(out, wraithTemplate, 1);
                    try { Thread.sleep(50); } catch (InterruptedException e) { e.printStackTrace(); }
                } catch (Exception e) { e.printStackTrace(); }
            }
        }
    }
}