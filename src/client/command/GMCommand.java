/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package client.command;

import client.MapleCharacter;
import client.MapleClient;
import client.properties.MapleJob;
import client.properties.Skill;
import client.properties.SkillFactory;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import constants.ItemConstants;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import net.server.Server;
import provider.MapleData;
import provider.MapleDataProviderFactory;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MaplePortal;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.maps.MapleMap;

/**
 * JourneyMS
 * 
 */
public class GMCommand extends Command {

    public GMCommand(MapleClient c, String[] args) {
        this.c = c;
        this.args = args;
    }
    
    @Override
    public void execute() {
        
        MapleCharacter chr = c.getPlayer();
        
        if (chr.getGMLevel() < 1) {
            chr.dropMessage("You may not use !");
            return;
        }
        
        switch(args[0]) {
            case "notice":
                String message = joinStringFrom(args, 1);
                Server.getInstance().gmChat(chr.getName() + " : " + message, null);
                break;
            case "goto":
                if (args.length < 2) {
                    chr.yellowMessage("Please use ~goto <keywords> or ~goto <mapid>");
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
            case "spawn":
                int num = Integer.valueOf(args[2]);
                if (!(num > 0))
                    num = 1;
                MapleMap curMap = chr.getMap();
                for (int i = 0; i < num; i++) {
                    MapleMonster mon = MapleLifeFactory.getMonster(Integer.valueOf(args[1]));
                    if (mon == null) {
                        chr.dropMessage(6, "Mob does not exist.");
                        break;
                    }
                    curMap.spawnMonster(mon);
                }
                break;
            case "id":
                try {
                    try (BufferedReader dis = new BufferedReader(new InputStreamReader(new URL("http://www.mapletip.com/search_java.php?search_value=" + args[1] + "&check=true").openConnection().getInputStream()))) {
                        String s;
                        while ((s = dis.readLine()) != null) {
                            chr.dropMessage(s);
                        }
                    }
                } catch (Exception e) {
                }
                break;
            case "item":
                int itemId = Integer.parseInt(args[1]);
                short quantity = 1;
                try {
                    quantity = Short.parseShort(args[2]);
                } catch (Exception e) {
                }
                if (args[0].equals("item")) {
                    int petid = -1;
                    if (ItemConstants.isPet(itemId)) {
                        petid = MaplePet.createPet(itemId);
                    }
                    MapleInventoryManipulator.addById(c, itemId, quantity, chr.getName(), petid, -1);
                } else {
                    Item toDrop;
                    if (MapleItemInformationProvider.getInstance().getInventoryType(itemId) == MapleInventoryType.EQUIP) {
                        toDrop = MapleItemInformationProvider.getInstance().getEquipById(itemId);
                    } else {
                        toDrop = new Item(itemId, (byte) 0, quantity);
                    }
                    c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), toDrop, c.getPlayer().getPosition(), true, true);
                }
                break;
            case "job":
                chr.changeJob(MapleJob.getById(Integer.parseInt(args[1])));
                chr.equipChanged();
                break;
            case "maxskills":
                for (MapleData skill_ : MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/" + "String.wz")).getData("Skill.img").getChildren()) {
                    try {
                        Skill skill = SkillFactory.getSkill(Integer.parseInt(skill_.getName()));
                        chr.changeSkillLevel(skill, (byte) skill.getMaxLevel(), skill.getMaxLevel(), -1);
                    } catch (NumberFormatException nfe) {
                        break;
                    } catch (NullPointerException npe) {
                    }
                }
                break;
            case "mesos":
                chr.gainMeso(Integer.parseInt(args[1]), true);
                break;
            case "startevent":
                for (MapleCharacter player : chr.getMap().getCharacters()) {
                    chr.getMap().startEvent(player);
                }
                c.getChannelServer().setEvent(null);
                break;
            case "servermessage":
                c.getWorldServer().setServerMessage(joinStringFrom(args, 1));
                break;
        }
    }
    
    private String joinStringFrom(String arr[], int start) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < arr.length; i++) {
            builder.append(arr[i]);
            if (i != arr.length - 1) {
                builder.append(" ");
            }
        }
        return builder.toString();
    }
}
