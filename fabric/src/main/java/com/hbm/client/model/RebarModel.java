// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.blockentity.BlockEntityRebar;
import com.hbm.config.RenderConfig;
import com.mojang.math.Quadrant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public final class RebarModel implements BlockStateModel {
    private static final float LO = 7F;
    private static final float OFFSET = 4F;
    private static final float MIN = -0.016F;
    private static final float MAX = 16.016F;

    private final ModelBaker baker;
    private final ResolvedModel base;
    private final Material.Baked particle;
    private final Material.Baked concrete;
    private final BlockStateModelPart rods;
    private final BlockStateModelPart simple;
    private final @Nullable BlockStateModelPart[] fills =
            new BlockStateModelPart[BlockEntityRebar.FULL + 1];

    private RebarModel(ModelBaker baker, ResolvedModel base) {
        this.baker = RuntimeBaker.capture(baker, base);
        this.base = base;
        TextureSlots slots = base.getTopTextureSlots();
        this.particle = base.resolveParticleMaterial(slots, baker);
        Material.Baked rebar = BlockModel.slot(baker, base, slots, "rebar");
        this.concrete = BlockModel.slot(baker, base, slots, "fill");

        List<ObjUnbakedGeometry.BoxQuad> full = new ArrayList<>();
        {
            {
                {
                    {
                        {
                            {
                                final Vector3f from = new Vector3f(3.0F, MIN, 3.0F);
                                final Vector3f to = new Vector3f(3.0F + 2F, MAX, 3.0F + 2F);
                                this.faces(
                                        full,
                                        rebar,
                                        from,
                                        to,
                                        new UvRotation[] {
                                            new UvRotation(
                                                    new CuboidFace.UVs(3.0F, 5.0F, 5.0F, 3.0F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(3.0F, 3.0F, 5.0F, 5.0F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            11.0F, -0.016000748F, 13.0F, 16.016F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            3.0F, -0.016000748F, 5.0F, 16.016F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            3.0F, -0.016000748F, 5.0F, 16.016F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            11.0F, -0.016000748F, 13.0F, 16.016F),
                                                    Quadrant.R0)
                                        });
                            }
                        }
                        {
                            {
                                final Vector3f from2 = new Vector3f(MIN, 3.0F, 3.0F);
                                final Vector3f to2 = new Vector3f(MAX, 3.0F + 2F, 3.0F + 2F);
                                this.faces(
                                        full,
                                        rebar,
                                        from2,
                                        to2,
                                        new UvRotation[] {
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            -0.016F, 5.0F, 16.016F, 3.0F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            -0.016F, 3.0F, 16.016F, 5.0F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            -0.016000748F, 11.0F, 16.016F, 13.0F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            -0.016F, 11.0F, 16.016F, 13.0F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(3.0F, 11.0F, 5.0F, 13.0F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(11.0F, 11.0F, 13.0F, 13.0F),
                                                    Quadrant.R0)
                                        });
                            }
                        }
                        {
                            {
                                final Vector3f from3 = new Vector3f(3.0F, 3.0F, MIN);
                                final Vector3f to3 = new Vector3f(3.0F + 2F, 3.0F + 2F, MAX);
                                this.faces(
                                        full,
                                        rebar,
                                        from3,
                                        to3,
                                        new UvRotation[] {
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            3.0F, 16.016F, 5.0F, -0.016F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            3.0F, -0.016F, 5.0F, 16.016F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(11.0F, 11.0F, 13.0F, 13.0F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(3.0F, 11.0F, 5.0F, 13.0F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            -0.016F, 11.0F, 16.016F, 13.0F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            -0.016000748F, 11.0F, 16.016F, 13.0F),
                                                    Quadrant.R0)
                                        });
                            }
                        }
                    }
                    {
                        {
                            {
                                final Vector3f from4 = new Vector3f(3.0F, MIN, 11.0F);
                                final Vector3f to4 = new Vector3f(3.0F + 2F, MAX, 11.0F + 2F);
                                this.faces(
                                        full,
                                        rebar,
                                        from4,
                                        to4,
                                        new UvRotation[] {
                                            new UvRotation(
                                                    new CuboidFace.UVs(3.0F, 13.0F, 5.0F, 11.0F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(3.0F, 11.0F, 5.0F, 13.0F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            11.0F, -0.016000748F, 13.0F, 16.016F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            3.0F, -0.016000748F, 5.0F, 16.016F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            11.0F, -0.016000748F, 13.0F, 16.016F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            3.0F, -0.016000748F, 5.0F, 16.016F),
                                                    Quadrant.R0)
                                        });
                            }
                        }
                        {
                            {
                                final Vector3f from5 = new Vector3f(MIN, 3.0F, 11.0F);
                                final Vector3f to5 = new Vector3f(MAX, 3.0F + 2F, 11.0F + 2F);
                                this.faces(
                                        full,
                                        rebar,
                                        from5,
                                        to5,
                                        new UvRotation[] {
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            -0.016F, 13.0F, 16.016F, 11.0F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            -0.016F, 11.0F, 16.016F, 13.0F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            -0.016000748F, 11.0F, 16.016F, 13.0F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            -0.016F, 11.0F, 16.016F, 13.0F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(11.0F, 11.0F, 13.0F, 13.0F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(3.0F, 11.0F, 5.0F, 13.0F),
                                                    Quadrant.R0)
                                        });
                            }
                        }
                        {
                            {
                                final Vector3f from6 = new Vector3f(3.0F, 11.0F, MIN);
                                final Vector3f to6 = new Vector3f(3.0F + 2F, 11.0F + 2F, MAX);
                                this.faces(
                                        full,
                                        rebar,
                                        from6,
                                        to6,
                                        new UvRotation[] {
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            3.0F, 16.016F, 5.0F, -0.016F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            3.0F, -0.016F, 5.0F, 16.016F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(11.0F, 3.0F, 13.0F, 5.0F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(3.0F, 3.0F, 5.0F, 5.0F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            -0.016F, 3.0F, 16.016F, 5.0F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            -0.016000748F, 3.0F, 16.016F, 5.0F),
                                                    Quadrant.R0)
                                        });
                            }
                        }
                    }
                }
            }
            {
                {
                    {
                        {
                            {
                                final Vector3f from7 = new Vector3f(11.0F, MIN, 3.0F);
                                final Vector3f to7 = new Vector3f(11.0F + 2F, MAX, 3.0F + 2F);
                                this.faces(
                                        full,
                                        rebar,
                                        from7,
                                        to7,
                                        new UvRotation[] {
                                            new UvRotation(
                                                    new CuboidFace.UVs(11.0F, 5.0F, 13.0F, 3.0F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(11.0F, 3.0F, 13.0F, 5.0F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            3.0F, -0.016000748F, 5.0F, 16.016F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            11.0F, -0.016000748F, 13.0F, 16.016F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            3.0F, -0.016000748F, 5.0F, 16.016F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            11.0F, -0.016000748F, 13.0F, 16.016F),
                                                    Quadrant.R0)
                                        });
                            }
                        }
                        {
                            {
                                final Vector3f from8 = new Vector3f(MIN, 11.0F, 3.0F);
                                final Vector3f to8 = new Vector3f(MAX, 11.0F + 2F, 3.0F + 2F);
                                this.faces(
                                        full,
                                        rebar,
                                        from8,
                                        to8,
                                        new UvRotation[] {
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            -0.016F, 5.0F, 16.016F, 3.0F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            -0.016F, 3.0F, 16.016F, 5.0F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            -0.016000748F, 3.0F, 16.016F, 5.0F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            -0.016F, 3.0F, 16.016F, 5.0F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(3.0F, 3.0F, 5.0F, 5.0F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(11.0F, 3.0F, 13.0F, 5.0F),
                                                    Quadrant.R0)
                                        });
                            }
                        }
                        {
                            {
                                final Vector3f from9 = new Vector3f(11.0F, 3.0F, MIN);
                                final Vector3f to9 = new Vector3f(11.0F + 2F, 3.0F + 2F, MAX);
                                this.faces(
                                        full,
                                        rebar,
                                        from9,
                                        to9,
                                        new UvRotation[] {
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            11.0F, 16.016F, 13.0F, -0.016F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            11.0F, -0.016F, 13.0F, 16.016F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(3.0F, 11.0F, 5.0F, 13.0F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(11.0F, 11.0F, 13.0F, 13.0F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            -0.016F, 11.0F, 16.016F, 13.0F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            -0.016000748F, 11.0F, 16.016F, 13.0F),
                                                    Quadrant.R0)
                                        });
                            }
                        }
                    }
                    {
                        {
                            {
                                final Vector3f from10 = new Vector3f(11.0F, MIN, 11.0F);
                                final Vector3f to10 = new Vector3f(11.0F + 2F, MAX, 11.0F + 2F);
                                this.faces(
                                        full,
                                        rebar,
                                        from10,
                                        to10,
                                        new UvRotation[] {
                                            new UvRotation(
                                                    new CuboidFace.UVs(11.0F, 13.0F, 13.0F, 11.0F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(11.0F, 11.0F, 13.0F, 13.0F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            3.0F, -0.016000748F, 5.0F, 16.016F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            11.0F, -0.016000748F, 13.0F, 16.016F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            11.0F, -0.016000748F, 13.0F, 16.016F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            3.0F, -0.016000748F, 5.0F, 16.016F),
                                                    Quadrant.R0)
                                        });
                            }
                        }
                        {
                            {
                                final Vector3f from11 = new Vector3f(MIN, 11.0F, 11.0F);
                                final Vector3f to11 = new Vector3f(MAX, 11.0F + 2F, 11.0F + 2F);
                                this.faces(
                                        full,
                                        rebar,
                                        from11,
                                        to11,
                                        new UvRotation[] {
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            -0.016F, 13.0F, 16.016F, 11.0F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            -0.016F, 11.0F, 16.016F, 13.0F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            -0.016000748F, 3.0F, 16.016F, 5.0F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            -0.016F, 3.0F, 16.016F, 5.0F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(11.0F, 3.0F, 13.0F, 5.0F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(3.0F, 3.0F, 5.0F, 5.0F),
                                                    Quadrant.R0)
                                        });
                            }
                        }
                        {
                            {
                                final Vector3f from12 = new Vector3f(11.0F, 11.0F, MIN);
                                final Vector3f to12 = new Vector3f(11.0F + 2F, 11.0F + 2F, MAX);
                                this.faces(
                                        full,
                                        rebar,
                                        from12,
                                        to12,
                                        new UvRotation[] {
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            11.0F, 16.016F, 13.0F, -0.016F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            11.0F, -0.016F, 13.0F, 16.016F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(3.0F, 3.0F, 5.0F, 5.0F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(11.0F, 3.0F, 13.0F, 5.0F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            -0.016F, 3.0F, 16.016F, 5.0F),
                                                    Quadrant.R0),
                                            new UvRotation(
                                                    new CuboidFace.UVs(
                                                            -0.016000748F, 3.0F, 16.016F, 5.0F),
                                                    Quadrant.R0)
                                        });
                            }
                        }
                    }
                }
            }
        }
        this.rods = part(full);

        List<ObjUnbakedGeometry.BoxQuad> three = new ArrayList<>();
        final Vector3f from13 = new Vector3f(LO, MIN, LO);
        final Vector3f to13 = new Vector3f(LO + 2F, MAX, LO + 2F);
        this.faces(
                three,
                rebar,
                from13,
                to13,
                new UvRotation[] {
                    new UvRotation(new CuboidFace.UVs(7.0F, 9.0F, 9.0F, 7.0F), Quadrant.R0),
                    new UvRotation(new CuboidFace.UVs(7.0F, 7.0F, 9.0F, 9.0F), Quadrant.R0),
                    new UvRotation(
                            new CuboidFace.UVs(7.0F, -0.016000748F, 9.0F, 16.016F), Quadrant.R0),
                    new UvRotation(
                            new CuboidFace.UVs(7.0F, -0.016000748F, 9.0F, 16.016F), Quadrant.R0),
                    new UvRotation(
                            new CuboidFace.UVs(7.0F, -0.016000748F, 9.0F, 16.016F), Quadrant.R0),
                    new UvRotation(
                            new CuboidFace.UVs(7.0F, -0.016000748F, 9.0F, 16.016F), Quadrant.R0)
                });
        final Vector3f from14 = new Vector3f(MIN, LO, LO);
        final Vector3f to14 = new Vector3f(MAX, LO + 2F, LO + 2F);
        this.faces(
                three,
                rebar,
                from14,
                to14,
                new UvRotation[] {
                    new UvRotation(new CuboidFace.UVs(-0.016F, 9.0F, 16.016F, 7.0F), Quadrant.R0),
                    new UvRotation(new CuboidFace.UVs(-0.016F, 7.0F, 16.016F, 9.0F), Quadrant.R0),
                    new UvRotation(
                            new CuboidFace.UVs(-0.016000748F, 7.0F, 16.016F, 9.0F), Quadrant.R0),
                    new UvRotation(new CuboidFace.UVs(-0.016F, 7.0F, 16.016F, 9.0F), Quadrant.R0),
                    new UvRotation(new CuboidFace.UVs(7.0F, 7.0F, 9.0F, 9.0F), Quadrant.R0),
                    new UvRotation(new CuboidFace.UVs(7.0F, 7.0F, 9.0F, 9.0F), Quadrant.R0)
                });
        final Vector3f from15 = new Vector3f(LO, LO, MIN);
        final Vector3f to15 = new Vector3f(LO + 2F, LO + 2F, MAX);
        this.faces(
                three,
                rebar,
                from15,
                to15,
                new UvRotation[] {
                    new UvRotation(new CuboidFace.UVs(7.0F, 16.016F, 9.0F, -0.016F), Quadrant.R0),
                    new UvRotation(new CuboidFace.UVs(7.0F, -0.016F, 9.0F, 16.016F), Quadrant.R0),
                    new UvRotation(new CuboidFace.UVs(7.0F, 7.0F, 9.0F, 9.0F), Quadrant.R0),
                    new UvRotation(new CuboidFace.UVs(7.0F, 7.0F, 9.0F, 9.0F), Quadrant.R0),
                    new UvRotation(new CuboidFace.UVs(-0.016F, 7.0F, 16.016F, 9.0F), Quadrant.R0),
                    new UvRotation(
                            new CuboidFace.UVs(-0.016000748F, 7.0F, 16.016F, 9.0F), Quadrant.R0)
                });
        this.simple = part(three);
    }

    private BlockStateModelPart part(List<ObjUnbakedGeometry.BoxQuad> quads) {
        return new SimpleModelWrapper(
                ObjUnbakedGeometry.bakeGroups(List.of(), quads, 0F, 0F, 0F),
                base.getTopAmbientOcclusion(),
                particle);
    }

    private void faces(
            List<ObjUnbakedGeometry.BoxQuad> b,
            Material.Baked material,
            Vector3f from,
            Vector3f to,
            UvRotation[] uvs) {
        for (Direction dir : Direction.VALUES) {
            UvRotation uv = uvs[dir.ordinal()];
            b.add(
                    new ObjUnbakedGeometry.BoxQuad(
                            Boxes.face(
                                    baker,
                                    from,
                                    to,
                                    dir,
                                    material,
                                    uv.uvs(),
                                    uv.quadrant(),
                                    CuboidFace.NO_TINT,
                                    Boxes.IDENTITY),
                            ObjUnbakedGeometry.WHITE,
                            null,
                            QuadLighting.cell(0, 0, 0)));
        }
    }

    private BlockStateModelPart rods() {
        return RenderConfig.simpleRebar ? simple : rods;
    }

    private BlockStateModelPart fill(int progress) {
        BlockStateModelPart part = fills[progress];
        if (part == null) {
            List<ObjUnbakedGeometry.BoxQuad> b = new ArrayList<>(6);
            final float y1 = progress * 16F / BlockEntityRebar.FULL;
            final Vector3f from = new Vector3f(0F, 0F, 0F);
            final Vector3f to = new Vector3f(16F, y1, 16F);
            this.faces(
                    b,
                    concrete,
                    from,
                    to,
                    new UvRotation[] {
                        new UvRotation(new CuboidFace.UVs(0.0F, 16.0F, 16.0F, 0.0F), Quadrant.R0),
                        new UvRotation(new CuboidFace.UVs(0.0F, 0.0F, 16.0F, 16.0F), Quadrant.R0),
                        new UvRotation(
                                new CuboidFace.UVs(0.0F, 16F - to.y(), 16.0F, 16.0F), Quadrant.R0),
                        new UvRotation(
                                new CuboidFace.UVs(0.0F, 16F - to.y(), 16.0F, 16.0F), Quadrant.R0),
                        new UvRotation(
                                new CuboidFace.UVs(0.0F, 16F - to.y(), 16.0F, 16.0F), Quadrant.R0),
                        new UvRotation(
                                new CuboidFace.UVs(0.0F, 16F - to.y(), 16.0F, 16.0F), Quadrant.R0)
                    });
            part = part(b);
            fills[progress] = part;
        }
        return part;
    }

    private static int progress(BlockAndTintGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof BlockEntityRebar rebar
                ? Math.clamp(rebar.progress(), 0, BlockEntityRebar.FULL)
                : 0;
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
        output.add(rods());
    }

    @Override
    public Material.Baked particleMaterial() {
        return particle;
    }

    @Override
    public int materialFlags() {
        return rods.materialFlags();
    }

    @Override
    public void emitQuads(
            QuadEmitter emitter,
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            Predicate<@Nullable Direction> cullTest) {
        List<BlockStateModelPart> parts = new ArrayList<>(2);
        collectParts(level, pos, state, random, parts);
        for (int i = 0; i < parts.size(); i++) parts.get(i).emitQuads(emitter, cullTest);
    }

    public void collectParts(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            List<BlockStateModelPart> output) {
        output.add(rods());
        int progress = progress(level, pos);
        if (progress > 0) output.add(fill(progress));
    }

    @Override
    public Object createGeometryKey(
            BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        return new GeometryKey(RenderConfig.simpleRebar, progress(level, pos));
    }

    private record GeometryKey(boolean simple, int progress) {}

    public record Family() implements SimpleBlockModel {

        @Override
        public BlockStateModel.UnbakedRoot root(Block block, BlockState state) {
            return new Root(BlockModel.base(block));
        }
    }

    public record Root(Identifier baseModel) implements BlockStateModel.UnbakedRoot {

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(baseModel);
        }

        @Override
        public BlockStateModel bake(BlockState blockState, ModelBaker baker) {
            return baker.compute(new SharedKey(this));
        }

        @Override
        public Object visualEqualityGroup(BlockState blockState) {
            return this;
        }
    }

    private record SharedKey(Root root) implements ModelBaker.SharedOperationKey<BlockStateModel> {

        @Override
        public BlockStateModel compute(ModelBaker baker) {
            return new RebarModel(baker, baker.getModel(root.baseModel()));
        }
    }
}
