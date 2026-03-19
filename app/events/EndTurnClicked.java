package events;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import structures.GameState;
import structures.basic.Tile;
import systems.CombatSystem;
import systems.GameEngine;
import commands.BasicCommands;
import actions.PlayCardAction;
import structures.GameUnit;
import structures.Pos;
import structures.basic.Card;

/**
 * Indicates that the user has clicked an object on the game canvas, in this case
 * the end-turn button.
 * 
 * { 
 *   messageType = “endTurnClicked”
 * }
 * 
 * @author Dr. Richard McCreadie
 *
 */
public class EndTurnClicked implements EventProcessor{

	@Override
	public void processEvent(ActorRef out, GameState gameState, JsonNode message) {
		// Execute all turn switching logic in the backend state machine
		gameState.switchTurn(out);

		// AI logic
		if (gameState.getCurrentTurn() == 2) {
	
		BasicCommands.setPlayer1Mana(out, gameState.getPlayer1());
		BasicCommands.setPlayer2Mana(out, gameState.getPlayer2());
		BasicCommands.addPlayer1Notification(out, "AI's Turn", 2);
		try { Thread.sleep(500); } catch (InterruptedException e) { e.printStackTrace(); }
		executeBasicAITurn(out, gameState);
		return;
	}


		//------
		// The following 4 lines are test code, only displayed in the terminal and do not affect game logic
        // if the terminal looks too messy, you can delete these 4 lines at any time
		System.out.println("End Turn Clicked! Now it is Player " + gameState.getCurrentTurn() + "'s turn.");
		System.out.println("Current Turn Number: " + gameState.getTurnNumber());
		System.out.println("Player 1 Mana: " + gameState.getPlayer1().getMana());
		System.out.println("Player 2 Mana: " + gameState.getPlayer2().getMana());
		//------

		// Send commands to the front-end browser to update the mana display on the UI
		BasicCommands.setPlayer1Mana(out, gameState.getPlayer1());
		BasicCommands.setPlayer2Mana(out, gameState.getPlayer2());

		// Display a UI notification indicating whose turn it is
		if (gameState.getCurrentTurn() == 1) {
			BasicCommands.addPlayer1Notification(out, "Your Turn", 2);
		} else {
			// Assuming the front-end is the human player's view, show a notification for AI's turn
			BasicCommands.addPlayer1Notification(out, "AI's Turn", 2);
		}

		// Add a short sleep after sending all UI update commands to ensure front-end animations process in order
		try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
	}

	private void executeBasicAITurn(ActorRef out, GameState state) {
		playOneAICard(out, state);
			actWithAIUnits(out, state);
			state.switchTurn(out);
			BasicCommands.setPlayer1Mana(out, state.getPlayer1());
			BasicCommands.setPlayer2Mana(out, state.getPlayer2());
			BasicCommands.addPlayer1Notification(out, "Your Turn", 2);
			try { Thread.sleep(100); } catch (InterruptedException e) { e.printStackTrace(); }
		}

		private void playOneAICard(ActorRef out, GameState state) {
			List<Card> hand = state.getPlayer2Hand();
			int mana = state.getPlayer2().getMana();
			for (int i = 0; i < hand.size(); i++) {
				Card card = hand.get(i);
				if (card.getManacost() > mana) continue;
				if (!card.getIsCreature()) continue;
				Tile summonTile = findFirstValidSummonTile(state, 2);
				if (summonTile == null) return;

				GameEngine.apply(state, new PlayCardAction(2, i, new Pos(summonTile.getTilex(), summonTile.getTiley())));

				GameUnit newUnit = state.getUnitOnTile(summonTile.getTilex(), summonTile.getTiley());
				if (newUnit != null) {
					BasicCommands.drawUnit(out, newUnit.getUnit(), summonTile);
					try { Thread.sleep(30); } catch (InterruptedException e) { e.printStackTrace(); }
					BasicCommands.setUnitAttack(out, newUnit.getUnit(), newUnit.getAttack());
					try { Thread.sleep(20); } catch (InterruptedException e) { e.printStackTrace(); }
					BasicCommands.setUnitHealth(out, newUnit.getUnit(), newUnit.getHealth());
					try { Thread.sleep(20); } catch (InterruptedException e) { e.printStackTrace(); }
				}
				BasicCommands.setPlayer1Mana(out, state.getPlayer1());
				BasicCommands.setPlayer2Mana(out, state.getPlayer2());
				return;
			}
		}

