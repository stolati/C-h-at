package models.persistance

import play.api.Play.current
import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import se.radley.plugin.salat._

//keep it !!! see salat
import models.persistance.mongoContext._
import com.novus.salat.annotations._

//this is a test on Spock stuff

//one will have elements on a table (with their own id), and iheritance

@Salat abstract class TestAbstractInTable(id : ObjectId = new ObjectId, toto : String = "tutu")
case class TAIT1(id : ObjectId = new ObjectId, toto : String = "tutu") extends TestAbstractInTable(id, toto)
case class TAIT2(id : ObjectId = new ObjectId, toto : String = "tutu") extends TestAbstractInTable(id, toto)
case class TAIT3(id : ObjectId = new ObjectId, toto : String = "tutu") extends TestAbstractInTable(id, toto)
case class TAIT4(id : ObjectId = new ObjectId, toto : String = "tutu") extends TestAbstractInTable(id, toto)


object TestAbstractInTable extends ModelCompanion[TestAbstractInTable, ObjectId] {
  val collection = mongoCollection("test_abstract_in_table")
  val dao = new SalatDAO[TestAbstractInTable, ObjectId](collection = collection) {}

  def getAll() = dao.find(MongoDBObject()).toList
}


//dao.find( ref = MongoDBObject("_id" -> MongaDBObject("$gte" -> 2)))
//   .sort(orderBy = MongoDBObject("_id" -> -1))
//   .skip(1)
//   .limit(1)
//   .toList

//findOneById(id = 4)





//one will have inheritance (same both), but will just be a field into another element
@Salat abstract class TestAbstractInField(toto : String = "tutu")
case class TAIF1(toto : String = "tutu") extends TestAbstractInField(toto)
case class TAIF2(toto : String = "tutu") extends TestAbstractInField(toto)
case class TAIF3(toto : String = "tutu") extends TestAbstractInField(toto)
case class TAIF4(toto : String = "tutu") extends TestAbstractInField(toto)

case class ContainerWithField(id : ObjectId = new ObjectId,
                              toto : String = "titi",
                              ta : TestAbstractInField = TAIF1()
                             )

object ContainerWithField extends ModelCompanion[ContainerWithField, ObjectId] {
  val collection = mongoCollection("test_abstract_with_field")
  val dao = new SalatDAO[ContainerWithField, ObjectId](collection = collection) {}

  def getAll() = dao.find(MongoDBObject()).toList
}



//test 3, object containing others

case class Test3Toto(toto : String = "tutu", x : Double = 0, y : Double = 0)

case class Test3Tutu(toto : Test3Toto = Test3Toto())

case class Test3Global(id : ObjectId = new ObjectId, tutu : Test3Tutu = Test3Tutu())


object Test3Global extends ModelCompanion[Test3Global, ObjectId] {
  val collection = mongoCollection("test_object_in_object")
  val dao = new SalatDAO[Test3Global, ObjectId](collection = collection) {}

  def getAll() = dao.find(MongoDBObject()).toList
}




object TestMe {
  def apply() {

    TestAbstractInTable.save(TAIT1())
    TestAbstractInTable.save(TAIT2())
    TestAbstractInTable.save(TAIT3())
    TestAbstractInTable.save(TAIT4())

    val a = TestAbstractInTable.getAll()
    println(a)


    ContainerWithField.save(ContainerWithField(ta = TAIF1()))
    ContainerWithField.save(ContainerWithField(ta = TAIF2()))
    ContainerWithField.save(ContainerWithField(ta = TAIF3()))
    ContainerWithField.save(ContainerWithField(ta = TAIF4()))


    val b = ContainerWithField.getAll()
    println(b)


    Test3Global.save(Test3Global())
    Test3Global.save(Test3Global(tutu = Test3Tutu(Test3Toto("tralala"))))

    val c = Test3Global.getAll()
    println(c)
  }
}

