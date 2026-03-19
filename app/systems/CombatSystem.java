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

    public static void executeAttack(ActorRef out, GameState state, GameUnit attacker, GameUnit defender) {
        int defenderHealthBefore = defender.getHealth();
        int attackerHealthBefore = attacker.getHealth();

        int atkDuration = BasicCommands.playUnitAnimation(out, attacker.getUnit(), UnitAnimationType.attack);
        BasicCommands.playUnitAnimation(out, defender.getUnit(), UnitAnimationType.hit);
        try { Thread.sleep(atkDuration + 100); } catch (InterruptedException e) { e.printStackTrace(); }

        int damageToDefender = attacker.getAttack();
        defender.takeDamage(damageToDefender);
        updateHealthUI(out, state, defender, defenderHealthBefore);

        boolean defenderDead = defender.isDead();

        if (!defenderDead && areAdjacent(attacker, defender)) {
            int counterDuration = BasicCommands.playUnitAnimation(out, defender.getUnit(), UnitAnimationType.attack);
            BasicCommands.playUnitAnimation(out, attacker.getUnit(), UnitAnimationType.hit);
            try { Thread.sleep(counterDuration + 100); } catch (InterruptedException e) { e.printStackTrace(); }

            int damageToAttacker = defender.getAttack();
            attacker.takeDamage(damageToAttacker);
            updateHealthUI(out, state, attacker, attackerHealthBefore);
        }

        BasicCommands.playUnitAnimation(out, attacker.getUnit(), UnitAnimationType.idle);
        BasicCommands.playUnitAnimation(out, defender.getUnit(), UnitAnimationType.idle);
        try { Thread.sleep(100); } catch (InterruptedException e) { e.printStackTrace(); }

        if (defender.isDead()) {
            handleDeath(out, state, defender);
        }
        if (attacker.isDead()) {
            handleDeath(out, state, attacker);
        }
    }

    private static boolean areAdjacent(GameUnit a, GameUnit b) {
        return Math.abs(a.getTileX() - b.getTileX()) <= 1
                && Math.abs(a.getTileY() - b.getTileY()) <= 1
                && !(a.getTileX() == b.getTileX() && a.getTileY() == b.getTileY());
    }

    private static void updateHealthUI(ActorRef out, GameState state, GameUnit unit, int previousHealth) {
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

            if (unit.getHealth() < previousHealth) {
                state.handleAvatarDamaged(unit.getOwner());
            }
        }
    }

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
