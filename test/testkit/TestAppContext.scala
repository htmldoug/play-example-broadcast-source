package testkit

import java.io.File

import com.typesafe.config.ConfigFactory
import play.api.{ApplicationLoader, Configuration, Environment, Mode}
import play.core.DefaultWebCommands
import play.core.server.ServerConfig

object TestAppContext {

  def apply(rootPath: File = new File(".")): ApplicationLoader.Context = {
    val classLoader = ApplicationLoader.getClass.getClassLoader
    val env = new Environment(rootPath, classLoader, Mode.Test)
    ApplicationLoader.createContext(env)
  }

  def apply(config: ServerConfig): ApplicationLoader.Context = {
    ApplicationLoader.Context(
      Environment.simple(path = config.rootDir, mode = config.mode),
      None, new DefaultWebCommands(), Configuration(ConfigFactory.load())
    )
  }
}
