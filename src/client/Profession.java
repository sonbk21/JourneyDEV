/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package client;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import tools.DatabaseConnection;
import tools.Pair;

/**
 * JourneyMS
 * 
 */
public class Professions {
    
    private Pair<MapleProfession, MapleProfession> professions;
    
    public Professions(MapleProfession left, MapleProfession right) {
        professions.left = left;
        professions.right = right;
    }
    
    public Professions() {
        professions.left = null;
        professions.right = null;
    }
    
    public enum ProfessionType {
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
    
    public class MapleProfession {
        
        private final ProfessionType type;
        private ProfessionLevel level;
        private Integer exp;
        
        public MapleProfession(ProfessionType type, ProfessionLevel level, int exp) {
            this.type = type;
            this.level = level;
            this.exp = exp;
        }
        
        public boolean gainExp(int gain, short chrlevel) {
            exp += gain;
            if (exp >= level.getNext().getThreshold() && chrlevel >= level.getNext().getLevelReq()) {
                level = level.getNext();
                return true;
            }
            return false;
        }
        
        public String getName(boolean left) {
            return type.getName();
        }
        
        public ProfessionType getType() {
            return type;
        }
        
        public ProfessionLevel getLevel() {
            return level;
        }
        
        public int getExp() {
            return exp;
        }
    }
    
    public void changeProfession(ProfessionType oldtype, ProfessionType newtype) {
        if (professions.left.getType() == oldtype)
            professions.left = new MapleProfession(newtype, ProfessionLevel.NEWBIE, 0);
        else
            professions.right = new MapleProfession(newtype, ProfessionLevel.NEWBIE, 0);
    }
    
    public MapleProfession getProfession(ProfessionType type) {
        if (professions.left == null)
            return null;
        
        if (professions.left.getType() == type)
            return professions.left;
        else
            return professions.right;
    }
    
    public void loadProfessions(final int charid) {
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT type, level, exp FROM professions WHERE charid = ?")) {
            ps.setInt(1, charid);
            try (ResultSet rs = ps.executeQuery()) {
                boolean first = true;
                while (rs.next()) {
                    MapleProfession loaded = new MapleProfession(ProfessionType.values()[rs.getByte("type")], ProfessionLevel.values()[rs.getByte("level")], rs.getInt("exp"));
                    if (first) {
                        professions.left = loaded;
                        first = false;
                    } else {
                        professions.right = loaded;
                    }
                }
            }
        } catch (SQLException sqle) {
            System.out.println("Error."+sqle);
        }
    }

    public void saveProfessions(final int charid) {
        if (professions.left == null) {
            return;
        }
        
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM professions WHERE charid = ?");
            ps.setInt(1, charid);
            ps.execute();
            ps.close();
            StringBuilder query = new StringBuilder();
            query.append("INSERT INTO professions VALUES (");
            query.append(charid);
            query.append(", ");
            query.append(professions.left.getType().ordinal());
            query.append(", ");
            query.append(professions.left.getLevel().ordinal());
            query.append(", ");
            query.append(professions.left.getExp());
            query.append(")");
            if (professions.right != null) {
                query.append(",(");
                query.append(charid);
                query.append(", ");
                query.append(professions.right.getType().ordinal());
                query.append(", ");
                query.append(professions.right.getLevel().ordinal());
                query.append(", ");
                query.append(professions.right.getExp());
                query.append(")");
            }
            ps = con.prepareStatement(query.toString());
            ps.execute();
            ps.close();
        } catch (SQLException e) {
            System.out.println("Error."+e);
        }
    }
}
