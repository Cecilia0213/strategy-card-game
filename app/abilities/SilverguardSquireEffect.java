package abilities;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.GameUnit;

/**
 * Sprint 4 - Member A
 * Silverguard Squire：Opening Gambit (whenever a unit is summoned onto the battlefield, trigger the following effect):
 * Give any adjacent allied unit that is directly in-front (left) or behind (right) the owning player's avatar +1 attack and +1 health permanently (this increases those creatures maximum health)
 */
public class SilverguardSquireEffect implements Effect {

    @Override
    public void execute(ActorRef out, GameState state, GameUnit source) {
        if (source == null || source.isDead()) return;


        GameUnit avatar = (source.getOwner() == 1) ? state.getPlayer1Avatar() : state.getPlayer2Avatar();
        if (avatar == null) return;

        int ax = avatar.getTileX();
        int ay = avatar.getTileY();


        int[] checkXCoords = {ax - 1, ax + 1};

        for (int nx : checkXCoords) {
            if (nx >= 1 && nx <= 9) {
                GameUnit target = state.getUnitOnTile(nx, ay);

                if (target != null && target.getOwner() == source.getOwner() && !target.isAvatar()) {
                    target.setAttack(target.getAttack() + 1);
                    target.setMaxHealth(target.getMaxHealth() + 1);
                    target.setHealth(target.getHealth() + 1);

                    BasicCommands.setUnitAttack(out, target.getUnit(), target.getAttack());
                    try { Thread.sleep(50); } catch (InterruptedException e) { e.printStackTrace(); }
                    BasicCommands.setUnitHealth(out, target.getUnit(), target.getHealth());
                    try { Thread.sleep(50); } catch (InterruptedException e) { e.printStackTrace(); }
                }
            }
        }
    }
}