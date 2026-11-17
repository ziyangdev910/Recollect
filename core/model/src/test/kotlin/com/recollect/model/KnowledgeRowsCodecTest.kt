package com.recollect.model

import kotlin.test.Test
import kotlin.test.assertEquals

class KnowledgeRowsCodecTest {

    /**
     * 阶段 3.1（C-046 方案 A、C-015）：`PlanPage.knowledgeRows` 的 JSON 往返。
     *
     * 输入取自 docs/specs/data-model.md 的示例；rows 的数量与顺序就是打印时的行序，
     * triggeredKnowledgePointIds 原样保留；解码不得按 number 重排（C-015 冻结）。
     */
    @Test
    fun `decodes and re-encodes the knowledge rows snapshot without reordering`() {
        val json =
            """[{"questionId":"q1","rows":[{"knowledgePointId":"k1","number":3},{"knowledgePointId":"k2","number":5}],"triggeredKnowledgePointIds":["k1"]}]"""

        val decoded = KnowledgeRowsCodec.decode(json)

        assertEquals(1, decoded.size, "示例 JSON 只有一条题目行快照")
        assertEquals(
            listOf(
                PlanPageRows(
                    questionId = "q1",
                    rows = listOf(KnowledgeRow("k1", 3), KnowledgeRow("k2", 5)),
                    triggeredKnowledgePointIds = listOf("k1"),
                ),
            ),
            decoded,
            "rows 数量/顺序与 triggeredKnowledgePointIds 必须原样保留",
        )
        assertEquals(json, KnowledgeRowsCodec.encode(decoded), "decode→encode 往返应还原原始 JSON")

        // C-015：行快照冻结打印时的顺序，number 5 排在 number 3 之前也不得重排。
        val unordered =
            """[{"questionId":"q2","rows":[{"knowledgePointId":"k5","number":5},{"knowledgePointId":"k3","number":3}],"triggeredKnowledgePointIds":["k3","k5"]}]"""

        val decodedUnordered = KnowledgeRowsCodec.decode(unordered)

        assertEquals(
            listOf(5, 3),
            decodedUnordered.single().rows.map { it.number },
            "不得按 number 重排 rows（C-015 冻结）",
        )
        assertEquals(
            listOf("k3", "k5"),
            decodedUnordered.single().triggeredKnowledgePointIds,
            "triggeredKnowledgePointIds 顺序原样保留",
        )
        assertEquals(unordered, KnowledgeRowsCodec.encode(decodedUnordered), "乱序行快照也必须无损往返")
    }
}
