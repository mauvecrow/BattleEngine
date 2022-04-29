package tech.quangson.battleengine.mechanics;

import java.util.Map;

public class TurnState {

    private final PlayerState player1State;
    private final PlayerState player2State;
    private final String[] sequence;

    public TurnState(PlayerState player1State, PlayerState player2State) {
        this.player1State = player1State;
        this.player2State = player2State;
        this.sequence = null;
    }

    public TurnState(PlayerState player1State, PlayerState player2State, String[] sequence) {
        this.player1State = player1State;
        this.player2State = player2State;
        this.sequence = sequence;
    }

    public record PlayerState(String playerId, GameMove move,
                               Map<String, Integer> stats) {
        public Integer getStat(String stat){
            return stats.get(stat);
        }
    }

    public PlayerState getPlayer1State() {
        return player1State;
    }

    public PlayerState getPlayer2State() {
        return player2State;
    }

}
