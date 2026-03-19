package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.GameUnit;
import structures.basic.Card;
import structures.basic.Tile;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles hand card selection.
 * If affordable, the clicked card becomes selected and legal target tiles are highlighted.
 */
public class CardClicked implements EventProcessor {

    @Override
    public void processEvent(ActorRef out, GameState gameState, JsonNode message) {

        if (!gameState.gameInitalised) return;
        if (gameState.isGameOver()) return;
        if (gameState.isUnitMoving()) return;

        int handPosition = message.get("position").asInt();
        int handIndex = handPosition - 1;

        List<Card> hand = getCurrentHand(gameState);
        if (handIndex < 0 || handIndex >= hand.size()) return;

        Card card = hand.get(handIndex);
        if (card == null) return;

        int currentMana = getCurrentPlayerMana(gameState);
        if (card.getManacost() > currentMana) {
            return;
        }

        clearHighlights(out, gameState);
        clearPreviousCardHighlight(out, gameState);

        gameState.setSelectedUnit(null);
        gameState.setSelectedCard(card);
        gameState.setSelectedCardHandPosition(handPosition);

        BasicCommands.drawCard(out, card, handPosition, 1);

        if (card.getIsCreature()) {
            List<Tile> summonTiles = findValidSummonTiles(gameState, gameState.getCurrentTurn(), card);
            gameState.setHighlightedTiles(summonTiles);
            for (Tile tile : summonTiles) {
                BasicCommands.drawTile(out, tile, 1);
            }
        } else {
            List<Tile> spellTargets = findValidSpellTargets(gameState, card, gameState.getCurrentTurn());
            gameState.setHighlightedTiles(spellTargets);

            int mode = isEnemyTargetSpell(card) ? 2 : 1;
            for (Tile tile : spellTargets) {
                BasicCommands.drawTile(out, tile, mode);
            }
        }
    }

    private static List<Card> getCurrentHand(GameState gameState) {
        return (gameState.getCurrentTurn() == 1) ? gameState.getPlayer1Hand() : gameState.getPlayer2Hand();
    }

    private static int getCurrentPlayerMana(GameState gameState) {
        return (gameState.getCurrentTurn() == 1)
                ? gameState.getPlayer1().getMana()
                : gameState.getPlayer2().getMana();
    }

    private static void clearPreviousCardHighlight(ActorRef out, GameState gameState) {
        int previousPos = gameState.getSelectedCardHandPosition();
        if (previousPos < 1) return;

        List<Card> hand = getCurrentHand(gameState);
        int previousIndex = previousPos - 1;
        if (previousIndex >= 0 && previousIndex < hand.size()) {
            BasicCommands.drawCard(out, hand.get(previousIndex), previousPos, 0);
        }
    }

    private static void clearHighlights(ActorRef out, GameState gameState) {
        for (Tile tile : gameState.getHighlightedTiles()) {
            BasicCommands.drawTile(out, tile, 0);
        }
        for (Tile tile : gameState.getAttackHighlightedTiles()) {
            BasicCommands.drawTile(out, tile, 0);
        }
        gameState.getHighlightedTiles().clear();
        gameState.getAttackHighlightedTiles().clear();
    }

    private static List<Tile> findValidSummonTiles(GameState gameState, int owner, Card card) {
        String name = card.getCardname();
        if ("Young Flamewing".equals(name) || "Ironcliff Guardian".equals(name)) {
            return findAllEmptyTiles(gameState);
        }

        List<Tile> valid = new ArrayList<>();

        for (int x = 1; x <= 9; x++) {
            for (int y = 1; y <= 5; y++) {
                GameUnit unit = gameState.getUnitOnTile(x, y);
                if (unit == null) continue;
                if (unit.getOwner() != owner) continue;

                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        if (dx == 0 && dy == 0) continue;

                        int nx = x + dx;
                        int ny = y + dy;

                        Tile tile = gameState.getTile(nx, ny);
                        if (tile == null) continue;
                        if (gameState.getUnitOnTile(nx, ny) != null) continue;

                        if (!valid.contains(tile)) {
                            valid.add(tile);
                        }
                    }
                }
            }
        }

        return valid;
    }

    private static List<Tile> findValidSpellTargets(GameState gameState, Card card, int playerId) {
        List<Tile> valid = new ArrayList<>();
        String name = card.getCardname();

        for (int x = 1; x <= 9; x++) {
            for (int y = 1; y <= 5; y++) {
                GameUnit unit = gameState.getUnitOnTile(x, y);
                if (unit == null) continue;
                if (unit.isAvatar()) continue;

                if (("True Strike".equals(name) || "Truestrike".equals(name) || "Beamshock".equals(name))
                        && unit.getOwner() != playerId) {
                    valid.add(gameState.getTile(x, y));
                }

                if ("Sundrop Elixir".equals(name) && unit.getOwner() == playerId) {
                    valid.add(gameState.getTile(x, y));
                }

                if ("Dark Terminus".equals(name) && unit.getOwner() != playerId) {
                    valid.add(gameState.getTile(x, y));
                }
            }
        }

        return valid;
    }

    private static List<Tile> findAllEmptyTiles(GameState gameState) {
        List<Tile> result = new ArrayList<>();
        for (int x = 1; x <= 9; x++) {
            for (int y = 1; y <= 5; y++) {
                if (gameState.getUnitOnTile(x, y) != null) continue;
                Tile tile = gameState.getTile(x, y);
                if (tile != null) result.add(tile);
            }
        }
        return result;
    }

    private static boolean isEnemyTargetSpell(Card card) {
        String name = card.getCardname();
        return "True Strike".equals(name)
                || "Truestrike".equals(name)
                || "Beamshock".equals(name)
                || "Dark Terminus".equals(name);
    }
}
