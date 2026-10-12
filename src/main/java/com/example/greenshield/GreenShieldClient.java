package com.example.greenshield;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.particle.ParticleTypes;
import org.lwjgl.glfw.GLFW;

public class GreenShieldClient implements ClientModInitializer {
    private KeyBinding toggleKey;
    private boolean active = false;
    private int particleTick = 0;
    private static final Identifier SHIELD_TEXTURE = new Identifier(GreenShield.MOD_ID, "textures/entity/shield_texture_1.png");

    @Override public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.green_shield.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G,
            "category.green_shield"));
        // Draw six translucent textured faces around the local player while active.
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (!active || context.world() == null || context.camera() == null) return;
            Camera camera = context.camera();
            Vec3d c = camera.getFocusedEntity() == null ? Vec3d.ZERO : camera.getFocusedEntity().getPos().add(0, 1.0, 0);
            double r = GreenShield.HALF_SIZE;
            MatrixStack matrices = context.matrixStack();
            if (matrices == null) return;
            matrices.push();
            matrices.translate(-camera.getPos().x, -camera.getPos().y, -camera.getPos().z);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
            RenderSystem.setShaderTexture(0, SHIELD_TEXTURE);
            BufferBuilder b = Tessellator.getInstance().getBuffer();
            b.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            float alpha = 58.0f / 255.0f;
            // Six faces; alpha keeps the player and world visible through the green energy field.
            quad(b, c.x-r,c.y-r,c.z+r, c.x+r,c.y-r,c.z+r, c.x+r,c.y+r,c.z+r, c.x-r,c.y+r,c.z+r, alpha);
            quad(b, c.x+r,c.y-r,c.z-r, c.x-r,c.y-r,c.z-r, c.x-r,c.y+r,c.z-r, c.x+r,c.y+r,c.z-r, alpha);
            quad(b, c.x-r,c.y-r,c.z-r, c.x-r,c.y-r,c.z+r, c.x-r,c.y+r,c.z+r, c.x-r,c.y+r,c.z-r, alpha);
            quad(b, c.x+r,c.y-r,c.z+r, c.x+r,c.y-r,c.z-r, c.x+r,c.y+r,c.z-r, c.x+r,c.y+r,c.z+r, alpha);
            quad(b, c.x-r,c.y+r,c.z+r, c.x+r,c.y+r,c.z+r, c.x+r,c.y+r,c.z-r, c.x-r,c.y+r,c.z-r, alpha);
            quad(b, c.x-r,c.y-r,c.z-r, c.x+r,c.y-r,c.z-r, c.x+r,c.y-r,c.z+r, c.x-r,c.y-r,c.z+r, alpha);
            BufferRenderer.drawWithGlobalProgram(b.end());
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            matrices.pop();
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                if (client.player == null) continue;
                active = !active;
                ClientPlayNetworking.send(GreenShield.TOGGLE_PACKET, net.fabricmc.fabric.api.networking.v1.PacketByteBufs.empty());
                client.player.sendMessage(Text.literal(active ? "§aGreen Shield: ACTIVANDO" : "§7Green Shield: DESACTIVANDO"), true);
            }
            if (!active || client.player == null || client.world == null) return;
            particleTick++;
            if (particleTick % 2 != 0) return;
            Vec3d c = client.player.getPos().add(0, 1.0, 0);
            double r = GreenShield.HALF_SIZE;
            // Client-side green cube edges; server-side particles make them visible to others too.
            for (int a = -1; a <= 1; a += 2) for (int b = -1; b <= 1; b += 2) {
                for (int s = 0; s <= 6; s++) {
                    double t = -r + 2*r*s/6.0;
                    client.world.addParticle(ParticleTypes.HAPPY_VILLAGER, c.x+t, c.y+a*r, c.z+b*r, 0, 0, 0);
                    client.world.addParticle(ParticleTypes.HAPPY_VILLAGER, c.x+a*r, c.y+t, c.z+b*r, 0, 0, 0);
                    client.world.addParticle(ParticleTypes.HAPPY_VILLAGER, c.x+a*r, c.y+b*r, c.z+t, 0, 0, 0);
                }
            }
        });
    }

    private static void quad(BufferBuilder b,
                             double x1,double y1,double z1, double x2,double y2,double z2,
                             double x3,double y3,double z3, double x4,double y4,double z4,
                             float alpha) {
        b.vertex(x1,y1,z1).texture(0,1).color(40,255,90,(int)(alpha*255)).next();
        b.vertex(x2,y2,z2).texture(1,1).color(40,255,90,(int)(alpha*255)).next();
        b.vertex(x3,y3,z3).texture(1,0).color(40,255,90,(int)(alpha*255)).next();
        b.vertex(x4,y4,z4).texture(0,0).color(40,255,90,(int)(alpha*255)).next();
    }
}
