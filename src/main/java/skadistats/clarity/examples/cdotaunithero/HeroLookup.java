package skadistats.clarity.examples.cdotaunithero;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;

import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

public class HeroLookup {
    private final Entity heroEntity;
    private final Map<FieldPath, Object> fieldValues = new HashMap<>();


    public Map<FieldPath, Object> getFieldValues() {
        return fieldValues;
    }
    
    public HeroLookup(Entity heroEntity) {
        this.heroEntity = heroEntity;
        DTClass heroClass = heroEntity.getDtClass();
        this.fieldValues.put(getBodyComponentFieldPath(heroClass, "cellX"), heroEntity.getPropertyForFieldPath(getBodyComponentFieldPath(heroClass, "cellX")));
        this.fieldValues.put(getBodyComponentFieldPath(heroClass, "cellY"), heroEntity.getPropertyForFieldPath(getBodyComponentFieldPath(heroClass, "cellY")));
        this.fieldValues.put(getBodyComponentFieldPath(heroClass, "cellZ"), heroEntity.getPropertyForFieldPath(getBodyComponentFieldPath(heroClass, "cellZ")));
        this.fieldValues.put(getBodyComponentFieldPath(heroClass, "vecX"), heroEntity.getPropertyForFieldPath(getBodyComponentFieldPath(heroClass, "vecX")));
        this.fieldValues.put(getBodyComponentFieldPath(heroClass, "vecY"), heroEntity.getPropertyForFieldPath(getBodyComponentFieldPath(heroClass, "vecY")));
        this.fieldValues.put(getBodyComponentFieldPath(heroClass, "vecZ"), heroEntity.getPropertyForFieldPath(getBodyComponentFieldPath(heroClass, "vecZ")));
    }

    private FieldPath getBodyComponentFieldPath(DTClass heroClass, String which) {
        return heroClass.getFieldPathForName(format("CBodyComponent.m_%s", which));
    }


    public Map<String, Object> getFieldValuesAsStringMap() {
        Map<String, Object> stringFieldValues = new HashMap<>();
        for (Map.Entry<FieldPath, Object> entry : fieldValues.entrySet()) {
            stringFieldValues.put(entry.getKey().toString(), entry.getValue());
        }
        return stringFieldValues;
    }
    public boolean isAnyFieldChanged(Entity e, FieldPath[] changedFieldPaths, int nChangedFieldPaths) {
        if (e != heroEntity) return false;
        return true;
    }

    public void updateFieldValues(Entity e, FieldPath[] changedFieldPaths) {
        for (FieldPath fp : changedFieldPaths) {
            fieldValues.put(fp, e.getPropertyForFieldPath(fp));
        }
    }
}
