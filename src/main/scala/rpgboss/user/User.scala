package impulsestorm.user

import java.sql._

import org.mindrot.jbcrypt.BCrypt

import org.postgresql.ds.PGPoolingDataSource

object UserNotFound

case class User(username: String, pwhash: String, email: String)

case class UserDBError(val msg: String) extends Exception(msg)

object UserDB {
  // create database connection
  classOf[org.postgresql.Driver]
  
  val source = new PGPoolingDataSource()
  source.setDataSourceName("Impulsestorm User dir")
  source.setServerName("localhost")
  source.setDatabaseName("impulsestorm")
  source.setUser("impulsestorm")
  source.setPassword("ispass")
  source.setMaxConnections(10)
  
  def using[Closeable <: {def close(): Unit}, B]
    (closeable: Closeable)(getB: Closeable => B): B =
  {
    try {
      getB(closeable)
    } finally {
      closeable.close()
    }
  }
  
  val nameRegex = """^[a-z][-a-z0-9_]*$""".r
  val emailRegex = """(?i)\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,4}\b""".r
  
  def isEmailValid(email: String) =
    emailRegex.pattern.matcher(email).matches
  
  val addUserSql = "INSERT INTO users VALUES (?, ?, ?)"
  val selUserSql = "SELECT * FROM users WHERE username = ?"
  val chgPassSql = "UPDATE users SET pwhash = ? WHERE username = ?"
  val chgEmailSql = "UPDATE users SET email = ? WHERE username = ?"
  
  protected def Ex(msg: String) = UserDBError(msg)
  
  protected def selUser(conn: Connection, username: String) : Option[User] = {
    using(conn.prepareStatement(selUserSql)) ( st => {
      st.setString(1, username.toLowerCase)
      using(st.executeQuery()) { rs =>
        if(rs.next())
          Some(User(rs.getString(1), rs.getString(2), rs.getString(3)))
        else
          None
      }
    })
  }
  
  def addUser(username: String, password: String, email: String) = {
    if(username.length < 3)
      throw(Ex("Username must be at least 3 characters."))    
    else if(username.length > 32)
      throw(Ex("Username must be 32 characters or less."))
    else if( !nameRegex.pattern.matcher(username.toLowerCase).matches )
      throw(Ex("Username may contain only lowercase letters, numbers, " +
               "dashes, and underscores. It must begin with a letter."))
    else if( password.length < 6 )
      throw(Ex("Passwords must be at least 6 characters."))
    else if( !isEmailValid(email) )
      throw(Ex("Invalid email."))
    else {
          
      using(source.getConnection()) { conn =>
        
        if(selUser(conn, username).isDefined)
          throw(Ex("Username already registered."))
        else using(conn.prepareStatement(addUserSql)) { st => 
          {
            st.setString(1, username.toLowerCase)
            st.setString(2, BCrypt.hashpw(password, BCrypt.gensalt()))
            st.setString(3, email)
            
            st.executeUpdate() == 1
          }
        }
      }
    }
    
  }
  
  def authenticate(username: String, password: String) : Option[User] =
    using(source.getConnection()) { conn =>
      authenticate(conn, username, password)
    }
  
  protected def authenticate(conn: Connection, username: String, 
                             password: String) : Option[User] = 
    selUser(conn, username) match {
      case Some(dbUser: User) => 
        if(BCrypt.checkpw(password, dbUser.pwhash)) Some(dbUser) else None
      case None => None
    }
  
  def changePassword(username: String, oldPass: String, newPass: String) = {
    using(source.getConnection()) { conn =>
      if(authenticate(conn, username, oldPass).isDefined) {
        using(conn.prepareStatement(chgPassSql))( st => 
        {
          st.setString(1, username.toLowerCase)
          st.setString(2, BCrypt.hashpw(newPass, BCrypt.gensalt()))
          
          st.executeUpdate() == 1
        })
      } else throw Ex("Incorrect old password provided.")
    }
  }
  
  def changeEmail(username: String, oldPass: String, email: String) = {
    using(source.getConnection()) { conn =>
      if(authenticate(conn, username, oldPass).isDefined) {
        using(conn.prepareStatement(chgEmailSql))( st => 
        {
          st.setString(1, username.toLowerCase)
          st.setString(2, email)
          
          st.executeUpdate() == 1
        })
      } else throw Ex("Incorrect old password provided.")
    }
  }
  
}
