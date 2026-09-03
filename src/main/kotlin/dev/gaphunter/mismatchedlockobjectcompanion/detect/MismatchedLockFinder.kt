package dev.gaphunter.mismatchedlockobjectcompanion.detect

import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiNewExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiSynchronizedStatement
import dev.gaphunter.mismatchedlockobjectcompanion.model.MismatchedLockHit

/**
 * Real alias analysis, but proving the OPPOSITE of what
 * `deadlock-lock-order-companion` proves -- NON-identity, not
 * identity. Two `synchronized(lockA)`/`synchronized(lockB)` blocks in
 * the same class both access the same field, but `lockA` and `lockB`
 * are each a `final` field initialized to its OWN separate `new`
 * expression -- the strongest identity guarantee Java offers short of
 * real points-to analysis (confirmed by research: real alias-analysis
 * tools like Chord use exactly this "same final field -> same heap
 * object" reasoning). Two DIFFERENT `new` expressions can never
 * produce the same reference, so the two lock objects are PROVABLY
 * never the same monitor -- the "mutual exclusion" the code appears
 * to implement doesn't actually exist.
 *
 * **A false positive here is unacceptable** (declaring two objects
 * "provably different" when they might be the same would be a much
 * worse mistake than a missed finding) -- this is why the check is
 * restricted to the ONE shape where the guarantee is absolute (a
 * `final` field's OWN initializer is a `new` expression), never a
 * constructor-assigned field, a getter, or anything requiring a
 * weaker inference.
 *
 * **v0.1 scope, stated honestly:** only an unqualified lock reference
 * (`synchronized(lockA)`, implicit `this`) -- `synchronized(other.lockA)`
 * is out of scope; only a field's OWN direct initializer
 * (`private final Object lockA = new Object();`) -- a field assigned
 * in a constructor body is never treated as provably distinct (a real
 * limitation, not a guess).
 */
object MismatchedLockFinder {

    fun findAll(file: PsiFile): List<MismatchedLockHit> {
        val hits = mutableListOf<MismatchedLockHit>()
        file.accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitClass(psiClass: PsiClass) {
                super.visitClass(psiClass)
                hits += findMismatchesIn(psiClass)
            }
        })
        return hits
    }

    private data class SyncBlockInfo(val lockFieldName: String, val guardedFieldNames: Set<String>, val anchor: PsiElement)

    private fun findMismatchesIn(psiClass: PsiClass): List<MismatchedLockHit> {
        val provablyDistinctLockFields = psiClass.fields
            .filter { isProvablyDistinctLockField(it) }
            .mapNotNull { it.name }
            .toSet()
        if (provablyDistinctLockFields.size < 2) return emptyList()

        val syncBlocks = findSyncBlocks(psiClass)
        val usagesByGuardedField = LinkedHashMap<String, MutableList<Pair<String, PsiElement>>>()
        for (block in syncBlocks) {
            if (block.lockFieldName !in provablyDistinctLockFields) continue
            for (guardedField in block.guardedFieldNames) {
                usagesByGuardedField.getOrPut(guardedField) { mutableListOf() } += block.lockFieldName to block.anchor
            }
        }

        val hits = mutableListOf<MismatchedLockHit>()
        for ((guardedField, usages) in usagesByGuardedField) {
            val firstLockName = usages.first().first
            for ((lockName, anchor) in usages) {
                if (lockName != firstLockName) {
                    hits += MismatchedLockHit(anchor, guardedField, firstLockName, lockName)
                }
            }
        }
        return hits
    }

    private fun findSyncBlocks(psiClass: PsiClass): List<SyncBlockInfo> {
        val result = mutableListOf<SyncBlockInfo>()
        psiClass.accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitSynchronizedStatement(statement: PsiSynchronizedStatement) {
                super.visitSynchronizedStatement(statement)
                val lockExpression = statement.lockExpression as? PsiReferenceExpression ?: return
                if (lockExpression.qualifierExpression != null) return
                val lockField = lockExpression.resolve() as? PsiField ?: return
                if (lockField.containingClass != psiClass) return
                val lockFieldName = lockField.name ?: return

                val body = statement.body ?: return
                val guardedFields = mutableSetOf<String>()
                body.accept(object : JavaRecursiveElementWalkingVisitor() {
                    override fun visitReferenceExpression(expr: PsiReferenceExpression) {
                        super.visitReferenceExpression(expr)
                        if (expr.qualifierExpression != null) return
                        val field = expr.resolve() as? PsiField ?: return
                        if (field.containingClass == psiClass && field.name != lockFieldName) guardedFields += field.name ?: return
                    }
                })
                if (guardedFields.isEmpty()) return
                result += SyncBlockInfo(lockFieldName, guardedFields, lockExpression)
            }
        })
        return result
    }

    private fun isProvablyDistinctLockField(field: PsiField): Boolean {
        if (!field.hasModifierProperty(PsiModifier.FINAL)) return false
        return field.initializer is PsiNewExpression
    }
}
