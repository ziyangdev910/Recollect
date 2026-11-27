package com.recollect.app

import com.recollect.feature.planning.PrintJob

/**
 * 生成计划页到演示打印稿的内存交接（不持久化；进程重建后需重新生成计划）。
 *
 * 只保存最近一次成功生成的 `PrintJob`；计划失败/空态时由 [update] 清空。
 * 2026-11-27 起独立打印页删除，保留该交接供首次演示生成可扫描的打印稿。
 */
class PrintPlanSession {
    var latest: PrintJob? = null
        private set

    fun update(job: PrintJob?) {
        latest = job
    }
}
