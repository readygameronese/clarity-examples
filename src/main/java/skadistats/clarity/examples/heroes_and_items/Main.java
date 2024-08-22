import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import skadistats.clarity.Clarity;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.processor.entities.Entities;
import skadistats.clarity.processor.entities.OnEntityUpdated;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;

import java.io.FileWriter;
import java.io.IOException;
import java.util.TreeMap;

public class HeroDataCollector {

    private final TreeMap<Integer, JsonObject> heroDataByTick = new TreeMap<>();

    @UsesEntities
    private Entities entities;
    @Insert
    private Context ctx;

    @OnEntityUpdated(classPattern = "CDOTA_Unit_Hero")
    public void onHeroUpdated(Entity hero, FieldPath[] fieldPaths, int num) {
        var tick = ctx.getTick();
        var heroData = heroDataByTick.computeIfAbsent(tick, k -> new JsonObject());
        heroData.addProperty("tick", tick);
        heroData.addProperty("game_time", ctx.getTick() * ctx.getMillisPerTick() / 1000.0f);
        var heroObject = new JsonObject();
        heroData.add("hero", heroObject);
        heroObject.addProperty("index", hero.getIndex());
        heroObject.addProperty("handle", hero.getHandle());
        var properties = new JsonObject();
        heroObject.add("properties", properties);
        for (var i = 0; i < num; i++) {
            properties.addProperty(
                    hero.getDtClass().getNameForFieldPath(fieldPaths[i]),
                    String.valueOf(hero.getPropertyForFieldPath(fieldPaths[i]))
            );
        }
    }

    public void saveToJson(String filename) throws IOException {
        var gson = new GsonBuilder().setPrettyPrinting().create();
        try (var writer = new FileWriter(filename)) {
            var jsonArray = new JsonArray();
            heroDataByTick.values().forEach(jsonArray::add);
            gson.toJson(jsonArray, writer);
        }
    }

    public static void main(String[] args) throws Exception {
        var replayFilename = "path/to/your/replay.dem"; // Replace with your replay file
        var outputFilename = "hero_data.json"; // Replace with your desired output filename

        var source = new MappedFileSource(replayFilename);
        var runner = new SimpleRunner(source);
        var collector = new HeroDataCollector();
        runner.runWith(collector);
        collector.saveToJson(outputFilename);
    }

}