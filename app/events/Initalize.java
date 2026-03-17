package events;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
// import demo.CommandDemo;
// import demo.Loaders_2024_Check;
import structures.GameState;

import structures.GameUnit;
import structures.basic.Card;
import structures.basic.Player;
import structures.basic.Tile;
import structures.basic.Unit;
import utils.BasicObjectBuilders;
import utils.OrderedCardLoader;
import utils.StaticConfFiles;

/**
 * Indicates that both the core game loop in the browser is starting, meaning
 * that it is ready to recieve commands from the back-end.
 * 
 * { 
 *   messageType = “initalize”
 * }
 * 
 * @author Dr. Richard McCreadie
 *
 */
public class Initalize implements EventProcessor{

	@Override
	public void processEvent(ActorRef out, GameState gameState, JsonNode message) {
		
		gameState.gameInitalised = true;
		
		gameState.something = true;
		
		// User 1 makes a change
		// CommandDemo.executeDemo(out); // this executes the command demo, comment out this when implementing your solution
		//Loaders_2024_Check.test(out);
	
		// draw the 9x5 board
        for (int x = 1; x <= 9; x++) {
            for (int y = 1; y <= 5; y++) {
                // create tile
                Tile tile = BasicObjectBuilders.loadTile(x, y);
                
                // store in game state
                gameState.setTile(x, y, tile);
                
                // draw on screen (mode 0 = normal appearance)
                BasicCommands.drawTile(out, tile, 0);
                
                // small delay so tiles appear one by one
                try { Thread.sleep(20); } catch (InterruptedException e) { e.printStackTrace(); }
            }
        }

        // create player objects
        // player constructor: (health, mana)
        // turn 1 mana = turnNumber + 1 = 1 + 1 = 2
        Player player1 = new Player(20, 2);
        Player player2 = new Player(20, 2);
        
        gameState.setPlayer1(player1);
        gameState.setPlayer2(player2);

        // display player stats on UI
        BasicCommands.setPlayer1Health(out, player1);
        try { Thread.sleep(50); } catch (InterruptedException e) { e.printStackTrace(); }
        
        BasicCommands.setPlayer1Mana(out, player1);
        try { Thread.sleep(50); } catch (InterruptedException e) { e.printStackTrace(); }
        
        BasicCommands.setPlayer2Health(out, player2);
        try { Thread.sleep(50); } catch (InterruptedException e) { e.printStackTrace(); }
        
        BasicCommands.setPlayer2Mana(out, player2);
        try { Thread.sleep(50); } catch (InterruptedException e) { e.printStackTrace(); }

        // create and place player 1 avatar
        // position: (1, 2) left side, middle row
        
        // get unique ID for this unit
        int p1AvatarId = gameState.getAndIncrementUnitId();
        
        // load the avatar unit from config
        Unit p1AvatarUnit = BasicObjectBuilders.loadUnit(
            StaticConfFiles.humanAvatar, 
            p1AvatarId, 
            Unit.class
        );
        
        // get the tile where avatar will be placed
        Tile p1AvatarTile = gameState.getTile(2, 3);
        
        // set unit's pixel position to match tile
        p1AvatarUnit.setPositionByTile(p1AvatarTile);
        
        // create GameUnit wrapper (owner=1, attack=2, health=20, isAvatar=true)
        GameUnit p1Avatar = new GameUnit(p1AvatarUnit, 1, 2, 20, true);
        
        // place on board and store reference
        gameState.placeUnit(2, 3, p1Avatar);
        gameState.setPlayer1Avatar(p1Avatar);

        // draw on screen
        BasicCommands.drawUnit(out, p1AvatarUnit, p1AvatarTile);
        try { Thread.sleep(200); } catch (InterruptedException e) { e.printStackTrace(); }
        
        // show attack value
        BasicCommands.setUnitAttack(out, p1AvatarUnit, 2);
        try { Thread.sleep(100); } catch (InterruptedException e) { e.printStackTrace(); }
        
        // show health value
        BasicCommands.setUnitHealth(out, p1AvatarUnit, 20);
        try { Thread.sleep(100); } catch (InterruptedException e) { e.printStackTrace(); }

        // create and place player 2 avatar
        // position: (7, 2) right side, middle row
        
        int p2AvatarId = gameState.getAndIncrementUnitId();
        
        Unit p2AvatarUnit = BasicObjectBuilders.loadUnit(
            StaticConfFiles.aiAvatar, 
            p2AvatarId, 
            Unit.class
        );
        
        Tile p2AvatarTile = gameState.getTile(8, 3);
        p2AvatarUnit.setPositionByTile(p2AvatarTile);
        
        GameUnit p2Avatar = new GameUnit(p2AvatarUnit, 2, 2, 20, true);
        
        gameState.placeUnit(8, 3, p2Avatar);
        gameState.setPlayer2Avatar(p2Avatar);

        BasicCommands.drawUnit(out, p2AvatarUnit, p2AvatarTile);
        try { Thread.sleep(100); } catch (InterruptedException e) { e.printStackTrace(); }
        
        BasicCommands.setUnitAttack(out, p2AvatarUnit, 2);
        try { Thread.sleep(50); } catch (InterruptedException e) { e.printStackTrace(); }
        
        BasicCommands.setUnitHealth(out, p2AvatarUnit, 20);
        try { Thread.sleep(50); } catch (InterruptedException e) { e.printStackTrace(); }
        
        List<Card> p1Deck = OrderedCardLoader.getPlayer1Cards(2);
        gameState.setPlayer1Deck(p1Deck);

        List<Card> p2Deck = OrderedCardLoader.getPlayer2Cards(2);
        gameState.setPlayer2Deck(p2Deck);

        // draw starting hands (3 cards each)
        
        // player 1 hand will draw and display on UI
        for (int i = 0; i < 3; i++) {
            if (!gameState.getPlayer1Deck().isEmpty()) {
                // remove top card from deck
                Card card = gameState.getPlayer1Deck().remove(0);
                
                // add to hand
                gameState.getPlayer1Hand().add(card);
                
                // display on UI (positions are 1-indexed: 1, 2, 3)
                // mode 0 = normal (not highlighted)
                BasicCommands.drawCard(out, card, i + 1, 0);
                try { Thread.sleep(50); } catch (InterruptedException e) { e.printStackTrace(); }
            }
        }

        // player 2 hand will draw but not display (AI hand is hidden)
        for (int i = 0; i < 3; i++) {
            if (!gameState.getPlayer2Deck().isEmpty()) {
                Card card = gameState.getPlayer2Deck().remove(0);
                gameState.getPlayer2Hand().add(card);
                // No BasicCommands.drawCard as AI hand is hidden from player
            }
        }

        // set initial turn
        gameState.setCurrentTurn(1);  // Human player goes first
        gameState.setTurnNumber(1);   // Turn 1
	}

}


