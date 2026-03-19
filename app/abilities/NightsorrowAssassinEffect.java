package abilities;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.GameUnit;
import structures.basic.UnitAnimationType;

/**
 * Sprint 4 - Member A
 * Nightsorrow Assassin：Opening Gambit
 * Destroy an enemy unit in an adjacent square that is below its maximum heath
 **/

public class NightsorrowAssassinEffect implements Effect {

    @Override
    public void execute(ActorRef out, GameState state, GameUnit source) {
        if (source == null || source.isDead()) return;

        int px = source.getTileX();
        int py = source.getTileY();
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};

        for (int[] dir : dirs) {
            int nx = px + dir[0];
            int ny = py + dir[1];

            if (nx >= 1 && nx <= 9 && ny >= 1 && ny <= 5) {
                GameUnit target = state.getUnitOnTile(nx, ny);

                if (target != null && target.getOwner() != source.getOwner() && !target.isAvatar()) {
                    if (target.getHealth() < target.getMaxHealth()) {

                        BasicCommands.playUnitAnimation(out, target.getUnit(), UnitAnimationType.death);
                        try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }

                        state.removeUnit(nx, ny);
                        BasicCommands.deleteUnit(out, target.getUnit());
                        try { Thread.sleep(100); } catch (InterruptedException e) { e.printStackTrace(); }

                        state.getEffectResolver().fire(TriggerType.ON_UNIT_DIED, out, state, target);
                        state.getEffectResolver().unregister(target); // 清理死者遗物

                        break;
                    }
                }
            }
        }
    }
}
