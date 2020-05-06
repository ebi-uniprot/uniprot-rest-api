package org.uniprot.api.unisave.repository.domain.impl;

import java.util.LinkedList;
import java.util.List;

import org.uniprot.api.unisave.repository.domain.DiffPatch;

import com.google.common.base.Strings;

public class DiffPatchImpl implements DiffPatch {
    private final diff_match_patch diffService = new diff_match_patch();

    @Override
    public String diff(String entry1, String entry2) {
        LinkedList<diff_match_patch.Diff> diffMain = diffService.diff_main(entry1, entry2);

        LinkedList<diff_match_patch.Patch> patchMake = diffService.patch_make(entry1, diffMain);

        return diffService.patch_toText(patchMake);
    }

    @Override
    public String patch(String entry, String patch) {
        patch = Strings.nullToEmpty(patch);

        List<diff_match_patch.Patch> patchFromText = diffService.patch_fromText(patch);
        LinkedList<diff_match_patch.Patch> patches = new LinkedList<>(patchFromText);
        Object[] appliedPatch = diffService.patch_apply(patches, entry);
        return String.class.cast(appliedPatch[0]);
    }
}
