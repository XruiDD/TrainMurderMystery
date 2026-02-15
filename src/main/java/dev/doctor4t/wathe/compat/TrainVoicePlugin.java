package dev.doctor4t.wathe.compat;

import com.google.common.collect.ImmutableList;
import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.VolumeCategory;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.index.WatheDataComponentTypes;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.item.WalkieTalkieItem;
import dev.doctor4t.wathe.item.component.WalkieTalkieComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TrainVoicePlugin implements VoicechatPlugin {
    public static final UUID GROUP_ID = UUID.randomUUID();
    public static VoicechatServerApi SERVER_API;
    public static Group GROUP;

    public static final String WALKIE_TALKIE_CATEGORY = "walkie_talkies";

    public static boolean isVoiceChatMissing() {
        return SERVER_API == null;
    }

    public static void addPlayer(@NotNull UUID player) {
        if (isVoiceChatMissing()) return;
        VoicechatConnection connection = SERVER_API.getConnectionOf(player);
        if (connection != null) {
            if (GROUP == null)
                GROUP = SERVER_API.groupBuilder().setHidden(true).setId(GROUP_ID).setName("Train Spectators").setPersistent(true).setType(Group.Type.OPEN).build();
            if (GROUP != null) connection.setGroup(GROUP);
        }
    }

    public static void resetPlayer(@NotNull UUID player) {
        if (isVoiceChatMissing()) return;
        VoicechatConnection connection = SERVER_API.getConnectionOf(player);
        if (connection != null) connection.setGroup(null);
    }

    @Override
    public void registerEvents(@NotNull EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
    }

    private void onServerStarted(VoicechatServerStartedEvent event) {
        SERVER_API = event.getVoicechat();
        VolumeCategory walkieTalkies = SERVER_API.volumeCategoryBuilder()
                .setId(WALKIE_TALKIE_CATEGORY)
                .setName("Walkie Talkies")
                .setDescription("The volume of walkie talkie transmissions")
                .build();
        SERVER_API.registerVolumeCategory(walkieTalkies);
    }

    private void onMicrophonePacket(MicrophonePacketEvent event) {
        VoicechatConnection connection = event.getSenderConnection();
        VoicechatServerApi serverApi = event.getVoicechat();
        if (connection == null) return;
        if (event.getPacket().getOpusEncodedData().length == 0) return;

        ServerPlayerEntity sourcePlayer = (ServerPlayerEntity) connection.getPlayer().getPlayer();
        if (sourcePlayer == null) return;

        // 检查说话者是否手持对讲机（主手或副手）
        ItemStack mainHandStack = sourcePlayer.getMainHandStack();
        ItemStack offHandStack = sourcePlayer.getOffHandStack();
        ItemStack heldWalkieTalkie = null;
        if (mainHandStack.getItem() instanceof WalkieTalkieItem) {
            heldWalkieTalkie = mainHandStack;
        } else if (offHandStack.getItem() instanceof WalkieTalkieItem) {
            heldWalkieTalkie = offHandStack;
        }
        if (heldWalkieTalkie == null) return;

        WalkieTalkieComponent component = heldWalkieTalkie.getOrDefault(WatheDataComponentTypes.WALKIE_TALKIE, WalkieTalkieComponent.DEFAULT);
        int senderChannel = component.channel();

        byte[] encodedData = event.getPacket().getOpusEncodedData();

        for (ServerPlayerEntity receiver : sourcePlayer.getServer().getPlayerManager().getPlayerList()) {
            if (receiver == sourcePlayer) continue;
            if (!isReceivingChannel(receiver, senderChannel)) continue;

            VoicechatConnection receiverConnection = serverApi.getConnectionOf(receiver.getUuid());
            if (receiverConnection == null) continue;

            // 发送定位语音包，声音来源于接收者自己持有的对讲机位置
            serverApi.sendLocationalSoundPacketTo(
                    receiverConnection,
                    event.getPacket().locationalSoundPacketBuilder()
                            .opusEncodedData(encodedData)
                            .position(serverApi.createPosition(receiver.getX(), receiver.getY(), receiver.getZ()))
                            .distance(8f)
                            .category(WALKIE_TALKIE_CATEGORY)
                            .build()
            );
        }
    }

    /**
     * 检查玩家物品栏中是否有指定频道的对讲机
     */
    public static boolean isReceivingChannel(PlayerEntity player, int channel) {
        for (ItemStack stack : getWalkieTalkies(player)) {
            WalkieTalkieComponent component = stack.getOrDefault(WatheDataComponentTypes.WALKIE_TALKIE, WalkieTalkieComponent.DEFAULT);
            if (component.channel() == channel) return true;
        }
        return false;
    }

    /**
     * 获取玩家物品栏中所有对讲机
     */
    public static List<ItemStack> getWalkieTalkies(PlayerEntity player) {
        List<List<ItemStack>> inventories = ImmutableList.of(player.getInventory().main, player.getInventory().offHand);
        List<ItemStack> walkieTalkies = new ArrayList<>();
        for (List<ItemStack> inventory : inventories) {
            for (ItemStack stack : inventory) {
                if (stack.isOf(WatheItems.WALKIE_TALKIE)) {
                    walkieTalkies.add(stack);
                }
            }
        }
        return walkieTalkies;
    }

    @Override
    public String getPluginId() {
        return Wathe.MOD_ID;
    }
}
