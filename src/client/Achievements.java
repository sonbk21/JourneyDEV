/*
 * JourneyMS
 * Achievement System
 */
package client;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import tools.DatabaseConnection;

public final class Achievements {
    
    private final Map<Achievement, Integer> achievements = new LinkedHashMap<>();
    private int trophies = 0;
    
    public enum Achievement {
        
        MIL("Millionaire", "(Collect 1.000.000 Mesos from Monsters)"),
        TMIL("Veteran Collector", "(Collect 10.000.000 Mesos from Monsters)"),
        HMIL("Master Collector", "(Collect 100.000.000 Mesos from Monsters)"),
        BIL("Gold Richie", "(Collect 1.000.000.000 Mesos from Monsters)"),
        ROG("Tristan's Apprentice", "(Defeat Balrog)"),
        ZAK("Defender of El Nath", "(Defeat Zakum)"),
        HT("Dragonslayer", "(Defeat Horntail)"),
        PB("Hero of Time", "(Defeat Pink Bean)"),
        DOJO("Master of Mu Lung", "(Complete the Mu Lung Dojo)"),
        EASY("Bossing Apprentice", "(Finish Easy Boss PQ)"),
        MED("Bossing Veteran", "(Finish Medium Boss PQ)"),
        HARD("Bossing Expert", "(Finish Hard Boss PQ)"),
        HELL("Impossible Request", "(Finish Hell Boss PQ)");
        
        private final String name, desc;
        
        Achievement(String name, String desc) {
            this.name = name;
            this.desc = desc;
        }
        
        public String getText(boolean boldtext) {
            return ((boldtext)?"#e"+name+"#n":name)+" "+desc;
        }
    }
    
    public enum Status {
        END, START, NULL;
    }
    
    public boolean add(Achievement avm, int value) {
        if (achievements.containsKey(avm)) {
            if (achievements.get(avm) == -1)
                return false;
            if (checkCompletable(avm, value)) {
                achievements.put(avm, -1);
                trophies ++;
                return true;
            }
            achievements.put(avm, achievements.get(avm) + value);
            return false;
        }
        achievements.put(avm, value);
        if (value == -1)
            trophies ++;
        return value == -1;
    }
    
    private boolean checkCompletable(Achievement avm, int value) {
        int total = achievements.get(avm) + value;
        return (avm.equals(Achievement.MIL) && total >= 1000000) || (avm.equals(Achievement.TMIL) && total >= 10000000) || (avm.equals(Achievement.HMIL) && total >= 100000000) ||
               (avm.equals(Achievement.BIL) && total >= 1000000000);
    }
    
    public Status getStatus(Achievement avm) {
        return (achievements.containsKey(avm))?(achievements.get(avm) == -1)?Status.END:Status.START:Status.NULL;
    }
    
    public int getTrophies() {
        return trophies;
    }
    
    public void clearTrophies() {
        trophies = 0;
    }
    
    public String fullName(String abr) {
        return Achievement.valueOf(abr).getText(false);
    }
    
    public void loadAchievements(final int charid) throws SQLException {
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT name, progress FROM achievements WHERE charid = ?")) {
            ps.setInt(1, charid);
            try (ResultSet rs = ps.executeQuery()) {
                byte ord;
                int progress;
                while (rs.next()) {
                    ord = rs.getByte("name");
                    progress = rs.getInt("progress");
                    if (ord == -1) {
                        trophies = progress;
                        continue;
                    }
                    achievements.put(Achievement.values()[ord], progress);
                }
            }
        }
    }

    public void saveAchievements(final int charid) {
        if (achievements.isEmpty()) {
            return;
        }
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM achievements WHERE charid = ?");
            ps.setInt(1, charid);
            ps.execute();
            ps.close();
            boolean first = true;
            StringBuilder query = new StringBuilder();
            for (Map.Entry<Achievement, Integer> all : achievements.entrySet()) {
                if (first) {
                    query.append("INSERT INTO achievements VALUES (");
                    first = false;
                } else {
                    query.append(",(");
                }
                query.append(charid);
                query.append(", ");
                query.append(all.getKey().ordinal());
                query.append(", ");
                query.append(all.getValue());
                query.append(")");
            }
            query.append(",(").append(charid).append(", -1, ").append(trophies).append(")");
            ps = con.prepareStatement(query.toString());
            ps.execute();
            ps.close();
        } catch (SQLException e) {
            System.out.println("Error."+e);
        }
    }
    
    public String getNpcText(boolean showProgress) {
        StringBuilder ret = new StringBuilder();
        for (Map.Entry<Achievement, Integer> entry : achievements.entrySet()) {
            if (!showProgress && entry.getValue() == -1) {
                ret.append(entry.getKey().getText(true));
                ret.append("\r\n");
            } else if (showProgress && entry.getValue() != -1) {
                ret.append("#b");
                ret.append(entry.getKey().getText(false));
                ret.append("\r\n  #rProgress: ");
                ret.append(entry.getValue());
                ret.append("#k");
            }
        }
        return ret.toString();
    }
}
