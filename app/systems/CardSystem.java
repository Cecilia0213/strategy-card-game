package systems;

import abilities.Keyword;
import abilities.Status;
import actions.PlayCardAction;
import effects.GameEffect;
import structures.GameState;
import structures.GameUnit;
import structures.Pos;
import structures.basic.BigCard;
import structures.basic.Card;
import structures.basic.Player;
import structures.basic.Unit;
import utils.BasicObjectBuilders;
import utils.StaticConfFiles;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles mana deduction, summoning and spell execution.
 * Sprint 4B: adds passive/special ability support for Provoke / Flying / Rush / Stun / Zeal.
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

        player.setMana(player.getMana() - manaCost);
        hand.remove(handIndex);
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
        if (!isValidSummonTile(state, playerId, card, x, y)) return false;

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
            applyKeywordsForCard(gameUnit, card.getCardname());

            if (!gameUnit.hasKeyword(Keyword.RUSH)) {
                gameUnit.addStatus(Status.SUMMONING_SICKNESS);
                gameUnit.setHasMoved(true);
                gameUnit.setHasAttacked(true);
            }

            state.placeUnit(x, y, gameUnit);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean handleSpellCard(GameState state, int playerId, Card card, Pos targetPos) {
        if (targetPos == null && !"Wraithling Swarm".equals(card.getCardname())) return false;

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

        if ("Beamshock".equals(cardName)) {
            return castBeamshock(state, playerId, targetPos);
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

        state.removeUnit(x, y);

        try {
            Unit wraith = BasicObjectBuilders.loadUnit(
                    StaticConfFiles.wraithling,
                    state.getAndIncrementUnitId(),
                    Unit.class
            );

            GameUnit newUnit = new GameUnit(wraith, playerId, 1, 1, false);
            newUnit.setCardName("Wraithling");
            newUnit.addStatus(Status.SUMMONING_SICKNESS);
            newUnit.setHasMoved(true);
            newUnit.setHasAttacked(true);
            state.placeUnit(x, y, newUnit);

        } catch (Exception e) {
            return false;
        }

        return true;
    }

    private static boolean castWraithlingSwarm(GameState state, int playerId) {
        GameUnit avatar = (playerId == 1) ? state.getPlayer1Avatar() : state.getPlayer2Avatar();
        int ax = avatar.getTileX();
        int ay = avatar.getTileY();
        int summoned = 0;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;

                int x = ax + dx;
                int y = ay + dy;

                if (state.getTile(x, y) == null) continue;
                if (state.getUnitOnTile(x, y) != null) continue;

                try {
                    Unit wraith = BasicObjectBuilders.loadUnit(
                            StaticConfFiles.wraithling,
                            state.getAndIncrementUnitId(),
                            Unit.class
                    );

                    GameUnit newUnit = new GameUnit(wraith, playerId, 1, 1, false);
                    newUnit.setCardName("Wraithling");
                    newUnit.addStatus(Status.SUMMONING_SICKNESS);
                    newUnit.setHasMoved(true);
                    newUnit.setHasAttacked(true);
                    state.placeUnit(x, y, newUnit);

                    summoned++;
                    if (summoned == 3) return true;
                } catch (Exception e) {
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
        if (target.getOwner() != playerId) return false;
        if (target.isAvatar()) return false;

        target.heal(4);
        return true;
    }

    private static boolean castBeamshock(GameState state, int playerId, Pos targetPos) {
        int x = getPosX(targetPos);
        int y = getPosY(targetPos);

        GameUnit target = state.getUnitOnTile(x, y);
        if (target == null) return false;
        if (target.getOwner() == playerId) return false;
        if (target.isAvatar()) return false;

        target.addStatus(Status.STUNNED);
        return true;
    }

    private static boolean isValidSummonTile(GameState state, int playerId, Card card, int x, int y) {
        String cardName = card.getCardname();
        if ("Young Flamewing".equals(cardName) || "Ironcliff Guardian".equals(cardName)) {
            return true;
        }

        for (int sx = 1; sx <= 9; sx++) {
            for (int sy = 1; sy <= 5; sy++) {
                GameUnit unit = state.getUnitOnTile(sx, sy);
                if (unit == null || unit.getOwner() != playerId) continue;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        if (dx == 0 && dy == 0) continue;
                        if (sx + dx == x && sy + dy == y) return true;
                    }
                }
            }
        }
        return false;
    }

    private static void applyKeywordsForCard(GameUnit unit, String cardName) {
        if ("Rock Pulveriser".equals(cardName)
                || "Swamp Entangler".equals(cardName)
                || "Silverguard Knight".equals(cardName)
                || "Ironcliff Guardian".equals(cardName)) {
            unit.addKeyword(Keyword.PROVOKE);
        }

        if ("Young Flamewing".equals(cardName) || "Ironcliff Guardian".equals(cardName)) {
            unit.addKeyword(Keyword.FLYING);
        }

        if ("Saberspine Tiger".equals(cardName)) {
            unit.addKeyword(Keyword.RUSH);
        }

        if ("Silverguard Knight".equals(cardName)) {
            unit.addKeyword(Keyword.ZEAL);
        }
    }

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
