package me.garfxld.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.garfxld.DataGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
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

    @Override
    public String generate() {
        var blockRegistry = BuiltInRegistries.BLOCK;
        var blockEntityTypeRegistry = BuiltInRegistries.BLOCK_ENTITY_TYPE;

        var blocksJson = new JsonObject();
        var currStateId = 0;

        // Unique shape data.
        // Key   = AABBs making up the shape
        // Value = actual VoxelShape
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

            /*
             * First pass:
             *
             * Collect every unique shape used by every state of this block.
             */
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

                for (var s : List.of(
                        shape,
                        collisionShape,
                        interactionShape,
                        occlusionShape,
                        visualShape
                )) {
                    shapesByKey.putIfAbsent(s.toAabbs(), s);
                }
            }

            /*
             * Build the final shape list and an index lookup.
             *
             * The index here corresponds to the index in the final
             * top-level "shapes" JSON array.
             */
            var shapeIndices = new LinkedHashMap<List<AABB>, Integer>();

            var shapes = new ArrayList<>(shapesByKey.values());

            for (int i = 0; i < shapes.size(); i++) {
                shapeIndices.put(shapes.get(i).toAabbs(), i);
            }

            var shapesJson = new JsonObject();

// Generate the default shape mapping
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
                    )
            );

            var defaultShapeKey = defaultShapes.stream()
                    .map(s -> String.valueOf(shapeIndices.get(s.toAabbs())))
                    .collect(java.util.stream.Collectors.joining(","));

            shapesJson.addProperty("*", defaultShapeKey);

// Generate only states that differ from the default
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
                        )
                );

                var stateShapeKey = stateShapes.stream()
                        .map(s -> String.valueOf(shapeIndices.get(s.toAabbs())))
                        .collect(java.util.stream.Collectors.joining(","));

                // Don't write it if it matches the default/fallback
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

            /*
             * Verify that our state ID counter matches Minecraft's
             * state registry.
             */
            if (currStateId != defaultStateId) {
                IO.println("Different " + block.getDescriptionId());
            }

            currStateId += possibleStates.size();

            /*
             * Block properties.
             */
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

        /*
         * Resolve block entities.
         */
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
