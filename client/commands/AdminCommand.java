package client.commands;

import java.util.concurrent.ScheduledFuture;
import client.MapleCharacter;
import constants.ServerConstants.PlayerGMRank;
import client.MapleClient;
import client.MapleStat;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;

import java.util.concurrent.ScheduledFuture;
import client.MapleCharacter;
import constants.ServerConstants.PlayerGMRank;
import client.MapleClient;
import client.MapleStat;
import client.Skill;
import client.SkillFactory;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import constants.ServerConstants;

import handling.channel.ChannelServer;
import handling.world.World;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import scripting.NPCScriptManager;
import server.ItemInformation;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.ShutdownServer;
import server.Timer.EventTimer;
import server.maps.MapleMap;
import server.quest.MapleQuest;
import tools.CPUSampler;
import tools.Pair;
import tools.StringUtil;
import tools.packet.EtcPacket;
import tools.packet.MaplePacketCreator;


import handling.channel.ChannelServer;
import handling.world.World;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import scripting.NPCScriptManager;
import server.ItemInformation;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.ShutdownServer;
import server.Timer.EventTimer;
import server.life.MapleMonster;
import tools.CPUSampler;
import tools.Pair;
import tools.packet.EtcPacket;
import tools.packet.MaplePacketCreator;

/**
 *
 * @author Emilyx3
 */
