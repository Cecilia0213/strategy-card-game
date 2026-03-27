package systems;

import abilities.TriggerType;
import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.GameUnit;
import structures.basic.UnitAnimationType;

/**
 * Handles combat between two units: attack, counter-attack, death, and
 * game-over detection.
 * Horn of the Forsaken is NOT hardcoded here; it should be triggered through
 * EffectResolver via ON_DAMAGE_DEALT.
 */
public final class CombatSystem {

    private CombatSystem() {
    }

    public static void executeAttack(ActorRef out, GameState state, GameUnit attacker, GameUnit defender) {
        if (attacker == null || defender == null)
            return;
        if (attacker.isDead() || defender.isDead())
            return;
        if (state == null || state.isGameOver())
            return;

        int defenderHealthBefore = defender.getHealth();
        int attackerHealthBefore = attacker.getHealth();

        // attacker animation
        int atkDuration = BasicCommands.playUnitAnimation(out, attacker.getUnit(), UnitAnimationType.attack);
        BasicCommands.playUnitAnimation(out, defender.getUnit(), UnitAnimationType.hit);
        try {
            Thread.sleep(atkDuration + 100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // defender animation
        int counterDuration = BasicCommands.playUnitAnimation(out, defender.getUnit(), UnitAnimationType.attack);
        BasicCommands.playUnitAnimation(out, attacker.getUnit(), UnitAnimationType.hit);
        try {
            Thread.sleep(counterDuration + 100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // simultaneous damage
        int damageToDefender = attacker.getAttack();
        defender.takeDamage(damageToDefender);
        updateHealthUI(out, state, defender, defenderHealthBefore);
        state.getEffectResolver().fire(TriggerType.ON_DAMAGE_DEALT, out, state, attacker);

        int damageToAttacker = defender.getAttack();
        attacker.takeDamage(damageToAttacker);
        updateHealthUI(out, state, attacker, attackerHealthBefore);

        // Return both to idle if they still exist visually
        BasicCommands.playUnitAnimation(out, attacker.getUnit(), UnitAnimationType.idle);
        BasicCommands.playUnitAnimation(out, defender.getUnit(), UnitAnimationType.idle);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Death resolution
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
        if (unit == null)
            return;

        if (!unit.isAvatar()) {
            BasicCommands.setUnitHealth(out, unit.getUnit(), Math.max(0, unit.getHealth()));
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return;
        }

        // Avatar: sync both unit health UI and player health UI
        BasicCommands.setUnitHealth(out, unit.getUnit(), Math.max(0, unit.getHealth()));
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        int clampedHealth = Math.max(0, unit.getHealth());

        if (unit.getOwner() == 1) {
            state.getPlayer1().setHealth(clampedHealth);
            BasicCommands.setPlayer1Health(out, state.getPlayer1());
        } else {
            state.getPlayer2().setHealth(clampedHealth);
            BasicCommands.setPlayer2Health(out, state.getPlayer2());
        }

        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Zeal / artifact robustness handling
        if (unit.getHealth() < previousHealth) {
            state.handleAvatarDamaged(unit.getOwner());
        }
    }

    private static void handleDeath(ActorRef out, GameState state, GameUnit unit) {
        if (unit == null)
            return;

        int deathDuration = BasicCommands.playUnitAnimation(out, unit.getUnit(), UnitAnimationType.death);
        try {
            Thread.sleep(deathDuration + 100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        state.removeUnit(unit.getTileX(), unit.getTileY());
        BasicCommands.deleteUnit(out, unit.getUnit());
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (unit.isAvatar()) {
            state.setGameOver(true);
            String msg = (unit.getOwner() == 1) ? "You Lose!" : "You Win!";
            BasicCommands.addPlayer1Notification(out, msg, 9999);

            // Clean up any triggers owned by the dead avatar
            state.getEffectResolver().unregister(unit);
            return;
        }

        // Notify all cards registered with ON_UNIT_DIED
        state.getEffectResolver().fire(TriggerType.ON_UNIT_DIED, out, state, unit);

        // Unregister dead unit to prevent stale triggers
        state.getEffectResolver().unregister(unit);
    }
}