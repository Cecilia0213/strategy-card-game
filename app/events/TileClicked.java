package events;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import actions.PlayCardAction;
import commands.BasicCommands;
import structures.GameState;
import structures.GameUnit;
import structures.basic.Card;
import structures.Pos;
import structures.basic.Tile;
import systems.GameEngine;
import systems.CombatSystem;

public class TileClicked implements EventProcessor {

    private static final int NORMAL = 0;
    private static final int MOVE = 1;
    private static final int ATTACK = 2;

    @Override
    public void processEvent(ActorRef out, GameState gameState, JsonNode message) {

        if (!gameState.gameInitalised) return;
        if (gameState.isGameOver()) return;
        if (gameState.isUnitMoving()) return;

        int x = message.get("tilex").asInt();
        int y = message.get("tiley").asInt();

        Tile clickedTile = gameState.getTile(x, y);
        if (clickedTile == null) return;

        Card selectedCard = gameState.getSelectedCard();
        GameUnit selectedUnit = gameState.getSelectedUnit();
        GameUnit clickedUnit = gameState.getUnitOnTile(x, y);

        // =====================================
        // CARD PLAY (Sprint3)
        // =====================================
        if (selectedCard != null) {
            handleCardPlay(out, gameState, selectedCard, x, y);
            return;
        }

        // =====================================
        // UNIT ACTION
        // =====================================
        if (selectedUnit != null) {

            // ATTACK
            if (clickedUnit != null
                    && clickedUnit.getOwner() != selectedUnit.getOwner()
                    && isAttackHighlighted(gameState, clickedTile)
                    && !selectedUnit.hasAttacked()) {

                CombatSystem.executeAttack(out, gameState, selectedUnit, clickedUnit);
                selectedUnit.setHasAttacked(true);
                selectedUnit.setHasMoved(true);


                clearHighlights(out, gameState);
                gameState.setSelectedUnit(null);
                return;
            }

            // MOVE
            if (clickedUnit == null
                    && isMoveHighlighted(gameState, clickedTile)
                    && !selectedUnit.hasMoved()) {

                gameState.setUnitMoving(true);

                BasicCommands.moveUnitToTile(out,
                        selectedUnit.getUnit(),
                        clickedTile);

                gameState.moveUnit(selectedUnit, x, y);
                selectedUnit.setHasMoved(true);

                clearHighlights(out, gameState);
                gameState.setSelectedUnit(null);
                return;
            }

            // RESELECT FRIENDLY
            if (clickedUnit != null
                    && clickedUnit.getOwner() == gameState.getCurrentTurn()) {

                clearHighlights(out, gameState);
                gameState.setSelectedUnit(clickedUnit);
                highlightActions(out, gameState, clickedUnit);
                return;
            }

            clearHighlights(out, gameState);
            gameState.setSelectedUnit(null);
            return;
        }

        // =====================================
        // SELECT UNIT
        // =====================================
        if (clickedUnit != null
                && clickedUnit.getOwner() == 1
                && gameState.getCurrentTurn() == 1) {

            clearHighlights(out, gameState);
            gameState.setSelectedUnit(clickedUnit);
            highlightActions(out, gameState, clickedUnit);
            return;
        }

        clearHighlights(out, gameState);
        gameState.clearSelection();
    }

    // =====================================
    // CARD PLAY
    // =====================================

    private void handleCardPlay(ActorRef out,
                                GameState gameState,
                                Card card,
                                int x,
                                int y) {

        int player = gameState.getCurrentTurn();
        int handPos = gameState.getSelectedCardHandPosition() - 1;
        String cardName = card.getCardname();
        GameUnit targetBefore = gameState.getUnitOnTile(x, y);
        Set<String> swarmEmptyTilesBefore = captureAdjacentEmptyTiles(gameState, player, cardName);

        PlayCardAction action = new PlayCardAction(player, handPos, new Pos(x, y));

        GameEngine.apply(gameState, action);

        GameUnit unit = gameState.getUnitOnTile(x, y);

        if (card.getIsCreature() && unit != null) {

            Tile tile = gameState.getTile(x, y);
            drawUnitWithStats(out, tile, unit);
        } else {
            refreshSpellUI(out, gameState, cardName, x, y, targetBefore, swarmEmptyTilesBefore);
        }

        refreshMana(out, gameState);
        refreshHand(out, gameState);

        gameState.setSelectedCard(null);
        gameState.setSelectedCardHandPosition(-1);

        clearHighlights(out, gameState);
    }

