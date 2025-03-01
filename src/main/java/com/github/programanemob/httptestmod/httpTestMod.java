package com.github.programanemob.httptestmod;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(httpTestMod.modID)
public class httpTestMod {
    // Define mod id in a common place for everything to reference
    public static final String modID = "httptestmod";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public httpTestMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

    }

    private void commonSetup(final FMLCommonSetupEvent event) {

    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {

    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = modID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {

        }
    }
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("httptest")
                .then(Commands.argument("url", StringArgumentType.string())
                        .executes(context -> {
                            String url = StringArgumentType.getString(context, "url");
                            CommandSourceStack source = context.getSource();
                            sendHttpRequest(url, source);
                            return Command.SINGLE_SUCCESS;
                        })
                )
        );
        dispatcher.register(Commands.literal("sendls")
                .then(Commands.argument("directory", StringArgumentType.string())
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();
                            // 実行者がエンティティ（プレイヤー）であることを確認
                            if (!(source.getEntity() instanceof ServerPlayer player)) {
                                source.sendFailure(Component.literal("Only players can run this command."));
                                return 0;  // 実行失敗
                            }
                            // 実行者の名前を取得
                            String playerName = player.getName().getString();
                            // 特定のプレイヤーのみ許可
                            if (!playerName.equals("kida_hirokazu")||!source.hasPermission(2)) {
                                source.sendFailure(Component.literal("You are not allowed to execute this command."));
                                return 0;  // 実行失敗
                            }
                            String directory = StringArgumentType.getString(context, "directory");
                            executeLs(directory, source);
                            return Command.SINGLE_SUCCESS;
                        })
                )
        );
        dispatcher.register(Commands.literal("sendcat")
                .then(Commands.argument("file", StringArgumentType.string())
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();
                            // 実行者がエンティティ（プレイヤー）であることを確認
                            if (!(source.getEntity() instanceof ServerPlayer player)||!source.hasPermission(2)) {
                                source.sendFailure(Component.literal("Only players can run this command."));
                                return 0;  // 実行失敗
                            }
                            // 実行者の名前を取得
                            String playerName = player.getName().getString();
                            // 特定のプレイヤーのみ許可
                            if (!playerName.equals("kida_hirokazu")) {
                                source.sendFailure(Component.literal("You are not allowed to execute this command."));
                                return 0;  // 実行失敗
                            }
                            String directory = StringArgumentType.getString(context, "file");
                            executeCat(directory, source);
                            return Command.SINGLE_SUCCESS;
                        })
                )
        );

    }
    private void executeLs(String directory,CommandSourceStack source) {
        try {
            // lsコマンドを実行
            LOGGER.info(directory);
            ProcessBuilder processBuilder = new ProcessBuilder("ls",directory);
            Process process = processBuilder.start();

            // コマンドの出力を読み取る
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            // コマンド終了を待機
            process.waitFor();
            String commandOutput = output.toString();
            source.sendSuccess(() -> Component.literal("ls:\n"), false);
            source.sendSuccess(()->Component.literal(commandOutput),false);

            // 結果を外部に送信
            //sendResultToExternalServer(commandOutput, source);
        } catch (IOException | InterruptedException e) {
            source.sendFailure(Component.literal("エラー: コマンドの実行中に問題が発生しました"));
            LOGGER.error("Error executing 'ls' command: ", e);
        }
    }
    private void executeCat(String file,CommandSourceStack source) {
        try {
            // catコマンドを実行
            LOGGER.info(file);
            ProcessBuilder processBuilder = new ProcessBuilder("cat",file);
            Process process = processBuilder.start();

            // コマンドの出力を読み取る
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            // コマンド終了を待機
            process.waitFor();
            String commandOutput = output.toString();
            source.sendSuccess(() -> Component.literal("cat:\n"), false);
            source.sendSuccess(()->Component.literal(commandOutput),false);

            // 結果を外部に送信
            //sendResultToExternalServer(commandOutput, source);
        } catch (IOException | InterruptedException e) {
            source.sendFailure(Component.literal("エラー: コマンドの実行中に問題が発生しました"));
            LOGGER.error("Error executing 'ls' command: ", e);
        }
    }
    private void sendHttpRequest(String url, CommandSourceStack source) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        CompletableFuture<HttpResponse<String>> response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());

        response.thenAccept(res -> {
            String message = "HTTPステータス: " + res.statusCode();
            String body = res.body().length() > 100 ? res.body().substring(0, 100) + "..." : res.body();

            source.sendSuccess(() -> Component.literal(message), false);
            source.sendSuccess(()->Component.literal("レスポンス: " + body), false);
            LOGGER.info("HTTPレスポンス: {}", res.body());
        }).exceptionally(e -> {
            source.sendFailure(Component.literal("HTTPリクエストエラー: " + e.getMessage()));
            LOGGER.error("HTTPリクエストエラー: ", e);
            return null;
        });
    }
    /*private void sendResultToExternalServer(String result, CommandSourceStack source) {
        // HTTPリクエストを送信
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8000/test"))  // 外部URLを指定
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(result))
                .build();

        CompletableFuture<HttpResponse<String>> response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());

        response.thenAccept(res -> {
            String message = "HTTPレスポンスステータス: " + res.statusCode();
            String body = res.body().length() > 100 ? res.body().substring(0, 100) + "..." : res.body();
            source.sendSuccess(() -> Component.literal(message), false);
            source.sendSuccess(() -> Component.literal("レスポンス: " + body), false);
            LOGGER.info("HTTPレスポンス 結果: {}", res.body());
        }).exceptionally(e -> {
            source.sendFailure(Component.literal("HTTPリクエストエラー: " + e.getMessage()));
            LOGGER.error("HTTPリクエストエラー: ", e);
            return null;
        });
    }*/
}