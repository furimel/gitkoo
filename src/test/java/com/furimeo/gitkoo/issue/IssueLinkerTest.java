package com.furimeo.gitkoo.issue;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class IssueLinkerTest {

    @Test
    void findReferencesDetectsHashNumbers() {
        assertThat(IssueLinker.findReferences("fix parser crash #42")).containsExactly(42);
        assertThat(IssueLinker.findReferences("relates to #13 and #7")).containsExactly(13, 7);
        assertThat(IssueLinker.findReferences("no refs here")).isEmpty();
        assertThat(IssueLinker.findReferences(null)).isEmpty();
    }

    @Test
    void findAutoCloseDetectsKeywords() {
        assertThat(IssueLinker.findAutoClose("fixes #42")).containsExactly(42);
        assertThat(IssueLinker.findAutoClose("closes #13")).containsExactly(13);
        assertThat(IssueLinker.findAutoClose("resolved #7")).containsExactly(7);
        assertThat(IssueLinker.findAutoClose("fix #100")).containsExactly(100);
        assertThat(IssueLinker.findAutoClose("fixed #200")).containsExactly(200);
        assertThat(IssueLinker.findAutoClose("close #300")).containsExactly(300);
        assertThat(IssueLinker.findAutoClose("closed #400")).containsExactly(400);
        assertThat(IssueLinker.findAutoClose("resolve #500")).containsExactly(500);
    }

    @Test
    void findAutoCloseIgnoresPlainReferences() {
        assertThat(IssueLinker.findAutoClose("see #42")).isEmpty();
        assertThat(IssueLinker.findAutoClose("reference #42 but not close")).isEmpty();
    }

    @Test
    void findAutoCloseIsCaseInsensitive() {
        assertThat(IssueLinker.findAutoClose("Fixes #42")).containsExactly(42);
        assertThat(IssueLinker.findAutoClose("CLOSES #13")).containsExactly(13);
        assertThat(IssueLinker.findAutoClose("Resolves #7")).containsExactly(7);
    }

    @Test
    void findAutoCloseHandlesMultiple() {
        List<Integer> result = IssueLinker.findAutoClose("fixes #1 and closes #2");
        assertThat(result).containsExactly(1, 2);
    }
}
