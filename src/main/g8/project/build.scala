import jodd.io.FileUtil
import sbt.Keys.TaskStreams
import sbt._

import scala.collection.mutable.ListBuffer
import scala.io.Source
import scala.sys.process.{ProcessIO, stringSeqToProcess}


object KotlinGradleSettings {
  val kotlinCompile = TaskKey[Unit]("kotlin-compile",
    "runs kotlin compilation, occurs before normal compilation")

  val kotlinClean = TaskKey[Unit]("kotlin-clean",
    "runs kotlin clean, occurs before normal clean")
}

object KotlinGradleComplie {

  val RESET = "\033[0m"
  val BLUE = "\033[34m"
  val GREEN = "\033[32m"
  val RED = "\033[31m"
  val YELLOW = "\033[33m"

  def Build(): Unit = {
    FileUtil.touch("play.gradle")
    println("====> call gradle ")
  }

  def TouchAppConf() = {
    // println("=============> Touch conf/.needReload")
    FileUtil.touch("conf/.needReload")
  }

  def BuildByGradle(s: TaskStreams) = {
    s.log.info("Build kotlin source code by gradle")
    val cmd = stringSeqToProcess(Seq("gradle", "--daemon", "-Pkotlin.incremental=true", "build"))

    def LogStdout(input: java.io.InputStream) = {
      val out = Source.fromInputStream(input, "UTF-8")
      out.getLines().foreach(li => {
        if (li.startsWith("w:")) {
          s.log.warn(WraperText(li, YELLOW))
        } else {
          if (li.contains("BUILD SUCCESSFUL")) {
            s.log.info(HighLight(li, "BUILD SUCCESSFUL", GREEN))
          } else {
            s.log.info(HighLight(li, "UP-TO-DATE", BLUE))
          }
        }
        if (li.endsWith(":build")) {
          TouchAppConf()
        }
      })
    }

    def WraperText(message: String, ansiColor: String): String = {
      return s"${ansiColor}${message}${RESET}"
    }

    def HighLight(msg: String, keyWords: String, ansiColor: String) = {
      val highLightStr = WraperText(keyWords, ansiColor)
      msg.replaceAll(keyWords, highLightStr)
    }

    val erroutLines = new ListBuffer[String]()

    def LogErrout(input: java.io.InputStream) = {
      val out = Source.fromInputStream(input, "UTF-8")
      out.getLines().foreach(li => {
        val fmtLine = li.replaceFirst(""".kt: \(""", ".kt (").replaceFirst(""", """, " ")
        erroutLines += fmtLine
        s.log.error(fmtLine)
      })
    }

    val p = cmd.run(new ProcessIO(_.close(), LogStdout, LogErrout))
    val exitCode = p.exitValue()

    exitCode match {
      case code if code == 0 => s.log.info("Compile kotlin source code by gradle succeed")
      case _ => {
        val errMsg = erroutLines.mkString("\n")
        throw new MessageOnlyException(s"Compilation failed. See log for more details\n\${errMsg}")
      }
    }

  }

  def CleanByGradle(s: TaskStreams) = {
    s.log.info("Clean for kotlin source code by gradle")
    val cmd = stringSeqToProcess(Seq("gradle", "--daemon", "clean"))

    def LogStdout(input: java.io.InputStream) = {
      val out = Source.fromInputStream(input, "UTF-8")
      out.getLines().foreach(li => println("[stdout] " + li))
    }

    def LogErrout(input: java.io.InputStream) = {
      val out = Source.fromInputStream(input, "UTF-8")
      out.getLines().foreach(li => println("[err] " + li))
    }

    val p = cmd.run(new ProcessIO(_.close(), LogStdout, LogErrout))
    val exitCode = p.exitValue()

    exitCode match {
      case code if code == 0 => s.log.info("Clean for kotlin source code by gradle succeed")
      case _ => throw new MessageOnlyException("Clean for kotlin source code failed. See log for more details")
    }
  }
}
