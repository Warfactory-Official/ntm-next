/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.render.item.weapon.sedna;

import com.hbm.items.weapon.sedna.GunBaseNTItem;
import com.hbm.items.weapon.sedna.mags.Magazine;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.hbm.registration.RegistryHandle;
import com.hbm.render.item.weapon.sedna.WeaponItemRenderer;
import com.hbm.render.loader.HFRWavefrontObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public final class GunRenderDefinition {

    private final GunAnimationChannels rig;
    private final Node[] setup;
    private final Node[] body;

    private GunRenderDefinition(GunAnimationChannels rig, Node[] setup, Node[] body) {
        this.rig = rig;
        this.setup = setup;
        this.body = body;
    }

    public static Builder on(GunAnimationChannels rig) {
        return new Builder(rig, null);
    }

    public static Predicate<Reading> fitted(RegistryHandle<XWeaponModManager.Declaration> id) {
        return r -> XWeaponModManager.hasUpgrade(r.stack(), r.config(), id);
    }

    public static Magazine<?> magazine(Reading r) {
        return r.gun()
                .getConfig(r.stack(), 0)
                .getReceivers(r.stack())
                .get(0)
                .getMagazine(r.stack());
    }

    public GunAnimationChannels rig() {
        return rig;
    }

    public void setupFirstPerson(GunRenderState frame, PoseStack pose) {
        new Live(frame, pose, null, 0).run(setup);
    }

    public void renderFirstPerson(
            GunRenderState frame, PoseStack pose, SubmitNodeCollector collector, int light) {
        new Live(frame, pose, collector, light).run(body);
    }

    public void restBody(ItemStack stack, RestBody target) {
        var rest = new Rest(stack, target);
        var pose = new Matrix4f();
        rest.run(setup, pose);
        rest.run(body, pose);
    }

    public interface Reading {
        ItemStack stack();

        default GunBaseNTItem gun() {
            return (GunBaseNTItem) stack().getItem();
        }

        int config();

        double get(GunAnimationChannels.Scalar channel);

        double x(GunAnimationChannels.Vec3 channel);

        double y(GunAnimationChannels.Vec3 channel);

        double z(GunAnimationChannels.Vec3 channel);

        double rotation(GunAnimationChannels.Part channel, int axis);

        boolean carries(GunAnimationChannels.Channel channel);

        float aim();

        float tickAim();

        boolean steadyAim();

        double shotRandom();

        @Nullable Inventory inventory();
    }

    @FunctionalInterface
    public interface Drive {
        double at(Reading r);
    }

    public interface Drawing extends Reading {
        GunRenderState frame();

        PoseStack pose();

        SubmitNodeCollector collector();

        int light();

        RenderType skin();
    }

    @FunctionalInterface
    public interface LiveDraw {
        void draw(Drawing d);
    }

    @FunctionalInterface
    public interface RestDraw {
        void pose(Reading r, Matrix4f pose, RestBody target);
    }

    private static final class Live implements Drawing {
        final GunRenderState frame;
        final PoseStack pose;
        final SubmitNodeCollector collector;
        final int light;
        int config;
        RenderType skin;

        Live(GunRenderState frame, PoseStack pose, SubmitNodeCollector collector, int light) {
            this.frame = frame;
            this.pose = pose;
            this.collector = collector;
            this.light = light;
        }

        void run(Node[] nodes) {
            var skin = this.skin;
            for (var node : nodes) node.draw(this);
            this.skin = skin;
        }

        public GunRenderState frame() {
            return frame;
        }

        public PoseStack pose() {
            return pose;
        }

        public SubmitNodeCollector collector() {
            return collector;
        }

        public int light() {
            return light;
        }

        public RenderType skin() {
            return skin;
        }

        public ItemStack stack() {
            return frame.getItemStack();
        }

        public GunBaseNTItem gun() {
            return frame.getGun();
        }

        public int config() {
            return config;
        }

        public double get(GunAnimationChannels.Scalar channel) {
            return frame.get(config, channel);
        }

        public double x(GunAnimationChannels.Vec3 channel) {
            return frame.x(config, channel);
        }

        public double y(GunAnimationChannels.Vec3 channel) {
            return frame.y(config, channel);
        }

        public double z(GunAnimationChannels.Vec3 channel) {
            return frame.z(config, channel);
        }

        public double rotation(GunAnimationChannels.Part channel, int axis) {
            return frame.rotation(config, channel, axis);
        }

        public boolean carries(GunAnimationChannels.Channel channel) {
            return frame.carries(config, channel);
        }

        public float aim() {
            return frame.aim();
        }

        public float tickAim() {
            return frame.tickAim();
        }

        public boolean steadyAim() {
            return frame.steadyAim();
        }

        public double shotRandom() {
            return frame.shotRandom();
        }

        public Inventory inventory() {
            return Minecraft.getInstance().player.getInventory();
        }
    }

    private static final class Rest implements Reading {
        final ItemStack stack;
        final RestBody target;
        int config;
        Identifier skin;
        boolean hidden;

        Rest(ItemStack stack, RestBody target) {
            this.stack = stack;
            this.target = target;
        }

        void run(Node[] nodes, Matrix4f pose) {
            var skin = this.skin;
            boolean hidden = this.hidden;
            for (var node : nodes) node.pose(this, pose);
            this.skin = skin;
            this.hidden = hidden;
        }

        public ItemStack stack() {
            return stack;
        }

        public int config() {
            return config;
        }

        public double get(GunAnimationChannels.Scalar channel) {
            return 0;
        }

        public double x(GunAnimationChannels.Vec3 channel) {
            return 0;
        }

        public double y(GunAnimationChannels.Vec3 channel) {
            return 0;
        }

        public double z(GunAnimationChannels.Vec3 channel) {
            return 0;
        }

        public double rotation(GunAnimationChannels.Part channel, int axis) {
            return 0;
        }

        public boolean carries(GunAnimationChannels.Channel channel) {
            return false;
        }

        public float aim() {
            return 0;
        }

        public float tickAim() {
            return 0;
        }

        public boolean steadyAim() {
            return false;
        }

        public double shotRandom() {
            return 0;
        }

        public @Nullable Inventory inventory() {
            return null;
        }
    }

    private interface Node {
        void draw(Live live);

        void pose(Rest rest, Matrix4f pose);
    }

    private record Shift(float x, float y, float z) implements Node {
        public void draw(Live live) {
            live.pose.translate(x, y, z);
        }

        public void pose(Rest rest, Matrix4f pose) {
            pose.translate(x, y, z);
        }
    }

    private record ShiftBy(Drive x, Drive y, Drive z) implements Node {
        public void draw(Live live) {
            live.pose.translate(x.at(live), y.at(live), z.at(live));
        }

        public void pose(Rest rest, Matrix4f pose) {
            pose.translate((float) x.at(rest), (float) y.at(rest), (float) z.at(rest));
        }
    }

    private record Slide(int axis, Drive by) implements Node {
        public void draw(Live live) {
            double v = by.at(live);
            live.pose.translate(axis == 0 ? v : 0, axis == 1 ? v : 0, axis == 2 ? v : 0);
        }

        public void pose(Rest rest, Matrix4f pose) {
            float v = (float) by.at(rest);
            if (v != 0) pose.translate(axis == 0 ? v : 0, axis == 1 ? v : 0, axis == 2 ? v : 0);
        }
    }

    private record Move(GunAnimationChannels.Vec3 channel) implements Node {
        public void draw(Live live) {
            live.frame.translate(live.pose, live.config, channel);
        }

        public void pose(Rest rest, Matrix4f pose) {}
    }

    private record Apply(GunAnimationChannels.Part channel) implements Node {
        public void draw(Live live) {
            live.frame.apply(live.pose, live.config, channel);
        }

        public void pose(Rest rest, Matrix4f pose) {}
    }

    private record Turn(Quaternionf rotation) implements Node {
        public void draw(Live live) {
            live.pose.rotate((Quaternionfc) rotation);
        }

        public void pose(Rest rest, Matrix4f pose) {
            pose.rotate(rotation);
        }
    }

    private record Spin(Axis axis, Drive degrees) implements Node {
        public void draw(Live live) {
            live.pose.rotate((Quaternionfc) axis.rotationDegrees((float) degrees.at(live)));
        }

        public void pose(Rest rest, Matrix4f pose) {
            float angle = (float) degrees.at(rest);
            if (angle != 0) pose.rotate(axis.rotationDegrees(angle));
        }
    }

    private record Size(float x, float y, float z) implements Node {
        public void draw(Live live) {
            live.pose.scale(x, y, z);
        }

        public void pose(Rest rest, Matrix4f pose) {
            pose.scale(x, y, z);
        }
    }

    private record SizeBy(Drive x, Drive y, Drive z) implements Node {
        public void draw(Live live) {
            live.pose.scale((float) x.at(live), (float) y.at(live), (float) z.at(live));
        }

        public void pose(Rest rest, Matrix4f pose) {
            float sx = (float) x.at(rest), sy = (float) y.at(rest), sz = (float) z.at(rest);
            if (sx == 0 || sy == 0 || sz == 0) rest.hidden = true;
            else pose.scale(sx, sy, sz);
        }
    }

    private record Aim(
            double restX, double restY, double restZ, double aimX, double aimY, double aimZ)
            implements Node {
        public void draw(Live live) {
            float progress = live.aim();
            live.pose.translate(
                    restX + (aimX - restX) * progress,
                    restY + (aimY - restY) * progress,
                    restZ + (aimZ - restZ) * progress);
        }

        public void pose(Rest rest, Matrix4f pose) {
            pose.translate((float) restX, (float) restY, (float) restZ);
        }
    }

    private record AimBy(Drive restX, Drive restY, Drive restZ, Drive aimX, Drive aimY, Drive aimZ)
            implements Node {
        public void draw(Live live) {
            float progress = live.aim();
            double x = restX.at(live), y = restY.at(live), z = restZ.at(live);
            live.pose.translate(
                    x + (aimX.at(live) - x) * progress,
                    y + (aimY.at(live) - y) * progress,
                    z + (aimZ.at(live) - z) * progress);
        }

        public void pose(Rest rest, Matrix4f pose) {
            pose.translate((float) restX.at(rest), (float) restY.at(rest), (float) restZ.at(rest));
        }
    }

    private record About(float x, float y, float z, Node[] nodes) implements Node {
        public void draw(Live live) {
            live.pose.translate(x, y, z);
            live.run(nodes);
            live.pose.translate(0 - x, 0 - y, 0 - z);
        }

        public void pose(Rest rest, Matrix4f pose) {
            pose.translate(x, y, z);
            rest.run(nodes, pose);
            pose.translate(0 - x, 0 - y, 0 - z);
        }
    }

    private record Group(Node[] nodes) implements Node {
        public void draw(Live live) {
            live.pose.pushPose();
            live.run(nodes);
            live.pose.popPose();
        }

        public void pose(Rest rest, Matrix4f pose) {
            rest.run(nodes, new Matrix4f(pose));
        }
    }

    private record When(Predicate<Reading> test, Node[] then, Node[] otherwise) implements Node {
        public void draw(Live live) {
            live.run(test.test(live) ? then : otherwise);
        }

        public void pose(Rest rest, Matrix4f pose) {
            rest.run(test.test(rest) ? then : otherwise, pose);
        }
    }

    private record Config(int index, Node[] nodes) implements Node {
        public void draw(Live live) {
            int outer = live.config;
            live.config = index;
            live.run(nodes);
            live.config = outer;
        }

        public void pose(Rest rest, Matrix4f pose) {
            int outer = rest.config;
            rest.config = index;
            rest.run(nodes, pose);
            rest.config = outer;
        }
    }

    private record Skin(
            @Nullable Predicate<Reading> test,
            Identifier texture,
            RenderType type,
            @Nullable Identifier otherTexture,
            @Nullable RenderType otherType)
            implements Node {
        public void draw(Live live) {
            live.skin = test == null || test.test(live) ? type : otherType;
        }

        public void pose(Rest rest, Matrix4f pose) {
            rest.skin = test == null || test.test(rest) ? texture : otherTexture;
        }
    }

    private record SkinBy(Function<Reading, Identifier> pick) implements Node {
        public void draw(Live live) {
            live.skin = RenderTypes.entityCutout(pick.apply(live));
        }

        public void pose(Rest rest, Matrix4f pose) {
            rest.skin = pick.apply(rest);
        }
    }

    private record Part(
            HFRWavefrontObject mesh, int id, ToIntFunction<Reading> color, boolean fullBright)
            implements Node {
        public void draw(Live live) {
            WeaponItemRenderer.submitPart(
                    live.collector,
                    live.pose,
                    live.skin,
                    mesh,
                    id,
                    fullBright ? WeaponItemRenderer.FULL_BRIGHT : live.light,
                    color.applyAsInt(live));
        }

        public void pose(Rest rest, Matrix4f pose) {
            if (!rest.hidden)
                rest.target.part(pose, mesh, id, rest.skin, color.applyAsInt(rest), fullBright);
        }
    }

    private record Whole(HFRWavefrontObject mesh) implements Node {
        public void draw(Live live) {
            int light = live.light;
            live.collector.submitCustomGeometry(
                    live.pose, live.skin, (p, buffer) -> mesh.render(p, buffer, light, -1));
        }

        public void pose(Rest rest, Matrix4f pose) {
            if (!rest.hidden) rest.target.model(pose, mesh, rest.skin);
        }
    }

    private record Batch(Part[] parts) implements Node {
        public void draw(Live live) {
            int[] colors = new int[parts.length];
            for (int i = 0; i < parts.length; i++) colors[i] = parts[i].color.applyAsInt(live);
            int light = live.light;
            live.collector.submitCustomGeometry(
                    live.pose,
                    live.skin,
                    (p, buffer) -> {
                        for (int i = 0; i < parts.length; i++) {
                            var part = parts[i];
                            part.mesh.renderPart(
                                    p,
                                    buffer,
                                    part.fullBright ? WeaponItemRenderer.FULL_BRIGHT : light,
                                    colors[i],
                                    part.id);
                        }
                    });
        }

        public void pose(Rest rest, Matrix4f pose) {
            for (var part : parts) part.pose(rest, pose);
        }
    }

    private record Flash(MuzzleFlash kind, int duration, double length, int color) implements Node {
        public void draw(Live live) {
            kind.submit(
                    live.collector,
                    live.pose,
                    live.frame.lastShot(live.config),
                    duration,
                    length,
                    color);
        }

        public void pose(Rest rest, Matrix4f pose) {}
    }

    private record Smoke(double width) implements Node {
        public void draw(Live live) {
            WeaponItemRenderer.renderSmokeNodes(
                    live.collector, live.pose, live.frame.smoke(live.config), width, live.light);
        }

        public void pose(Rest rest, Matrix4f pose) {}
    }

    private record Custom(LiveDraw drawn, @Nullable RestDraw posed) implements Node {
        public void draw(Live live) {
            drawn.draw(live);
        }

        public void pose(Rest rest, Matrix4f pose) {
            if (posed != null && !rest.hidden) posed.pose(rest, pose, rest.target);
        }
    }

    private record Label(
            Function<Reading, String> text,
            ToIntFunction<Reading> color,
            float x,
            float y,
            float z,
            Axis along,
            Quaternionf[] turns)
            implements Node {
        public void draw(Live live) {
            String label = text.apply(live);
            float scale = WeaponItemRenderer.LABEL_SCALE;
            float half = (Minecraft.getInstance().font.width(label) / 2) * scale;
            var pose = live.pose;
            pose.pushPose();
            pose.translate(
                    along == Axis.XP ? x + half : along == Axis.XN ? x - half : x,
                    along == Axis.YP ? y + half : along == Axis.YN ? y - half : y,
                    along == Axis.ZP ? z + half : along == Axis.ZN ? z - half : z);
            pose.scale(scale, -scale, scale);
            for (var turn : turns) pose.rotate((Quaternionfc) turn);
            live.collector.submitText(
                    pose,
                    0,
                    0,
                    Component.literal(label).getVisualOrderText(),
                    false,
                    Font.DisplayMode.NORMAL,
                    WeaponItemRenderer.FULL_BRIGHT,
                    color.applyAsInt(live),
                    0,
                    0);
            pose.popPose();
        }

        public void pose(Rest rest, Matrix4f pose) {
            if (rest.hidden) return;
            float scale = WeaponItemRenderer.LABEL_SCALE;
            var after = new Matrix4f().scale(scale, -scale, scale);
            for (var turn : turns) after.rotate(turn);
            float sign = along == Axis.XP || along == Axis.YP || along == Axis.ZP ? scale : -scale;
            var shift =
                    new Vector3f(
                            along == Axis.XP || along == Axis.XN ? sign : 0,
                            along == Axis.YP || along == Axis.YN ? sign : 0,
                            along == Axis.ZP || along == Axis.ZN ? sign : 0);
            rest.target.text(
                    new Matrix4f(pose).translate(x, y, z),
                    shift,
                    after,
                    text.apply(rest),
                    color.applyAsInt(rest));
        }
    }

    public static final class Builder {
        private final GunAnimationChannels rig;
        private final List<Node> setup = new ArrayList<>();
        private final List<Node> nodes = new ArrayList<>();
        private final boolean root;
        private boolean inBody;
        private @Nullable HFRWavefrontObject mesh;
        private boolean skinned;

        private Builder(GunAnimationChannels rig, @Nullable Builder parent) {
            this.rig = rig;
            root = parent == null;
            inBody = !root;
            if (parent != null) {
                mesh = parent.mesh;
                skinned = parent.skinned;
            }
        }

        private Builder add(Node node) {
            (inBody ? nodes : setup).add(node);
            return this;
        }

        public Builder body() {
            if (!root || inBody) throw new IllegalStateException("body() ends the root setup once");
            inBody = true;
            return this;
        }

        public Builder shift(double x, double y, double z) {
            return add(new Shift((float) x, (float) y, (float) z));
        }

        public Builder shift(Drive x, Drive y, Drive z) {
            return add(new ShiftBy(x, y, z));
        }

        public Builder slideX(Drive by) {
            return add(new Slide(0, by));
        }

        public Builder slideY(Drive by) {
            return add(new Slide(1, by));
        }

        public Builder slideZ(Drive by) {
            return add(new Slide(2, by));
        }

        public Builder move(GunAnimationChannels.Vec3 channel) {
            return add(new Move(channel));
        }

        public Builder apply(GunAnimationChannels.Part channel) {
            return add(new Apply(channel));
        }

        public Builder turn(Axis axis, double degrees) {
            return add(new Turn(axis.rotationDegrees((float) degrees)));
        }

        public Builder spin(Axis axis, Drive degrees) {
            return add(new Spin(axis, degrees));
        }

        public Builder size(double scale) {
            return add(new Size((float) scale, (float) scale, (float) scale));
        }

        public Builder size(Drive x, Drive y, Drive z) {
            return add(new SizeBy(x, y, z));
        }

        public Builder aim(
                double restX, double restY, double restZ, double aimX, double aimY, double aimZ) {
            return add(new Aim(restX, restY, restZ, aimX, aimY, aimZ));
        }

        public Builder aim(
                Drive restX, Drive restY, Drive restZ, Drive aimX, Drive aimY, Drive aimZ) {
            return add(new AimBy(restX, restY, restZ, aimX, aimY, aimZ));
        }

        public Builder about(double x, double y, double z, Consumer<Builder> nodes) {
            return add(new About((float) x, (float) y, (float) z, child(nodes)));
        }

        public Builder hinge(double x, double y, double z, Axis axis, Drive degrees) {
            return about(x, y, z, h -> h.spin(axis, degrees));
        }

        public Builder group(Consumer<Builder> nodes) {
            return add(new Group(child(nodes)));
        }

        public Builder when(Predicate<Reading> test, Consumer<Builder> nodes) {
            return add(new When(test, child(nodes), new Node[0]));
        }

        public Builder either(
                Predicate<Reading> test, Consumer<Builder> then, Consumer<Builder> otherwise) {
            return add(new When(test, child(then), child(otherwise)));
        }

        public Builder config(int index, Consumer<Builder> nodes) {
            return add(new Config(index, child(nodes)));
        }

        public Builder mesh(HFRWavefrontObject mesh) {
            this.mesh = mesh;
            return this;
        }

        public Builder skin(Identifier texture) {
            skinned = true;
            return add(new Skin(null, texture, RenderTypes.entityCutout(texture), null, null));
        }

        public Builder skin(Predicate<Reading> test, Identifier texture, Identifier otherwise) {
            skinned = true;
            return add(
                    new Skin(
                            test,
                            texture,
                            RenderTypes.entityCutout(texture),
                            otherwise,
                            RenderTypes.entityCutout(otherwise)));
        }

        public Builder skin(Function<Reading, Identifier> pick) {
            skinned = true;
            return add(new SkinBy(pick));
        }

        public Builder part(String name) {
            return part(name, _ -> -1, false);
        }

        public Builder part(String name, ToIntFunction<Reading> color) {
            return part(name, color, false);
        }

        public Builder glow(String name) {
            return part(name, _ -> -1, true);
        }

        private Builder part(String name, ToIntFunction<Reading> color, boolean fullBright) {
            if (mesh == null || !skinned)
                throw new IllegalStateException("part " + name + " needs a mesh and a skin");
            return add(new Part(mesh, mesh.partId(name), color, fullBright));
        }

        public Builder whole() {
            if (mesh == null || !skinned)
                throw new IllegalStateException("whole() needs a mesh and a skin");
            return add(new Whole(mesh));
        }

        public Builder batch(Consumer<Builder> parts) {
            var nodes = child(parts);
            var batched = new Part[nodes.length];
            for (int i = 0; i < nodes.length; i++) {
                if (!(nodes[i] instanceof Part part))
                    throw new IllegalStateException("a batch holds parts alone");
                batched[i] = part;
            }
            return add(new Batch(batched));
        }

        public Builder roll(int degrees) {
            return spin(Axis.XP, r -> degrees * r.shotRandom());
        }

        public Builder label(
                Function<Reading, String> text,
                ToIntFunction<Reading> color,
                double x,
                double y,
                double z,
                Axis along,
                Quaternionf... turns) {
            return add(new Label(text, color, (float) x, (float) y, (float) z, along, turns));
        }

        public Builder flash(MuzzleFlash kind, int duration, double length) {
            return flash(kind, duration, length, 0);
        }

        public Builder flash(MuzzleFlash kind, int duration, double length, int color) {
            return add(new Flash(kind, duration, length, color));
        }

        public Builder smoke(double width) {
            return add(new Smoke(width));
        }

        public Builder custom(LiveDraw live, @Nullable RestDraw rest) {
            return add(new Custom(live, rest));
        }

        public GunRenderDefinition build() {
            if (!root) throw new IllegalStateException("only the root builds");
            return new GunRenderDefinition(
                    rig, setup.toArray(new Node[0]), nodes.toArray(new Node[0]));
        }

        private Node[] child(Consumer<Builder> declare) {
            var child = new Builder(rig, this);
            declare.accept(child);
            return child.nodes.toArray(new Node[0]);
        }
    }
}
