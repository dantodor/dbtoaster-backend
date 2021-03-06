package ddbt.tpcc.loadtest

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
// import java.text.DecimalFormat
// import java.util.Map
import java.util.Properties
// import java.util.Set
// import TpccThread._
import scala.collection.JavaConversions._

import org.slf4j.LoggerFactory
import org.slf4j.Logger

object DatabaseConnector {

  private val dbcLogger = LoggerFactory.getLogger(classOf[Driver])

  def connectToDB(driverClassName:String, jdbcUrl:String, db_user:String, db_password:String): Connection = {
    DatabaseConnector.dbcLogger.info("Connection to database: driver: " + driverClassName + 
      " url: " + 
      jdbcUrl)
    Class.forName(driverClassName)
    val prop = new Properties()
    val connPropFile = new File("conf/jdbc-connection.properties")
    if (connPropFile.exists()) {
      DatabaseConnector.dbcLogger.info("Loading JDBC connection properties from " + connPropFile.getAbsolutePath)
      try {
        val is = new FileInputStream(connPropFile)
        prop.load(is)
        is.close()
        if (DatabaseConnector.dbcLogger.isDebugEnabled) {
          DatabaseConnector.dbcLogger.debug("Connection properties: {")
          val entries = prop.entrySet()
          for (entry <- entries) {
            DatabaseConnector.dbcLogger.debug(entry.getKey + " = " + entry.getValue)
          }
          DatabaseConnector.dbcLogger.debug("}")
        }
      } catch {
        case e: IOException => DatabaseConnector.dbcLogger.error("", e)
      }
    } else {
      DatabaseConnector.dbcLogger.warn(connPropFile.getAbsolutePath + 
        " does not exist! Using default connection properties")
    }
    prop.put("user", db_user)
    prop.put("password", db_password)
    val conn = DriverManager.getConnection(jdbcUrl, prop)
    conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ)
    conn.setAutoCommit(false)
    conn
  }
}