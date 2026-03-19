package systems;

import abilities.Effect;
import abilities.TriggeredEffect;
import abilities.TriggerType;
import akka.actor.ActorRef;
import structures.GameState;
import structures.GameUnit;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Manages registration and firing of triggered unit effects.
 *
 * Usage pattern:
 *   1. When a unit with a triggered ability is summoned, call register().
 *   2. At each game event (summon, death, turn start, …), call fire().
 *   3. When a unit dies, call unregister() to remove its triggers.
 *
 * Member A - Sprint 3.
 */
public final class EffectResolver {

    private final List<TriggeredEffect> registry = new ArrayList<>();

    /**
     * Registers a triggered effect so it will fire on future events.
     *
     * @param trigger  the game event that should cause the effect to fire
     * @param effect   the logic to execute
     * @param owner    the unit that owns this trigger
     */
    public void register(TriggerType trigger, Effect effect, GameUnit owner) {
        registry.add(new TriggeredEffect(trigger, effect, owner));
    }

    /**
     * Fires all effects registered for the given trigger type.
     *
     * @param event   the game event that just occurred
     * @param out     WebSocket actor for sending commands to the front-end
     * @param state   current game state
     */
    public void fire(TriggerType event, ActorRef out, GameState state,GameUnit source) {
        for (TriggeredEffect te : registry) {
            if (te.getTrigger() == event) {
                te.getEffect().execute(out, state, te.getOwner());
            }
        }
    }

    /**
     * Removes all triggers owned by the given unit (call when the unit dies or leaves play).
     *
     * @param owner  the unit whose triggers should be removed
     */
    public void unregister(GameUnit owner) {
        Iterator<TriggeredEffect> it = registry.iterator();
        while (it.hasNext()) {
            if (it.next().getOwner() == owner) it.remove();
        }
    }

    /** Returns the number of currently registered triggers (useful for testing). */
    public int size() {
        return registry.size();
    }
}
