package dev.gaphunter.mismatchedlockobjectcompanion.inspection

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiFile
import dev.gaphunter.mismatchedlockobjectcompanion.detect.MismatchedLockFinder
import dev.gaphunter.mismatchedlockobjectcompanion.model.MismatchedLockHit
import dev.gaphunter.mismatchedlockobjectcompanion.review.ReviewPrompt

/** Flags a `synchronized` block whose lock object is PROVEN to never be the same as another lock guarding the same field elsewhere in the class -- see [MismatchedLockFinder]. */
class MismatchedLockInspection : LocalInspectionTool() {

    companion object {
        const val MAX_FILE_LENGTH = 500_000
    }

    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (file.text.length > MAX_FILE_LENGTH) return null

        val hits = MismatchedLockFinder.findAll(file)
        if (hits.isEmpty()) return null

        val problems = hits.map { hit ->
            manager.createProblemDescriptor(
                hit.anchor,
                messageFor(hit),
                isOnTheFly,
                emptyArray(),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            )
        }

        val path = file.virtualFile?.path
        if (path != null) {
            for (hit in hits) {
                val lineNumber = file.viewProvider.document?.getLineNumber(hit.anchor.textRange.startOffset) ?: -1
                ReviewPrompt.recordHit(file.project, "$path:$lineNumber:${hit.guardedFieldName}")
            }
        }

        return problems.toTypedArray()
    }

    private fun messageFor(hit: MismatchedLockHit): String =
        "'${hit.guardedFieldName}' is guarded by 'synchronized(${hit.firstLockName})' elsewhere in this class, but here it's guarded by " +
            "'synchronized(${hit.secondLockName})' -- both are final fields each initialized to their own separate 'new', so they can " +
            "NEVER be the same monitor: this synchronization provides no real mutual exclusion"
}
