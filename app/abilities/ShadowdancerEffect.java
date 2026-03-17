package abilities;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.GameUnit;
import structures.basic.UnitAnimationType;

/**
 * Sprint 4 - Member A
 * Shadowdancer ：Deathwatch
 * Deal 1 damage to the enemy avatar and heal yourself for 1
 */
public class ShadowdancerEffect implements Effect {

    @Override
    public void execute(ActorRef out, GameState state, GameUnit source) {
        if (source == null || source.isDead()) return;

        GameUnit friendlyAvatar = (source.getOwner() == 1) ? state.getPlayer1Avatar() : state.getPlayer2Avatar();
        GameUnit enemyAvatar = (source.getOwner() == 1) ? state.getPlayer2Avatar() : state.getPlayer1Avatar();

        if (enemyAvatar != null && !enemyAvatar.isDead()) {
            enemyAvatar.takeDamage(1);
            BasicCommands.playUnitAnimation(out, enemyAvatar.getUnit(), UnitAnimationType.hit);
            try { Thread.sleep(100); } catch (InterruptedException e) { e.printStackTrace(); }
            BasicCommands.playUnitAnimation(out, enemyAvatar.getUnit(), UnitAnimationType.idle);

            BasicCommands.setUnitHealth(out, enemyAvatar.getUnit(), enemyAvatar.getHealth());
            try { Thread.sleep(50); } catch (InterruptedException e) { e.printStackTrace(); }

            if (enemyAvatar.getOwner() == 1) {
                state.getPlayer1().setHealth(enemyAvatar.getHealth());
                BasicCommands.setPlayer1Health(out, state.getPlayer1());
            } else {
                state.getPlayer2().setHealth(enemyAvatar.getHealth());
                BasicCommands.setPlayer2Health(out, state.getPlayer2());
            }

            if (enemyAvatar.isDead()) {
                state.setGameOver(true);
                String msg = (enemyAvatar.getOwner() == 1) ? "You Lose!" : "You Win!";
                BasicCommands.addPlayer1Notification(out, msg, 10);
            }
        }

        if (friendlyAvatar != null && !friendlyAvatar.isDead()) {
            if (friendlyAvatar.getHealth() < friendlyAvatar.getMaxHealth()) {
                friendlyAvatar.setHealth(friendlyAvatar.getHealth() + 1);

                BasicCommands.setUnitHealth(out, friendlyAvatar.getUnit(), friendlyAvatar.getHealth());
                try { Thread.sleep(50); } catch (InterruptedException e) { e.printStackTrace(); }

                if (friendlyAvatar.getOwner() == 1) {
                    state.getPlayer1().setHealth(friendlyAvatar.getHealth());
                    BasicCommands.setPlayer1Health(out, state.getPlayer1());
                } else {
                    state.getPlayer2().setHealth(friendlyAvatar.getHealth());
                    BasicCommands.setPlayer2Health(out, state.getPlayer2());
                }
            }
        }
    }
}