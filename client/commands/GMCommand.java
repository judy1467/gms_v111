/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.commands;

import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.MapleClient;
import client.MapleStat;
import client.Skill;
import client.SkillFactory;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.ItemFlag;
import client.inventory.MapleInventoryType;
import client.commands.CommandProcessorUtil;
import client.commands.InternCommand.벤;
import client.commands.InternCommand.TempBan;
import constants.GameConstants;
import constants.ServerConstants.PlayerGMRank;
import handling.channel.ChannelServer;
import handling.world.World;
import java.util.Arrays;
import scripting.EventInstanceManager;
import scripting.EventManager;
import server.MapleCarnivalChallenge;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MapleShopFactory;
import server.events.MapleEvent;
import server.events.MapleEventType;
import server.life.MapleMonster;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import tools.packet.EtcPacket;
import tools.StringUtil;
import tools.packet.MaplePacketCreator;
import tools.packet.MaplePacketCreator.InventoryPacket;
import tools.packet.MobPacket;

/**
 *
 * @author Emilyx3
 */
public class GMCommand {

    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.GM;
    }




    public static class 스킬포인트 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().setRemainingSp(CommandProcessorUtil.getOptionalIntArg(splitted, 1, 1));
            c.getPlayer().updateSingleStat(MapleStat.AVAILABLESP, 0); // we don't care the value here
            return 1;
        }
    }

    public static class 직업 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (MapleCarnivalChallenge.getJobNameById(Integer.parseInt(splitted[1])).length() == 0) {
                c.getPlayer().dropMessage(5, "잘못된 직업");
                return 0;
            }
            c.getPlayer().changeJob(Integer.parseInt(splitted[1]));
            return 1;
        }
    }

    
        public static class 모두저장 extends CommandExecute {
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
            c.getPlayer().dropMessage(6, "서버 저장이 완료되었습니다.");
            return 1;
        }
    }  
   
    public static class 상점 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleShopFactory shop = MapleShopFactory.getInstance();
            int shopId = Integer.parseInt(splitted[1]);
            if (shop.getShop(shopId) != null) {
                shop.getShop(shopId).sendShop(c);
            } else {
                shop.getShop(1337).sendShop(c);
            }
            return 1;
        }
    }

    public static class 레벨업 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().getLevel() < 200) {
                c.getPlayer().gainExp(500000000, true, false, true);
            }
            return 1;
        }
    }

    public static class 아이템 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final int itemId = Integer.parseInt(splitted[1]);
            final short quantity = (short) CommandProcessorUtil.getOptionalIntArg(splitted, 2, 1);
            final short day = (short) CommandProcessorUtil.getOptionalIntArg(splitted, 3, 0);
            if (!c.getPlayer().isAdmin()) {
                for (int i : GameConstants.itemBlock) {
                    if (itemId == i) {
                        c.getPlayer().dropMessage(5, "이 아이템은 얻을 수 없습니다");
                        return 0;
                    }
                }
            }
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            if (GameConstants.isPet(itemId)) {
                c.getPlayer().dropMessage(5, "펫아이템은 캐시샵에서 구매해주세요");
            } else if (!ii.itemExists(itemId)) {
                c.getPlayer().dropMessage(5, itemId + " 은 존재하지 않는 아이템 코드입니다");
            } else {
                Item item;
                short flag = (short) ItemFlag.LOCK.getValue();

                if (GameConstants.getInventoryType(itemId) == MapleInventoryType.EQUIP) {
                    item = ii.oriStats((Equip) ii.getEquipById(itemId));
                } else {
                    item = new client.inventory.Item(itemId, (byte) 0, quantity, (byte) 0);

                }

                if (day != 0) {
                    item.setExpiration(System.currentTimeMillis() + (day * 37 * 60 * 60 * 1000));
                }
                MapleInventoryManipulator.addbyItem(c, item);
            }
            return 1;
        }
    }

    public static class 레벨 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().setLevel(Short.parseShort(splitted[1]));
            c.getPlayer().levelUp();
            if (c.getPlayer().getExp() < 0) {
                c.getPlayer().gainExp(-c.getPlayer().getExp(), false, false, true);
            }
            return 1;
        }
    }


    public static class 폭격 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap map = c.getPlayer().getMap();
            double range = Double.POSITIVE_INFINITY;
            int damage = 2147000000;
            MapleMonster mob;
            for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(MapleMapObjectType.MONSTER))) {
                mob = (MapleMonster) monstermo;
                map.broadcastMessage(MobPacket.damageMonster(mob.getObjectId(), damage));
                mob.damage(c.getPlayer(), damage, false);
            }
            return 1;
        }
    }

   


    public static class 몹리셋 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().killAllMonsters(false);
            return 1;
        }
    }

    public static class 공지 extends CommandExecute {

        protected static int getNoticeType(String typestring) {
            if (typestring.equals("n")) {
                return 0;
            } else if (typestring.equals("p")) {
                return 1;
            } else if (typestring.equals("l")) {
                return 2;
            } else if (typestring.equals("nv")) {
                return 5;
            } else if (typestring.equals("v")) {
                return 5;
            } else if (typestring.equals("b")) {
                return 6;
            }
            return -1;
        }

        @Override
        public int execute(MapleClient c, String[] splitted) {
            int joinmod = 1;
            int range = -1;
            if (splitted[1].equals("m")) {
                range = 0;
            } else if (splitted[1].equals("c")) {
                range = 1;
            } else if (splitted[1].equals("w")) {
                range = 2;
            }

            int tfrom = 2;
            if (range == -1) {
                range = 2;
                tfrom = 1;
            }
            int type = getNoticeType(splitted[tfrom]);
            if (type == -1) {
                type = 0;
                joinmod = 0;
            }
            StringBuilder sb = new StringBuilder();
            if (splitted[tfrom].equals("nv")) {
                sb.append("[공지]");
            } else {
                sb.append("");
            }
            joinmod += tfrom;
            sb.append(StringUtil.joinStringFrom(splitted, joinmod));

            byte[] packet = MaplePacketCreator.serverNotice(type, sb.toString());
            if (range == 0) {
                c.getPlayer().getMap().broadcastMessage(packet);
            } else if (range == 1) {
                ChannelServer.getInstance(c.getChannel()).broadcastPacket(packet);
            } else if (range == 2) {
                World.Broadcast.broadcastMessage(packet);
            }
            return 1;
        }
    }

    public static class 노란공지 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            int range = -1;
            if (splitted[1].equals("m")) {
                range = 0;
            } else if (splitted[1].equals("c")) {
                range = 1;
            } else if (splitted[1].equals("w")) {
                range = 2;
            }
            if (range == -1) {
                range = 2;
            }
            byte[] packet = MaplePacketCreator.yellowChat((splitted[0].equals("!y") ? ("[" + c.getPlayer().getName() + "] ") : "") + StringUtil.joinStringFrom(splitted, 2));
            if (range == 0) {
                c.getPlayer().getMap().broadcastMessage(packet);
            } else if (range == 1) {
                ChannelServer.getInstance(c.getChannel()).broadcastPacket(packet);
            } else if (range == 2) {
                World.Broadcast.broadcastMessage(packet);
            }
            return 1;
        }
    }

    public static class Y extends 노란공지 {
    }


    public static class TempBanIP extends TempBan {
        private final boolean ipBan;

        public TempBanIP() {
            ipBan = true;
        }
    }

    public static class 아이피벤 extends 벤 {
        private final boolean 아이피벤;

        public 아이피벤() {
            아이피벤 = true;
        }
    }

    public static class TDrops extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().toggleDrops();
            return 1;
        }
    }
    
    public static class rb extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().doReborn();
            return 1;
        }
    }
}
