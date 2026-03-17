package systems;

import actions.PlayCardAction;
import utils.StaticConfFiles;
import effects.GameEffect;
import structures.GameState;
import structures.GameUnit;
import structures.basic.BigCard;
import structures.basic.Card;
import structures.basic.Player;
import structures.Pos;
import structures.basic.Unit;
import utils.BasicObjectBuilders;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles mana deduction, summoning and spell execution.
 * Sprint 3: first implement creature summon + basic targeted spell logic.
 */
public final class CardSystem {

    private CardSystem() {}

    public static List<GameEffect> playCard(GameState state, PlayCardAction action) {
        List<GameEffect> effects = new ArrayList<>();

        int playerId = action.getPlayerId();
        int handIndex = action.getHandIndex();
        Pos targetPos = action.getTargetPos();

        List<Card> hand = getHand(state, playerId);
        Player player = getPlayer(state, playerId);

        if (hand == null || player == null) return effects;
        if (handIndex < 0 || handIndex >= hand.size()) return effects;

        Card card = hand.get(handIndex);
        if (card == null) return effects;

        int manaCost = card.getManacost();
        if (player.getMana() < manaCost) return effects;

        boolean success;
        if (card.getIsCreature()) {
            success = handleCreatureCard(state, playerId, card, targetPos);
        } else {
            success = handleSpellCard(state, playerId, card, targetPos);
        }

        if (!success) return effects;

        // deduct mana only after successful play
        player.setMana(player.getMana() - manaCost);

        // remove card from hand
        hand.remove(handIndex);

        // clear selection only
        state.clearSelection();

        return effects;
    }

    private static List<Card> getHand(GameState state, int playerId) {
        return (playerId == 1) ? state.getPlayer1Hand() : state.getPlayer2Hand();
    }

    private static Player getPlayer(GameState state, int playerId) {
        return (playerId == 1) ? state.getPlayer1() : state.getPlayer2();
    }

