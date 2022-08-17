package client.commands;

//import client.MapleInventory;
//import client.MapleInventoryType;
import client.inventory.Item;
import server.RankingWorker;
import client.MapleCharacter;
import constants.ServerConstants.PlayerGMRank;
import client.MapleClient;
import client.MapleStat;
import client.inventory.MapleInventoryType;
import client.commands.CommandExecute.PokemonExecute;
import client.commands.CommandExecute.TradeExecute;
import constants.BattleConstants.PokemonItem;
import constants.GameConstants;
import handling.channel.ChannelServer;
import handling.world.World;
import java.util.Arrays;
import java.util.List;
import scripting.NPCScriptManager;
import server.*;
import server.RankingWorker.RankingInformation;
import server.life.MapleMonster;
import server.maps.FieldLimitType;


import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.SavedLocationType;
import tools.FileoutputUtil;
import tools.StringUtil;
import tools.packet.MaplePacketCreator;

/**
 *
 * @author Emilyx3
 */
public class PlayerCommand {

      public static class 랙 extends PlayerCommand.렉 {}
    public static class fpr extends PlayerCommand.렉 {}
    public static class For extends PlayerCommand.렉 {}
    public static class 렉 extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            c.removeClickedNPC();
            NPCScriptManager.getInstance().dispose(c);
            c.getSession().write(MaplePacketCreator.enableActions());
            return 1;
        }    
    }
    
       public static class 이동 extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            NPCScriptManager.getInstance().start(c, 1012000);
            return 1;
        }
    }
    

    public static class 마을 extends PlayerCommand.광장 {}
    public static class 광장 extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            final MapleCharacter chr = c.getPlayer();
            final int mapID = chr.getMapId();
            if (c.getPlayer().getLevel() < 10) {
                c.getPlayer().dropMessage(5, "레벨 10 이상만 광장 명령어를 사용할 수 있습니다.");
                return 0;
            }
            if (chr.getMapId() == 910000000) {
                chr.changeMap(ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(chr.getSavedLocation(SavedLocationType.FREE_MARKET)));
                return 1;
            }
            for (int i : GameConstants.blockedMaps) {
                if (mapID == i) {
                    c.getPlayer().dropMessage(5, "이곳에서는 광장 명령어를 사용할 수 없습니다.");
                    return 0;
                }
            }
            if (c.getPlayer().hasBlockedInventory()
                    || c.getPlayer().getMap().getSquadByMap() != null
                    || c.getPlayer().getEventInstance() != null
                    || c.getPlayer().getMap().getEMByMap() != null
                    || mapID >= 990000000
                    || FieldLimitType.VipRock.check(c.getPlayer().getMap().getFieldLimit())) {
                c.getPlayer().dropMessage(5, "이곳에서는 광장 명령어를 사용할 수 없습니다.");
                return 0;
            }
            if ((mapID >= 680000210 && mapID <= 680000502)
                    || (mapID / 1000 == 980000 && mapID != 980000000)
                    || (mapID / 100 == 1030008)
                    || (mapID / 100 == 922010)
                    || (mapID / 10 == 13003000)
                    || (mapID < 100000000)
                    || (mapID >= 910000001 && mapID <= 910000022)
                    || (mapID >= 910000001 && mapID <= 910000022)) {
                
                c.getPlayer().dropMessage(5, "이곳에서는 광장 명령어를 사용할 수 없습니다.");
                return 0;
            }
            chr.saveLocation(SavedLocationType.FREE_MARKET, chr.getMapId());
            chr.changeMap(ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(123456789));
            chr.dropMessage(6, "광장으로 이동합니다.");
            return 1;
        }
    }
    
