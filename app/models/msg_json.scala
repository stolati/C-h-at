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

case class MapSurfaceVisible(content : Seq[Seq[MapElementVisible]]) extends Msg
case class MapElementVisible(code : String) extends Msg

//Client connection
case class PlayerCredential(username : String, password : String) extends Msg
case class OKPlayerCredential() extends Msg
case class KOPlayerCredential(msg : String) extends Msg
case class GetPlayerList() extends Msg
case class PlayerList(content : List[String]) extends Msg



//client -> server class
case class Me_Move(pos : Position) extends Msg
case class Me_JumpingId(id : Id) extends Msg
case class Ask_Map() extends Msg

//server -> client class
case class Player_Move(id : Id, pos : Position) extends Msg
case class Player_Join(id : Id, pos : Position) extends Msg
case class Player_Quit(id : Id) extends Msg
case class CurrentMap(your_body : Body, others_body : Seq[Body], map : MapSurfaceVisible) extends Msg

case class YouQuit() extends Msg
case class YouJump(url : String) extends Msg

//server -> server
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








//object MSG_JSON {
//
//  trait Msg
//
//
//  case class OnlyForTest(m : Msg) extends Msg
//
//  //put here the message that transit throught the network
//
//  //general class
//  case class Id(id : String = "") extends Msg
//  case class Body(id : Id, map_id : Id, pos : Pos) extends Msg
//
//  case class MapSurfaceVisible(content : Seq[Seq[MapElementVisible]]) extends Msg
//  case class MapElementVisible(code : String) extends Msg
//
//
//  //client -> server class
//  case class Me_Move(pos : Pos) extends Msg
//  case class Me_JumpingId(id : Id) extends Msg
//  case class Ask_Map() extends Msg
//
//  //server -> client class
//  case class Player_Move(id : Id, pos : Pos) extends Msg
//  case class Player_Join(id : Id, pos : Pos) extends Msg
//  case class Player_Quit(id : Id) extends Msg
//  case class CurrentMap(your_body : Body, others_body : Seq[Body], map : MapSurfaceVisible) extends Msg
//
//  case class YouQuit() extends Msg
//  case class YouJump(url : String) extends Msg
//
//  //server -> server
//  case class PlayerJumpingInit(mapName : String, pos : Pos) extends Msg
//  case class PlayerJumpingId(id : Id) extends Msg
//
//  //credential stuffs
//  case class PlayerCredential(username : String, password : String) extends Msg
//  case class OKPlayerCredential() extends Msg
//  case class KOPlayerCredential() extends Msg
//
//
//
//
//  //helper functions
//
//  def fromJson(s : String) : Any = fromJson(play_json.Json.parse(s))
//
//  def fromJson(json : play_json.JsValue) : Any = {
//    val (kind, data) = ( (json \ "kind").as[String], play_json.Json.stringify(json \ "data") )
//    kind match {
//      case "Id" => jerkson.parse[Id](data)
//      case "Pos" => jerkson.parse[Pos](data)
//      case "Body" => jerkson.parse[Body](data)
//      case "MapSurfaceVisible" => jerkson.parse[MapSurfaceVisible](data)
//      case "MapElementVisible" => jerkson.parse[MapElementVisible](data)
//      case "Me_Move" => Me_Move(Pos( (json \ "data" \ "pos" \ "x").as[Double], (json \ "data" \ "pos" \ "y").as[Double]) ) //jerkson.parse[Me_Move](data)
//      case "Me_JumpingId" => jerkson.parse[Me_JumpingId](data)
//      case "Ask_Map" => Ask_Map() //jerkson.parse[Ask_Map](data)
//      case "Player_Move" => jerkson.parse[Player_Move](data)
//      case "Player_Join" => jerkson.parse[Player_Join](data)
//      case "Player_Quit" => jerkson.parse[Player_Quit](data)
//      case "CurrentMap" => jerkson.parse[CurrentMap](data)
//      case "YouQuit" => YouQuit() //jerkson.parse[YouQuit](data)
//      case "YouJump" => jerkson.parse[YouJump](data)
//      case "PlayerJumpingInit" => PlayerJumpingInit( (json \ "data" \ "mapName").as[String], Pos( (json \ "data" \ "pos" \ "x").as[Double], (json \ "data" \ "pos" \ "y").as[Double]))  //jerkson.parse[PlayerJumpingInit](data)
//      case "PlayerJumpingId" => PlayerJumpingId(Id( (json \ "data" \ "id" \ "id").as[String] ))  //jerkson.parse[PlayerJumpingId](data)
//      case "PlayerCredential" => PlayerCredential( (json \ "data" \ "username").as[String], (json \ "data" \ "password").as[String])
//      case "OKPlayerCredential" => OKPlayerCredential()
//      case "KOPlayerCredential" => KOPlayerCredential()
//    }
//  }
//
//  def toJsonPlay(o : Any) = play_json.Json.parse(toJson(o))
//
//  def toJson(o: Any) = {
//    play_json.Json.stringify(play_json.JsObject(Seq(
//      "kind" -> play_json.JsString(getClassName(o)),
//      "data" -> play_json.Json.parse(jerkson.generate(o))
//    )))
//  }
//
//  def getClassName(o : Any) = o match {
//    case e : Id => "Id"
//    case e : Pos => "Pos"
//    case e : Body => "Body"
//    case e : MapSurfaceVisible => "MapSurfaceVisible"
//    case e : MapElementVisible => "MapElementVisible"
//    case e : Me_Move => "Me_Move"
//    case e : Me_JumpingId => "Me_JumpingId"
//    case e : Ask_Map => "Ask_Map"
//    case e : Player_Move => "Player_Move"
//    case e : Player_Join => "Player_Join"
//    case e : Player_Quit => "Player_Quit"
//    case e : CurrentMap => "CurrentMap"
//    case e : YouQuit => "YouQuit"
//    case e : YouJump => "YouJump"
//    case e : PlayerJumpingInit => "PlayerJumpingInit"
//    case e : PlayerJumpingId => "PlayerJumpingId"
//    case e : PlayerCredential =>"PlayerCredential"
//    case e : OKPlayerCredential => "OKPlayerCredential"
//    case e : KOPlayerCredential => "KOPlayerCredential"
//  }
//
//}









