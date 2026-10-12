package com.example.greenshield;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GreenShield implements ModInitializer {
    public static final String MOD_ID = "green_shield";
    public static final Identifier TOGGLE_PACKET = new Identifier(MOD_ID, "toggle");
    public static final double HALF_SIZE = 3.0;
    private static final Set<UUID> ACTIVE = ConcurrentHashMap.newKeySet();

    @Override public void onInitialize() {
        ServerPlayNetworking.registerGlobalReceiver(TOGGLE_PACKET, (server, player, handler, buf, responseSender) ->
            server.execute(() -> {
                if (ACTIVE.remove(player.getUuid())) {
                    player.sendMessage(net.minecraft.text.Text.literal("§7Green Shield: DESACTIVADO"), true);
                } else {
                    ACTIVE.add(player.getUuid());
                    player.sendMessage(net.minecraft.text.Text.literal("§aGreen Shield: ACTIVADO"), true);
                }
            })
        );
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity owner : server.getPlayerManager().getPlayerList()) {
                if (!ACTIVE.contains(owner.getUuid())) continue;
                ServerWorld world = owner.getServerWorld();
                Vec3d center = owner.getPos().add(0, 1.0, 0);
                Box cube = new Box(center.x - HALF_SIZE, center.y - HALF_SIZE, center.z - HALF_SIZE,
                                   center.x + HALF_SIZE, center.y + HALF_SIZE, center.z + HALF_SIZE);
                for (Entity entity : world.getOtherEntities(owner, cube, e -> e.isAlive())) {
                    if (entity instanceof ProjectileEntity) {
                        entity.discard();
                        continue;
                    }
                    // Keep every other entity outside the cube. Entities already inside are expelled
                    // through the nearest face, and their momentum is cancelled for a stable barrier.
                    Vec3d p = entity.getPos();
                    double dx = p.x - center.x, dy = p.y - center.y, dz = p.z - center.z;
                    double ax = Math.abs(dx), ay = Math.abs(dy), az = Math.abs(dz);
                    double margin = 0.15;
                    if (ax >= ay && ax >= az) {
                        entity.setPosition(center.x + Math.copySign(HALF_SIZE + margin, dx == 0 ? 1 : dx), p.y, p.z);
                    } else if (ay >= ax && ay >= az) {
                        entity.setPosition(p.x, center.y + Math.copySign(HALF_SIZE + margin, dy == 0 ? 1 : dy), p.z);
                    } else {
                        entity.setPosition(p.x, p.y, center.z + Math.copySign(HALF_SIZE + margin, dz == 0 ? 1 : dz));
                    }
                    entity.setVelocity(Vec3d.ZERO);
                    entity.velocityModified = true;
                }
                // Sparse server-side green particles make the barrier visible to nearby players too.
                if (world.getTime() % 3 == 0) {
                    spawnEdges(world, center, HALF_SIZE);
                }
            }
        });
    }

    private static void spawnEdges(ServerWorld world, Vec3d c, double r) {
        double[][] corners = new double[8][3]; int n = 0;
        for (int x : new int[]{-1, 1}) for (int y : new int[]{-1, 1}) for (int z : new int[]{-1, 1})
            corners[n++] = new double[]{c.x + x*r, c.y + y*r, c.z + z*r};
        for (int i = 0; i < corners.length; i++) for (int j = i + 1; j < corners.length; j++) {
            int diffs = 0; for (int k = 0; k < 3; k++) if (Math.abs(corners[i][k] - corners[j][k]) > 0.001) diffs++;
            if (diffs != 1) continue;
            for (int s = 0; s <= 4; s++) {
                double t = s / 4.0;
                world.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                    MathHelper.lerp(t, corners[i][0], corners[j][0]),
                    MathHelper.lerp(t, corners[i][1], corners[j][1]),
                    MathHelper.lerp(t, corners[i][2], corners[j][2]), 1, 0, 0, 0, 0);
            }
        }
    }

    public static boolean isActive(UUID id) { return ACTIVE.contains(id); }
}
