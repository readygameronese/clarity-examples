package skadistats.clarity.examples.cdotaunithero;

import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.Vector;

import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

public class HeroLookup {
    private final Entity heroEntity;
    private final Map<FieldPath, Object> previousFieldValues = new HashMap<>();
    private final Map<FieldPath, Object> fieldValues = new HashMap<>();
    private final FieldPath fpCellX;
    private final FieldPath fpCellY;
    private final FieldPath fpCellZ;
    private final FieldPath fpVecX;
    private final FieldPath fpVecY;
    private final FieldPath fpVecZ;

    public Map<FieldPath, Object> getFieldValues() {
        return fieldValues;
    }

    public Entity getHeroEntity() {
        return heroEntity;
    }

    public HeroLookup(Entity heroEntity) {
        this.heroEntity = heroEntity;
        DTClass heroClass = heroEntity.getDtClass();
        this.fpCellX = getBodyComponentFieldPath(heroClass, "cellX");
        this.fpCellY = getBodyComponentFieldPath(heroClass, "cellY");
        this.fpCellZ = getBodyComponentFieldPath(heroClass, "cellZ");
        this.fpVecX = getBodyComponentFieldPath(heroClass, "vecX");
        this.fpVecY = getBodyComponentFieldPath(heroClass, "vecY");
        this.fpVecZ = getBodyComponentFieldPath(heroClass, "vecZ");
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
        if (e.getHandle() != heroEntity.getHandle()) return false;
        return changedFieldPaths.length > 0;
    }

    public boolean isPositionChanged(Entity e, FieldPath[] changedFieldPaths) {
        if (e != heroEntity) return false;
        for (FieldPath changedFieldPath : changedFieldPaths) {
            if (changedFieldPath.equals(fpCellX)) return true;
            if (changedFieldPath.equals(fpCellY)) return true;
            if (changedFieldPath.equals(fpCellZ)) return true;
            if (changedFieldPath.equals(fpVecX)) return true;
            if (changedFieldPath.equals(fpVecY)) return true;
            if (changedFieldPath.equals(fpVecZ)) return true;
        }
        return false;
    }

    public Vector getPosition() {
        return new Vector(
                getPositionComponent(fpCellX, fpVecX),
                getPositionComponent(fpCellY, fpVecY),
                getPositionComponent(fpCellZ, fpVecZ)
        );
    }

    private float getPositionComponent(FieldPath fpCell, FieldPath fpVec) {
        int cell = heroEntity.getPropertyForFieldPath(fpCell);
        float vec = heroEntity.getPropertyForFieldPath(fpVec);
        return cell * 128.0f + vec;
    }
}