    private void refreshSpellUI(ActorRef out,
                                GameState gameState,
                                String cardName,
                                int x,
                                int y,
                                GameUnit targetBefore,
                                Set<String> swarmEmptyTilesBefore) {

        GameUnit targetAfter = gameState.getUnitOnTile(x, y);

        if ("True Strike".equals(cardName) || "Truestrike".equals(cardName)) {
            if (targetAfter == null) {
                if (targetBefore != null) {
                    BasicCommands.deleteUnit(out, targetBefore.getUnit());
                }
            } else {
                BasicCommands.setUnitHealth(out, targetAfter.getUnit(), targetAfter.getHealth());
            }
            return;
        }

        if ("Sundrop Elixir".equals(cardName)) {
            if (targetAfter != null) {
                BasicCommands.setUnitHealth(out, targetAfter.getUnit(), targetAfter.getHealth());
            }
            return;
        }

        if ("Dark Terminus".equals(cardName)) {
            if (targetBefore != null) {
                BasicCommands.deleteUnit(out, targetBefore.getUnit());
            }

            if (targetAfter != null) {
                drawUnitWithStats(out, gameState.getTile(x, y), targetAfter);
            }
            return;
        }

        if ("Wraithling Swarm".equals(cardName)) {
            drawNewAdjacentSummons(out, gameState, playerAvatar(gameState, gameState.getCurrentTurn()), swarmEmptyTilesBefore);
        }
    }

