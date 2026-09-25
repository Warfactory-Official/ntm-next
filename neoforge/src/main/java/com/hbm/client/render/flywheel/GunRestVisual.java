// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.client.render.WeaponRenderTypes;
import com.hbm.items.weapon.sedna.AkimboGhost;
import com.hbm.main.ResourceManager;
import com.hbm.render.item.weapon.sedna.ItemRenderWeaponBase;
import com.hbm.render.item.weapon.sedna.RestBody;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.render.loader.UnitQuad;
import com.hbm.util.GameTime;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.AffineUvTransformedInstance;
import dev.engine_room.flywheel.lib.instance.PosedInstance;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public final class GunRestVisual extends HbmItemVisual {
    private final ItemRenderWeaponBase renderer;
    private final boolean otherArm;
    private final List<Element> declared;
    private final List<Posing> posings = new ArrayList<>();
    private final List<WorldText> texts = new ArrayList<>();
    private final Matrix4f frame = new Matrix4f();
    private final Matrix4f spun = new Matrix4f();
    private final Matrix4f world = new Matrix4f();
    private final Matrix3f normal = new Matrix3f();

    public GunRestVisual(
            VisualizationContext ctx,
            ItemRenderWeaponBase renderer,
            ItemStack stack,
            boolean otherArm) {
        super(ctx);
        this.renderer = renderer;
        this.otherArm = otherArm;
        declared = declare(renderer, stack);
        for (Element element : declared) posings.add(element.create(this));
    }

    private static List<Element> declare(ItemRenderWeaponBase renderer, ItemStack stack) {
        List<Element> elements = new ArrayList<>();
        renderer.restBody(stack, new Declaration(elements, null));
        return elements;
    }

    @Override
    public boolean update(ItemStack stack) {
        return !AkimboGhost.isGhost(stack) && declare(renderer, stack).equals(declared);
    }

    @Override
    protected void frame(Matrix4fc pose, int light, int overlay, float partialTick) {
        ItemRenderWeaponBase.restFrame(frame.set(pose), otherArm);
        @Nullable Spin last = null;
        for (int i = 0; i < declared.size(); i++) {
            Element element = declared.get(i);
            Spin spin = element.spin();
            Matrix4fc base = frame;
            if (spin != null) {
                if (!spin.equals(last)) {
                    spun.set(frame)
                            .mul(spin.pivot())
                            .rotate(
                                    Axis.ZP.rotationDegrees(
                                            (float) (GameTime.now() / spin.msPerDegree() % 360D)));
                    last = spin;
                }
                base = spun;
            }
            posings.get(i).pose(base, light);
        }
    }

    private Matrix4f place(Matrix4fc base, Matrix4fc pose) {
        world.set(base).mul(pose);
        if (otherArm) world.scale(-1F, 1F, 1F);
        return world;
    }

    @Override
    public void hide() {
        super.hide();
        for (WorldText text : texts) text.setVisible(false);
    }

    @Override
    public void delete() {
        super.delete();
        for (WorldText text : texts) text.delete();
        texts.clear();
    }

    @FunctionalInterface
    private interface Posing {
        void pose(Matrix4fc base, int light);
    }

    private sealed interface Element permits Part, Quad, Glint, Text {
        @Nullable Spin spin();

        Posing create(GunRestVisual visual);
    }

    private record Spin(Matrix4f pivot, double msPerDegree) {}

    private record Part(
            Matrix4f pose,
            HFRWavefrontObject model,
            int part,
            Identifier texture,
            int color,
            boolean fullBright,
            @Nullable Spin spin)
            implements Element {
        @Override
        public Posing create(GunRestVisual visual) {
            TransformedInstance instance =
                    visual.transformed(
                            ItemMaterials.part(model, part, ItemMaterials.cutout(texture, false)));
            return (base, light) ->
                    write(
                            instance,
                            visual.place(base, pose),
                            color,
                            fullBright ? ItemRenderWeaponBase.FULL_BRIGHT : light);
        }
    }

    private record Quad(
            Matrix4f pose, Matrix4f corners, UnitQuad quad, Identifier texture, @Nullable Spin spin)
            implements Element {
        @Override
        public Posing create(GunRestVisual visual) {
            PosedInstance instance =
                    visual.posed(
                            ItemMaterials.group(
                                    quad.group(), true, ItemMaterials.flatCutout(texture, false)));
            Matrix4f cornered = new Matrix4f();
            return (base, light) -> {
                Matrix4f at = visual.place(base, pose);
                visual.normal.set(at).invert().transpose();
                write(instance, cornered.set(at).mul(corners), visual.normal, -1, light);
            };
        }
    }

    private record Glint(Matrix4f pose, HFRWavefrontObject model, int part, @Nullable Spin spin)
            implements Element {
        @Override
        public Posing create(GunRestVisual visual) {
            AffineUvTransformedInstance[] layers = new AffineUvTransformedInstance[3];
            for (int layer = 0; layer < layers.length; layer++) {
                layers[layer] =
                        visual.affineUv(
                                ItemMaterials.part(
                                        model,
                                        part,
                                        ItemMaterials.balefireGlint(ResourceManager.glint_bf_tex)));
            }
            return (base, light) -> {
                Matrix4f at = visual.place(base, pose);
                for (int layer = 0; layer < layers.length; layer++) {

                    Matrix4f uv =
                            WeaponRenderTypes.balefireGlintMatrix(
                                    layer, WeaponRenderTypes.FATMAN_GLINT_SPEED);
                    layers[layer].uv(uv.m00(), uv.m10(), uv.m01(), uv.m11(), uv.m30(), uv.m31());
                    write(layers[layer], at, WeaponRenderTypes.BALEFIRE_GLINT_TINT, light);
                }
            };
        }
    }

    private record Text(
            Matrix4f before,
            Vector3f shift,
            Matrix4f after,
            String text,
            int color,
            @Nullable Spin spin)
            implements Element {
        @Override
        public Posing create(GunRestVisual visual) {
            WorldText line = new WorldText(visual.instancers, WorldText.Style.NORMAL, true);
            line.set(Component.literal(text), 0F, 0F, color);
            visual.texts.add(line);
            return (base, light) -> {
                line.setVisible(true);
                line.write(
                        (width, out) -> {
                            float half = width / 2;
                            out.set(base)
                                    .mul(before)
                                    .translate(shift.x() * half, shift.y() * half, shift.z() * half)
                                    .mul(after);
                            if (visual.otherArm) out.scale(-1F, 1F, 1F);
                        });
            };
        }
    }

    private record Declaration(List<Element> elements, @Nullable Spin spin) implements RestBody {
        @Override
        public void part(
                Matrix4fc pose,
                HFRWavefrontObject model,
                int part,
                Identifier texture,
                int color,
                boolean fullBright) {
            elements.add(
                    new Part(new Matrix4f(pose), model, part, texture, color, fullBright, spin));
        }

        @Override
        public void quad(Matrix4fc pose, Matrix4fc corners, UnitQuad quad, Identifier texture) {
            elements.add(new Quad(new Matrix4f(pose), new Matrix4f(corners), quad, texture, spin));
        }

        @Override
        public void balefireGlint(Matrix4fc pose, HFRWavefrontObject model, int part) {
            elements.add(new Glint(new Matrix4f(pose), model, part, spin));
        }

        @Override
        public void text(
                Matrix4fc before, Vector3fc shift, Matrix4fc after, String text, int color) {
            elements.add(
                    new Text(
                            new Matrix4f(before),
                            new Vector3f(shift),
                            new Matrix4f(after),
                            text,
                            color,
                            spin));
        }

        @Override
        public void spinZ(Matrix4fc pivot, double msPerDegree, Consumer<RestBody> turned) {
            assert spin == null;
            turned.accept(new Declaration(elements, new Spin(new Matrix4f(pivot), msPerDegree)));
        }
    }
}
