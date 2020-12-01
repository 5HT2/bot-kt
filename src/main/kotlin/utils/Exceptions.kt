package org.kamiblue.botkt.utils

import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType

val NO_READER_EXCEPTION = SimpleCommandExceptionType(LiteralMessage("There was no reader to read the argument."))
