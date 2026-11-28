package com.recollect.feature.confirm

import kotlin.test.Test
import kotlin.test.assertEquals

/** 确认后笔记摘要的显示格式（2026-11-28 用户要求）。 */
class ReviewNoteTextTest {

    @Test
    fun empty_note_shows_none_after_the_colon() {
        assertEquals("复习笔记：无", reviewNoteText(null))
        assertEquals("复习笔记：无", reviewNoteText(""))
        assertEquals("复习笔记：无", reviewNoteText("   \n  "))
    }

    @Test
    fun single_line_note_follows_the_colon() {
        assertEquals("复习笔记：x+1=2", reviewNoteText("x+1=2"))
        assertEquals("复习笔记：x+1=2", reviewNoteText("  x+1=2  "))
    }

    @Test
    fun multi_line_note_starts_on_a_new_line() {
        assertEquals(
            "复习笔记：\n第一行\n第二行",
            reviewNoteText("第一行\n第二行"),
        )
    }
}
