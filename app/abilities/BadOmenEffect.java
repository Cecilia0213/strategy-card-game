package abilities;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.GameUnit;

/**
 * Sprint 4 - Member A
 * Bad Omen: Deathwatch : Deathwatch (whenever a unit, friendly or enemy dies, trigger the following effect)
 * This unit gains +1 attack permanently
 */
public class BadOmenEffect implements Effect {

    @Override
    public void execute(ActorRef out, GameState state, GameUnit source) {
        // source is the Bad Omen standing on the chessboard
        if (source != null && !source.isDead()) {
            source.setAttack(source.getAttack() + 1);

            BasicCommands.setUnitAttack(out, source.getUnit(), source.getAttack());
            try { Thread.sleep(50); } catch (InterruptedException e) { e.printStackTrace(); }
        }
    }
}