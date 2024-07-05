package io.github.starofall.s3hypersync

import ch.qos.logback.classic.{Level, LoggerContext}
import org.slf4j.LoggerFactory

object SyncLogUtil {

  /** is NOT active on deployment */
  var colorActive = !(
    sys.env.get("ENVIRONMENT").contains("production") || sys.env.get("ENVIRONMENT").contains("staging")
    )

  /** sets the log level based on -vvv amount */
  def setRootLogLevel(levelInt: Int): Unit = {
    val level          = levelInt match {
      case 0 => Level.INFO
      case 1 => Level.DEBUG
      case _ => Level.TRACE
    }
    val iLoggerFactory = LoggerFactory.getILoggerFactory
    iLoggerFactory match {
      case loggerContext: LoggerContext =>
        val rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
        rootLogger.setLevel(level)
      case _                            =>
        throw new IllegalStateException(s"Unexpected ILoggerFactory implementation: ${iLoggerFactory.getClass}")
    }
  }


  import scala.language.implicitConversions


  trait Logger {
    lazy val log = org.slf4j.LoggerFactory.getLogger(getClass)
    implicit def hasRainbow(s: String): RainbowString = new RainbowString(s)

  }

  class RainbowString(s: String) {

    import Console._

    /** Colorize the given string foreground to ANSI black */
    def black = if (colorActive) {
      BLACK + s + RESET
    } else {
      s
    }

    /** Colorize the given string foreground to ANSI red */
    def red = if (colorActive) {
      RED + s + RESET
    } else {
      s
    }

    /** Colorize the given string foreground to ANSI red */
    def green = if (colorActive) {
      GREEN + s + RESET
    } else {
      s
    }

    /** Colorize the given string foreground to ANSI red */
    def yellow = if (colorActive) {
      YELLOW + s + RESET
    } else {
      s
    }

    /** Colorize the given string foreground to ANSI red */
    def blue = if (colorActive) {
      BLUE + s + RESET
    } else {
      s
    }

    /** Colorize the given string foreground to ANSI red */
    def magenta = if (colorActive) {
      MAGENTA + s + RESET
    } else {
      s
    }

    /** Colorize the given string foreground to ANSI red */
    def cyan = if (colorActive) {
      CYAN + s + RESET
    } else {
      s
    }

    /** Colorize the given string foreground to ANSI red */
    def white = if (colorActive) {
      WHITE + s + RESET
    } else {
      s
    }

    /** Colorize the given string background to ANSI red */
    def onBlack = if (colorActive) {
      BLACK_B + s + RESET
    } else {
      s
    }

    /** Colorize the given string background to ANSI red */
    def onRed = if (colorActive) {
      RED_B + s + RESET
    } else {
      s
    }

    /** Colorize the given string background to ANSI red */
    def onGreen = if (colorActive) {
      GREEN_B + s + RESET
    } else {
      s
    }

    /** Colorize the given string background to ANSI red */
    def onYellow = if (colorActive) {
      YELLOW_B + s + RESET
    } else {
      s
    }

    /** Colorize the given string background to ANSI red */
    def onBlue = if (colorActive) {
      BLUE_B + s + RESET
    } else {
      s
    }

    /** Colorize the given string background to ANSI red */
    def onMagenta = if (colorActive) {
      MAGENTA_B + s + RESET
    } else {
      s
    }

    /** Colorize the given string background to ANSI red */
    def onCyan = if (colorActive) {
      CYAN_B + s + RESET
    } else {
      s
    }

    /** Colorize the given string background to ANSI red */
    def onWhite = if (colorActive) {
      WHITE_B + s + RESET
    } else {
      s
    }

    /** Make the given string bold */
    def bold = if (colorActive) {
      BOLD + s + RESET
    } else {
      s
    }

    /** Underline the given string */
    def underlined = if (colorActive) {
      UNDERLINED + s + RESET
    } else {
      s
    }

    /** Make the given string blink (some terminals may turn this off) */
    def blink = if (colorActive) {
      BLINK + s + RESET
    } else {
      s
    }

    /** Reverse the ANSI colors of the given string */
    def reversed = if (colorActive) {
      REVERSED + s + RESET
    } else {
      s
    }

    /** Make the given string invisible using ANSI color codes */
    def invisible = if (colorActive) {
      INVISIBLE + s + RESET
    } else {
      s
    }
  }

}
