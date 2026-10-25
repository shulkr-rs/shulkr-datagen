package me.garfxld.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.garfxld.DataGenerator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

import java.util.ArrayList;
import java.util.List;

public class FluidGenerator implements DataGenerator {

    @Override
    public String generate() {
        var fluidsJson = new JsonObject();
        var statesById = new ArrayList<JsonObject>();

        for (var fluid : BuiltInRegistries.FLUID) {
            var key = BuiltInRegistries.FLUID.getKey(fluid);

            var fluidJson = new JsonObject();
            fluidJson.addProperty("id", BuiltInRegistries.FLUID.getId(fluid));
            fluidJson.addProperty(
                    "defaultStateId",
                    Fluid.FLUID_STATE_REGISTRY.getId(fluid.defaultFluidState())
            );

            fluidsJson.add(key.toString(), fluidJson);

            List<FluidState> possibleStates = fluid.getStateDefinition().getPossibleStates();
            for (var state : possibleStates) {
                var stateJson = new JsonObject();
                stateJson.addProperty("fluid", key.toString());
                stateJson.addProperty("isEmpty", state.isEmpty());
                stateJson.addProperty("isSource", state.isSource());
                stateJson.addProperty("isFull", state.isFull());
                stateJson.addProperty("isRandomlyTicking", state.isRandomlyTicking());
                stateJson.addProperty("amount", state.getAmount());
                stateJson.addProperty("falling", state.isFalling());
                stateJson.addProperty(
                        "blockStateId",
                        Block.getId(state.createLegacyBlock())
                );

                var id = Fluid.FLUID_STATE_REGISTRY.getId(state);
                while (statesById.size() <= id) {
                    statesById.add(null);
                }
                statesById.set(id, stateJson);
            }
        }

        var statesJson = new JsonArray();
        for (var state : statesById) {
            if (state == null) {
                throw new IllegalStateException("gap in the fluid state registry");
            }
            statesJson.add(state);
        }

        var json = new JsonObject();
        json.add("fluids", fluidsJson);
        json.add("states", statesJson);
        return json.toString();
    }

}
