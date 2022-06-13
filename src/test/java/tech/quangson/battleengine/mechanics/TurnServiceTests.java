package tech.quangson.battleengine.mechanics;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class TurnServiceTests {

    private final TurnService ts = new TurnServiceImpl();

    @Test
    // Note: P1P2 => P1 is faster than P2
    void testSequenceP1P2(){
        // setup
        var p1 = basicBuild();
        var stats = basicStats();
        stats.put("Agility", 50); // less than basic agility
        var p2 = basicBuild(stats);
        var turn = new TurnState(p1, p2);
        // test
        var sequence = ts.determineSequence(turn);
        Assertions.assertArrayEquals(new String[]{basicBuild,statBuild}, sequence);
    }

    @Test
    void testSequenceP2P1(){
        // setup
        var p1 = basicBuild();
        var stats = basicStats();
        stats.put("Agility", 150); // more than basic agility
        var p2 = basicBuild(stats);
        var turn = new TurnState(p1, p2);
        // test
        var sequence = ts.determineSequence(turn);
        Assertions.assertArrayEquals(new String[]{statBuild,basicBuild}, sequence);
    }

    @Test
    void testSequenceP1P2_P2Blocks(){
        // setup
        var p1 = basicBuild();
        var stats = basicStats();
        stats.put("Agility", 50); // less than basic agility
        var move = new MoveBuilder().category("Cover").type("Block").build();
        var p2 = basicBuild(move, stats);
        var turn = new TurnState(p1, p2);
        // test
        var sequence = ts.determineSequence(turn);
        Assertions.assertArrayEquals(new String[]{moveStatBuild,basicBuild}, sequence);
    }

    @Test
    void testSequenceP2P1_P1Blocks(){
        // setup
        var move = new MoveBuilder().category("Cover").type("Block").build();
        var p1 = basicBuild(move);
        var stats = basicStats();
        stats.put("Agility", 150); // more than basic agility
        var p2 = basicBuild(stats);
        var turn = new TurnState(p1, p2);
        // test
        var sequence = ts.determineSequence(turn);
        Assertions.assertArrayEquals(new String[]{moveBuild,statBuild}, sequence);
    }

    @Test
    void testSequenceP2P1_P1Blocks_P2Blocks(){
        // setup
        var move = new MoveBuilder().category("Cover").type("Block").build();
        var p1 = basicBuild(move);
        var stats = basicStats();
        stats.put("Agility", 150); // more than basic agility
        var p2 = basicBuild(move, stats);
        var turn = new TurnState(p1,p2);
        // test
        var sequence = ts.determineSequence(turn);
        Assertions.assertArrayEquals(new String[]{moveStatBuild, moveBuild}, sequence);

    }

    @Test
    void testBuff_P2Buff_ForceBy20(){
        var p1 = basicBuild();
        var move = new MoveBuilder()
                .type("Buff")
                .buffs(Map.of("Force",20))
                .build();
        var stats = basicStats();
        stats.put("Force", 100);
        var p2 = basicBuild(move, stats);
        var turn = new TurnState(p1,p2);
        // test
        var resultState = ts.evaluateTurn(turn)[1];
        var p2OldForceStat = turn.getPlayer2State().getStat("Force");
        var p2NewForceStat = resultState.getPlayer2State().getStat("Force");
        Assertions.assertEquals(100, p2OldForceStat);
        Assertions.assertEquals(120, p2NewForceStat);
    }

    @Test
    void testDebuff_P1DebuffsP2_ReflexBy50(){
        var move = new MoveBuilder()
                .type("Debuff")
                .debuffs(Map.of("Reflex", 50))
                .build();
        var p1 = basicBuild(move);
        var stats = basicStats();
        stats.put("Reflex", 100);
        var p2 = basicBuild(stats);
        var turn = new TurnState(p1,p2);
        // test
        var resultState = ts.evaluateTurn(turn)[1];
        var p2OldStat = turn.getPlayer2State().getStat("Reflex");
        var p2NewStat = resultState.getPlayer2State().getStat("Reflex");
        Assertions.assertEquals(100, p2OldStat);
        Assertions.assertEquals(50, p2NewStat);
    }

    @Test
    void testCombatDamage(){
        var p1 = basicBuild();
        var stats = basicStats();
        stats.put("Force",80);
        stats.put("Reflex",80);
        var p2 = basicBuild(stats);
        var turn = new TurnState(p1, p2);
        // test
        var resultState = ts.evaluateTurn(turn)[1];
        /*
            note: p2 should go first due to speed tie (by design)
            p1 damage is stronger by going second since p2's energy will go down after attack
         */
        var damageOnP1 = p1.getStat("Health") - resultState.getPlayer1State().getStat("Health");
        var damageOnP2 = p2.getStat("Health") - resultState.getPlayer2State().getStat("Health");
        Assertions.assertEquals(4040, damageOnP1);
        Assertions.assertEquals(4418, damageOnP2);

    }

    @Test
    void testMagicDamage(){
        var stats1 = basicStats();
        stats1.put("Spirit",90);
        stats1.put("Focus",50);
        var move = new MoveBuilder().category("Magic").build();
        var p1 = basicBuild(move, stats1);
        var stats2 = basicStats();
        stats2.put("Focus",60);
        var p2 = new PlayerBuilder()
                .playerId("p2") //can't have two stat builds b/c they will have the same id
                .move(move)
                .stats(stats2)
                .build();
        var turn = new TurnState(p1, p2);
        // test
        var resultState = ts.evaluateTurn(turn)[1];
        /*
            note: p2 should go first due to speed tie (by design)
            p1 damage is stronger by going second since p2's energy will go down after attack
         */
        var damageOnP1 = p1.getStat("Health") - resultState.getPlayer1State().getStat("Health");
        var damageOnP2 = p2.getStat("Health") - resultState.getPlayer2State().getStat("Health");
        Assertions.assertEquals(5600, damageOnP1);
        Assertions.assertEquals(6950, damageOnP2);

    }

    @Test
    void testSpecialDamage(){
        var stats1 = basicStats();
        stats1.put("Spirit",90);
        stats1.put("Focus",50);
        stats1.put("Health", 30000);
        var move = new MoveBuilder().category("Special").build();
        var p1 = basicBuild(move, stats1);
        var stats2 = basicStats();
        stats2.put("Force",80);
        stats2.put("Focus",60);
        stats2.put("Reflex",80);
        stats2.put("Health", 30000);
        var p2 = new PlayerBuilder()
                .playerId("p2") //can't have two stat builds b/c they will have the same id
                .move(move)
                .stats(stats2)
                .build();
        var turn = new TurnState(p1, p2);
        // test
        var resultState = ts.evaluateTurn(turn)[1];
        /*
            note: p2 should go first due to speed tie (by design)
            p1 damage is stronger by going second since p2's energy will go down after attack
         */
        var damageOnP1 = p1.getStat("Health") - resultState.getPlayer1State().getStat("Health");
        var damageOnP2 = p2.getStat("Health") - resultState.getPlayer2State().getStat("Health");
        Assertions.assertEquals(14840, damageOnP1);
        Assertions.assertEquals(13700, damageOnP2);
    }

    @Test
    void testBlockingMitigatesDamage(){
        var strongBlock = new MoveBuilder()
                .name("Powerful Guard")
                .category("Cover")
                .type("Block")
                .basePower(75)
                .build();
        var stats1 = basicStats();
        stats1.put("Spirit",90);
        stats1.put("Focus",50);
        var p1 = new PlayerBuilder()
                .move(strongBlock)
                .stats(stats1)
                .playerId("Iron Knight")
                .build();

        var strongSpell = new MoveBuilder()
                .name("Planetary Devastation")
                .category("Magic")
                .type("Damage")
                .basePower(120)
                .build();
        var stats2 = basicStats();
        stats2.put("Force",80);
        stats2.put("Focus",60);
        stats2.put("Reflex",80);
        stats2.put("Agility", 150);
        var p2 = new PlayerBuilder()
                .playerId("Dark Mage")
                .move(strongSpell)
                .stats(stats2)
                .build();
        var turn = new TurnState(p1,p2);
        // test
        var resultState = ts.evaluateTurn(turn)[1];
        var damageOnP1 = p1.getStat("Health") - resultState.getPlayer1State().getStat("Health");
        Assertions.assertEquals(3425, damageOnP1);
        Assertions.assertEquals(p2.getStat("Health"), resultState.getPlayer2State().getStat("Health"));

    }

    @Test
    void testBuffDebuffSameStat(){
        var buffingMove = new MoveBuilder()
                .type("Buff")
                .buffs(Map.of("Force", 20))
                .build();
        var p1 = basicBuild(buffingMove);
        var debuffingMove = new MoveBuilder()
                .type("Debuff")
                .debuffs(Map.of("Force",20))
                .build();
        var p2Stats = basicStats();
        p2Stats.put("Agility", 120);
        var p2 = basicBuild(debuffingMove, p2Stats);
        var turn = new TurnState(p1, p2);
        // test
        var resultingTurns = ts.evaluateTurn(turn);
        var turn1 = resultingTurns[0];
        var p1Force_t1 = turn1.getPlayer1State().getStat("Force");
        var turn2 = resultingTurns[1];
        var p1Force_t2 = turn2.getPlayer1State().getStat("Force");

        Assertions.assertEquals(60, p1Force_t1);
        Assertions.assertEquals(72, p1Force_t2);
    }


    // ------------------------            utility methods to help create tests  ---------------------
    private static final String defaultBuild = "defaultBuild";
    private static final String basicBuild = "basicBuild";
    private static final String moveBuild = "moveBuild";
    private static final String statBuild = "statBuild";
    private static final String moveStatBuild = "moveStatBuild";
    private TurnState.PlayerState basicBuild(){
        return new TurnState.PlayerState(basicBuild, new MoveBuilder().build(), basicStats());
    }

    private static TurnState.PlayerState basicBuild(GameMove move){
        return new TurnState.PlayerState(moveBuild, move, basicStats());
    }

    private static TurnState.PlayerState basicBuild(Map<String, Integer> stats){
        return new TurnState.PlayerState(statBuild, new MoveBuilder().build(), stats);
    }

    private static TurnState.PlayerState basicBuild(GameMove move, Map<String, Integer> stats){
        return new TurnState.PlayerState(moveStatBuild, move, stats);
    }

    private static class PlayerBuilder {
        private String playerId = defaultBuild;
        private GameMove move = new MoveBuilder().build();
        private Map<String, Integer> stats = basicStats();

        PlayerBuilder playerId(String id){ playerId = id; return this;}
        PlayerBuilder move(GameMove move){ this.move = move; return this;}
        PlayerBuilder stats(Map<String, Integer> stats){this.stats = stats; return this;}
        TurnState.PlayerState build(){
            return new TurnState.PlayerState(playerId, move, stats);
        }

    }

    private static Map<String, Integer> basicStats(){
        return new HashMap<>(Map.of(
                "Force", 75,
                "Focus", 75,
                "Reflex", 75,
                "Spirit", 75,
                "Health", 9000,
                "Energy", 150,
                "Agility", 75
        ));
    }

    private static class MoveBuilder {
        private String name = "Basic Move";
        private String category = "Combat";
        private String type = "Damage";
        private int basePower = 60;
        private int cost = 50;
        private int limit = 10;
        private int priority = 1;
        private Map<String, Integer> buffs = null;
        private Map<String, Integer> debuffs = null;

        MoveBuilder name(String name){ this.name = name; return this;}
        MoveBuilder category(String category){ this.category = category; return this;}
        MoveBuilder type(String type){ this.type = type; return this;}
        MoveBuilder basePower(int basePower){ this.basePower = basePower; return this;}
        MoveBuilder cost(int cost){ this.cost = cost; return this;}
        MoveBuilder limit(int limit){ this.limit = limit; return this;}
        MoveBuilder priority(int priority){this.priority = priority; return this;}
        MoveBuilder buffs(Map<String, Integer> buffs){ this.buffs = buffs; return this;}
        MoveBuilder debuffs(Map<String, Integer> debuffs){ this.debuffs = debuffs; return this;}
        GameMove build(){
            return new GameMove(name, category,type,basePower,cost,limit,priority,buffs,debuffs);
        }
    }

}