public class AdminCommand {

    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.ADMIN;
    }

    
            public static class 점검공지 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            int type = Integer.parseInt(splitted[1]);
            if (type == 1) {
                World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(1, "안녕하세요. 플레이스타 입니다. 잠시후 " + splitted[2] + "뒤 서버패치가 있을예정이오니, 모든 유저분 께서는 클라이언트를 종료해주시길 바랍니다. 게임이용에 불편을 드려서 죄송합니다."));
                World.Broadcast.broadcastMessage(MaplePacketCreator.serverMessage("안녕하세요. 플레이스타 입니다. 잠시후 " + splitted[2] + "뒤 서버패치가 있을예정이오니, 모든 유저분 께서는 클라이언트를 종료해주시길 바랍니다. 게임이용에 불편을 드려서 죄송합니다."));
            } else if (type == 2) {
                World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, "위에 사전에 공지하였듯이 클라이언트에서 로그아웃하지않고 계속이용 하시는경우에 데이터가 손실되어도 복구 처리는 도와드리지않습니다."));
            }
            return 1;
        }

    }
                  public static class 전체저장 extends CommandExecute {
        @Override
        public int execute(MapleClient c, String[] splitted) {
            // User Data Save Start
         for (ChannelServer ch : ChannelServer.getAllInstances())
             for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters())
            chr.saveToDB(true, true);
            // User Data Save End
            // Server Data Save Start
            World.Guild.save();
            World.Alliance.save();
            World.Family.save();
            // Server Data Save End
            c.getPlayer().dropMessage(6, "저장이 완료되었습니다.");
            return 1;
        }
    }    
   
 public static class 서버종료 extends CommandExecute {

        protected static Thread t = null;

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(6, "서버를 종료합니다");
            
            if (t == null || !t.isAlive()) {
                         for (ChannelServer ch : ChannelServer.getAllInstances())
             for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters())
            chr.saveToDB(true, true);
            // User Data Save End
            // Server Data Save Start
            World.Guild.save();
            World.Alliance.save();
            World.Family.save();
                t = new Thread(ShutdownServer.getInstance());
				ShutdownServer.getInstance().shutdown();
                t.start();
            } else {
                c.getPlayer().dropMessage(6, "잠시 후, 서버가 종료됩니다. 모든 내용을 저장해주세요");
            }
            return 1;
        }
    }

    public static class 서버종료시간 extends 서버종료 {

        private static ScheduledFuture<?> ts = null;
        private int minutesLeft = 0;

        @Override
        public int execute(MapleClient c, String[] splitted) {
            minutesLeft = Integer.parseInt(splitted[1]);
            c.getPlayer().dropMessage(6, "서버 종료까지 약" + minutesLeft + "분 남았습니다.");
            if (ts == null && (t == null || !t.isAlive())) {
                         for (ChannelServer ch : ChannelServer.getAllInstances())
             for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters())
            chr.saveToDB(true, true);
            // User Data Save End
            // Server Data Save Start
            World.Guild.save();
            World.Alliance.save();
            World.Family.save();
                t = new Thread(ShutdownServer.getInstance());
                ts = EventTimer.getInstance().register(new Runnable() {

                    public void run() {
                        if (minutesLeft == 0) {
							ShutdownServer.getInstance().shutdown();
                            t.start();
                            ts.cancel(false);
							return;
                        }
                        World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(0, "서버가 " + minutesLeft + "분 후 종료됩니다. 안전하게 로그오프해주세요. 재시작 일정인 경우, 알림이나 공지사항을 참조해 주시기 바랍니다."));
                        minutesLeft--;
                    }
                }, 60000);
            } else {
                c.getPlayer().dropMessage(6, "잠시 후, 서버가 종료됩니다. 모든 내용을 저장해주세요");
            }
            return 1;
        }
    }
    public static class 전체메소 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                for (MapleCharacter mch : cserv.getPlayerStorage().getAllCharacters()) {
                    mch.gainMeso(Integer.parseInt(splitted[1]), true);
                }
            }
            return 1;
        }
    }
    
   
    
    public static class PC방배율 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length > 1) {
                final int rate = Integer.parseInt(splitted[1]);
                if (splitted.length > 2 && splitted[2].equalsIgnoreCase("all")) {
                    MapleMonster.setPC(rate);
                } else {
                    MapleMonster.setPC(rate);
                }
                c.getPlayer().dropMessage(6, "피시방경험치퍼센트 " + rate + "%");
            } else {
                c.getPlayer().dropMessage(6, "예제: !PC방배율 < 배율(숫자) > (%단위)");
            }
            return 1;
        }
    }

   public static class 전체종료 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            int range = -1;
            if (splitted[1].equals("맵")) {
                range = 0;
            } else if (splitted[1].equals("채널")) {
                range = 1;
            } else if (splitted[1].equals("월드")) {
                range = 2;
            } else {
                c.getPlayer().dropMessage(6, "!전체DC [맵][채널][월드]");
            }
            if (range == -1) {
                range = 1;
            }
            if (range == 0) {
                c.getPlayer().getMap().disconnectAll();
            } else if (range == 1) {
                c.getChannelServer().getPlayerStorage().disconnectAll(true);
            } else if (range == 2) {
                for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                    cserv.getPlayerStorage().disconnectAll(true);
                }
            }
            return 1;
        }
    }
   
   public static class 좌표 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(5, "맵 : " + c.getPlayer().getMap().getId());
            c.getPlayer().dropMessage(5, "x : " + c.getPlayer().getPosition().x);
            c.getPlayer().dropMessage(5, "y : " + c.getPlayer().getPosition().y);
            c.getPlayer().dropMessage(5, "Cy : " + c.getPlayer().getPosition().y);
            c.getPlayer().dropMessage(5, "Rx0 : x 값에서 + 50 계산 값 : " + (c.getPlayer().getPosition().x + 50));
            c.getPlayer().dropMessage(5, "Rx1 : x 값에서 - 50 계산 값 : " + (c.getPlayer().getPosition().x - 50));
            c.getPlayer().dropMessage(5, "Fh : " + c.getPlayer().getMap().getFootholds().findBelow(c.getPlayer().getPosition()).getId());
            return 1;
        }
    }
   
   public static class 엔피시삭제 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().resetNPCs();
            return 1;
        }
    }

  
    public static class 버닝 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (!c.getPlayer().getClient().getChannelServer().isBurning()) {
                    c.getPlayer().getClient().getChannelServer().setBurning(true);
                    c.getPlayer().dropMessage(1, "버닝 켜짐");
                    World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, "현재 " + c.getPlayer().getClient().getChannel() + "채널에서 버닝이벤트가 진행되고 있습니다."));
                    World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, "이벤트 기간동안은 얻는 경험치의 1.5배 만큼 더 받게 됩니다."));
            } else {
                    c.getPlayer().getClient().getChannelServer().setBurning(false);
                    c.getPlayer().dropMessage(1, "버닝 꺼짐");
                    World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, "" + c.getPlayer().getClient().getChannel() + "채널 버닝이벤트가 종료되었습니다."));
            }
            return 1;
        }
    }
 
    public static class 맥스스킬 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().Maxskill();
            return 1;
        }
    }
    
    public static class 검색 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length == 1) {
                c.getPlayer().dropMessage(6, splitted[0] + ": <엔피시> <몹> <아이템> <맵> <스킬> <퀘스트>");
            } else if (splitted.length == 2) {
                c.getPlayer().dropMessage(6, "제공된 코드으로만 입력이 가능합니다. 현재 가능한 코드는 <엔피시> <몹> <아이템> <맵> <스킬> <퀘스트> 입니다.");
            } else {
                String type = splitted[1];
                String search = StringUtil.joinStringFrom(splitted, 2);
                MapleData data = null;
                MapleDataProvider dataProvider = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/" + "String.wz"));
                c.getPlayer().dropMessage(6, "<< 종류 : " + type + " | 검색어 : " + search + ">>");

                if (type.equalsIgnoreCase("엔피시")) {
                    List<String> retNpcs = new ArrayList<String>();
                    data = dataProvider.getData("Npc.img");
                    List<Pair<Integer, String>> npcPairList = new LinkedList<Pair<Integer, String>>();
                    for (MapleData npcIdData : data.getChildren()) {
                        npcPairList.add(new Pair<Integer, String>(Integer.parseInt(npcIdData.getName()), MapleDataTool.getString(npcIdData.getChildByPath("name"), "NO-NAME")));
                    }
                    for (Pair<Integer, String> npcPair : npcPairList) {
                        if (npcPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                            retNpcs.add(npcPair.getLeft() + " - " + npcPair.getRight());
                        }
                    }
                    if (retNpcs != null && retNpcs.size() > 0) {
                        for (String singleRetNpc : retNpcs) {
                            c.getPlayer().dropMessage(6, singleRetNpc);
                        }
                    } else {
                        c.getPlayer().dropMessage(6, "검색된 엔피시가 없습니다.");
                    }

                } else if (type.equalsIgnoreCase("맵")) {
                    List<String> retMaps = new ArrayList<String>();
                    data = dataProvider.getData("Map.img");
                    List<Pair<Integer, String>> mapPairList = new LinkedList<Pair<Integer, String>>();
                    for (MapleData mapAreaData : data.getChildren()) {
                        for (MapleData mapIdData : mapAreaData.getChildren()) {
                            mapPairList.add(new Pair<Integer, String>(Integer.parseInt(mapIdData.getName()), MapleDataTool.getString(mapIdData.getChildByPath("streetName"), "NO-NAME") + " - " + MapleDataTool.getString(mapIdData.getChildByPath("mapName"), "NO-NAME")));
                        }
                    }
                    for (Pair<Integer, String> mapPair : mapPairList) {
                        if (mapPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                            retMaps.add(mapPair.getLeft() + " - " + mapPair.getRight());
                        }
                    }
                    if (retMaps != null && retMaps.size() > 0) {
                        for (String singleRetMap : retMaps) {
                            c.getPlayer().dropMessage(6, singleRetMap);
                        }
                    } else {
                        c.getPlayer().dropMessage(6, "검색된 맵이 없습니다.");
                    }
                } else if (type.equalsIgnoreCase("몹")) {
                    List<String> retMobs = new ArrayList<String>();
                    data = dataProvider.getData("Mob.img");
                    List<Pair<Integer, String>> mobPairList = new LinkedList<Pair<Integer, String>>();
                    for (MapleData mobIdData : data.getChildren()) {
                        mobPairList.add(new Pair<Integer, String>(Integer.parseInt(mobIdData.getName()), MapleDataTool.getString(mobIdData.getChildByPath("name"), "NO-NAME")));
                    }
                    for (Pair<Integer, String> mobPair : mobPairList) {
                        if (mobPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                            retMobs.add(mobPair.getLeft() + " - " + mobPair.getRight());
                        }
                    }
                    if (retMobs != null && retMobs.size() > 0) {
                        for (String singleRetMob : retMobs) {
                            c.getPlayer().dropMessage(6, singleRetMob);
                        }
                    } else {
                        c.getPlayer().dropMessage(6, "검색된 아이템이 없습니다.");
                    }

                } else if (type.equalsIgnoreCase("아이템")) {
                    List<String> retItems = new ArrayList<String>();
                    for (ItemInformation itemPair : MapleItemInformationProvider.getInstance().getAllItems()) {
                        if (itemPair != null && itemPair.name != null && itemPair.name.toLowerCase().contains(search.toLowerCase())) {
                            retItems.add(itemPair.itemId + " - " + itemPair.name);
                        }
                    }
                    if (retItems != null && retItems.size() > 0) {
                        for (String singleRetItem : retItems) {
                            c.getPlayer().dropMessage(6, singleRetItem);
                        }
                    } else {
                        c.getPlayer().dropMessage(6, "검색된 아이템이 없습니다.");
                    }
                } else if (type.equalsIgnoreCase("퀘스트")) {
                    List<String> retItems = new ArrayList<String>();
                    for (MapleQuest itemPair : MapleQuest.getAllInstances()) {
                        if (itemPair.getName().length() > 0 && itemPair.getName().toLowerCase().contains(search.toLowerCase())) {
                            retItems.add(itemPair.getId() + " - " + itemPair.getName());
                        }
                    }
                    if (retItems != null && retItems.size() > 0) {
                        for (String singleRetItem : retItems) {
                            c.getPlayer().dropMessage(6, singleRetItem);
                        }
                    } else {
                        c.getPlayer().dropMessage(6, "검색된 스킬이 없습니다.");
                    }
                } else if (type.equalsIgnoreCase("스킬")) {
                    List<String> retSkills = new ArrayList<String>();
                    for (Skill skil : SkillFactory.getAllSkills()) {
                        if (skil.getName() != null && skil.getName().toLowerCase().contains(search.toLowerCase())) {
                            retSkills.add(skil.getId() + " - " + skil.getName());
                        }
                    }
                    if (retSkills != null && retSkills.size() > 0) {
                        for (String singleRetSkill : retSkills) {
                            c.getPlayer().dropMessage(6, singleRetSkill);
                        }
                    } else {
                        c.getPlayer().dropMessage(6, "검색된 스킬이 없습니다.");
                    }
                } else {
                    c.getPlayer().dropMessage(6, "해당 검색은 처리할 수 없습니다.");
                }
            }
            return 0;
        }
    } 

    public static class 맥스스탯 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getStat().setStr((short)32767, c.getPlayer());
            c.getPlayer().getStat().setDex((short)32767, c.getPlayer());
            c.getPlayer().getStat().setInt((short)32767, c.getPlayer());
            c.getPlayer().getStat().setLuk((short)32767, c.getPlayer());
            
            c.getPlayer().getStat().setMaxHp((short)99999, c.getPlayer());
            c.getPlayer().getStat().setMaxMp((short)99999, c.getPlayer());
            
            c.getPlayer().updateSingleStat(MapleStat.STR, 32767);
            c.getPlayer().updateSingleStat(MapleStat.DEX, 32767);
            c.getPlayer().updateSingleStat(MapleStat.INT, 32767);
            c.getPlayer().updateSingleStat(MapleStat.LUK, 32767);
            
            c.getPlayer().updateSingleStat(MapleStat.MAXHP, 99999);
            c.getPlayer().updateSingleStat(MapleStat.MAXMP, 99999);
            return 1;
        }
    }
}