    private Set<String> captureAdjacentEmptyTiles(GameState gameState, int playerId, String cardName) {
        Set<String> emptyTiles = new HashSet<>();

        if (!"Wraithling Swarm".equals(cardName)) {
            return emptyTiles;
        }

        GameUnit avatar = playerAvatar(gameState, playerId);
        if (avatar == null) return emptyTiles;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;

                int nx = avatar.getTileX() + dx;
                int ny = avatar.getTileY() + dy;

                if (gameState.getTile(nx, ny) == null) continue;
                if (gameState.getUnitOnTile(nx, ny) != null) continue;

                emptyTiles.add(tileKey(nx, ny));
            }
        }

        return emptyTiles;
    }

    private void drawNewAdjacentSummons(ActorRef out,
                                        GameState gameState,
                                        GameUnit avatar,
                                        Set<String> emptyTilesBefore) {
        if (avatar == null) return;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;

                int nx = avatar.getTileX() + dx;
                int ny = avatar.getTileY() + dy;

                if (!emptyTilesBefore.contains(tileKey(nx, ny))) continue;

                GameUnit summonedUnit = gameState.getUnitOnTile(nx, ny);
                Tile tile = gameState.getTile(nx, ny);

                if (summonedUnit != null && tile != null) {
                    drawUnitWithStats(out, tile, summonedUnit);
                }
            }
        }
    }

    private GameUnit playerAvatar(GameState gameState, int playerId) {
        return (playerId == 1) ? gameState.getPlayer1Avatar() : gameState.getPlayer2Avatar();
    }

    private String tileKey(int x, int y) {
        return x + ":" + y;
    }

    private void drawUnitWithStats(ActorRef out, Tile tile, GameUnit unit) {
        BasicCommands.drawUnit(out, unit.getUnit(), tile);
        try { Thread.sleep(30); } catch (InterruptedException e) { e.printStackTrace(); }
        BasicCommands.setUnitAttack(out, unit.getUnit(), unit.getAttack());
        try { Thread.sleep(20); } catch (InterruptedException e) { e.printStackTrace(); }
        BasicCommands.setUnitHealth(out, unit.getUnit(), unit.getHealth());
        try { Thread.sleep(20); } catch (InterruptedException e) { e.printStackTrace(); }
    }

    // =====================================
    // HIGHLIGHT
    // =====================================

    private void highlightActions(ActorRef out,
                                  GameState gameState,
                                  GameUnit unit) {

        List<Tile> moves = computeMoves(gameState, unit);
        List<Tile> attacks = computeAttacks(gameState, unit);

        for (Tile t : moves)
            BasicCommands.drawTile(out, t, MOVE);

        for (Tile t : attacks)
            BasicCommands.drawTile(out, t, ATTACK);

        gameState.setHighlightedTiles(moves);
        gameState.setAttackHighlightedTiles(attacks);
    }

    private boolean isMoveHighlighted(GameState gameState, Tile tile) {
        for (Tile t : gameState.getHighlightedTiles())
            if (t.getTilex()==tile.getTilex() && t.getTiley()==tile.getTiley())
                return true;
        return false;
    }

    private boolean isAttackHighlighted(GameState gameState, Tile tile) {
        for (Tile t : gameState.getAttackHighlightedTiles())
            if (t.getTilex()==tile.getTilex() && t.getTiley()==tile.getTiley())
                return true;
        return false;
    }

    // =====================================
    // MOVEMENT
    // =====================================

    private List<Tile> computeMoves(GameState gameState, GameUnit unit) {

        List<Tile> list = new ArrayList<>();

        int ux = unit.getTileX();
        int uy = unit.getTileY();

        int[][] dirs = {
                {1,0},{-1,0},{0,1},{0,-1},
                {1,1},{1,-1},{-1,1},{-1,-1}
        };

        for (int[] d: dirs) {

            int nx = ux + d[0];
            int ny = uy + d[1];

            Tile t = gameState.getTile(nx,ny);
            if (t==null) continue;
            if (gameState.getUnitOnTile(nx,ny)!=null) continue;

            list.add(t);
        }

        return list;
    }

    // =====================================
    // ATTACK
    // =====================================

    private List<Tile> computeAttacks(GameState gameState, GameUnit unit){

        List<Tile> list = new ArrayList<>();

        int ux=unit.getTileX();
        int uy=unit.getTileY();

        for(int dx=-1;dx<=1;dx++)
        for(int dy=-1;dy<=1;dy++){

            if(dx==0 && dy==0) continue;

            int nx=ux+dx;
            int ny=uy+dy;

            Tile t=gameState.getTile(nx,ny);
            if(t==null) continue;

            GameUnit target=gameState.getUnitOnTile(nx,ny);

            if(target!=null && target.getOwner()!=unit.getOwner())
                list.add(t);
        }

        return list;
    }

    // =====================================
    // UI
    // =====================================

    private void refreshMana(ActorRef out, GameState state){
        BasicCommands.setPlayer1Mana(out,state.getPlayer1());
        BasicCommands.setPlayer2Mana(out,state.getPlayer2());
    }

    private void refreshHand(ActorRef out, GameState state){

        List<Card> hand =
                state.getCurrentTurn()==1
                        ? state.getPlayer1Hand()
                        : state.getPlayer2Hand();

        for(int i=1;i<=6;i++)
            BasicCommands.deleteCard(out,i);

        for(int i=0;i<hand.size() && i<6;i++)
            BasicCommands.drawCard(out,hand.get(i),i+1,0);
    }

    private void clearHighlights(ActorRef out, GameState state){

        List<Tile> all=new ArrayList<>();
        all.addAll(state.getHighlightedTiles());
        all.addAll(state.getAttackHighlightedTiles());

        for(Tile t:all)
            BasicCommands.drawTile(out,t,NORMAL);

        state.getHighlightedTiles().clear();
        state.getAttackHighlightedTiles().clear();
    }
}
