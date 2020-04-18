package org.uniprot.api.unisave.repository.domain.impl;

import com.google.common.base.Strings;
import org.uniprot.api.unisave.repository.domain.DiffPatch;

import java.util.LinkedList;
import java.util.List;

public class DiffPatchImpl implements DiffPatch {

    private diff_match_patch diffService = new diff_match_patch();

    @Override
    public String diff(String entry1, String entry2) {
        LinkedList<diff_match_patch.Diff> diff_main = diffService.diff_main(entry1, entry2);

        LinkedList<diff_match_patch.Patch> patch_make = diffService.patch_make(entry1, diff_main);

        String patch_toText = diffService.patch_toText(patch_make);
        return patch_toText;
    }

    @Override
    public String patch(String entry, String patch) {

        patch = Strings.nullToEmpty(patch);

        List<diff_match_patch.Patch> patch_fromText = diffService.patch_fromText(patch);
        LinkedList<diff_match_patch.Patch> linkedList = new LinkedList<>();
        linkedList.addAll(patch_fromText);
        Object[] patch_apply = diffService.patch_apply(linkedList, entry);
        String text = String.class.cast(patch_apply[0]);
        return text;
    }
}
