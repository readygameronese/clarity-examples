package skadistats.clarity.examples.cdotaunithero;

import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;

import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

public class HeroLookup {
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
        DTClass heroClass = heroEntity.getDtClass();
        initializeFieldValues(heroClass, "cellX");
        initializeFieldValues(heroClass, "cellY");
        initializeFieldValues(heroClass, "cellZ");
        initializeFieldValues(heroClass, "vecX");
        initializeFieldValues(heroClass, "vecY");
        initializeFieldValues(heroClass, "vecZ");
    }

    private void initializeFieldValues(DTClass heroClass, String which) {
        FieldPath fieldPath = getBodyComponentFieldPath(heroClass, which);
        Object value = heroEntity.getPropertyForFieldPath(fieldPath);
        fieldValues.put(fieldPath, value);
        previousFieldValues.put(fieldPath, value); 
    }

    private FieldPath getBodyComponentFieldPath(DTClass heroClass, String which) {
        return heroClass.getFieldPathForName(format("CBodyComponent.m_%s", which));
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