public static class 원정대소환 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().getMapId() == 280030000 || c.getPlayer().getMapId() == 280030001 || //자쿰맵
                c.getPlayer().getMapId() == 240060200 || c.getPlayer().getMapId() == 240060201) //혼테일맵
            {
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            /*if (victim.getLevel() < 200) {
                c.getPlayer().dropMessage(6, "레벨 200 미만의 캐릭터를 소환할 수 없습니다.");
                return 0;                
            }*/
            if (victim != null) {
                if ((c.getPlayer().getGMLevel() < 2 && (victim.isInBlockedMap() || victim.getGMLevel() > 1))) { //GM 레벨이 높은 유저 소환 불가
                    c.getPlayer().dropMessage(6, "다시 시도해 주세요.");
                    return 0;
                }
                victim.changeMap(c.getPlayer().getMap(), c.getPlayer().getMap().findClosestPortal(c.getPlayer().getTruePosition()));
            } else {
                int ch = World.Find.findChannel(splitted[1]);
                if (ch < 0) { //채널 오류
                    c.getPlayer().dropMessage(6, "다시 시도해 주세요.");
                    return 0;
                }
                victim = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim == null || (c.getPlayer().getGMLevel() < 2 && (victim.isInBlockedMap() || victim.getGMLevel() > 1))) { //GM 레벨이 높은 유저 소환 불가
                    c.getPlayer().dropMessage(6, "다시 시도해 주세요.");
                    return 0;
                }
                c.getPlayer().dropMessage(6, "해당 유저가 채널을 이동 중입니다.");
                victim.dropMessage(6, "채널 이동 중입니다.");
                if (victim.getMapId() != c.getPlayer().getMapId()) {
                    final MapleMap mapp = victim.getClient().getChannelServer().getMapFactory().getMap(c.getPlayer().getMapId());
                    victim.changeMap(mapp, mapp.findClosestPortal(c.getPlayer().getTruePosition()));
                }
                victim.changeChannel(c.getChannel());
                c.getPlayer().dropMessage(6, splitted[1] + "유저를 소환합니다.");
            }
            } else {
                c.getPlayer().dropMessage(6, "보스맵이 아닌 곳에서는 소환을 할 수 없습니다.");                
                return 0;
            }
            return 1;
        }
    }  
    
    
    public static class 독안개 extends CommandExecute {
        public int execute(MapleClient c, String[] splitted) {
            World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, " [모집] " + c.getPlayer().getName() + " 님이 " + c.getChannel() + "채널 에서 독안개숲 파티퀘스트 멤버를 찾고있습니다."));
            return 1;
        }
    }
    
    
    
    public static class 커닝 extends CommandExecute {
        public int execute(MapleClient c, String[] splitted) {
            World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, " [모집] " + c.getPlayer().getName() + " 님이 " + c.getChannel() + "채널 에서 커닝 파티퀘스트 멤버를 찾고있습니다."));
            return 1;
        }
    }
    
        public static class 월묘 extends CommandExecute {
        public int execute(MapleClient c, String[] splitted) {
            World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, " [모집] " + c.getPlayer().getName() + " 님이 " + c.getChannel() + "채널 에서 월묘의떡 파티퀘스트 멤버를 찾고있습니다."));
            return 1;
        }
    }
    
        public static class 카니발 extends CommandExecute {
        public int execute(MapleClient c, String[] splitted) {
            World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, " [모집] " + c.getPlayer().getName() + " 님이 " + c.getChannel() + "채널 에서 몬스터카니발 파티퀘스트 멤버를 찾고있습니다."));
            return 1;
        }
    } 
        
        public static class 길대 extends CommandExecute {
        public int execute(MapleClient c, String[] splitted) {
            World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, " [모집] " + c.getPlayer().getName() + " 님이 " + c.getChannel() + "채널 에서 길드대항전 멤버를 찾고있습니다."));
            return 1;
        }
    }                

        public static class 로미오 extends CommandExecute {
        public int execute(MapleClient c, String[] splitted) {
            World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, " [모집] " + c.getPlayer().getName() + " 님이 " + c.getChannel() + "채널 에서 로미오와줄리엣 파티퀘스트 멤버를 찾고있습니다."));
            return 1;
        }
    }

        public static class 자쿰 extends CommandExecute {
        public int execute(MapleClient c, String[] splitted) {
            World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, " [모집] " + c.getPlayer().getName() + " 님이 " + c.getChannel() + "채널 에서 자쿰 원정대 멤버를 찾고있습니다."));
            return 1;
        }
    }

        public static class 혼테일 extends CommandExecute {
        public int execute(MapleClient c, String[] splitted) {
            World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, " [모집] " + c.getPlayer().getName() + " 님이 " + c.getChannel() + "채널 에서 혼테일 원정대 멤버를 찾고있습니다."));
            return 1;
        }
    }        
        
        public static class 핑크빈 extends CommandExecute {
        public int execute(MapleClient c, String[] splitted) {
            World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, " [모집] " + c.getPlayer().getName() + " 님이 " + c.getChannel() + "채널 에서 핑크빈 원정대 멤버를 찾고있습니다."));
            return 1;
        }
    }   

        public static class 루시드 extends CommandExecute {
        public int execute(MapleClient c, String[] splitted) {
            World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, " [모집] " + c.getPlayer().getName() + " 님이 " + c.getChannel() + "채널 에서 루시드 레이드 멤버를 찾고있습니다."));
            return 1;
        }
    }
        
       
        public static class 루디 extends CommandExecute {
        public int execute(MapleClient c, String[] splitted) {
            World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, " [모집] " + c.getPlayer().getName() + " 님이 " + c.getChannel() + "채널 에서 루디브리엄 파티퀘스트 멤버를 찾고있습니다."));
            return 1;
        }
    }

        public static class 오르비스 extends CommandExecute {
        public int execute(MapleClient c, String[] splitted) {
            World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, " [모집] " + c.getPlayer().getName() + " 님이 " + c.getChannel() + "채널 에서 여신의흔적 파티퀘스트 멤버를 찾고있습니다."));
            return 1;
        }
    }         

        public static class 파티사냥 extends CommandExecute {
        public int execute(MapleClient c, String[] splitted) {
            World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, " [모집] " + c.getPlayer().getName() + " 님이 " + c.getChannel() + "채널 에서 파티사냥 멤버를 찾고있습니다."));
            return 1;
        }
    }

        public static class 데비존 extends CommandExecute {
        public int execute(MapleClient c, String[] splitted) {
            World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, " [모집] " + c.getPlayer().getName() + " 님이 " + c.getChannel() + "채널 에서 해적 데비존 파티퀘스트 멤버를 찾고있습니다."));
            return 1;
        }
    }
    
            public static class 명령어 extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(5, "@힘, @덱스, @인트, @럭 스텟을 올려줍니다.");
            c.getPlayer().dropMessage(5, "@광장,마을 : 플레이스타 광장으로 이동합니다.");
            c.getPlayer().dropMessage(5, "@이동 : 이동엔피시 출력");
            c.getPlayer().dropMessage(5, "@저장 - 현재 나의 모든정보를 저장합니다.");
            c.getPlayer().dropMessage(5, "@온라인 - 현재 서버내의 접속자를 알려줍니다.");
            c.getPlayer().dropMessage(5, "@캐시충전 - 캐시를 충전합니다. 100메소 = 1 캐시 (레벨 20 부터 사용가능)");
            c.getPlayer().dropMessage(5, "@렉,for,렉,fpr - 공격,엔피시대화 등이 먹통일때 사용.");
            c.getPlayer().dropMessage(5, "@정보 - 맵에 몬스터 드롭 정보를알려주는 기능");
            c.getPlayer().dropMessage(5, "@실뎀 - 실제데미지를 알려주는명령어 [온/오프]");
            c.getPlayer().dropMessage(5, "@월묘 : 월묘의떡 멤버 모집");
            c.getPlayer().dropMessage(5, "@커닝 : 커닝시티 파퀘 멤버 모집");
            c.getPlayer().dropMessage(5, "@카니발 : 몬스터카니발 파퀘 멤버 모집");
            c.getPlayer().dropMessage(5, "@로미오 : 로미오와 줄리엣 파퀘 멤버 모집");
            c.getPlayer().dropMessage(5, "@독안개 : 독안개숲 파티퀘스트 멤버 모집");
            c.getPlayer().dropMessage(5, "@자쿰 : 자쿰 멤버 모집");
            c.getPlayer().dropMessage(5, "@혼테일 : 혼테일 멤버 모집");
            c.getPlayer().dropMessage(5, "@핑크빈 : 핑크빈 멤버 모집");
            c.getPlayer().dropMessage(5, "@루시드 : 루시드 멤버 모집");
            c.getPlayer().dropMessage(5, "@루디 : 루디브리엄 파퀘 멤버 모집");
            c.getPlayer().dropMessage(5, "@오르비스 : 여신의흔적 파퀘 멤버 모집");
            c.getPlayer().dropMessage(5, "@데비존 : 해적 데비존 파퀘 멤버 모집");
            c.getPlayer().dropMessage(5, "@파티사냥 : 파티사냥 멤버 모집");
            c.getPlayer().dropMessage(5, "@길대 : 길대 멤버 모집");         
                        c.getPlayer().dropMessage(5, "@원정대소환 : @원정대소환 유저명 - 보스맵에서 팅긴 유저명을 입력하면 소환가능."); 
            return 1;
        }
    }
            
    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.NORMAL;
    }

    public static class 힘 extends DistributeStatCommands {

        public 힘() {
            stat = MapleStat.STR;
        }
    }

    public static class 덱스 extends DistributeStatCommands {

        public 덱스() {
            stat = MapleStat.DEX;
        }
    }

    public static class 인트 extends DistributeStatCommands {

        public 인트() {
            stat = MapleStat.INT;
        }
    }

    public static class 럭 extends DistributeStatCommands {

        public 럭() {
            stat = MapleStat.LUK;
        }
    }

    public abstract static class DistributeStatCommands extends CommandExecute {

        protected MapleStat stat = null;
        private static int statLim = 32767;

        private void setStat(MapleCharacter player, int amount) {
            switch (stat) {
                case STR:
                    player.getStat().setStr((short) (amount <= 0 ? -amount : amount), player);
                    player.updateSingleStat(MapleStat.STR, player.getStat().getStr());
                    if (amount <= 0) {
                        player.setRemainingAp((short) (player.getRemainingAp() - amount));
                    }
                    break;
                case DEX:
                    player.getStat().setDex((short) (amount <= 0 ? -amount : amount), player);
                    player.updateSingleStat(MapleStat.DEX, player.getStat().getDex());
                    break;
                case INT:
                    player.getStat().setInt((short) (amount <= 0 ? -amount : amount), player);
                    player.updateSingleStat(MapleStat.INT, player.getStat().getInt());
                    break;
                case LUK:
                    player.getStat().setLuk((short) (amount <= 0 ? -amount : amount), player);
                    player.updateSingleStat(MapleStat.LUK, player.getStat().getLuk());
                    break;
            }
        }

        private int getStat(MapleCharacter player) {
            switch (stat) {
                case STR:
                    return player.getStat().getStr();
                case DEX:
                    return player.getStat().getDex();
                case INT:
                    return player.getStat().getInt();
                case LUK:
                    return player.getStat().getLuk();
                default:
                    throw new RuntimeException(); //Will never happen.
            }
        }

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "Invalid number entered.");
                return 0;
            }
            int change = 0;
            try {
                change = Integer.parseInt(splitted[1]);
            } catch (NumberFormatException nfe) {
                c.getPlayer().dropMessage(5, "올리실 스탯의 양을 입력해주세요.");
                return 0;
            }
            if (change <= 0) {
                c.getPlayer().dropMessage(5, "0보단 큰 수를 입력해야합니다.");
                return 0;
            }
            if (c.getPlayer().getRemainingAp() < change) {
                c.getPlayer().dropMessage(5, "AP가 모자랍니다.");
                return 0;
            }
            if (getStat(c.getPlayer()) + change > statLim) {
                c.getPlayer().dropMessage(5, "스탯의 최대치는" + statLim + "입니다.");
                return 0;
            }
            setStat(c.getPlayer(), getStat(c.getPlayer()) + change);
            c.getPlayer().setRemainingAp((short) (c.getPlayer().getRemainingAp() - change));
            c.getPlayer().updateSingleStat(MapleStat.AVAILABLEAP, c.getPlayer().getRemainingAp());
            c.getPlayer().dropMessage(5, StringUtil.makeEnumHumanReadable(stat.name()) + "이 " + change + "만큼 올랐습니다.");
            return 1;
        }
    }

    public abstract static class OpenNPCCommand extends CommandExecute {

        protected int npc = -1;
        private static int[] npcs = { //Ish yur job to make sure these are in order and correct ;(
            9270035,
            9010017,
            9000000,
            9000030,
            9010000,
            9000085,
            9000018};

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (npc != 6 && npc != 5 && npc != 4 && npc != 3 && npc != 1 && c.getPlayer().getMapId() != 910000000) { //drpcash can use anywhere
                if (c.getPlayer().getLevel() < 10 && c.getPlayer().getJob() != 200) {
                    c.getPlayer().dropMessage(5, "You must be over level 10 to use this command.");
                    return 0;
                }
                if (c.getPlayer().isInBlockedMap()) {
                    c.getPlayer().dropMessage(5, "You may not use this command here.");
                    return 0;
                }
            } else if (npc == 1) {
                if (c.getPlayer().getLevel() < 70) {
                    c.getPlayer().dropMessage(5, "You must be over level 70 to use this command.");
                    return 0;
                }
            }
            if (c.getPlayer().hasBlockedInventory()) {
                c.getPlayer().dropMessage(5, "You may not use this command here.");
                return 0;
            }
            NPCScriptManager.getInstance().start(c, npcs[npc]);
            return 1;
        }
    }


    public static class 정보 extends OpenNPCCommand {

        public 정보() {
            npc = 4;
        }
    }

    public static class 온라인 extends CommandExecute {


        public int execute(MapleClient c, String[] splitted) {
            for (ChannelServer ch : ChannelServer.getAllInstances()) {
                c.getPlayer().dropMessage(6, "채널 " + ch.getChannel() + " (" + ch.getPlayerStorage().getAllCharacters().size() + "명) : " + ch.getPlayerStorage().getOnlinePlayers(true));
            }
            return 1;
        }
    }
    
  public static class 실뎀 extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            if (!c.getPlayer().isDma()) {
                c.getPlayer().setDma(true);
                c.getPlayer().dropMessage(6, "실제데미지 표시 기능이 켜졌습니다.");
            } else {
                c.getPlayer().setDma(false);
                c.getPlayer().dropMessage(6, "실제데미지 표시 기능이 꺼졌습니다.");
            }
            return 1;
        }
    }
    
