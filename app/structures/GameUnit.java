package structures;

import abilities.Keyword;
import abilities.Status;
import structures.basic.Unit;

import java.util.EnumSet;
import java.util.Set;

public class GameUnit {

    private Unit unit;            // template Unit object
    private int owner;            // 1 is for human and 2 is for AI
    private int attack;
    private int baseAttack;
    private int health;
    private int maxHealth;
    private boolean hasMoved;
    private boolean hasAttacked;
    private boolean isAvatar;
    private int tileX;
    private int tileY;
    private String cardName;
    private final Set<Keyword> keywords;
    private final Set<Status> statuses;

    // constructor
    public GameUnit(Unit unit, int owner, int attack, int health, boolean isAvatar) {
        this.unit = unit;
        this.owner = owner;
        this.attack = attack;
        this.baseAttack = attack;
        this.health = health;
        this.maxHealth = health;
        this.hasMoved = false;
        this.hasAttacked = false;
        this.isAvatar = isAvatar;
        this.tileX = 0;
        this.tileY = 0;
        this.keywords = EnumSet.noneOf(Keyword.class);
        this.statuses = EnumSet.noneOf(Status.class);
    }

    // setters and getters

    public Unit getUnit() { return unit; }
    public void setUnit(Unit unit) { this.unit = unit; }

    public int getOwner() { return owner; }
    public void setOwner(int owner) { this.owner = owner; }

    public int getAttack() { return attack; }
    public void setAttack(int attack) { this.attack = attack; }

    public int getBaseAttack() { return baseAttack; }
    public void setBaseAttack(int baseAttack) { this.baseAttack = baseAttack; }

    public int getHealth() { return health; }
    public void setHealth(int health) { this.health = health; }

    public int getMaxHealth() { return maxHealth; }
    public void setMaxHealth(int maxHealth) { this.maxHealth = maxHealth; }

    public boolean hasMoved() { return hasMoved; }
    public void setHasMoved(boolean hasMoved) { this.hasMoved = hasMoved; }

    public boolean hasAttacked() { return hasAttacked; }
    public void setHasAttacked(boolean hasAttacked) { this.hasAttacked = hasAttacked; }

    public boolean isAvatar() { return isAvatar; }
    public void setIsAvatar(boolean isAvatar) { this.isAvatar = isAvatar; }

    public int getTileX() { return tileX; }
    public int getTileY() { return tileY; }
    public void setPosition(int x, int y) {
        this.tileX = x;
        this.tileY = y;
    }

    public String getCardName() { return cardName; }
    public void setCardName(String cardName) { this.cardName = cardName; }

    public Set<Keyword> getKeywords() { return keywords; }
    public boolean hasKeyword(Keyword keyword) { return keywords.contains(keyword); }
    public void addKeyword(Keyword keyword) { keywords.add(keyword); }

    public Set<Status> getStatuses() { return statuses; }
    public boolean hasStatus(Status status) { return statuses.contains(status); }
    public void addStatus(Status status) { statuses.add(status); }
    public void removeStatus(Status status) { statuses.remove(status); }

    // game logic methods

    public void takeDamage(int dmg) {
        this.health -= dmg;
    }

    public void heal(int amount) {
        this.health = Math.min(this.maxHealth, this.health + amount);
    }

    public boolean isDead() {
        return this.health <= 0;
    }

    public void resetTurnFlags() {
        this.hasMoved = false;
        this.hasAttacked = false;
    }

    /**
     * Start-of-turn refresh for the active player's units.
     * Stunned units skip exactly one turn, then the stun is consumed.
     */
    public void startTurnReset() {
        if (hasStatus(Status.STUNNED)) {
            this.hasMoved = true;
            this.hasAttacked = true;
            removeStatus(Status.STUNNED);
            return;
        }
        resetTurnFlags();
    }
}
