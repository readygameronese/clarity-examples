package skadistats.clarity.examples.cdotaunithero;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;

import java.util.HashMap;
import java.util.Map;

public class ItemLookup {
    private static final Logger log = LoggerFactory.getLogger(ItemLookup.class);
    private final Entity itemEntity;
    private final Map<FieldPath, Object> previousFieldValues = new HashMap<>();
    private final Map<FieldPath, Object> fieldValues = new HashMap<>();

    public ItemLookup(Entity itemEntity) {
        this.itemEntity = itemEntity;
        initializeFieldValues("m_iCurrentCharges"); 
    }

    private void initializeFieldValues(String fieldName) {
        DTClass itemClass = itemEntity.getDtClass();
        FieldPath fieldPath = itemClass.getFieldPathForName(fieldName); 

        if (fieldPath != null) {
            Object value = itemEntity.getPropertyForFieldPath(fieldPath);
            fieldValues.put(fieldPath, value);
            previousFieldValues.put(fieldPath, value);
        } else {
            log.warn("Field '{}' not found in item entity {}", fieldName, itemEntity.getDtClass().getDtName());
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
        if (e.getHandle() != itemEntity.getHandle()) return false; // Compare to itemEntity
        return changedFieldPaths.length > 0;
    }
}