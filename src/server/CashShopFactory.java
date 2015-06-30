/*
 * Copyright (C) 2015 SYJourney
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package server;

import client.inventory.Item;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import server.CashShop.CashItem;
import server.CashShop.SpecialCashItem;
import tools.DatabaseConnection;
import tools.Pair;

/**
 * Author: SYJourney
 * This file is part of the Journey MMORPG Server
 */
 
public class CashShopFactory {

    private final Map<Integer, CashItem> items = new HashMap<>();
    private final Map<Integer, List<Integer>> blockedItems = new HashMap<>();
    private final List<Pair<Integer, Integer>> bestitems = new ArrayList<>();
    private final Map<Integer, List<Integer>> packages = new HashMap<>();
    private final AtomicInteger blockedCSLength;
    
    private static CashShopFactory instance = null;
    
    public static CashShopFactory getInstance() {
        if (instance == null) {
            instance = new CashShopFactory();
        }
        return instance;
    }
    
    private CashShopFactory() {
        MapleDataProvider etc = MapleDataProviderFactory.getDataProvider(new File("wz/Etc.wz"));
        MapleData blockedCS = MapleDataProviderFactory.getDataProvider(new File("wz/Journey.wz")).getData("BlockedCS.img");
        blockedCSLength = new AtomicInteger(0);
        
        blockedCS.getChildren().stream().map((item) -> MapleDataTool.getInt(item)).forEach((id) -> blockedItems.put(id, null));
        etc.getData("Commodity.img").getChildren().stream().forEach((item) -> {
            int sn = MapleDataTool.getIntConvert("SN", item);
            int itemId = MapleDataTool.getIntConvert("ItemId", item);
            int price = MapleDataTool.getIntConvert("Price", item, 0);
            long period = MapleDataTool.getIntConvert("Period", item, 1);
            short count = (short) MapleDataTool.getIntConvert("Count", item, 1);
            boolean onSale = MapleDataTool.getIntConvert("OnSale", item, 0) == 1;
            items.put(sn, new CashItem(sn, itemId, price, period, count, onSale));
            if (blockedItems.containsKey(itemId) && onSale && sn/10000000 > 3) {
                List<Integer> SNs;
                if (blockedItems.get(itemId) == null) {
                    SNs = new LinkedList<>();
                } else {
                    SNs = blockedItems.get(itemId);
                }
                SNs.add(sn);
                blockedItems.put(itemId, SNs);
                blockedCSLength.incrementAndGet();
            }
        });
        
        etc.getData("CashPackage.img").getChildren().stream().forEach((cpackage) -> {
            final List<Integer> pkitems = new ArrayList<>();
            cpackage.getChildByPath("SN").getChildren().stream().forEach((item) -> pkitems.add(MapleDataTool.getInt(item)));
            packages.put(Integer.parseInt(cpackage.getName()), pkitems);
        });
        
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = DatabaseConnection.getConnection().prepareStatement("SELECT * FROM bestitems ORDER BY rank ASC");
            rs = ps.executeQuery();
            while (rs.next()) {
                bestitems.add( new Pair(rs.getInt("sn"), 1));
            }
            byte size = (byte) bestitems.size();
            if (size < 5) {
                for (byte i = 0; i < 5 - size; i++) {
                    bestitems.add( new Pair(30100000, 1));
                }
            }
        } catch (SQLException ex) {
            System.out.println(ex);
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
            } catch (SQLException ex) {
            }
        }
    }

    public CashItem getItem(int sn) {
        return items.get(sn);
    }
    
    public boolean isRemoved(int itemid) {
        return blockedItems.containsKey(itemid);
    }
        
    public void incrementPurchases(int sn) {
        for (byte i = 0; i < bestitems.size(); i++) {
            if (bestitems.get(i).getLeft() == sn) {
                bestitems.add(i, new Pair(sn, bestitems.get(i).getRight() + 1));
                bestitems.remove(i+1);
                return;
            }
        }
        bestitems.add( new Pair(sn, 1));
    }
        
    public int getBestItem(int rank) {
        return bestitems.get(rank).getLeft();
    }
        
    public Stream<Stream<Integer>> getBlockedItems() {
        return blockedItems.values().stream().filter((item) -> item != null).map((item) -> item.stream());
    }
        
    public int getBlockedItemsLength() {
        return blockedCSLength.get();
    }

    public List<Item> getPackage(int itemId) {
        List<Item> cashPackage = new ArrayList<>();
        packages.get(itemId).stream().forEach((sn) -> cashPackage.add(getItem(sn).toItem()));
        return cashPackage;
    }

    public boolean isPackage(int itemId) {
        return packages.containsKey(itemId);
    }
    
    public void saveBestItems() {
        Collections.sort(bestitems, (b, a) -> a.getRight().compareTo(b.getRight()));
        
        PreparedStatement ps = null;
        try {
            Connection con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("DELETE FROM bestitems");
            ps.execute();
            
            ps = con.prepareStatement("INSERT INTO bestitems VALUES (?, ?)");
            for (byte i = 0; i < 5; i++) {
                ps.setInt(1, i);
                ps.setInt(2, bestitems.get(i).getLeft());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException sqle) {
            System.out.println(sqle);
        } finally {
            try {
                if (ps != null) ps.close();
            } catch (SQLException sqle) {
                
            }
        }
    }
}