		private void actWithAIUnits(ActorRef out, GameState state) {
			List<GameUnit> aiUnits = getUnitsForOwner(state, 2);
			for (GameUnit unit : aiUnits) {
				if (unit.hasAttacked() || unit.hasMoved()) continue;
				if (unit.isAvatar()) continue;

				GameUnit adjacentEnemy = findAdjacentEnemy(state, unit);
				if (adjacentEnemy != null) {
					CombatSystem.executeAttack(out, state, unit, adjacentEnemy);
					unit.setHasAttacked(true);
					unit.setHasMoved(true);
					try { Thread.sleep(300); } catch (InterruptedException e) { e.printStackTrace(); }
					continue;
				}

				Tile moveTile = findBestMoveTowardAvatar(state, unit, state.getPlayer1Avatar());
				if (moveTile != null) {
					BasicCommands.moveUnitToTile(out, unit.getUnit(), moveTile);
					state.moveUnit(unit, moveTile.getTilex(), moveTile.getTiley());
					unit.setHasMoved(true);
					try { Thread.sleep(700); } catch (InterruptedException e) { e.printStackTrace(); }
				}
			}
		}

		private Tile findFirstValidSummonTile(GameState state, int owner) {
			for (int x = 1; x <= 9; x++) {
				for (int y = 1; y <= 5; y++) {
					GameUnit unit = state.getUnitOnTile(x, y);
					if (unit == null || unit.getOwner() != owner) continue;

					for (int dx = -1; dx <= 1; dx++) {
						for (int dy = -1; dy <= 1; dy++) {
							if (dx == 0 && dy == 0) continue;
							int nx = x + dx;
							int ny = y + dy;

							Tile tile = state.getTile(nx, ny);
							if (tile == null) continue;
							if (state.getUnitOnTile(nx, ny) != null) continue;
							return tile;
						}
					}
				}
			}
			return null;
		}

		private List<GameUnit> getUnitsForOwner(GameState state, int owner) {
			List<GameUnit> units = new ArrayList<>();
			for (int x = 1; x <= 9; x++) {
				for (int y = 1; y <= 5; y++) {
					GameUnit unit = state.getUnitOnTile(x, y);
					if (unit != null && unit.getOwner() == owner) {
						units.add(unit);
					}
				}
			}
			return units;
		}

		private GameUnit findAdjacentEnemy(GameState state, GameUnit unit) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					if (dx == 0 && dy == 0) continue;
					int nx = unit.getTileX() + dx;
					int ny = unit.getTileY() + dy;
					GameUnit target = state.getUnitOnTile(nx, ny);
					if (target != null && target.getOwner() != unit.getOwner()) {
						return target;
					}
				}
			}
			return null;
		}

		private Tile findBestMoveTowardAvatar(GameState state, GameUnit unit, GameUnit targetAvatar) {
			Tile bestTile = null;
			int bestDistance = Integer.MAX_VALUE;
			int[][] dirs = {
				{1,0},{-1,0},{0,1},{0,-1},
				{1,1},{1,-1},{-1,1},{-1,-1}
			};
			for (int[] d : dirs) {
				int nx = unit.getTileX() + d[0];
				int ny = unit.getTileY() + d[1];
				Tile tile = state.getTile(nx, ny);
				if (tile == null) continue;
				if (state.getUnitOnTile(nx, ny) != null) continue;
				int dist = Math.abs(nx - targetAvatar.getTileX()) + Math.abs(ny - targetAvatar.getTileY());
				if (dist < bestDistance) {
					bestDistance = dist;
					bestTile = tile;
				}
			}
			return bestTile;
		}

}
