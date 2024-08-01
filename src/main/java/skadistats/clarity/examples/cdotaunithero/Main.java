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
import skadistats.clarity.processor.reader.OnTickEnd;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.processor.sendtables.DTClasses;
import skadistats.clarity.processor.sendtables.OnDTClassesComplete;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.Clarity;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static java.lang.String.format;

@UsesEntities
public class Main {

    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final Map<Integer, Map<Integer, Map<String, Object>>> heroUpdates = new TreeMap<>();
    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

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
        } else {
            handleHeroUpdate(e, changedFieldPaths);
        }
    }

    private void handlePlayerResourceUpdate(Entity e, FieldPath[] changedFieldPaths, int nChangedFieldPaths) {
        for (int p = 0; p < 10; p++) {
            PlayerResourceLookup lookup = playerLookup[p];
            if (lookup.isSelectedHeroChanged(e, changedFieldPaths, nChangedFieldPaths)) {
                int playerIndex = p;
                deferredActions.add(() -> {
                    int heroHandle = lookup.getSelectedHeroHandle(e);
                    System.out.format("Player %02d got assigned hero %d\n", playerIndex, heroHandle);
                    Entity heroEntity = entities.getByHandle(heroHandle);
                    heroLookup[playerIndex] = new HeroLookup(heroEntity);
                });
            }
        }
    }

    private void handleHeroUpdate(Entity e, FieldPath[] changedFieldPaths) {
        for (int p = 0; p < 10; p++) {
            HeroLookup lookup = heroLookup[p];
            if (lookup == null) continue;
            if (lookup.isAnyFieldChanged(e, changedFieldPaths)) {
                Map<String, Object> changedValues = lookup.updateAndGetChangedFieldValues(e, changedFieldPaths);
                if (!changedValues.isEmpty()) {
                    heroUpdates.computeIfAbsent(currentTick, k -> new HashMap<>())
                            .put(p, changedValues);
                }
            }
        }
    }

    @OnTickEnd
    protected void onTickEnd(boolean synthetic) {
        deferredActions.forEach(Runnable::run);
        deferredActions.clear();
        currentTick++;
    }

    @OnEntityCreated
    public void onCreated(Context ctx, Entity e) {
        clearCachedState(e);
        ensureFieldPathForEntityInitialized(e);
        FieldPath lifeStatePath = getFieldPathForEntity(e);
        if (lifeStatePath != null) {
            processLifeStateChange(e, lifeStatePath);
        }
    }

    @OnEntityUpdated
    public void onUpdated(Context ctx, Entity e, FieldPath[] fieldPaths, int num) {
        FieldPath lifeStatePath = getFieldPathForEntity(e);
        if (lifeStatePath != null) {
            for (int i = 0; i < num; i++) {
                if (fieldPaths[i].equals(lifeStatePath)) {
                    processLifeStateChange(e, lifeStatePath);
                    break;
                }
            }
        }
    }

    @OnEntityDeleted
    public void onDeleted(Context ctx, Entity e) {
        clearCachedState(e);
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
                    Map<String, Object> eventData = recordHeroEvent(playerIndex, newState); // Get eventData
                    if (eventData != null) { // Check if eventData is valid
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

    private Map<String, Object> recordHeroEvent(int playerIndex, int newState) { // Returns Map<String, Object> or null
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

        String filename = args[0].replaceAll(".dem$", "_cdotaunithero.json");
        try (FileWriter file = new FileWriter(filename)) {
            objectMapper.writeValue(file, heroUpdates);
        } catch (IOException ex) {
            log.error("Error writing JSON file: {}", ex.getMessage());
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