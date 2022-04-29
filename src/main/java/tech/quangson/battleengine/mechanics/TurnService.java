package tech.quangson.battleengine.mechanics;

public interface TurnService {

    TurnState evaluateTurn(TurnState turn);

    String[] determineSequence(TurnState turn);

}
