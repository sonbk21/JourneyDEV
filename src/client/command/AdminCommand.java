/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package client.command;

import client.MapleCharacter;
import client.MapleClient;
import constants.ServerConstants;
import net.server.Server;
import server.MapleRewardManager;
import server.MapleRewardManager.RewardEvent;
import server.TimerManager;
import server.life.MapleLifeFactory;
import server.life.MapleNPC;
import tools.MaplePacketCreator;

/**
 * JourneyMS
 * 
 */
public class AdminCommand extends Command {

    public AdminCommand(MapleClient c, String[] args) {
        this.c = c;
        this.args = args;
    }
    
    @Override
    public void execute() {
        
        MapleCharacter chr = c.getPlayer();
        
        if (chr.getGMLevel() < 2 && !c.getAccountName().equals("Risa")) {
            chr.dropMessage("You may not use @");
            return;
        }
        
        switch(args[0]) {
            case "npc":
                if (args.length < 1) {
                    break;
                }
                MapleNPC npc = MapleLifeFactory.getNPC(Integer.parseInt(args[1]));
                if (npc != null) {
                    npc.setPosition(chr.getPosition());
                    npc.setCy(chr.getPosition().y);
                    npc.setRx0(chr.getPosition().x + 50);
                    npc.setRx1(chr.getPosition().x - 50);
                    npc.setFh(chr.getMap().getFootholds().findBelow(c.getPlayer().getPosition()).getId());
                    chr.getMap().addMapObject(npc);
                    chr.getMap().broadcastMessage(MaplePacketCreator.spawnNPC(npc));
                }
                break;
            case "setgmlevel":
                MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(args[1]);
                victim.setGM(Integer.parseInt(args[2]));
                chr.message("Done.");
                victim.getClient().disconnect(false, false);
                break;
            case "shutdown":
                int time = 60000;
                if (args[0].equals("shutdownnow")) {
                    time = 1;
                } else if (args.length > 1) {
                    time *= Integer.parseInt(args[1]);
                }
                TimerManager.getInstance().schedule(Server.getInstance().shutdown(false), time);
                break;
            case "reload":
                try {
                    MapleRewardManager.getInstance().loadEventRewards(RewardEvent.valueOf(args[1]), c.getWorld() == ServerConstants.SUPREME_WORLD);
                } catch (Exception e) {
                    chr.dropMessage(6, "Failed.");
                }
                break;
            default:
                chr.yellowMessage("Command " + args[0] + " does not exist.");
                break;
        }
    }
}
