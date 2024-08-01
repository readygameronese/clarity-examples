package skadistats.clarity.examples.cdotaunithero;

import skadistats.clarity.io.Util;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;

import static java.lang.String.format;

public class PlayerResourceLookup {

    private final FieldPath fpSelectedHero;
    private int selectedHeroHandle; // Store the selected hero handle

    public PlayerResourceLookup(DTClass playerResourceClass, int idx) {
        this.fpSelectedHero = playerResourceClass.getFieldPathForName(
                format("m_vecPlayerTeamData.%s.m_hSelectedHero", Util.arrayIdxToString(idx))
        );
        this.selectedHeroHandle = -1; // Initialize with an invalid handle
    }

    public boolean isSelectedHeroChanged(Entity playerResource, FieldPath[] changedFieldPaths, int nChangedFieldPaths) {
        for (int f = 0; f < nChangedFieldPaths; f++) {
            FieldPath changedFieldPath = changedFieldPaths[f];
            if (changedFieldPath.equals(fpSelectedHero)) return true;
        }
        return false;
    }

    // Now requires the playerResource entity:
    public int getSelectedHeroHandle(Entity playerResource) { 
        this.selectedHeroHandle = playerResource.getPropertyForFieldPath(fpSelectedHero);
        return this.selectedHeroHandle; 
    }
}