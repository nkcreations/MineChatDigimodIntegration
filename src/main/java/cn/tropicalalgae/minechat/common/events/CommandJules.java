package cn.tropicalalgae.minechat.common.events;

import cn.tropicalalgae.minechat.common.persistence.JulesSavedData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import cn.tropicalalgae.minechat.MineChat;

@Mod.EventBusSubscriber(modid = MineChat.MOD_ID)
public class CommandJules {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("jules")
            .then(Commands.literal("reset")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    context.getSource().sendFailure(Component.literal("Uso: /jules reset <all|player>"));
                    return 0;
                })
                .then(Commands.literal("all")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        JulesSavedData.get(player.serverLevel()).clearData();
                        JulesSavedData.get(player.serverLevel()).setDirty();
                        context.getSource().sendSuccess(() -> Component.literal("Toda la memoria de relaciones de Jules ha sido borrada."), true);
                        return 1;
                    })
                )
            ) // End of /jules reset
            // Placeholder for persona command
            .then(Commands.literal("persona")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("set")
                    .then(Commands.argument("species", StringArgumentType.word())
                    .then(Commands.argument("key", StringArgumentType.word())
                    .then(Commands.argument("value", StringArgumentType.greedyString())
                    .executes(context -> {
                        // This is complex to implement safely. For now, just confirm it works.
                        String species = StringArgumentType.getString(context, "species");
                        String key = StringArgumentType.getString(context, "key");
                        String value = StringArgumentType.getString(context, "value");
                        context.getSource().sendSuccess(() -> Component.literal("Comando recibido: " + species + " " + key + " " + value), true);
                        return 1;
                    }))))
            )
        );
    }
}
