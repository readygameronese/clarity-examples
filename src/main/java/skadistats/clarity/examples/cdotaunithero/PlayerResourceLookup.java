package skadistats.clarity.examples.cdotaunithero; // Make sure this matches your package declaration

import skadistats.clarity.io.Util;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;

import static java.lang.String.format;

public class PlayerResourceLookup {

    private final FieldPath fpSelectedHero;

    public PlayerResourceLookup(DTClass playerResourceClass, int idx) {
        this.fpSelectedHero = playerResourceClass.getFieldPathForName(
                format("m_vecPlayerTeamData.%s.m_hSelectedHero", Util.arrayIdxToString(idx))
        );
    }

    public boolean isSelectedHeroChanged(Entity playerResource, FieldPath[] changedFieldPaths, int nChangedFieldPaths) {
        for (int f = 0; f < nChangedFieldPaths; f++) {
            FieldPath changedFieldPath = changedFieldPaths[f];
            if (changedFieldPath.equals(fpSelectedHero)) return true;
        }
        return false;
    }

    public int getSelectedHeroHandle(Entity playerResource) {
        return playerResource.getPropertyForFieldPath(fpSelectedHero);
    }
}
