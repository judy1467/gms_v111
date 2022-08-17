
package constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuickMove {
    private static Map<Integer, List<QuickMoveEntry>> quickmoves = new HashMap<Integer, List<QuickMoveEntry>>();
    
    public static void QuickMoveLoad() {    
        List<QuickMoveNpc> QuickMap = new ArrayList<QuickMoveNpc>();
        QuickMap.add(new QuickMoveNpc(100000000, true, true, true, true, true, true, true, true, true));
        QuickMap.add(new QuickMoveNpc(101000000, true, true, true, true, true, true, true, true, true));
        QuickMap.add(new QuickMoveNpc(102000000, true, true, true, true, true, true, true, true, true));
        QuickMap.add(new QuickMoveNpc(103000000, true, true, true, true, true, true, true, true, true));
        QuickMap.add(new QuickMoveNpc(104000000, true, false, true, false, false, true, true, true, true));
        QuickMap.add(new QuickMoveNpc(120000100, true, true, true, true, true, true, true, true, true));
        QuickMap.add(new QuickMoveNpc(105000000, true, true, true, true, true, true, true, true, true));
        QuickMap.add(new QuickMoveNpc(211000000, true, true, true, false, true, true, true, true, true));
        QuickMap.add(new QuickMoveNpc(220000000, true, true, true, true, true, true, true, true, true));
        QuickMap.add(new QuickMoveNpc(221000000, true, true, true, false, true, true, true, true, true));
        QuickMap.add(new QuickMoveNpc(222000000, true, true, true, false, true, true, true, true, true));
        QuickMap.add(new QuickMoveNpc(240000000, true, true, true, true, true, true, true, true, true));
        QuickMap.add(new QuickMoveNpc(230000000, true, true, true, false, true, true, true, true, true));
        QuickMap.add(new QuickMoveNpc(251000000, true, true, true, false, true, true, true, true, true));
        QuickMap.add(new QuickMoveNpc(310000000, true, true, true, true, true, true, true, true, true));
        QuickMap.add(new QuickMoveNpc(261000000, true, true, true, false, true, true, true, true, true));
        QuickMap.add(new QuickMoveNpc(260000000, true, true, true, true, true, true, true, true, true));
        QuickMap.add(new QuickMoveNpc(250000000, true, true, true, true, true, false, true, true, true));
        QuickMap.add(new QuickMoveNpc(200000000, true, true, true, true, true, false, true, true, true));
        QuickMap.add(new QuickMoveNpc(910000000, true, true, true, true, true, true, true, true, true));
        for (QuickMoveNpc qmtp : QuickMap) {
            List<QuickMoveEntry> asd = new ArrayList<QuickMoveEntry>();
            if (qmtp.npc1) {
                asd.add(new QuickMoveEntry(9010022, 2, 10, "파티 퀘스트 등 각종 컨텐츠 맵으로 이동시켜주는 #c<차원의 거울>#을 이용한다."));
            }
            if (qmtp.npc2) {
                asd.add(new QuickMoveEntry(9071003, 1, 20, "파티원들과 힘을 합쳐 강력한 몬스터들을 공략할 수 있는 파티 플레이존\n#c<몬스터 파크>#로 이동한다.\n#c레벨 60 이상, 레벨 95 미만 혹은 레벨 135 이상 참여 가능"));
            }
            if (qmtp.npc3) {
                asd.add(new QuickMoveEntry(9000086, 5, 0, "다른 대륙 이동으로 이동할 수 있는 가장 가까운 #c<대륙 이동 정거장>#으로 이동한다."));
            }
            if (qmtp.npc4) {
                asd.add(new QuickMoveEntry(9000087, 3, 0, "다른 유저들과 아이템을 거래할 수 있는 #c<자유 시장>#으로 이동한다."));
            }
            if (qmtp.npc5) {
                asd.add(new QuickMoveEntry(9000088, 4, 30, "전문 기술의 마을 #c<마이스터 빌>#로 이동한다.\n#c레벨 30이상 부터 이동 가능."));
            }
            if (qmtp.npc6) {
                asd.add(new QuickMoveEntry(9000089, 6, 0, "인근의 주요 지역으로 캐릭터를 이동시켜 주는 #c<택시>#를 이용한다."));
            }
            if (qmtp.npc7) {
                asd.add(new QuickMoveEntry(9010037, 7, 10, "레전드 코인으로 구매가능한 #c<전설의 상점>#을 이용한다."));
            }
            if (qmtp.npc8) {
                asd.add(new QuickMoveEntry(9010038, 8, 10, "레전드 코인으로 구매가능한 #c<전설의 상점>#을 이용한다."));
            }
            if (qmtp.npc9) {
                asd.add(new QuickMoveEntry(9070003, 0, 10, "#c<배틀스퀘어>#로 이동한다."));
            }
            quickmoves.put(qmtp.mapid, asd);
        }
    }
    
    
    public static List<QuickMoveEntry> getQuickMoves(int mapid) {
        return quickmoves.get(mapid);
    }
    
    public static class QuickMoveNpc {
        boolean npc1, npc2, npc3, npc4, npc5, npc6, npc7, npc8, npc9;
        int mapid;
        public QuickMoveNpc (int map, boolean npc1, boolean npc2, boolean npc3, boolean npc4, boolean npc5, boolean npc6, boolean npc7, boolean npc8, boolean npc9) {
            this.mapid = map; 
            this.npc1 = npc1; 
            this.npc2 = npc2; 
            this.npc3 = npc3; 
            this.npc4 = npc4 ; 
            this.npc5 = npc5 ; 
            this.npc6 = npc6; 
            this.npc7 = npc7; 
            this.npc8 = npc8; 
            this.npc9 = npc9;
        }
    }
}
