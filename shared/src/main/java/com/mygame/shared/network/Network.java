package com.mygame.shared.network;

import com.esotericsoftware.kryo.Kryo;
import com.mygame.shared.model.LeaderboardEntry;
import com.mygame.shared.model.MatchHistoryEntry;
import com.mygame.shared.model.PlayerProfile;
import com.mygame.shared.model.Rank;
import com.mygame.shared.network.packets.*;

/**
 * Network utility class for registering all packets with Kryo.
 */
public class Network {

    /**
     * Register all network packets and models with Kryo.
     * This must be called on both client and server before connecting.
     */
    public static void registerPackets(Kryo kryo) {
        // Register models
        kryo.register(PlayerProfile.class);
        kryo.register(Rank.class);
        kryo.register(LeaderboardEntry.class);
        kryo.register(MatchHistoryEntry.class);
        kryo.register(com.mygame.shared.model.RoomInfo.class);
        kryo.register(com.mygame.shared.model.Quest.class);
        kryo.register(java.time.LocalDateTime.class);
        kryo.register(java.util.ArrayList.class);

        // Register authentication packets
        kryo.register(LoginRequest.class);
        kryo.register(LoginResponse.class);
        kryo.register(RegisterRequest.class);
        kryo.register(RegisterResponse.class);

        // Register lobby packets
        kryo.register(LeaderboardRequest.class);
        kryo.register(LeaderboardResponse.class);
        kryo.register(MatchHistoryRequest.class);
        kryo.register(MatchHistoryResponse.class);
        kryo.register(DailyRewardRequest.class);
        kryo.register(DailyRewardResponse.class);
        kryo.register(GetQuestsRequest.class);
        kryo.register(GetQuestsResponse.class);
        kryo.register(ClaimQuestRequest.class);
        kryo.register(ClaimQuestResponse.class);

        // Register room packets
        kryo.register(CreateRoomRequest.class);
        kryo.register(CreateRoomResponse.class);
        kryo.register(JoinRoomRequest.class);
        kryo.register(JoinRoomResponse.class);
        kryo.register(LeaveRoomRequest.class);
        kryo.register(ListRoomsRequest.class);
        kryo.register(ListRoomsResponse.class);
        kryo.register(RoomUpdatePacket.class);
        kryo.register(StartGameRequest.class);
        kryo.register(StartGameResponse.class);
        kryo.register(KickPacket.class);
        kryo.register(com.mygame.shared.model.RoomInfo.RoomPlayerInfo.class);
        kryo.register(com.mygame.shared.model.GameType.class);

        // Register game packets
        kryo.register(com.mygame.shared.network.packets.game.PlayerActionPacket.class);
        kryo.register(com.mygame.shared.network.packets.game.GameStatePacket.class);
        kryo.register(com.mygame.shared.network.packets.game.GameStartPacket.class);
        kryo.register(com.mygame.shared.network.packets.game.GameEndPacket.class);
        kryo.register(com.mygame.shared.network.packets.game.PlayerTurnPacket.class);

        // Register game state classes for Poker
        kryo.register(com.mygame.shared.game.poker.PokerGameState.class);
        kryo.register(com.mygame.shared.game.poker.PokerGameState.Stage.class);
        kryo.register(com.mygame.shared.game.poker.PokerGameState.SidePot.class);
        kryo.register(com.mygame.shared.game.card.Card.class);
        kryo.register(com.mygame.shared.game.card.Suit.class);
        kryo.register(java.util.HashMap.class);
        kryo.register(java.util.HashSet.class);

        // Register game state classes for Tien Len
        kryo.register(com.mygame.shared.game.tienlen.TienLenGameState.class);
        kryo.register(com.mygame.shared.game.tienlen.TienLenCombinationType.class);

        // Register voting packets for play again system
        kryo.register(com.mygame.shared.network.packets.game.PlayAgainVotePacket.class);
        kryo.register(com.mygame.shared.network.packets.game.PlayAgainStatusPacket.class);
    }
}
