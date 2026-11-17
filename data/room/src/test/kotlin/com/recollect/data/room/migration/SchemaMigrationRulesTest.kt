package com.recollect.data.room.migration

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * 迁移守卫：新增 `knowledge_point.number` / `plan_page.pageId` 唯一索引前，
 * 遇到重复值必须失败并提示，不得静默重编号。
 */
class SchemaMigrationRulesTest {

    @Test
    fun duplicateNumberOrPageIdFailsInsteadOfRenumbering() {
        listOf(
            Triple("knowledge_point", "number", listOf<Any?>(1, 2, 2)),
            Triple("plan_page", "pageId", listOf<Any?>("AB12CD-01", "AB12CD-01", "AB12CD-02")),
        ).forEach { (table, column, values) ->
            val error = assertThrows<IllegalStateException> {
                SchemaMigrationRules.requireUniqueValues(table, column, values)
            }
            val message = error.message.orEmpty()
            assertTrue(message.contains("$table.$column"), message)
            assertTrue(message.contains("重复"), message)
            assertTrue(message.contains("不得静默重编号"), message)
        }
    }

    @Test
    fun uniqueValuesPass() {
        assertDoesNotThrow {
            SchemaMigrationRules.requireUniqueValues("knowledge_point", "number", listOf(1, 2, 3))
            SchemaMigrationRules.requireUniqueValues("plan_page", "pageId", listOf("AB12CD-01", "AB12CD-02"))
        }
    }
}
