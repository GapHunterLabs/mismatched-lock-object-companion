package dev.gaphunter.mismatchedlockobjectcompanion.model

import com.intellij.psi.PsiElement

/** A confirmed broken mutual exclusion: [guardedFieldName] is accessed inside `synchronized([firstLockName])` in one place and `synchronized([secondLockName])` in another, where both lock fields are PROVEN (final, each initialized to its own separate `new` expression) to never be the same object. [anchor] is the second (mismatching) lock expression. */
data class MismatchedLockHit(
    val anchor: PsiElement,
    val guardedFieldName: String,
    val firstLockName: String,
    val secondLockName: String,
)
