package tech.quangson.battleengine.mechanics;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class TurnServiceImpl implements TurnService{

    public static final String TypeDamage = "Damage";
    public static final String TypeBuff = "Buff";
    public static final String TypeDebuff = "Debuff";
    public static final String TypeBlock = "Block";

    public static final String CategoryCombat = "Combat";
    public static final String CategoryMagic = "Magic";
    public static final String CategorySpecial = "Special";

    public static final String StatForce = "Force";
    public static final String StatFocus = "Focus";
    public static final String StatReflex = "Reflex";
    public static final String StatSpirit = "Spirit";
    public static final String StatHealth = "Health";
    public static final String StatEnergy = "Energy";
    public static final String StatAgility = "Agility";

    @Override
    public TurnState[] evaluateTurn(TurnState turn) {
        var result = new TurnState[2];
        var playerSequence = determineSequence(turn);
        var first = derivePlayerState(turn, playerSequence[0]);
        var second = derivePlayerState(turn, playerSequence[1]);
        var firstMoveType = first.move().type();
        var firstUpdatedStats = new HashMap<>(first.stats());
        var secondMoveType = second.move().type();
        var secondUpdatedStats = new HashMap<>(second.stats());

        switch (firstMoveType) {
            case TypeDamage -> applyDamage(first, second, firstUpdatedStats, secondUpdatedStats);
            case TypeBuff -> applyBuff(first, firstUpdatedStats);
            case TypeDebuff -> applyDebuff(first, secondUpdatedStats);
        }
        applyCost(first, firstUpdatedStats);
        var firstRoundResult = buildTurnState(turn, Map.copyOf(firstUpdatedStats), Map.copyOf(secondUpdatedStats));
        result[0] = firstRoundResult;

        switch(secondMoveType){
            case TypeDamage -> applyDamage(second, first, secondUpdatedStats, firstUpdatedStats);
            case TypeBuff -> applyBuff(second, secondUpdatedStats);
            case TypeDebuff -> applyDebuff(second, firstUpdatedStats);
        }
        applyCost(second, secondUpdatedStats);

        var secondRoundResult =  buildTurnState(turn, Map.copyOf(firstUpdatedStats), Map.copyOf(secondUpdatedStats));
        result[1] = secondRoundResult;
        return result;
    }

    @Override
    public String[] determineSequence(TurnState turn){
        Integer p1Agility = turn.getPlayer1State().getStat(StatAgility);
        Integer p2Agility = turn.getPlayer2State().getStat(StatAgility);
        boolean p1Block = turn.getPlayer1State().move().type().equals(TypeBlock);
        boolean p2Block = turn.getPlayer2State().move().type().equals(TypeBlock);
        String[] sequence;

        String player1 = turn.getPlayer1State().playerId();
        String player2 = turn.getPlayer2State().playerId();
        if(p1Agility > p2Agility){
            if(!p1Block && p2Block){
                sequence = new String[]{player2, player1};
            }
            else{
                sequence = new String[]{player1, player2};
            }
        }
        else {
            if(p1Block && !p2Block){
                sequence = new String[]{player1, player2};
            }
            else{
                sequence = new String[]{player2, player1};
            }
        }
        return sequence;
    }

    private TurnState.PlayerState derivePlayerState(TurnState turn, String playerId){
        var p1Id = turn.getPlayer1State().playerId();

        return playerId.equals(p1Id) ? turn.getPlayer1State() : turn.getPlayer2State();
    }

    private int damageCalculation(TurnState.PlayerState attacker, TurnState.PlayerState defender,
                                  Map<String, Integer> sourceStats, Map<String, Integer> targetStats){
        String attackCategory = attacker.move().category();
        double atkBasePower = attacker.move().basePower();
        double damage;
        // attacker stats
        double atkEnergy = sourceStats.get(StatEnergy);
        double atkSpirit = sourceStats.get(StatSpirit);
        double atkForce = sourceStats.get(StatForce);
        // target stats
        double defEnergy = targetStats.get(StatEnergy);
        double defReflex = targetStats.get(StatReflex);
        double defFocus = targetStats.get(StatFocus);

        // damage formula constants
        int balanceValue = 100;
        int powerScale = 100;
        switch (attackCategory) {
            case CategoryCombat -> damage = (atkEnergy/(defEnergy + balanceValue) * atkForce / defReflex * atkBasePower + 2) * powerScale;
            case CategoryMagic -> damage = (atkEnergy / (defEnergy + balanceValue) * atkSpirit / defFocus * atkBasePower + 2) * powerScale;
            case CategorySpecial -> damage = ((atkForce + atkSpirit + atkEnergy) / (defReflex + defFocus) * atkBasePower + 2) * powerScale;
            default -> damage = 0;
        }

        boolean hasBlock = defender.move().type().equals(TypeBlock);
        double blockPower = defender.move().basePower();
        return (int) (!hasBlock ? damage : damage * (1-blockPower/100));
    }

    private void applyBuff(TurnState.PlayerState target, Map<String, Integer> updatedStats){
        var buffs = target.move().buffs();
        for(String buff : buffs.keySet()){
            double buffAmt = buffs.get(buff);
            double newAmt = updatedStats.get(buff) * (1 + buffAmt/100);
            updatedStats.put(buff, (int) newAmt);
        }
    }

    private void applyDebuff(TurnState.PlayerState target, Map<String, Integer> updatedStats){
        var debuffs = target.move().debuffs();
        for(String debuff : debuffs.keySet()){
            double debuffAmt = debuffs.get(debuff);
            double newAmt = updatedStats.get(debuff)  * (1 - debuffAmt/100);
            updatedStats.put(debuff, (int) newAmt);
        }
    }

    private void applyDamage(TurnState.PlayerState source, TurnState.PlayerState target,
                             Map<String, Integer> sourceStats, Map<String, Integer> targetStats){
        var prevHealth = targetStats.get(StatHealth);
        var damage = damageCalculation(source, target, sourceStats, targetStats);
        var updatedHealth = Math.max(prevHealth - damage, 0); // prevent negative health result
        targetStats.put(StatHealth, updatedHealth);
    }

    private void applyCost(TurnState.PlayerState target, Map<String, Integer> updatedStats){
        var oldStats = target.stats();
        var cost = target.move().cost();
        var prevEnergy = oldStats.get(StatEnergy);
        var updatedEnergy = Math.max(prevEnergy - cost, 0); // prevent negative energy result
        updatedStats.put(StatEnergy, updatedEnergy);
    }

    private TurnState buildTurnState(TurnState turn, Map<String, Integer> firstUpdatedStats, Map<String, Integer> secondUpdatedStats) {
        var player1Id = turn.getPlayer1State().playerId();
        var player2Id = turn.getPlayer2State().playerId();
        var player1Move = turn.getPlayer1State().move();
        var player2Move = turn.getPlayer2State().move();

        var playerSequence = determineSequence(turn);
        var firstId = playerSequence[0];

        boolean player1IsFirst = player1Id.equals(firstId);
        TurnState.PlayerState player1;
        TurnState.PlayerState player2;
        if(player1IsFirst){
            player1 = new TurnState.PlayerState(player1Id, player1Move, firstUpdatedStats);
            player2 = new TurnState.PlayerState(player2Id, player2Move, secondUpdatedStats);
        }
        else {
            player1 = new TurnState.PlayerState(player1Id, player1Move, secondUpdatedStats);
            player2 = new TurnState.PlayerState(player2Id, player2Move, firstUpdatedStats);
        }
        return new TurnState(player1, player2, playerSequence);
    }
}
