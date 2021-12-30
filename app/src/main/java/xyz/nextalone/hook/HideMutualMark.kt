/*
 * QNotified - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 dmca@ioctl.cc
 * https://github.com/ferredoxin/QNotified
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by ferredoxin.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 * <https://github.com/ferredoxin/QNotified/blob/master/LICENSE.md>.
 */
package xyz.nextalone.hook

import xyz.nextalone.util.method
import xyz.nextalone.util.replace
import xyz.nextalone.util.tryOrFalse
import me.singleneuron.qn_kernel.data.requireMinQQVersion
import nil.nadph.qnotified.util.QQVersion
import nil.nadph.qnotified.base.annotation.FunctionEntry
import nil.nadph.qnotified.hook.CommonDelayableHook

@FunctionEntry
object HideMutualMark : CommonDelayableHook("na_hide_intimate_image_kt") {

    override fun initOnce() = tryOrFalse {
        "Lcom/tencent/mobileqq/widget/navbar/NavBarAIO;->setTitleIconLeftForMutualMark(Lcom/tencent/mobileqq/mutualmark/info/MutualMarkForDisplayInfo;Lcom/tencent/mobileqq/mutualmark/info/MutualMarkForDisplayInfo;)V".method.replace(
            this,
            null
        )
    }

    override fun isValid() = requireMinQQVersion(QQVersion.QQ_8_5_5)
}
