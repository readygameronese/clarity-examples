package skadistats.clarity.examples.cdotaunithero;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.event.Insert;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.processor.entities.Entities;
import skadistats.clarity.processor.entities.OnEntityUpdated;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.reader.OnTickEnd;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.processor.sendtables.DTClasses;
import skadistats.clarity.processor.sendtables.OnDTClassesComplete;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.Clarity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

@UsesEntities
public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    @Insert
    private DTClasses dtClasses;

    @Insert
    private Entities entities;

    private DTClass playerResourceClass;
    private final PlayerResourceLookup[] playerLookup = new PlayerResourceLookup[10];
    private final HeroLookup[] heroLookup = new HeroLookup[10];
    private final List<Runnable> deferredActions = new ArrayList<>();

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
        } else {
            for (int p = 0; p < 10; p++) {
                HeroLookup lookup = heroLookup[p];
                if (lookup == null) continue;
                if (lookup.isAnyFieldChanged(e, changedFieldPaths, nChangedFieldPaths)) {
                    lookup.updateFieldValues(e, changedFieldPaths);
        
                    System.out.println("Player " + p + " hero data updated:");
                    for (Map.Entry<FieldPath, Object> entry : lookup.getFieldValues().entrySet()) {
                        // Get field name from DTClass
                        String fieldName = e.getDtClass().getNameForFieldPath(entry.getKey());
                        System.out.println(fieldName + ": " + entry.getValue()); 
                    }
                }
            }
        }
    }

    @OnTickEnd
    protected void onTickEnd(boolean synthetic) {
        deferredActions.forEach(Runnable::run);
        deferredActions.clear();
    }

    public void run(String[] args) throws Exception {
        long tStart = System.currentTimeMillis();
        MappedFileSource s = new MappedFileSource(args[0]);
        SimpleRunner runner = new SimpleRunner(s);
        runner.runWith(this);
        long tMatch = System.currentTimeMillis() - tStart;
        log.info("total time taken: {}s", (tMatch) / 1000.0);
        s.close();
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