    private static boolean handleCreatureCard(GameState state, int playerId, Card card, Pos targetPos) {
        if (targetPos == null) return false;

        int x = getPosX(targetPos);
        int y = getPosY(targetPos);

        if (state.getTile(x, y) == null) return false;
        if (state.getUnitOnTile(x, y) != null) return false;

        try {
            Unit basicUnit = BasicObjectBuilders.loadUnit(
                    card.getUnitConfig(),
                    state.getAndIncrementUnitId(),
                    Unit.class
            );

            BigCard bigCard = card.getBigCard();
            int attack = bigCard.getAttack();
            int health = bigCard.getHealth();

            GameUnit gameUnit = new GameUnit(basicUnit, playerId, attack, health, false);
            gameUnit.setCardName(card.getCardname());

            if ("Saberspine Tiger".equals(card.getCardname())) {
                gameUnit.setHasMoved(false);
                gameUnit.setHasAttacked(false);
            } else {
                gameUnit.setHasMoved(true);
                gameUnit.setHasAttacked(true);
            }

            state.placeUnit(x, y, gameUnit);
            // Sprint 4: Connect register() - Register trigger-type effect
            String cardName = card.getCardname();
            if ("Bad Omen".equals(cardName)) {
                state.getEffectResolver().register(abilities.TriggerType.ON_UNIT_DIED, new abilities.BadOmenEffect(), gameUnit);
            } else if ("Shadow Watcher".equals(cardName)) {
                state.getEffectResolver().register(abilities.TriggerType.ON_UNIT_DIED, new abilities.ShadowWatcherEffect(), gameUnit);
            }else if ("Bloodmoon Priestess".equals(cardName)) {
                state.getEffectResolver().register(abilities.TriggerType.ON_UNIT_DIED, new abilities.BloodmoonPriestessEffect(), gameUnit);
            } else if ("Shadowdancer".equals(cardName)) {
                state.getEffectResolver().register(abilities.TriggerType.ON_UNIT_DIED, new abilities.ShadowdancerEffect(), gameUnit);
            }
            else if ("Gloom Chaser".equals(cardName)) {
                state.getEffectResolver().register(abilities.TriggerType.ON_SUMMON, new abilities.GloomChaserEffect(), gameUnit);
            } else if ("Nightsorrow Assassin".equals(cardName)) {
                state.getEffectResolver().register(abilities.TriggerType.ON_SUMMON, new abilities.NightsorrowAssassinEffect(), gameUnit);
            } else if ("Silverguard Squire".equals(cardName)) {
                state.getEffectResolver().register(abilities.TriggerType.ON_SUMMON, new abilities.SilverguardSquireEffect(), gameUnit);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean handleSpellCard(GameState state, int playerId, Card card, Pos targetPos) {
        if (targetPos == null) return false;

        String cardName = card.getCardname();

        if ("Dark Terminus".equals(cardName)) {
            return castDarkTerminus(state, playerId, targetPos);
        }

        if ("Wraithling Swarm".equals(cardName)) {
            return castWraithlingSwarm(state, playerId);
        }

        if ("True Strike".equals(cardName) || "Truestrike".equals(cardName)) {
            return castTrueStrike(state, playerId, targetPos);
        }

        if ("Sundrop Elixir".equals(cardName)) {
            return castSundropElixir(state, playerId, targetPos);
        }

        return false;
    }

    private static boolean castDarkTerminus(GameState state, int playerId, Pos pos) {
        int x = pos.x;
        int y = pos.y;

        GameUnit target = state.getUnitOnTile(x, y);

        if (target == null) return false;
        if (target.getOwner() == playerId) return false;
        if (target.isAvatar()) return false;

        // remove enemy unit
        state.removeUnit(x, y);

        try {

            Unit wraith = BasicObjectBuilders.loadUnit(
                    StaticConfFiles.wraithling,
                    state.getAndIncrementUnitId(),
                    Unit.class
            );

            GameUnit newUnit = new GameUnit(wraith, playerId, 1, 1, false);
            // Prevent the minions summoned by spells from attacking immediately
            newUnit.setHasMoved(true);
            newUnit.setHasAttacked(true);

            state.placeUnit(x, y, newUnit);

        } catch (Exception e) {
            return false;
    }

        return true;
    }

    private static boolean castWraithlingSwarm(GameState state, int playerId) {

        GameUnit avatar =
            (playerId == 1) ? state.getPlayer1Avatar() : state.getPlayer2Avatar();

        int ax = avatar.getTileX();
        int ay = avatar.getTileY();

        int summoned = 0;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {

                if (dx == 0 && dy == 0) continue;

                int x = ax + dx;
                int y = ay + dy;

                if (state.getTile(x,y) == null) continue;
                if (state.getUnitOnTile(x,y) != null) continue;

                try {

                    Unit wraith = BasicObjectBuilders.loadUnit(
                            StaticConfFiles.wraithling,
                            state.getAndIncrementUnitId(),
                            Unit.class
                    );

                    GameUnit newUnit = new GameUnit(wraith, playerId, 1, 1, false);

                    // Prevent the summoned minions by magic from attacking immediately
                    newUnit.setHasMoved(true);
                    newUnit.setHasAttacked(true);

                    state.placeUnit(x,y,newUnit);

                    summoned++;

                    if (summoned == 3) return true;

                } catch(Exception e){
                    return false;
                }
            }
        }

        return summoned > 0;
    }

    private static boolean castTrueStrike(GameState state, int playerId, Pos targetPos) {
        int x = getPosX(targetPos);
        int y = getPosY(targetPos);

        GameUnit target = state.getUnitOnTile(x, y);
        if (target == null) return false;

        // enemy non-avatar unit only
        if (target.getOwner() == playerId) return false;
        if (target.isAvatar()) return false;

        target.takeDamage(2);

        if (target.isDead()) {
            state.removeUnit(target.getTileX(), target.getTileY());
        }

        return true;
    }

    private static boolean castSundropElixir(GameState state, int playerId, Pos targetPos) {
        int x = getPosX(targetPos);
        int y = getPosY(targetPos);

        GameUnit target = state.getUnitOnTile(x, y);
        if (target == null) return false;

        // allied non-avatar unit only
        if (target.getOwner() != playerId) return false;
        if (target.isAvatar()) return false;

        int healedHealth = Math.min(target.getMaxHealth(), target.getHealth() + 4);
        target.setHealth(healedHealth);

        return true;
    }

    /**
     * Make Pos access resilient in case your Pos class exposes x/y either as fields
     * or via getters.
     */
    private static int getPosX(Pos pos) {
        try {
            Field f = pos.getClass().getField("x");
            return f.getInt(pos);
        } catch (Exception ignored) { }

        try {
            Method m = pos.getClass().getMethod("getX");
            return (Integer) m.invoke(pos);
        } catch (Exception ignored) { }

        throw new IllegalStateException("Cannot read x from Pos");
    }

    private static int getPosY(Pos pos) {
        try {
            Field f = pos.getClass().getField("y");
            return f.getInt(pos);
        } catch (Exception ignored) { }

        try {
            Method m = pos.getClass().getMethod("getY");
            return (Integer) m.invoke(pos);
        } catch (Exception ignored) { }

        throw new IllegalStateException("Cannot read y from Pos");
    }
}