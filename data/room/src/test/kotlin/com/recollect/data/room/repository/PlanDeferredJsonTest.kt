package com.recollect.data.room.repository

import com.recollect.scheduler.DeferReason
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * `plan.deferred` 编解码：推迟快照必须能原样回读，未知结构不得静默吞掉。
 */
class PlanDeferredJsonTest {

    @Test
    fun `round trips records including special characters`() {
        val records = listOf(
            PlanDeferredRecord("q-1", "13", sourcePage = "2-同步练习", reason = DeferReason.TIME_OVERFLOW),
            PlanDeferredRecord("q-2", "题号 \"2\"\\\n", sourcePage = "", reason = DeferReason.COOLING),
            PlanDeferredRecord("q-3", "第 3 题", sourcePage = "3", reason = DeferReason.UNRESOLVED_KNOWLEDGE),
        )

        val json = PlanDeferredJson.encode(records)

        assertEquals(records, PlanDeferredJson.decode(json))
    }

    @Test
    fun `legacy snapshot without sourcePage decodes to an empty source page`() {
        val decoded = PlanDeferredJson.decode(
            """[{"questionId":"q-1","questionNumber":"1","reason":"TIME_OVERFLOW"}]""",
        )

        assertEquals(
            listOf(PlanDeferredRecord("q-1", "1", reason = DeferReason.TIME_OVERFLOW)),
            decoded,
        )
    }

    @Test
    fun `empty array decodes to empty list`() {
        assertEquals(emptyList<PlanDeferredRecord>(), PlanDeferredJson.decode("[]"))
        assertEquals("[]", PlanDeferredJson.encode(emptyList()))
    }

    @Test
    fun `unknown reason fails instead of being dropped`() {
        assertThrows(IllegalArgumentException::class.java) {
            PlanDeferredJson.decode(
                """[{"questionId":"q-1","questionNumber":"1","reason":"SOMETHING_ELSE"}]""",
            )
        }
    }

    @Test
    fun `missing field fails`() {
        assertThrows(IllegalArgumentException::class.java) {
            PlanDeferredJson.decode("""[{"questionId":"q-1","reason":"COOLING"}]""")
        }
    }

    @Test
    fun `trailing content fails`() {
        assertThrows(IllegalArgumentException::class.java) {
            PlanDeferredJson.decode("""[] extra""")
        }
    }
}
