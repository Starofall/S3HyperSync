package io.github.starofall.s3hypersync

import ch.qos.logback.classic.{Level, LoggerContext}
import org.slf4j.LoggerFactory

import scala.language.implicitConversions


object SyncLogging {

  var colorActive: Boolean = true

  def initLogger(conf: JobDefinition): Unit = {
    colorActive = !conf.noColor.getOrElse(false)
    setRootLogLevel(conf.verbose.getOrElse(0))
  }

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

    /** Make the given string bold */
    def bold = if (colorActive) {
      BOLD + s + RESET
    } else {
      s
    }
  }

}
