package abilities;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.GameUnit;

/**
 * Sprint 4 - Member A
 * Shadow Watcher：Deathwatch
 * This unit gains +1 attack and +1 health permanently (this increases the creatures maximum health)
 */
public class ShadowWatcherEffect implements Effect {

    @Override
    public void execute(ActorRef out, GameState state, GameUnit source) {
        if (source != null && !source.isDead()) {
            source.setAttack(source.getAttack() + 1);
            source.setMaxHealth(source.getMaxHealth() + 1);
            source.setHealth(source.getHealth() + 1);

            BasicCommands.setUnitAttack(out, source.getUnit(), source.getAttack());
            try { Thread.sleep(50); } catch (InterruptedException e) { e.printStackTrace(); }

            BasicCommands.setUnitHealth(out, source.getUnit(), source.getHealth());
            try { Thread.sleep(50); } catch (InterruptedException e) { e.printStackTrace(); }
        }
    }
}