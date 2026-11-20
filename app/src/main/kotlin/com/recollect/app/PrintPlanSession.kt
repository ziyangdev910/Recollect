package com.recollect.app

import com.recollect.feature.planning.PrintJob

/**
 * 计划页到打印页的内存交接（不持久化；进程重建后需重新生成计划）。
 *
 * 只保存最近一次成功生成的 `PrintJob`；计划失败/空态时由 [update] 清空。
 */
class PrintPlanSession {
    var latest: PrintJob? = null
        private set

    fun update(job: PrintJob?) {
        latest = job
    }
}
