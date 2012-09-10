package models.msg_json

import com.avaje.ebeaninternal.server.lib.thread.ThreadPool
import org.reflections.vfs.Vfs.File

//keep it !!! see salat
import models.persistance.mongoContext._

trait Msg //each msg should inherite this trait

case class OnlyForTest(m : Msg) extends Msg

//put here the message that transit throught the network

//general class
case class Id(id : String = "") extends Msg
case class Body(id : Id, map_id : Id, pos : Position) extends Msg
case class Position(x : Double, y : Double) extends Msg

//Client connection
case class PlayerCredential(username : String, password : String) extends Msg
case class OKPlayerCredential() extends Msg
case class KOPlayerCredential(msg : String) extends Msg
case class GetPlayerList() extends Msg
case class PlayerList(content : List[String]) extends Msg
//TODO case class CreatePlayer(username : String, password : String) extends Msg
//TODO case class PlayerCreated() extends Msg
//TODO case class PlayerNotCreated(msg : String) extends Msg

//Map Info
case class CurrentMap(your_body : Body, others_body : Seq[Body], map : MapSurfaceVisible) extends Msg

case class MapSurfaceVisible(content : Seq[Seq[MapElementVisible]]) extends Msg
case class MapElementVisible(code : String) extends Msg

case class YouQuit() extends Msg

//Moving Info
case class Me_Move(pos : Position) extends Msg //the player sending "I am moving" info
case class Player_Status(id : Id, pos : Position) extends Msg
case class Player_Join(id : Id, pos : Position) extends Msg
case class Player_Quit(id : Id) extends Msg

case class Me_JumpingId(id : Id) extends Msg

//Jumping stuff
case class YouJump(url : String) extends Msg
case class PlayerJumpingInit(mapName : String, pos : Position) extends Msg
case class PlayerJumpingId(id : Id) extends Msg





object Transform {
  //for more info (and maybe less speak stuff) : http://www.scalafied.com/105/default-and-customized-lift-json-type-hints

  case class ComplexTypeHints(packageName : String, className : String) extends net.liftweb.json.TypeHints {

    import org.clapper.classutil.ClassFinder
    import collection.JavaConversions._

    val hints: List[Class[_]] = {

      //manual part, from http://dzone.com/snippets/get-all-classes-within-package
      val classLoader = Thread.currentThread().getContextClassLoader()
      val path = packageName.replace('.', '/')
      val ressources = classLoader.getResources(path)
      var dirs = ressources.map(r => new java.io.File(r.getFile()) )

      //lib part, use http://software.clapper.org/classutil/
      val allClasses = ClassFinder(dirs.toSeq).getClasses
      val filterClasses = ClassFinder.concreteSubclasses(className, allClasses)
      //filter on isConcrete class stuff
      filterClasses.filter{ _.isConcrete }.map{ k => Class.forName(k.name) }.toList
    }

    //from FullTypeHints
    //def hintFor(clazz: Class[_]) = clazz.getName
    //def classFor(hint: String) = Some(Thread.currentThread.getContextClassLoader.loadClass(hint))

    //from ShortTypeHints
    def hintFor(clazz: Class[_]) = clazz.getName.substring(clazz.getName.lastIndexOf(".")+1)
    def classFor(hint: String) = hints find (hintFor(_) == hint)
  }

  import net.liftweb.json._
  import net.liftweb.json.Serialization._

  implicit val formats = new Formats {
    val dateFormat = DefaultFormats.lossless.dateFormat
    override val typeHints = ComplexTypeHints("models.msg_json", "models.msg_json.Msg")
    override val typeHintFieldName = "_t"
  }

  def toJson(o : Any) : String = o match {
    case e : Msg => write(e)
    case _ => throw new Exception("Object not Msg given to 'toJson' : " + o.toString)
  }

  def fromJson(s : String) : Msg = read[Msg](s)

}


object MsgJsonTest {

  def testWith(o : Msg){
    val asJsonStr : String = Transform.toJson(o)
    println(asJsonStr)
    val asObject : Msg = Transform.fromJson(asJsonStr)
    println(asObject)
    println(asObject == o)
  }

  def testAll(){
    testWith(YouQuit())
    testWith(YouJump("toto"))
    testWith(PlayerJumpingId(Id("134_id")))
    testWith(OnlyForTest(Id("134_id")))
    testWith(OnlyForTest(PlayerJumpingId(Id("134_id"))))
  }

}

