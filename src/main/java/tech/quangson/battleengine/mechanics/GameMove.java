package tech.quangson.battleengine.mechanics;

import java.util.Map;

public record GameMove(String moveName,
        String category,
        String type,
        int basePower,
        int cost,
        int limit,
        int priority,
        Map<String, Integer> buffs,
        Map<String, Integer> debuffs){}
