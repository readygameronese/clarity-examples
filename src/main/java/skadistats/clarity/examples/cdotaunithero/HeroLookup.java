package skadistats.clarity.examples.cdotaunithero;

import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

public class HeroLookup {
    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());
    private final Entity heroEntity;
    private final Map<FieldPath, Object> previousFieldValues = new HashMap<>();
    private final Map<FieldPath, Object> fieldValues = new HashMap<>();

    public Map<FieldPath, Object> getFieldValues() {
        return fieldValues;
    }

    public Entity getHeroEntity() {
        return heroEntity;
    } 

    public HeroLookup(Entity heroEntity) {
        this.heroEntity = heroEntity;
    }

    public Integer getPlayerID() {
        try {
            return heroEntity.getProperty("m_iPlayerID");
        } catch (IllegalArgumentException ex) {
            log.warn("Property 'm_iPlayerID' not found for entity class {}", heroEntity.getDtClass().getDtName());
            return null;
        }
    }


    public Map<String, Object> updateAndGetChangedFieldValues(Entity e, FieldPath[] changedFieldPaths) {
        Map<String, Object> changedValues = new HashMap<>();

        for (FieldPath fp : changedFieldPaths) {
            Object newValue = e.getPropertyForFieldPath(fp);
            fieldValues.put(fp, newValue); 

            if (!newValue.equals(previousFieldValues.get(fp))) {
                String fieldName = e.getDtClass().getNameForFieldPath(fp);
                changedValues.put(fieldName, newValue);
                previousFieldValues.put(fp, newValue); 
            }
        }

        return changedValues; 
    }


    public boolean isAnyFieldChanged(Entity e, FieldPath[] changedFieldPaths) {
        if (e != heroEntity) return false;
        return changedFieldPaths.length > 0;
    }

}