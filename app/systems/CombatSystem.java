package systems;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.GameUnit;
import structures.basic.UnitAnimationType;

/**
 * Handles combat between two units: attack, counter-attack, death, and game-over detection.
 */
public final class CombatSystem {

    private CombatSystem() {}

    /**
     * Executes an attack from attacker → defender, then counter-attack if defender survives.
     * Handles death animations, board removal, and avatar death → game over.
     */
    public static void executeAttack(ActorRef out, GameState state, GameUnit attacker, GameUnit defender) {
        // Attacker attacks, defender takes hit
        int atkDuration = BasicCommands.playUnitAnimation(out, attacker.getUnit(), UnitAnimationType.attack);
        BasicCommands.playUnitAnimation(out, defender.getUnit(), UnitAnimationType.hit);

        try { Thread.sleep(atkDuration + 100); } catch (InterruptedException e) { e.printStackTrace(); }

        int counterDuration = BasicCommands.playUnitAnimation(out, defender.getUnit(), UnitAnimationType.attack);
        BasicCommands.playUnitAnimation(out, attacker.getUnit(), UnitAnimationType.hit);
        try { Thread.sleep(counterDuration + 100); } catch (InterruptedException e) { e.printStackTrace(); }

        int damageToDefender = attacker.getAttack();
        int damageToAttacker = defender.getAttack();
        defender.takeDamage(damageToDefender);
        attacker.takeDamage(damageToAttacker);
        updateHealthUI(out, state, defender);
        updateHealthUI(out, state, attacker);


        // Return both to idle
        BasicCommands.playUnitAnimation(out, attacker.getUnit(), UnitAnimationType.idle);
        BasicCommands.playUnitAnimation(out, defender.getUnit(), UnitAnimationType.idle);
        try { Thread.sleep(100); } catch (InterruptedException e) { e.printStackTrace(); }

        boolean defenderDead = defender.isDead();
        boolean attackerDead = attacker.isDead();

        if (defenderDead) {
            handleDeath(out, state, defender);
        }
        if (attackerDead) {
            handleDeath(out, state, attacker);
        }
    }

    /** Updates the health display for a unit, and also player health if the unit is an avatar. */
    private static void updateHealthUI(ActorRef out, GameState state, GameUnit unit) {
        BasicCommands.setUnitHealth(out, unit.getUnit(), unit.getHealth());
        try { Thread.sleep(50); } catch (InterruptedException e) { e.printStackTrace(); }

        if (unit.isAvatar()) {
            if (unit.getOwner() == 1) {
                state.getPlayer1().setHealth(unit.getHealth());
                BasicCommands.setPlayer1Health(out, state.getPlayer1());
            } else {
                state.getPlayer2().setHealth(unit.getHealth());
                BasicCommands.setPlayer2Health(out, state.getPlayer2());
            }
            try { Thread.sleep(50); } catch (InterruptedException e) { e.printStackTrace(); }
        }
    }

    /** Plays death animation, removes unit from board and UI, checks for game over. */
    private static void handleDeath(ActorRef out, GameState state, GameUnit unit) {
        int deathDuration = BasicCommands.playUnitAnimation(out, unit.getUnit(), UnitAnimationType.death);
        try { Thread.sleep(deathDuration + 100); } catch (InterruptedException e) { e.printStackTrace(); }

        state.removeUnit(unit.getTileX(), unit.getTileY());
        BasicCommands.deleteUnit(out, unit.getUnit());
        try { Thread.sleep(100); } catch (InterruptedException e) { e.printStackTrace(); }

        if (unit.isAvatar()) {
            state.setGameOver(true);
            String msg = (unit.getOwner() == 1) ? "You Lose!" : "You Win!";
            BasicCommands.addPlayer1Notification(out, msg, 10);
        }
    }
}
