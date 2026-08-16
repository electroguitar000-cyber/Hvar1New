package org.anonymous.withercps.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import org.anonymous.withercps.WitherCPS;
import org.anonymous.withercps.sessions.click.ClickService;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.logging.Level;

@SuppressWarnings("resource")
public record PacketInjector(WitherCPS plugin, ClickService clickService) {

    private static final String KEY = "withercps";

    public void inject(Player player) {
        try {
            Channel channel = ((CraftPlayer) player).getHandle().connection.connection.channel;

            channel.eventLoop().execute(() -> {
                ChannelPipeline pipeline = channel.pipeline();

                if (pipeline.context(KEY) != null) {
                    pipeline.remove(KEY);
                }

                pipeline.addBefore("packet_handler", KEY, new InteractHandler(player.getUniqueId(), clickService));
            });
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Critical error injecting packet handler for: \"" + player.getName() + "\"", e);
        }
    }

    public void uninject(Player player) {
        try {
            Channel channel = ((CraftPlayer) player).getHandle().connection.connection.channel;

            if (!channel.isActive()) {
                return;
            }

            channel.eventLoop().execute(() -> {
                ChannelPipeline pipeline = channel.pipeline();

                if (pipeline.context(KEY) != null) {
                    pipeline.remove(KEY);
                }
            });
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Critical error removing packet handler for: \"" + player.getName() + "\"", e);
        }
    }

    private static class InteractHandler extends ChannelDuplexHandler {

        private final UUID uuid;
        private final ClickService clickService;

        public InteractHandler(UUID uuid, ClickService clickService) {
            this.uuid = uuid;
            this.clickService = clickService;
        }

        @Override
        public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
            if (message instanceof ServerboundInteractPacket packet) {
                int entityId = packet.getEntityId();

                if (packet.isAttack() && clickService.getBoats().contains(entityId)) {
                    clickService.register(uuid, true);
                }
            }
            super.channelRead(context, message);
        }
    }
}