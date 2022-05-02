package tech.quangson.battleengine.mechanics;

import java.util.Map;

public class TurnState {

    private PlayerState player1State;
    private PlayerState player2State;
    private String[] sequence;

    public TurnState(){}

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

    public void setPlayer1State(PlayerState player1State) {
        this.player1State = player1State;
    }

    public PlayerState getPlayer2State() {
        return player2State;
    }

    public void setPlayer2State(PlayerState player2State) {
        this.player2State = player2State;
    }

    public String[] getSequence() {
        return sequence;
    }

    public void setSequence(String[] sequence) {
        this.sequence = sequence;
    }
}
