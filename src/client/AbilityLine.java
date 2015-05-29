/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package client.properties;

import tools.Randomizer;

/**
 * JourneyMS
 * 
 */
public class AbilityLine {

    private final AbilityStat effect;
    private final AbilityLevel superior;
    
    public enum AbilityStat {
        STR, DEX, INT, LUK, HP, MP, BDM;
    }
    
    public enum AbilityLevel {
        ZERO, ONE, TWO;
    }
    
    public AbilityLine(AbilityStat effect, AbilityLevel superior) {
        this.effect = effect;
        this.superior = superior;
    }
    
    public AbilityLine(short line) {
        this.effect = AbilityStat.values()[line%100];
        this.superior = AbilityLevel.values()[line/100 - (line/1000)*10];
    }
    
    public AbilityLine() {
        this.effect = AbilityStat.values()[Randomizer.nextInt(AbilityStat.values().length)];
        this.superior = AbilityLevel.values()[Randomizer.nextInt(AbilityLevel.values().length)];
    }
    
    public int getValue(byte rank) {
        return (effect.equals(AbilityStat.BDM)?2:1)*(3*rank - superior.ordinal());
    }
    
    public String getName(byte rank) {
        return String.valueOf(effect).replaceFirst("BDM", "Boss Damage").concat(" ").concat(String.valueOf(getValue(rank))).concat("%");
    }
    
    public AbilityStat getEffect() {
        return effect;
    }
    
    public AbilityLevel getSuperior() {
        return superior;
    }
}