public static class 캐시충전 extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().getLevel() < 20) {
                c.getPlayer().dropMessage(1, "레벨 20이하는 사용이 불가능합니다.");
                return 1;
            }
            int cash = Integer.parseInt(splitted[1]);
            int meso = cash * 100;
            if (cash >= 1 && cash <= 1000000) {
                if (c.getPlayer().getMeso() >= meso) {
                    c.getPlayer().modifyCSPoints(1, cash, false);
                    c.getPlayer().gainMeso(-meso, false);
                    c.getPlayer().dropMessage(5, "" + cash + "캐시 충전 완료.");
                } else {
                    c.getPlayer().dropMessage(1, "메소가 부족하여 캐시충전이 불가능합니다.");
                }
            } else {
                c.getPlayer().dropMessage(1, "캐시는 한번에 1000000 까지만 충전 가능.");
            }
            return 1;
        }
    }
              public static class 저장 extends CommandExecute {
        public int execute(MapleClient c, String[] splitted) {
            for (ChannelServer ch : ChannelServer.getAllInstances())
            for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters())
            chr.saveToDB(true, true);
            World.Guild.save();
            World.Alliance.save();
            World.Family.save();
            c.getPlayer().dropMessage(6, "저장이 완료되었습니다.");
            return 1;
        }
    }   
}
