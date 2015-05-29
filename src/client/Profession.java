/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package client;

import tools.Pair;

/**
 * JourneyMS
 * 
 */
public class Profession {
    
    private Pair<MapleProfession, MapleProfession> professions;
    private Pair<Integer, Integer> exp;
    private Pair<ProfessionLevel, ProfessionLevel> level;
    
    public Profession() {
        professions.left = MapleProfession.NONE;
        professions.right = MapleProfession.NONE;
        exp.left = 0;
        exp.right = 0;
        level.left = ProfessionLevel.NEWBIE;
        level.right = ProfessionLevel.NEWBIE;
    }
    
    public enum MapleProfession {
        NONE, MINING, HERBALISM, CRAFTING, ALCHEMY;
        
        public String getName() {
            return this.toString().substring(0, 1).concat(this.toString().toLowerCase());
        }
    }
    
    public enum ProfessionLevel {
        NEWBIE(100, 10), APPRENTICE(250, 30), ASSISTANT(500, 70), FOREMAN(1200, 100), MASTER(2500, 120), UNREACHABLE(0, 255);
        private final int threshold;
        private final int levelreq;
        
        ProfessionLevel(int threshold, int levelreq) {
            this.threshold = threshold;
            this.levelreq = levelreq;
        }
        
        public int getThreshold() {
            return threshold;
        }
        
        public int getLevelReq() {
            return levelreq;
        }
        
        public ProfessionLevel getNext() {
            return ProfessionLevel.values()[this.ordinal() + 1];
        }
        
        public String getName() {
            return this.toString().substring(0, 1).concat(this.toString().toLowerCase());
        }
    }
    
    public void gainExp(MapleProfession type, int gain, int chrlevel) {
        boolean left = professions.getLeft() == type;
        if (left) {
            exp.left += gain;
            if (exp.left >= level.left.getNext().getThreshold() && chrlevel >= level.left.getNext().getLevelReq())
                level.left = level.left.getNext();
        } else {
            exp.right += gain;
            if (exp.right >= level.right.getNext().getThreshold() && chrlevel >= level.left.getNext().getLevelReq())
                level.right = level.right.getNext();
        }
    }
    
    public String getName(boolean left) {
        return (left)?professions.getLeft().getName():professions.getRight().getName();
    }
}
