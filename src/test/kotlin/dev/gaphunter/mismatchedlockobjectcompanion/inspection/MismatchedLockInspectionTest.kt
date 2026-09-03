package dev.gaphunter.mismatchedlockobjectcompanion.inspection

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MismatchedLockInspectionTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(MismatchedLockInspection::class.java)
    }

    fun `test the same field guarded by two provably different final locks is flagged`() {
        myFixture.configureByText(
            "Counter1.java",
            """
            class Counter1 {
                private final Object lockA = new Object();
                private final Object lockB = new Object();
                private int count;

                void increment() {
                    synchronized (lockA) {
                        count++;
                    }
                }

                void reset() {
                    synchronized (lockB) {
                        count = 0;
                    }
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.any { it.description?.contains("NEVER be the same monitor") == true })
    }

    fun `test the same field guarded by the same lock consistently is not flagged`() {
        myFixture.configureByText(
            "Counter2.java",
            """
            class Counter2 {
                private final Object lock = new Object();
                private int count;

                void increment() {
                    synchronized (lock) {
                        count++;
                    }
                }

                void reset() {
                    synchronized (lock) {
                        count = 0;
                    }
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("NEVER be the same monitor") == true })
    }

    fun `test a non-final lock candidate is never treated as provably distinct`() {
        myFixture.configureByText(
            "Counter3.java",
            """
            class Counter3 {
                private Object lockA = new Object();
                private final Object lockB = new Object();
                private int count;

                void increment() {
                    synchronized (lockA) {
                        count++;
                    }
                }

                void reset() {
                    synchronized (lockB) {
                        count = 0;
                    }
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("NEVER be the same monitor") == true })
    }

    fun `test two different locks guarding two different fields is not flagged`() {
        myFixture.configureByText(
            "Counter4.java",
            """
            class Counter4 {
                private final Object lockA = new Object();
                private final Object lockB = new Object();
                private int countA;
                private int countB;

                void incrementA() {
                    synchronized (lockA) {
                        countA++;
                    }
                }

                void incrementB() {
                    synchronized (lockB) {
                        countB++;
                    }
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("NEVER be the same monitor") == true })
    }
}
