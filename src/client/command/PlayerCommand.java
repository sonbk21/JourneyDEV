/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package client.command;

import client.MapleCharacter;
import client.MapleClient;
import client.properties.MapleStat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import scripting.npc.NPCScriptManager;
import server.MaplePortal;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import tools.MaplePacketCreator;

/**
 * JourneyMS
 * 
 */
public class PlayerCommand extends Command {

    public PlayerCommand(MapleClient c, String[] args) {
        this.c = c;
        this.args = args;
    }
    
    @Override
    public void execute() {
        
        MapleCharacter chr = c.getPlayer();
        
        switch(args[0]) {
            case "dispose":
                NPCScriptManager.getInstance().dispose(c);
                c.announce(MaplePacketCreator.enableActions());
                chr.dropMessage(6, "Done.");
                break;
            case "help":
                chr.dropMessage(6, "Available Commands:");
                chr.dropMessage(6, "~dispose : Use it when you can't attack or talk to an NPC.");
                chr.dropMessage(6, "~info : Display information about your character.");
                chr.dropMessage(6, "~time : Display the server time.");
                chr.dropMessage(6, "~admin : Calls the mighty Maple Administrator. (Universal NPC)");
                chr.dropMessage(6, "~bosshp : Display the HP of nearby bosses.");
                if (chr.isSupremeWorld()) {
                    chr.dropMessage(6, "~goto <mapname> : Warps you to any map. (Supreme World only)");
                    chr.dropMessage(6, "~togglerebirth : Turns auto rebirth on or off. (Supreme World only)");
                    chr.dropMessage(6, "~<stat> <amount> : Adds ap into a stat. (Supreme World only)");
                }
                break;
            case "info":
                chr.dropMessage(6, "Vote Points: "+c.getVPoints());
                chr.dropMessage(6, "Cash: "+chr.getCashShop().getCash(1));
                chr.dropMessage(6, "Rank: "+chr.getRank());
                if (chr.getAbilities().getLines() > 0) {
                    String msg = "Inner Ability ("+chr.getAbilities().getName()+"): ";
                    for (byte i = 0; i < chr.getAbilities().getLines(); i++)
                        msg += chr.getAbilities().getLine(i).getName(chr.getAbilities().getRank())+"  ";
                    chr.dropMessage(6, msg);
                }
                break;
            case "time":
                final SimpleDateFormat time = new SimpleDateFormat("HH:mm:ss");
                chr.dropMessage(6, "Server Time: "+time.format(Calendar.getInstance().getTime()));
                break;
            case "admin":
                NPCScriptManager.getInstance().start(c, 9010000, "admin", chr);
                break;
            case "goto":
                if (!chr.isSupremeWorld()) {
                    chr.message("You may only use this command in the supreme World.");
                    break;
                }
                if (args.length < 2) {
                    chr.message("Please use ~goto <mapname>");
                }
                MapleMap target;
                try {
                    target = c.getChannelServer().getMapFactory().getMap(Integer.valueOf(args[1]));
                } catch (NumberFormatException nfe) {
                    target = c.getChannelServer().getMapFactory().searchMapByName(args);
                }
                if (target == null) {
                    chr.dropMessage(6, "Map does not exist.");
                    break;
                }
                MaplePortal targetPortal = target.getPortal(0);
                chr.changeMap(target, targetPortal);
                break;
            case "togglerebirth":
                if (!chr.isSupremeWorld()) {
                    chr.message("You may only use this command in the supreme World.");
                    break;
                }
                if (chr.toggleAutoRebirth())
                    chr.dropMessage(6, "Autorebirth is now turned ON.");
                else
                    chr.dropMessage(6, "Autorebirth is now turned OFF.");
                break;
            case "rebirth":
                if (!chr.isSupremeWorld()) {
                    chr.message("You may only use this command in the supreme World.");
                    break;
                }
                if (chr.getLevel() != 200) {
                    chr.message("You may only use this command after reaching level 200.");
                    break;
                }
                chr.doRebirth();
                break;
            case "bosshp":
                boolean found = false;
                List<MapleMapObject> monsters = chr.getMap().getMapObjectsInRange(c.getPlayer().getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.MONSTER));
                for (MapleMapObject mob : monsters) {
                    MapleMonster monster = (MapleMonster) mob;
                    if (monster.getStats().isBoss()) {
                        c.getPlayer().dropMessage(6, monster.getName()+" with "+formatHpOutput(monster.getHp())+"/"+formatHpOutput(monster.getMaxHp())+"HP");
                        found = true;
                    }
                }
                if (!found) {
                    c.getPlayer().dropMessage(6, "No monster was found.");
                }
                break;
            case "str":
            case "dex":
            case "int":
            case "luk":
                if (!chr.isSupremeWorld()) {
                    chr.message("You may only use this command in the supreme World.");
                    break;
                }
                try {
                    MapleStat type = MapleStat.getByString(args[0].toUpperCase());
                    if (c.getPlayer().getRemainingAp() >= Integer.valueOf(args[1])) {
                        c.getPlayer().addStat(type, Short.valueOf(args[1]));
                        c.getPlayer().setRemainingAp(c.getPlayer().getRemainingAp() - Integer.valueOf(args[1]));
                        c.getPlayer().updateSingleStat(MapleStat.AVAILABLEAP, c.getPlayer().getRemainingAp());
                    } else {
                        c.getPlayer().dropMessage(6, "You don't have enough AP left.");
                    }
                } catch (NumberFormatException nfe ){
                    c.getPlayer().dropMessage(6, "Please use ~<stat> <amount>.");
                }
                break;
            default:
                if (chr.getGMLevel() == 0) {
                    chr.yellowMessage("Player Command "+ args[0] + " does not exist.");
                }
        }
    }
    
    private static String formatHpOutput(long in) {
        StringBuilder output = new StringBuilder();
        String temp = Long.toString(in);
        int ind;
        for (ind = temp.length()-1; ind >= 0; ind --) {
            if ((temp.length()-1-ind)%3 == 0 && (temp.length()-1-ind) > 0)
                output.append(",");
            output.append(temp.charAt(ind));
        }
        temp = output.toString();
        output.delete(0, output.length());
        for (ind = temp.length()-1; ind >= 0; ind --) {
            output.append(temp.charAt(ind));
        }
        return output.toString();
    }
}
