/*
 * ALL RIGHTS RESERVED
 *
 * Copyright (c) 2024 Calum H. (IMB11) and enjarai
 *
 * THE SOFTWARE IS PROVIDED "AS IS," WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.mineblock11.skinshuffle.networking;

import com.mineblock11.skinshuffle.SkinShuffle;
import com.mineblock11.skinshuffle.api.SkinQueryResult;
import com.mineblock11.skinshuffle.client.config.SkinPresetManager;
import com.mineblock11.skinshuffle.util.SkinShuffleClientPlayer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;

public class ClientSkinHandling {
    private static boolean handshakeTakenPlace = false;

    private static boolean reconnectRequired = false;

    public static boolean isReconnectRequired() {
        return reconnectRequired;
    }

    public static void setReconnectRequired(boolean reconnectRequired) {
        ClientSkinHandling.reconnectRequired = reconnectRequired;
    }

    public static boolean isInstalledOnServer() {
        return handshakeTakenPlace;
    }

    public static void sendRefresh(SkinQueryResult result) {
        /*? <1.20.5 {*/
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeProperty(result.toProperty());
        ClientPlayNetworking.send(SkinShuffle.id("refresh"), buf);
        /*?} else {*/
        /*ClientPlayNetworking.send(new SkinRefreshPayload(result.toProperty()));
        *//*?}*/
    }

    public static void init() {

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            SkinPresetManager.setApiPreset(null);
        });

        ClientPlayConnectionEvents.INIT.register((handler, client) -> {
            if(client.world == null) return;
            handshakeTakenPlace = false;
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            handshakeTakenPlace = false;
            setReconnectRequired(false);
            SkinPresetManager.setApiPreset(null);
        });

        /*? <1.20.5 {*/
        ClientPlayNetworking.registerGlobalReceiver(SkinShuffle.id("handshake"), (client1, handler1, buf, responseSender) -> {
            handshakeTakenPlace = true;
        });

        ClientPlayNetworking.registerGlobalReceiver(SkinShuffle.id("refresh_player_list_entry"), (client, handler, buf, responseSender) -> {
            int id = buf.readVarInt();
            client.execute(() -> {
                ClientWorld world = client.world;
                if (world != null) {
                    Entity entity = world.getEntityById(id);
                    if (entity instanceof AbstractClientPlayerEntity player) {
                        ((SkinShuffleClientPlayer) player).skinShuffle$refreshPlayerListEntry();
                    }
                }
            });
        });
        /*?} else {*/
        /*ClientPlayNetworking.registerGlobalReceiver(HandshakePayload.PACKET_ID, (payload, context) -> {
            handshakeTakenPlace = true;
        });

        ClientPlayNetworking.registerGlobalReceiver(RefreshPlayerListEntryPayload.PACKET_ID, (payload, context) -> {
            int id = payload.entityID();
            MinecraftClient client = context.client();
            client.execute(() -> {
                ClientWorld world = client.world;
                if (world != null) {
                    Entity entity = world.getEntityById(id);
                    if (entity instanceof AbstractClientPlayerEntity player) {
                        ((SkinShuffleClientPlayer) player).skinShuffle$refreshPlayerListEntry();
                    }
                }
            });
        });
        *//*?}*/
    }
}
