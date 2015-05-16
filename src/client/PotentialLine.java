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
public class PotentialLine {

    private final PotentialStat effect;
    private final PotentialLevel superior;
    
    public enum PotentialStat {
        STR, DEX, INT, LUK, HP, MP, BDM;
    }
    
    public enum PotentialLevel {
        ZERO, ONE, TWO;
    }
    
    public PotentialLine(PotentialStat effect, PotentialLevel superior) {
        this.effect = effect;
        this.superior = superior;
    }
    
    public PotentialLine(int line) {
        this.effect = PotentialStat.values()[line%100];
        this.superior = PotentialLevel.values()[line/100 - (line/1000)*10];
    }
    
    public PotentialLine() {
        this.effect = PotentialStat.values()[Randomizer.nextInt(PotentialStat.values().length)];
        this.superior = PotentialLevel.values()[Randomizer.nextInt(PotentialLevel.values().length)];
    }
    
    public int getValue(int rank) {
        return (effect.equals(PotentialStat.BDM)?2:1)*(3*rank - superior.ordinal());
    }
    
    public String getName(int rank) {
        return String.valueOf(effect).replaceFirst("BDM", "Boss Damage").concat(" ").concat(String.valueOf(getValue(rank))).concat("%");
    }
    
    public PotentialStat getEffect() {
        return effect;
    }
    
    public PotentialLevel getSuperior() {
        return superior;
    }
}
