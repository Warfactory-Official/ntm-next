// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockTallPlant;
import com.hbm.blocks.machine.MachineThresher;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ModItems;
import com.hbm.lib.ModDamageTypes;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.packet.toclient.BlockDustBurstPayload;
import com.hbm.platform.Services;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.AudioLoop;
import com.hbm.tileentity.FoldedCoreResident;
import com.hbm.tileentity.GraphResident;
import com.hbm.tileentity.IFluidCopiable;
import com.hbm.tileentity.Synced;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BlockEntityThresher extends BlockEntity
        implements Synced,
                GraphResident,
                FoldedCoreResident,
                AudioLoop,
                FluidTankEndpoint,
                IFluidCopiable,
                SyncUnitSchema {

    public static final int TANK_CAPACITY = 100;
    public static final float SWING_MAX_ANGLE = 82.5F;
    public static final float SWING_SPEED = 82.5F / 60F;
    public static final int IDLE_DELAY_BASE = 200, IDLE_DELAY_RAND = 100;
    public static final int SCAN_RADIUS = 3;
    public static final float ENTITY_DAMAGE = 100F;
    public static final double AUDIO_RANGE_SQ = 15 * 15;
    public static @Nullable Consumer<BlockEntityThresher> CLIENT_SOUND;

    @SyncField(units = 1L << 3)
    public final FluidTankNTM tank = new FluidTankNTM(NTMFluids.WOODOIL, TANK_CAPACITY);

    private final FluidTankNTM[] receiving;

    @SyncField(units = 1L << 0)
    public boolean isOn;

    @SyncField(units = 1L << 1)
    public boolean isSuspended;

    public float syncAngle;

    @SyncField(units = 1L << 2)
    public float angle;

    public float prevAngle;
    public float spin;
    public float lastSpin;
    private int delay;
    private int state;
    private int turnProgress;

    public BlockEntityThresher(BlockPos pos, BlockState state) {
        super(ModBlockEntities.THRESHER.get(), pos, state);
        receiving = new FluidTankNTM[] {tank};
    }

    public static boolean acceptsFuel(Fluid type) {
        return type == NTMFluids.WOODOIL
                || type == NTMFluids.ETHANOL
                || type == NTMFluids.FISHOIL
                || type == NTMFluids.HEAVYOIL
                || type == NTMFluids.COALCREOSOTE;
    }

    public static void tickServer(
            Level level, BlockPos pos, BlockState state, BlockEntityThresher be) {
        be.tickServer((ServerLevel) level);
    }

    private Direction rearDirection() {
        return getBlockState().getValue(MachineThresher.FACING).getOpposite();
    }

    public boolean acceptsFuelFrom(@Nullable Direction side) {
        if (side == null) return true;
        Direction dir = rearDirection();
        Direction rot = dir.getClockWise();
        return side == rot || side == rot.getOpposite() || side == Direction.DOWN;
    }

    private void tickServer(ServerLevel level) {
        Direction dir = rearDirection();
        Direction rot = dir.getClockWise();

        if (!isSuspended && TickPhase.every(this, SharedConstants.TICKS_PER_SECOND)) {
            if (tank.getFill() > 0) {
                tank.setFill(tank.getFill() - 1);
                isOn = true;
            } else {
                isOn = false;
            }
        }

        if (isOn && !isSuspended) {
            if (state == 0) {
                delay--;
                if (delay <= 0) state = 1;
            }
            if (state == 1) {
                angle += SWING_SPEED;
                if (angle >= SWING_MAX_ANGLE) {
                    angle = SWING_MAX_ANGLE;
                    state = 2;
                }
            } else if (state == 2) {
                angle -= SWING_SPEED;
                if (angle <= 0F) {
                    angle = 0F;
                    state = 0;
                    delay = IDLE_DELAY_BASE + level.getRandom().nextInt(IDLE_DELAY_RAND);
                }
            }

            if (angle != 0F) swing(level, dir, rot);
        }

        networkPackNT(100);
    }

    private void swing(ServerLevel level, Direction dir, Direction rot) {
        Vec3 pivot =
                new Vec3(
                        worldPosition.getX() + 0.5 - dir.getStepX(),
                        worldPosition.getY() + 0.5,
                        worldPosition.getZ() + 0.5 - dir.getStepZ());
        Vec3 upperArm = new Vec3(-dir.getStepX() * 4, 0, -dir.getStepZ() * 4);
        Vec3 lowerArm = new Vec3(-dir.getStepX() * 4, 0, -dir.getStepZ() * 4);
        float rad = (float) Math.toRadians(SWING_MAX_ANGLE - angle);
        if (dir.getStepZ() != 0) {
            upperArm = upperArm.xRot(rad);
            lowerArm = lowerArm.xRot(-rad);
        }
        if (dir.getStepX() != 0) {
            upperArm = upperArm.zRot(rad);
            lowerArm = lowerArm.zRot(-rad);
        }
        Vec3 armTip = new Vec3(-dir.getStepX() * 2, 0, -dir.getStepZ() * 2);

        double endX = pivot.x + upperArm.x + lowerArm.x + armTip.x;
        double endZ = pivot.z + upperArm.z + lowerArm.z + armTip.z;
        int y = worldPosition.getY();

        for (int i = -SCAN_RADIUS; i <= SCAN_RADIUS; i++) {
            int hitX = Mth.floor(endX + rot.getStepX() * i);
            int hitZ = Mth.floor(endZ + rot.getStepZ() * i);
            BlockPos hit = new BlockPos(hitX, y, hitZ);
            BlockState bs = level.getBlockState(hit);
            Block b = bs.getBlock();

            if (bs.isSolidRender() && !bs.isSignalSource() && !canCut(b)) {
                state = 2;
                break;
            }

            if (b == Blocks.SUNFLOWER) {
                if (level.getRandom().nextInt(250) == 0) {
                    spawnBreakFx(level, hit, bs);
                    dropItem(level, new ItemStack(Blocks.SUNFLOWER));
                }
                continue;
            }
            if (b == Blocks.TALL_GRASS) {
                if (level.getRandom().nextInt(100) == 0) {
                    spawnBreakFx(level, hit, bs);
                    dropItem(level, new ItemStack(Items.WHEAT_SEEDS));
                }
                continue;
            }
            if (b instanceof BlockTallPlant) {
                cutTallPlant(level, b, hit, bs);
                continue;
            }

            if (b == Blocks.SUGAR_CANE || b == Blocks.CACTUS) {
                cutCane(level, b, hitX, y, hitZ);
                continue;
            }

            if (canCut(b) && !shouldIgnore(level, hit, bs)) cutCrop(level, hit, bs);
        }

        AABB box =
                new AABB(endX, y + 0.5, endZ, endX, y + 0.5, endZ)
                        .inflate(
                                Math.abs(dir.getStepX() * 0.5) + Math.abs(rot.getStepX() * 4.5),
                                0.5,
                                Math.abs(dir.getStepZ() * 0.5) + Math.abs(rot.getStepZ() * 4.5));
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (e.isAlive()
                    && e.hurtServer(
                            level,
                            level.damageSources().source(ModDamageTypes.BLENDER),
                            ENTITY_DAMAGE)) {
                if (e instanceof Enemy && !e.isAlive())
                    dropItem(level, new ItemStack(ModItems.NITRA_SMALL));
                level.playSound(
                        null,
                        e.getX(),
                        e.getY(),
                        e.getZ(),
                        SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                        SoundSource.BLOCKS,
                        2.0F,
                        0.95F + level.getRandom().nextFloat() * 0.2F);
                int count = Math.min((int) Math.ceil(e.getMaxHealth() / 4), 250) * 4;
                Services.NETWORK.sendToAllAround(
                        new BlockDustBurstPayload(
                                Blocks.REDSTONE_BLOCK.defaultBlockState(),
                                e.getX(),
                                e.getY() + e.getBbHeight() * 0.5,
                                e.getZ(),
                                count,
                                0.1D),
                        new TargetPoint(level, e.getX(), e.getY(), e.getZ(), 50));
            }
        }
    }

    private static boolean canCut(Block block) {
        return block instanceof BonemealableBlock
                || block == Blocks.NETHER_WART
                || block == Blocks.MELON
                || block == Blocks.PUMPKIN;
    }

    private static boolean shouldIgnore(ServerLevel level, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        if (block instanceof StemBlock) return true;
        if (block == Blocks.NETHER_WART)
            return state.getValue(NetherWartBlock.AGE) < NetherWartBlock.MAX_AGE;
        return block instanceof BonemealableBlock crop
                && crop.isValidBonemealTarget(level, pos, state);
    }

    private void spawnBreakFx(ServerLevel level, BlockPos pos, BlockState state) {
        level.levelEvent(2001, pos, Block.getId(state));
    }

    private void cutTallPlant(ServerLevel level, Block plant, BlockPos pos, BlockState bs) {
        if (bs.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER) {
            pos = pos.above();
            bs = level.getBlockState(pos);
            if (!bs.is(plant)) return;
        }
        if (plant == ModBlocks.PLANT_TALL_CD2.get() || plant == ModBlocks.PLANT_TALL_CD3.get())
            return;

        spawnBreakFx(level, pos, bs);
        for (ItemStack drop : Block.getDrops(bs, level, pos, null)) dropItem(level, drop);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    private void cutCane(ServerLevel level, Block target, int x, int y, int z) {
        int offset = level.getBlockState(new BlockPos(x, y - 1, z)).is(target) ? -1 : 0;
        for (int i = 2 + offset; i > offset; i--) {
            BlockPos pos = new BlockPos(x, y + i, z);
            BlockState bs = level.getBlockState(pos);
            spawnBreakFx(level, pos, bs);
            for (ItemStack drop : Block.getDrops(bs, level, pos, null)) dropItem(level, drop);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private void cutCrop(ServerLevel level, BlockPos pos, BlockState bs) {
        spawnBreakFx(level, pos, bs);

        BlockState replacement = Blocks.AIR.defaultBlockState();
        boolean replanted = false;

        for (ItemStack drop : Block.getDrops(bs, level, pos, null)) {
            if (!replanted
                    && drop.getItem() instanceof BlockItem seed
                    && seed.getBlock() instanceof VegetationBlock plant) {
                BlockState planted = plant.defaultBlockState();

                if (planted.canSurvive(level, pos)) {
                    replacement = planted;
                    replanted = true;
                    drop.shrink(1);
                    if (drop.isEmpty()) continue;
                }
            }
            dropItem(level, drop);
        }

        if (bs.getBlock() == Blocks.WHEAT && !replanted)
            replacement = Blocks.WHEAT.defaultBlockState();

        level.setBlock(pos, replacement, 3);
    }

    private void dropItem(ServerLevel level, ItemStack drop) {
        Direction dir = getBlockState().getValue(MachineThresher.FACING);
        double x = worldPosition.getX() + 0.5 - dir.getStepX() * 0.75;
        double z = worldPosition.getZ() + 0.5 - dir.getStepZ() * 0.75;
        ItemEntity item = new ItemEntity(level, x, worldPosition.getY(), z, drop);
        item.setPickUpDelay(10);
        item.setDeltaMovement(
                dir.getStepX() * -0.2 + 0.2, item.getDeltaMovement().y, dir.getStepZ() * -0.2);
        level.addFreshEntity(item);
    }

    public void tickClient() {
        if (CLIENT_SOUND != null) CLIENT_SOUND.accept(this);

        lastSpin = spin;
        if (isOn && !isSuspended) {
            if (angle > 0) spin += 15F;
            Direction dir = rearDirection();
            Direction rot = dir.getClockWise();
            level.addParticle(
                    ParticleTypes.SMOKE,
                    worldPosition.getX() + 0.5 + dir.getStepX() * 0.8125 + rot.getStepX() * 0.375,
                    worldPosition.getY() + 1.5625,
                    worldPosition.getZ() + 0.5 + dir.getStepZ() * 0.8125 + rot.getStepZ() * 0.375,
                    0,
                    0,
                    0);
        }
        if (spin >= 360F) {
            spin -= 360F;
            lastSpin -= 360F;
        }

        prevAngle = angle;
        if (turnProgress > 0) {
            double d0 = Mth.wrapDegrees(syncAngle - (double) angle);
            angle = (float) (angle + d0 / turnProgress);
            turnProgress--;
        } else {
            angle = syncAngle;
        }
    }

    @Override
    public AudioWrapper createAudioLoop() {
        return AudioSystem.getLoopedSound(
                ModSounds.ENGINE_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                1.0F,
                10F,
                1.0F + level.getRandom().nextFloat() * 0.1F,
                10);
    }

    public void toggleSuspended() {
        isSuspended = !isSuspended;
        setChanged();
    }

    public boolean acceptsFace(Direction dir) {
        return acceptsFuelFrom(dir);
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return receiving;
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return receiving;
    }

    @Override
    public FluidTankNTM getTankToPaste() {
        return tank;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        isOn = input.getBooleanOr("isOn", isOn);
        isSuspended = input.getBooleanOr("isSuspended", isSuspended);
        angle = input.getFloatOr("angle", angle);
        state = input.getIntOr("state", state);
        input.child("tank").ifPresent(tank::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("isOn", isOn);
        output.putBoolean("isSuspended", isSuspended);
        output.putFloat("angle", angle);
        output.putInt("state", state);
        tank.serialize(output.child("tank"));
    }

    private void readAngle(ByteBuf input) {
        syncAngle = input.readFloat();
    }

    @Override
    public void afterSyncUnits(long units) {
        turnProgress = 3;
    }

    @Override
    public long syncUnitMask() {
        return 0xfL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeBoolean(this.isOn);
            case 1 -> output.writeBoolean(this.isSuspended);
            case 2 -> output.writeFloat(this.angle);
            case 3 -> this.tank.packetSerialize(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.isOn = input.readBoolean();
            case 1 -> this.isSuspended = input.readBoolean();
            case 2 -> readAngle(input);
            case 3 -> this.tank.packetDeserialize(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
