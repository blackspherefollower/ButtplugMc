package com.cyloci.buttplugmc;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import com.cyloci.utils.Sleep;

import org.blackspherefollower.buttplug.client.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.ChatColor;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.javax.server.config.JavaxWebSocketServletContainerInitializer;

import javax.websocket.Session;
import javax.websocket.server.ServerEndpointConfig;

public class ButtplugClientManager {

    private final JavaPlugin plugin;
    private final ConcurrentHashMap<UUID, ButtplugClientWSEndpoint> clients;

    private final ButtplugClientWSServerExample wsServer;

    public ButtplugClientManager(JavaPlugin plugin) throws Exception {
        this.plugin = plugin;
        this.clients = new ConcurrentHashMap<>();
        this.wsServer = new ButtplugClientWSServerExample(this, 54321);
    }

    public Collection<ButtplugClientWSEndpoint> getClients() {
        return this.clients.values();
    }

    public ButtplugClientWSEndpoint getClient(Player player) throws Exception {
        UUID playerId = player.getUniqueId();
        boolean withPlayerFeedback = false;
        ButtplugClientWSEndpoint client = this.clients.get(playerId);
        if (client == null || client.getConnectionState() == ButtplugClientWSEndpoint.ConnectionState.DISCONNECTED ) {
            player.sendMessage(ChatColor.AQUA + "Reconnecting to Intiface...");
            withPlayerFeedback = true;
            client = connectButtplugClient(player.getAddress().getAddress());
            clients.put(playerId, client);
        }
        scanForToys(client, player, withPlayerFeedback);
        return client;
    }

    private ButtplugClientWSEndpoint connectButtplugClient(InetAddress address) throws Exception {
        URI uri;
        try {
            uri = new URI("ws://" + address.getHostAddress() + ":12345/buttplug");
        } catch (URISyntaxException e) {
            this.plugin.getLogger().info(e.toString());
            return null;
        }
        ButtplugClientWSClient client = new ButtplugClientWSClient("ButtplugMC");
        client.connect(uri);
        return client;
    }

    private void scanForToys(ButtplugClientWSEndpoint client, Player player, boolean withPlayerFeedback) throws IOException, ExecutionException, InterruptedException {
        int attempts = 0;
        while (client.getDevices().size() == 0) {
            client.startScanning();
            if (withPlayerFeedback) {
                player.sendMessage(ChatColor.AQUA + "Searching for toys...");
            }
            Sleep.sleep(1000);
            attempts++;
            if (attempts == 5) {
                if (withPlayerFeedback) {
                    player.sendMessage(ChatColor.RED + "Couldn't find any toys.");
                }
                return;
            }
        }
        if (withPlayerFeedback) {
            if (client.getDevices().size() > 0) {
                player.sendMessage(ChatColor.AQUA + "Connected to the following toys:");
                client.getDevices().forEach(device -> {
                    player.sendMessage(ChatColor.AQUA + device.getDisplayName());
                    try {
                        if(device.getScalarVibrateCount() > 0) {
                            device.sendScalarVibrateCmd(1.0);
                            Sleep.sleep(250);
                            device.sendScalarVibrateCmd(0.0);
                            Sleep.sleep(250);
                            device.sendScalarVibrateCmd(1.0);
                            Sleep.sleep(250);
                            device.sendScalarVibrateCmd(0.0);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } catch (ExecutionException e) {
                        throw new RuntimeException(e);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
            } else {
                player.sendMessage(ChatColor.RED + "Could not find any toys.");
            }
        }
    }

    public boolean addConnection(String user, ButtplugClientWSEndpoint client) throws IOException, ExecutionException, InterruptedException {
        Player player = plugin.getServer().getPlayer( user);
        if( player == null) {
            client.disconnect();
            return false;
        }
        ButtplugClientWSEndpoint old = clients.putIfAbsent(player.getUniqueId(), client);
        if( old == null) {
            scanForToys(client, player, false);
            return true;
        }
        if( old.getConnectionState() == ButtplugClientWSEndpoint.ConnectionState.DISCONNECTED) {
            clients.put(player.getUniqueId(), client);
            scanForToys(client, player, false);
            return true;
        }
        client.disconnect();
        return false;
    }

    class ButtplugClientWSServerExample {
        private final Server server;
        private final ServerConnector connector;

        private final ButtplugClientManager clientManger;

        public ButtplugClientWSServerExample(ButtplugClientManager cMgr, int port) throws Exception {
            server = new Server();
            connector = new ServerConnector(server);
            connector.setPort(port);
            server.addConnector(connector);
            this.clientManger = cMgr;

            // Setup the basic application "context" for this application at "/"
            // This is also known as the handler tree (in jetty speak)
            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/*");
            server.setHandler(context);

            // Configure specific websocket behavior
            JavaxWebSocketServletContainerInitializer.configure(context, (servletContext, wsContainer) ->
            {
                // Configure default max size
                wsContainer.setDefaultMaxTextMessageBufferSize(65535);

                // Add websockets
                wsContainer.addEndpoint(ServerEndpointConfig.Builder.create(ButtplugClientWSEndpoint.class, "/{user}").configurator(new ServerEndpointConfig.Configurator() {
                    @Override
                    public <T> T getEndpointInstance(Class<T> endpointClass) {
                        if (endpointClass == ButtplugClientWSEndpoint.class) {
                            ButtplugClientWSServer client = new ButtplugClientWSServer("Java WS Server Buttplug Client");
                            client.setOnConnected(new IConnectedEvent() {
                                @Override
                                public void onConnected(ButtplugClientWSEndpoint client) {
                                    new Thread(() -> {
                                        try {
                                            if (client instanceof ButtplugClientWSServer) {
                                                Session s = ((ButtplugClientWSServer) client).getSession();
                                                clientManger.addConnection(s.getPathParameters().getOrDefault("user", "notavaliduser"), client);
                                            }
                                        } catch (ExecutionException e) {
                                            throw new RuntimeException(e);
                                        } catch (InterruptedException e) {
                                            throw new RuntimeException(e);
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        } catch (Exception e) {
                                            throw new RuntimeException(e);
                                        }
                                    }).start();
                                }
                            });
                            return (T) client;
                        }
                        return null;
                    }
                }).build());
            });
            server.start();
        }

        public URI getURI() {
            return server.getURI();
        }

        public void stop() throws Exception {
            server.stop();
        }

        public void join() throws InterruptedException {
            server.join();
        }
    }
}
