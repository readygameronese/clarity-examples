package skadistats.clarity.examples.cdotaunithero;

import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;

import java.util.HashMap;
import java.util.Map;

public class ItemLookup {
    private final Entity itemEntity;
    private final Map<FieldPath, Object> previousFieldValues = new HashMap<>();
    private final Map<FieldPath, Object> fieldValues = new HashMap<>();

    // Field path for current charges
    private final FieldPath fpCurrentCharges; 

    public ItemLookup(Entity itemEntity) {
        this.itemEntity = itemEntity;
        DTClass itemClass = itemEntity.getDtClass();

        // Initialize the FieldPath FIRST 
        this.fpCurrentCharges = itemClass.getFieldPathForName("m_iCurrentCharges"); 

        // Now initialize the field value 
        initializeFieldValues(fpCurrentCharges);
        // ... initialize other FieldPaths (if needed)
    }

    private void initializeFieldValues(FieldPath fieldPath) {
        Object value = itemEntity.getPropertyForFieldPath(fieldPath);
        fieldValues.put(fieldPath, value);
        previousFieldValues.put(fieldPath, value);
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
        if (e != itemEntity) return false;
        return changedFieldPaths.length > 0;
    }
}