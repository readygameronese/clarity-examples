package skadistats.clarity.examples.cdotaunithero;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.event.Insert;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.processor.entities.Entities;
import skadistats.clarity.processor.entities.OnEntityCreated;
import skadistats.clarity.processor.entities.OnEntityDeleted;
import skadistats.clarity.processor.entities.OnEntityUpdated;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.reader.OnTickEnd;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.processor.sendtables.DTClasses;
import skadistats.clarity.processor.sendtables.OnDTClassesComplete;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.wire.shared.demo.proto.Demo;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static java.lang.String.format;

@UsesEntities
public class Main {

    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final Map<Integer, Map<Integer, Map<String, Object>>> heroUpdates = new TreeMap<>();
    private final Map<Integer, Map<Integer, Map<String, Object>>> itemUpdates = new TreeMap<>();
    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());
    private final Map<Integer, Map<String, Object>> playerIDMapping = new HashMap<>();
    private Demo.CDemoFileInfo demoFileInfo; // Correct usage 

    @Insert
    private Context ctx; 

    @Insert
    private DTClasses dtClasses;

    @Insert
    private Entities entities;

    private DTClass playerResourceClass;
    private final PlayerResourceLookup[] playerLookup = new PlayerResourceLookup[10];
    private final HeroLookup[] heroLookup = new HeroLookup[10];
    private final List<Runnable> deferredActions = new ArrayList<>();

    private final Map<Integer, FieldPath> lifeStatePaths = new HashMap<>();
    private final Map<Integer, Integer> currentLifeState = new HashMap<>();
    private int currentTick = 0;
    private int gameStartTick = -1;
    private int gameEndTick = -1;

    @OnDTClassesComplete
    protected void onDtClassesComplete() {
        playerResourceClass = dtClasses.forDtName("CDOTA_PlayerResource");
        for (int i = 0; i < 10; i++) {
            playerLookup[i] = new PlayerResourceLookup(playerResourceClass, i);
        }
    }

    @OnEntityUpdated
    protected void onEntityUpdated(Entity e, FieldPath[] changedFieldPaths, int nChangedFieldPaths) {
        if (e.getDtClass() == playerResourceClass) {
            handlePlayerResourceUpdate(e, changedFieldPaths, nChangedFieldPaths);
        } else if (e.getDtClass().getDtName().startsWith("CDOTA_Unit_Hero")) {
            handleHeroUpdate(e, changedFieldPaths);
        } else if (e.getDtClass().getDtName().startsWith("CDOTA_Item")) {
            handleItemUpdate(e, changedFieldPaths);
        }
    }

    @OnEntityCreated
    public void onEntityCreated(Context ctx, Entity e) {
        if (e.getDtClass().getDtName().startsWith("CDOTA_Unit_Hero")) {
            clearCachedState(e);
            ensureFieldPathForEntityInitialized(e);
            FieldPath lifeStatePath = getFieldPathForEntity(e);
            if (lifeStatePath != null) {
                processLifeStateChange(e, lifeStatePath);
            }
        } else if (e.getDtClass().getDtName().startsWith("CDOTA_Item")) {
            handleItemUpdate(e, null);
        }
    }

    @OnEntityDeleted
    public void onEntityDeleted(Context ctx, Entity e) {
        if (e.getDtClass().getDtName().startsWith("CDOTA_Unit_Hero")) {
            clearCachedState(e);
        } else if (e.getDtClass().getDtName().startsWith("CDOTA_Item")) {
            itemUpdates.remove(e.getIndex());
        }
    }

    private void handlePlayerResourceUpdate(Entity e, FieldPath[] changedFieldPaths, int nChangedFieldPaths) {
        for (int p = 0; p < 10; p++) {
            PlayerResourceLookup lookup = playerLookup[p];
            if (lookup.isSelectedHeroChanged(e, changedFieldPaths, nChangedFieldPaths)) {
                int playerIndex = p;
                deferredActions.add(() -> {
                    int heroHandle = lookup.getSelectedHeroHandle(e);
                    Entity heroEntity = entities.getByHandle(heroHandle);
                    HeroLookup heroLookup = new HeroLookup(heroEntity);

                    Integer playerID = heroLookup.getPlayerID();
                    if (playerID != null) {
                        // Populate the mapping
                        Map<String, Object> playerInfo = new HashMap<>();
                        playerInfo.put("playerIndex", playerIndex);
                        playerInfo.put("heroName", heroEntity.getDtClass().getDtName());  // Optionally include hero name
                        playerIDMapping.put(playerID, playerInfo);
                    }
                    this.heroLookup[playerIndex] = heroLookup;
                });
            }
        }
    }

    private void handleHeroUpdate(Entity e, FieldPath[] changedFieldPaths) {
        for (int p = 0; p < 10; p++) {
            HeroLookup lookup = heroLookup[p];
            if (lookup == null) continue;

            Integer playerID = lookup.getPlayerID();
            if (playerID == null) continue;  // Skip if playerID couldn't be retrieved

            if (lookup.isAnyFieldChanged(e, changedFieldPaths)) {
                Map<String, Object> changedValues = lookup.updateAndGetChangedFieldValues(e, changedFieldPaths);
                if (!changedValues.isEmpty()) {
                    heroUpdates.computeIfAbsent(currentTick, k -> new HashMap<>())
                            .put(playerID, changedValues);  // Use playerID as the key
                }
            }
        }
    }

    private void handleItemUpdate(Entity e, FieldPath[] changedFieldPaths) {
        Map<String, Object> changedValues = new HashMap<>();

        // Capture the item name
        String itemName = e.getDtClass().getDtName();
        changedValues.put("itemName", itemName);

        // Safely capture the player owner ID
        try {
            Integer playerOwnerId = e.getProperty("m_iPlayerOwnerID");
            if (playerOwnerId != null) {
                changedValues.put("playerOwnerID", playerOwnerId);
            }
        } catch (IllegalArgumentException ex) {
            log.warn("Property 'm_iPlayerOwnerID' not found for entity class {}", e.getDtClass().getDtName());
        }

        // Safely capture item charges if the property exists
        try {
            Integer itemCharges = e.getProperty("m_iCurrentCharges");
            if (itemCharges != null) {
                changedValues.put("charges", itemCharges);
            }
        } catch (IllegalArgumentException ex) {
            log.warn("Property 'm_iCurrentCharges' not found for entity class {}", e.getDtClass().getDtName());
        }

        // Add the collected information to itemUpdates if there are any changes
        if (!changedValues.isEmpty()) {
            itemUpdates.computeIfAbsent(currentTick, k -> new HashMap<>())
                    .put(e.getIndex(), changedValues);
        }
    }


    @OnMessage(Demo.CDemoFileInfo.class)  // Correct usage
    public void onDemoFileInfo(Demo.CDemoFileInfo demoFileInfo) { // Correct usage
        this.demoFileInfo = demoFileInfo; 
    }

    @OnTickEnd
    protected void onTickEnd(boolean synthetic) {
        if (gameStartTick == -1 && !playerIDMapping.isEmpty()) {
            gameStartTick = currentTick; 
        }
        
        // Correcting game end detection
        if (gameStartTick != -1 && gameEndTick == -1 && 
            demoFileInfo != null && ctx.getTick() == demoFileInfo.getPlaybackTicks() - 1) { 
            gameEndTick = currentTick; 
        }

        deferredActions.forEach(Runnable::run);
        deferredActions.clear();
        currentTick++;
    }

    private void ensureFieldPathForEntityInitialized(Entity e) {
        Integer cid = e.getDtClass().getClassId();
        if (!lifeStatePaths.containsKey(cid)) {
            lifeStatePaths.put(cid, e.getDtClass().getFieldPathForName("m_lifeState"));
        }
    }

    private FieldPath getFieldPathForEntity(Entity e) {
        return lifeStatePaths.get(e.getDtClass().getClassId());
    }

    private void clearCachedState(Entity e) {
        currentLifeState.remove(e.getIndex());
    }

    private void processLifeStateChange(Entity e, FieldPath p) {
        int oldState = currentLifeState.containsKey(e.getIndex()) ? currentLifeState.get(e.getIndex()) : -1;
        int newState = e.getPropertyForFieldPath(p);
        if (oldState != newState) {
            currentLifeState.put(e.getIndex(), newState);

            // Only process if it's a hero unit
            if (e.getDtClass().getDtName().startsWith("CDOTA_Unit_Hero_")) {
                int playerIndex = getPlayerIndexForHero(e);
                if (playerIndex != -1) {
                    Map<String, Object> eventData = recordHeroEvent(playerIndex, newState);
                    if (eventData != null) {
                        heroUpdates.computeIfAbsent(currentTick, k -> new HashMap<>())
                                .put(playerIndex, eventData);
                    }
                }
            }
        }
    }

    private int getPlayerIndexForHero(Entity heroEntity) {
        for (int i = 0; i < 10; i++) {
            if (heroLookup[i] != null && heroLookup[i].getHeroEntity() == heroEntity) {
                return i;
            }
        }
        return -1;
    }

    private Map<String, Object> recordHeroEvent(int playerIndex, int newState) {
        String eventName = "";
        Map<String, Object> eventData = new HashMap<>();

        switch (newState) {
            case 0:
                eventName = "spawned";
                break;
            case 2:
                eventName = "died";
                break;
        }

        if (!eventName.isEmpty()) {
            eventData.put("event", eventName);
            return eventData;
        }

        return null;
    }

    public void run(String[] args) throws Exception {
        long tStart = System.currentTimeMillis();
        SimpleRunner r = null;
        try {
            r = new SimpleRunner(new MappedFileSource(args[0])).runWith(this);
        } finally {
            long tMatch = System.currentTimeMillis() - tStart;
            log.info("total time taken: {}s", (tMatch) / 1000.0);
            if (r != null) {
                r.getSource().close();
            }
        }

        float tickRate = (float) demoFileInfo.getPlaybackTime() / demoFileInfo.getPlaybackTicks(); 
        float gameStartTimeSeconds = gameStartTick * tickRate; 
        float gameEndTimeSeconds = gameEndTick * tickRate; 
        
        log.info("tickRate: {}s", tickRate);
        log.info("gameStartTime: {}s (tick {})", gameStartTimeSeconds, gameStartTick);
        log.info("gameEndTime: {}s (tick {})", gameEndTimeSeconds, gameEndTick);
        
        int calculatedGameDurationTicks = gameEndTick - gameStartTick + 1; // +1 to include start tick 
        log.info("Calculated game duration: {} ticks", calculatedGameDurationTicks);

        if (calculatedGameDurationTicks != demoFileInfo.getPlaybackTicks()) {
            log.warn("WARNING: Calculated game duration does not match demo playback ticks!");
        }

        // Prepare the final JSON structure
        Map<String, Object> finalOutput = new LinkedHashMap<>();
        finalOutput.put("playerIDMapping", playerIDMapping);
        finalOutput.put("heroUpdates", heroUpdates);
        Map<String, Object> gameStartTime = new HashMap<>();
        gameStartTime.put("seconds", gameStartTimeSeconds);
        gameStartTime.put("tick", gameStartTick);
        finalOutput.put("gameStartTime", gameStartTime); 
        finalOutput.put("gameEndTime", gameEndTimeSeconds); // You can create a similar map for endTime if needed

        // Save hero updates along with playerIDMapping
        String heroFilename = args[0].replaceAll(".dem$", "_cdotaunithero.json");
        try (FileWriter file = new FileWriter(heroFilename)) {
            objectMapper.writeValue(file, finalOutput);
        } catch (IOException ex) {
            log.error("Error writing hero JSON file: {}", ex.getMessage());
        }

        // Save item updates as usual
        String itemFilename = args[0].replaceAll(".dem$", "_cdotaitem.json");
        try (FileWriter file = new FileWriter(itemFilename)) {
            objectMapper.writeValue(file, itemUpdates);
        } catch (IOException ex) {
            log.error("Error writing item JSON file: {}", ex.getMessage());
        }
    }

    public static void main(String[] args) throws Exception {
        try {
            new Main().run(args);
        } catch (Exception e) {
            Thread.sleep(1000);
            throw e;
        }
    }
}