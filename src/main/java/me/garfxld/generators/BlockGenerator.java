package me.garfxld.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.garfxld.DataGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BlockGenerator implements DataGenerator {

    public static final int IS_AIR = 1;
    public static final int IS_LIQUID = 1 << 1;
    public static final int IS_SOLID = 1 << 2;
    public static final int IS_SOLID_RENDER = 1 << 3;
    public static final int CAN_OCCLUDE = 1 << 4;
    public static final int CAN_BE_REPLACED = 1 << 5;
    public static final int USE_SHAPE_FOR_LIGHT_OCCLUSION = 1 << 6;
    public static final int PROPAGATES_SKYLIGHT_DOWN = 1 << 7;
    public static final int IS_RANDOMLY_TICKING = 1 << 8;
    public static final int IGNITED_BY_LAVA = 1 << 9;
    public static final int HAS_DYNAMIC_SHAPE = 1 << 10;

    /// Light levels live in the low bits, the flags above them.
    private static int packProperties(BlockState state) {
        var flags = 0;
        if (state.isAir()) flags |= IS_AIR;
        if (state.liquid()) flags |= IS_LIQUID;
        if (state.isSolid()) flags |= IS_SOLID;
        if (state.isSolidRender()) flags |= IS_SOLID_RENDER;
        if (state.canOcclude()) flags |= CAN_OCCLUDE;
        if (state.canBeReplaced()) flags |= CAN_BE_REPLACED;
        if (state.useShapeForLightOcclusion()) flags |= USE_SHAPE_FOR_LIGHT_OCCLUSION;
        if (state.propagatesSkylightDown()) flags |= PROPAGATES_SKYLIGHT_DOWN;
        if (state.isRandomlyTicking()) flags |= IS_RANDOMLY_TICKING;
        if (state.ignitedByLava()) flags |= IGNITED_BY_LAVA;
        if (state.getBlock().hasDynamicShape()) flags |= HAS_DYNAMIC_SHAPE;

        var emission = state.getLightEmission();
        var dampening = state.getLightDampening();
        if (emission > 15 || dampening > 15) {
            throw new IllegalStateException("light level does not fit in 4 bits");
        }

        return emission | (dampening << 4) | (flags << 8);
    }


    @Override
    public String generate() {
        var blockRegistry = BuiltInRegistries.BLOCK;
        var blockEntityTypeRegistry = BuiltInRegistries.BLOCK_ENTITY_TYPE;

        var blocksJson = new JsonObject();
        var currStateId = 0;

        var shapesByKey = new LinkedHashMap<List<AABB>, VoxelShape>();

        for (var block : blockRegistry) {
            final var key = blockRegistry.getKey(block);
            final var defaultBlockState = block.defaultBlockState();
            final var possibleStates = block.getStateDefinition().getPossibleStates();

            var blockJson = new JsonObject();
            blockJson.addProperty("id", blockRegistry.getId(block));

            var defaultStateId = Block.BLOCK_STATE_REGISTRY.getId(defaultBlockState);
            blockJson.addProperty("defaultStateId", defaultStateId);
            blockJson.addProperty("minStateId", currStateId);
            blockJson.addProperty(
                    "maxStateId",
                    currStateId + possibleStates.size() - 1
            );

            for (var blockState : possibleStates) {
                var shape = blockState.getShape(
                        EmptyBlockGetter.INSTANCE,
                        BlockPos.ZERO
                );

                var collisionShape = blockState.getCollisionShape(
                        EmptyBlockGetter.INSTANCE,
                        BlockPos.ZERO
                );

                var interactionShape = blockState.getInteractionShape(
                        EmptyBlockGetter.INSTANCE,
                        BlockPos.ZERO
                );

                var occlusionShape = blockState.getOcclusionShape();

                var visualShape = blockState.getVisualShape(
                        EmptyBlockGetter.INSTANCE,
                        BlockPos.ZERO,
                        CollisionContext.empty()
                );

                var supportShape = blockState.getBlockSupportShape(
                        EmptyBlockGetter.INSTANCE,
                        BlockPos.ZERO
                );

                for (var s : List.of(
                        shape,
                        collisionShape,
                        interactionShape,
                        occlusionShape,
                        visualShape,
                        supportShape
                )) {
                    shapesByKey.putIfAbsent(s.toAabbs(), s);
                }
            }


            var shapeIndices = new LinkedHashMap<List<AABB>, Integer>();

            var shapes = new ArrayList<>(shapesByKey.values());

            for (int i = 0; i < shapes.size(); i++) {
                shapeIndices.put(shapes.get(i).toAabbs(), i);
            }

            var shapesJson = new JsonObject();

            var defaultShapes = List.of(
                    defaultBlockState.getShape(
                            EmptyBlockGetter.INSTANCE,
                            BlockPos.ZERO
                    ),
                    defaultBlockState.getCollisionShape(
                            EmptyBlockGetter.INSTANCE,
                            BlockPos.ZERO
                    ),
                    defaultBlockState.getInteractionShape(
                            EmptyBlockGetter.INSTANCE,
                            BlockPos.ZERO
                    ),
                    defaultBlockState.getOcclusionShape(),
                    defaultBlockState.getVisualShape(
                            EmptyBlockGetter.INSTANCE,
                            BlockPos.ZERO,
                            CollisionContext.empty()
                    ),
                    defaultBlockState.getBlockSupportShape(
                            EmptyBlockGetter.INSTANCE,
                            BlockPos.ZERO
                    )
            );

            var defaultShapeKey = defaultShapes.stream()
                    .map(s -> String.valueOf(shapeIndices.get(s.toAabbs())))
                    .collect(java.util.stream.Collectors.joining(","));

            shapesJson.addProperty("*", defaultShapeKey);

            for (var blockState : possibleStates) {
                if (blockState == defaultBlockState) {
                    continue;
                }

                var stateShapes = List.of(
                        blockState.getShape(
                                EmptyBlockGetter.INSTANCE,
                                BlockPos.ZERO
                        ),
                        blockState.getCollisionShape(
                                EmptyBlockGetter.INSTANCE,
                                BlockPos.ZERO
                        ),
                        blockState.getInteractionShape(
                                EmptyBlockGetter.INSTANCE,
                                BlockPos.ZERO
                        ),
                        blockState.getOcclusionShape(),
                        blockState.getVisualShape(
                                EmptyBlockGetter.INSTANCE,
                                BlockPos.ZERO,
                                CollisionContext.empty()
                        ),
                        blockState.getBlockSupportShape(
                                EmptyBlockGetter.INSTANCE,
                                BlockPos.ZERO
                        )
                );

                var stateShapeKey = stateShapes.stream()
                        .map(s -> String.valueOf(shapeIndices.get(s.toAabbs())))
                        .collect(java.util.stream.Collectors.joining(","));

                if (stateShapeKey.equals(defaultShapeKey)) {
                    continue;
                }

                var stateId = Block.BLOCK_STATE_REGISTRY.getId(blockState);

                shapesJson.addProperty(
                        String.valueOf(stateId),
                        stateShapeKey
                );




            }

            blockJson.add("shapes", shapesJson);

            var fluidStatesJson = new JsonObject();
            var defaultFluidStateId = Fluid.FLUID_STATE_REGISTRY.getId(
                    defaultBlockState.getFluidState()
            );
            fluidStatesJson.addProperty("*", defaultFluidStateId);

            for (var blockState : possibleStates) {
                if (blockState == defaultBlockState) {
                    continue;
                }

                var fluidStateId = Fluid.FLUID_STATE_REGISTRY.getId(
                        blockState.getFluidState()
                );
                if (fluidStateId == defaultFluidStateId) {
                    continue;
                }

                fluidStatesJson.addProperty(
                        String.valueOf(Block.BLOCK_STATE_REGISTRY.getId(blockState)),
                        fluidStateId
                );
            }

            blockJson.add("fluidStates", fluidStatesJson);

            var propertiesJson = new JsonObject();
            var defaultProperties = packProperties(defaultBlockState);
            propertiesJson.addProperty("*", defaultProperties);

            for (var blockState : possibleStates) {
                if (blockState == defaultBlockState) {
                    continue;
                }

                var stateProperties = packProperties(blockState);
                if (stateProperties == defaultProperties) {
                    continue;
                }

                propertiesJson.addProperty(
                        String.valueOf(Block.BLOCK_STATE_REGISTRY.getId(blockState)),
                        stateProperties
                );
            }

            blockJson.add("stateProperties", propertiesJson);

            var transformsJson = new JsonObject();
            for (var blockState : possibleStates) {
                var stateId = Block.BLOCK_STATE_REGISTRY.getId(blockState);

                var entry = new JsonObject();
                addTransform(entry, "cw90", blockState, blockState.rotate(Rotation.CLOCKWISE_90));
                addTransform(entry, "cw180", blockState, blockState.rotate(Rotation.CLOCKWISE_180));
                addTransform(
                        entry,
                        "ccw90",
                        blockState,
                        blockState.rotate(Rotation.COUNTERCLOCKWISE_90)
                );
                addTransform(
                        entry,
                        "leftRight",
                        blockState,
                        blockState.mirror(Mirror.LEFT_RIGHT)
                );
                addTransform(
                        entry,
                        "frontBack",
                        blockState,
                        blockState.mirror(Mirror.FRONT_BACK)
                );

                if (!entry.isEmpty()) {
                    transformsJson.add(String.valueOf(stateId), entry);
                }
            }

            if (!transformsJson.isEmpty()) {
                blockJson.add("transforms", transformsJson);
            }

            if (defaultBlockState.hasOffsetFunction()) {
                var offsetJson = new JsonObject();
                offsetJson.addProperty("type", offsetType(defaultBlockState));
                offsetJson.addProperty(
                        "maxHorizontal",
                        invokeFloat(block, "getMaxHorizontalOffset")
                );
                offsetJson.addProperty(
                        "maxVertical",
                        invokeFloat(block, "getMaxVerticalOffset")
                );
                blockJson.add("offset", offsetJson);
            }

            if (currStateId != defaultStateId) {
                IO.println("Different " + block.getDescriptionId());
            }

            currStateId += possibleStates.size();

            var propsJson = new JsonArray();

            for (var property : block.getStateDefinition().getProperties()) {
                propsJson.add(
                        getConstantName(BlockStateProperties.class, property)
                );
            }

            if (!propsJson.isEmpty()) {
                blockJson.add("properties", propsJson);
            }

            blocksJson.add(key.toString(), blockJson);
        }

        for (var blockEntityType : blockEntityTypeRegistry) {
            final var location = blockEntityTypeRegistry.getKey(blockEntityType);

            try {
                Field fcField = BlockEntityType.class.getDeclaredField("validBlocks");
                fcField.setAccessible(true);

                for (Block validBlock : (Set<Block>) fcField.get(blockEntityType)) {
                    final String namespace = blockRegistry.getKey(validBlock).toString();

                    final JsonObject blockJson = blocksJson
                            .get(namespace)
                            .getAsJsonObject();

                    blockJson.addProperty(
                            "blockEntity",
                            location.toString()
                    );
                }
            } catch (IllegalAccessException | NoSuchFieldException e) {
                e.printStackTrace();
            }
        }

        /*
         * Build the top-level shape array.
         *
         * The index of each entry here is what gets stored in
         * block.shapes["*"] / block.shapes[stateId].
         */
        var shapes = new ArrayList<>(shapesByKey.values());

        IO.println(shapes.size());

        var shapesJson = new JsonArray();

        for (var shape : shapes) {
            var shapeJson = new JsonArray();

            for (var aabb : shape.toAabbs()) {
                var bb = Stream.of(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ)
                        .map(Object::toString)
                        .collect(Collectors.joining(","));
                shapeJson.add(bb);
            }

            shapesJson.add(shapeJson);
        }

        var finalJson = new JsonObject();
        finalJson.add("shapes", shapesJson);
        finalJson.add("blocks", blocksJson);

        return finalJson.toString();
    }

    /// Records a transform only when it actually changes the state.
    private static void addTransform(
            JsonObject json,
            String name,
            BlockState from,
            BlockState to
    ) {
        if (from == to) {
            return;
        }

        json.addProperty(name, Block.BLOCK_STATE_REGISTRY.getId(to));
    }

    /// The offset type, found by sampling whether the offset ever moves on Y.
    private static String offsetType(BlockState state) {
        for (var x = 0; x < 16; x++) {
            for (var z = 0; z < 16; z++) {
                if (state.getOffset(new BlockPos(x, 0, z)).y != 0.0) {
                    return "XYZ";
                }
            }
        }

        return "XZ";
    }

    private static float invokeFloat(Block block, String name) {
        try {
            var method = BlockBehaviour.class.getDeclaredMethod(name);
            method.setAccessible(true);
            return (float) method.invoke(block);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("failed to read " + name, e);
        }
    }

    public static String getConstantName(Class<?> clazz, Object value) {
        for (Field field : clazz.getFields()) {
            try {
                if (field.get(null) == value) {
                    return field.getName();
                }
            } catch (IllegalAccessException ignored) {
            }
        }
        return null;
    }

}
