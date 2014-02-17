package oltp.opt.lifters

import org.dbtoaster.dbtoasterlib.K3Collection._
import scala.collection.mutable.Map
import xml._
import scala.virtualization.lms.common._
import scala.virtualization.lms.internal.{GenericNestedCodegen, GenerationFailedException, Effects}
import scala.reflect.SourceContext
import scala.language.implicitConversions

import ddbt.lib.store._

trait SEntryOps extends Base{
  class SEntryOpsCls[E<:Entry:Manifest](x: Rep[E]) {
    def update(i: Int, v:Rep[Any]) = steUpdate[E](x , i ,v)
    def increase(i: Int, v:Rep[Any]) = steIncrease[E](x , i ,v)
    def +=(i: Int, v:Rep[Any]) = increase(i ,v)
    def decrease(i: Int, v:Rep[Any]) = steDecrease[E](x , i ,v)
    def -=(i: Int, v:Rep[Any]) = decrease(i ,v)
    def get(i: Int) = steGet[E](x , i)
  }

  implicit def conv2SEntry2[T1:Manifest,T2:Manifest](x: Rep[SEntry2[T1,T2]])(implicit pos: SourceContext) =
    (x.get(1).asInstanceOf[Rep[T1]],x.get(2).asInstanceOf[Rep[T2]])
  implicit def conv2SEntry3[T1:Manifest,T2:Manifest,T3:Manifest](x: Rep[SEntry3[T1,T2,T3]])(implicit pos: SourceContext) =
    (x.get(1).asInstanceOf[Rep[T1]],x.get(2).asInstanceOf[Rep[T2]],x.get(3).asInstanceOf[Rep[T3]])
  implicit def conv2SEntry4[T1:Manifest,T2:Manifest,T3:Manifest,T4:Manifest](x: Rep[SEntry4[T1,T2,T3,T4]])(implicit pos: SourceContext) =
    (x.get(1).asInstanceOf[Rep[T1]],x.get(2).asInstanceOf[Rep[T2]],x.get(3).asInstanceOf[Rep[T3]],x.get(4).asInstanceOf[Rep[T4]])
  implicit def conv2SEntry5[T1:Manifest,T2:Manifest,T3:Manifest,T4:Manifest,T5:Manifest](x: Rep[SEntry5[T1,T2,T3,T4,T5]])(implicit pos: SourceContext) =
    (x.get(1).asInstanceOf[Rep[T1]],x.get(2).asInstanceOf[Rep[T2]],x.get(3).asInstanceOf[Rep[T3]],x.get(4).asInstanceOf[Rep[T4]],x.get(5).asInstanceOf[Rep[T5]])
  implicit def conv2SEntry6[T1:Manifest,T2:Manifest,T3:Manifest,T4:Manifest,T5:Manifest,T6:Manifest](x: Rep[SEntry6[T1,T2,T3,T4,T5,T6]])(implicit pos: SourceContext) =
    (x.get(1).asInstanceOf[Rep[T1]],x.get(2).asInstanceOf[Rep[T2]],x.get(3).asInstanceOf[Rep[T3]],x.get(4).asInstanceOf[Rep[T4]],x.get(5).asInstanceOf[Rep[T5]],x.get(6).asInstanceOf[Rep[T6]])
  implicit def conv2SEntry7[T1:Manifest,T2:Manifest,T3:Manifest,T4:Manifest,T5:Manifest,T6:Manifest,T7:Manifest](x: Rep[SEntry7[T1,T2,T3,T4,T5,T6,T7]])(implicit pos: SourceContext) =
    (x.get(1).asInstanceOf[Rep[T1]],x.get(2).asInstanceOf[Rep[T2]],x.get(3).asInstanceOf[Rep[T3]],x.get(4).asInstanceOf[Rep[T4]],x.get(5).asInstanceOf[Rep[T5]],x.get(6).asInstanceOf[Rep[T6]],x.get(7).asInstanceOf[Rep[T7]])
  implicit def conv2SEntry8[T1:Manifest,T2:Manifest,T3:Manifest,T4:Manifest,T5:Manifest,T6:Manifest,T7:Manifest,T8:Manifest](x: Rep[SEntry8[T1,T2,T3,T4,T5,T6,T7,T8]])(implicit pos: SourceContext) =
    (x.get(1).asInstanceOf[Rep[T1]],x.get(2).asInstanceOf[Rep[T2]],x.get(3).asInstanceOf[Rep[T3]],x.get(4).asInstanceOf[Rep[T4]],x.get(5).asInstanceOf[Rep[T5]],x.get(6).asInstanceOf[Rep[T6]],x.get(7).asInstanceOf[Rep[T7]],x.get(8).asInstanceOf[Rep[T8]])
  implicit def conv2SEntry9[T1:Manifest,T2:Manifest,T3:Manifest,T4:Manifest,T5:Manifest,T6:Manifest,T7:Manifest,T8:Manifest,T9:Manifest](x: Rep[SEntry9[T1,T2,T3,T4,T5,T6,T7,T8,T9]])(implicit pos: SourceContext) =
    (x.get(1).asInstanceOf[Rep[T1]],x.get(2).asInstanceOf[Rep[T2]],x.get(3).asInstanceOf[Rep[T3]],x.get(4).asInstanceOf[Rep[T4]],x.get(5).asInstanceOf[Rep[T5]],x.get(6).asInstanceOf[Rep[T6]],x.get(7).asInstanceOf[Rep[T7]],x.get(8).asInstanceOf[Rep[T8]],x.get(9).asInstanceOf[Rep[T9]])
  implicit def conv2SEntry10[T1:Manifest,T2:Manifest,T3:Manifest,T4:Manifest,T5:Manifest,T6:Manifest,T7:Manifest,T8:Manifest,T9:Manifest,T10:Manifest](x: Rep[SEntry10[T1,T2,T3,T4,T5,T6,T7,T8,T9,T10]])(implicit pos: SourceContext) =
    (x.get(1).asInstanceOf[Rep[T1]],x.get(2).asInstanceOf[Rep[T2]],x.get(3).asInstanceOf[Rep[T3]],x.get(4).asInstanceOf[Rep[T4]],x.get(5).asInstanceOf[Rep[T5]],x.get(6).asInstanceOf[Rep[T6]],x.get(7).asInstanceOf[Rep[T7]],x.get(8).asInstanceOf[Rep[T8]],x.get(9).asInstanceOf[Rep[T9]],x.get(10).asInstanceOf[Rep[T10]])
  implicit def conv2SEntry11[T1:Manifest,T2:Manifest,T3:Manifest,T4:Manifest,T5:Manifest,T6:Manifest,T7:Manifest,T8:Manifest,T9:Manifest,T10:Manifest,T11:Manifest](x: Rep[SEntry11[T1,T2,T3,T4,T5,T6,T7,T8,T9,T10,T11]])(implicit pos: SourceContext) =
    (x.get(1).asInstanceOf[Rep[T1]],x.get(2).asInstanceOf[Rep[T2]],x.get(3).asInstanceOf[Rep[T3]],x.get(4).asInstanceOf[Rep[T4]],x.get(5).asInstanceOf[Rep[T5]],x.get(6).asInstanceOf[Rep[T6]],x.get(7).asInstanceOf[Rep[T7]],x.get(8).asInstanceOf[Rep[T8]],x.get(9).asInstanceOf[Rep[T9]],x.get(10).asInstanceOf[Rep[T10]],x.get(11).asInstanceOf[Rep[T11]])
  implicit def conv2SEntry12[T1:Manifest,T2:Manifest,T3:Manifest,T4:Manifest,T5:Manifest,T6:Manifest,T7:Manifest,T8:Manifest,T9:Manifest,T10:Manifest,T11:Manifest,T12:Manifest](x: Rep[SEntry12[T1,T2,T3,T4,T5,T6,T7,T8,T9,T10,T11,T12]])(implicit pos: SourceContext) =
    (x.get(1).asInstanceOf[Rep[T1]],x.get(2).asInstanceOf[Rep[T2]],x.get(3).asInstanceOf[Rep[T3]],x.get(4).asInstanceOf[Rep[T4]],x.get(5).asInstanceOf[Rep[T5]],x.get(6).asInstanceOf[Rep[T6]],x.get(7).asInstanceOf[Rep[T7]],x.get(8).asInstanceOf[Rep[T8]],x.get(9).asInstanceOf[Rep[T9]],x.get(10).asInstanceOf[Rep[T10]],x.get(11).asInstanceOf[Rep[T11]],x.get(12).asInstanceOf[Rep[T12]])
  implicit def conv2SEntry13[T1:Manifest,T2:Manifest,T3:Manifest,T4:Manifest,T5:Manifest,T6:Manifest,T7:Manifest,T8:Manifest,T9:Manifest,T10:Manifest,T11:Manifest,T12:Manifest,T13:Manifest](x: Rep[SEntry13[T1,T2,T3,T4,T5,T6,T7,T8,T9,T10,T11,T12,T13]])(implicit pos: SourceContext) =
    (x.get(1).asInstanceOf[Rep[T1]],x.get(2).asInstanceOf[Rep[T2]],x.get(3).asInstanceOf[Rep[T3]],x.get(4).asInstanceOf[Rep[T4]],x.get(5).asInstanceOf[Rep[T5]],x.get(6).asInstanceOf[Rep[T6]],x.get(7).asInstanceOf[Rep[T7]],x.get(8).asInstanceOf[Rep[T8]],x.get(9).asInstanceOf[Rep[T9]],x.get(10).asInstanceOf[Rep[T10]],x.get(11).asInstanceOf[Rep[T11]],x.get(12).asInstanceOf[Rep[T12]],x.get(13).asInstanceOf[Rep[T13]])
  implicit def conv2SEntry14[T1:Manifest,T2:Manifest,T3:Manifest,T4:Manifest,T5:Manifest,T6:Manifest,T7:Manifest,T8:Manifest,T9:Manifest,T10:Manifest,T11:Manifest,T12:Manifest,T13:Manifest,T14:Manifest](x: Rep[SEntry14[T1,T2,T3,T4,T5,T6,T7,T8,T9,T10,T11,T12,T13,T14]])(implicit pos: SourceContext) =
    (x.get(1).asInstanceOf[Rep[T1]],x.get(2).asInstanceOf[Rep[T2]],x.get(3).asInstanceOf[Rep[T3]],x.get(4).asInstanceOf[Rep[T4]],x.get(5).asInstanceOf[Rep[T5]],x.get(6).asInstanceOf[Rep[T6]],x.get(7).asInstanceOf[Rep[T7]],x.get(8).asInstanceOf[Rep[T8]],x.get(9).asInstanceOf[Rep[T9]],x.get(10).asInstanceOf[Rep[T10]],x.get(11).asInstanceOf[Rep[T11]],x.get(12).asInstanceOf[Rep[T12]],x.get(13).asInstanceOf[Rep[T13]],x.get(14).asInstanceOf[Rep[T14]])
  implicit def conv2SEntry15[T1:Manifest,T2:Manifest,T3:Manifest,T4:Manifest,T5:Manifest,T6:Manifest,T7:Manifest,T8:Manifest,T9:Manifest,T10:Manifest,T11:Manifest,T12:Manifest,T13:Manifest,T14:Manifest,T15:Manifest](x: Rep[SEntry15[T1,T2,T3,T4,T5,T6,T7,T8,T9,T10,T11,T12,T13,T14,T15]])(implicit pos: SourceContext) =
    (x.get(1).asInstanceOf[Rep[T1]],x.get(2).asInstanceOf[Rep[T2]],x.get(3).asInstanceOf[Rep[T3]],x.get(4).asInstanceOf[Rep[T4]],x.get(5).asInstanceOf[Rep[T5]],x.get(6).asInstanceOf[Rep[T6]],x.get(7).asInstanceOf[Rep[T7]],x.get(8).asInstanceOf[Rep[T8]],x.get(9).asInstanceOf[Rep[T9]],x.get(10).asInstanceOf[Rep[T10]],x.get(11).asInstanceOf[Rep[T11]],x.get(12).asInstanceOf[Rep[T12]],x.get(13).asInstanceOf[Rep[T13]],x.get(14).asInstanceOf[Rep[T14]],x.get(15).asInstanceOf[Rep[T15]])
  implicit def conv2SEntry16[T1:Manifest,T2:Manifest,T3:Manifest,T4:Manifest,T5:Manifest,T6:Manifest,T7:Manifest,T8:Manifest,T9:Manifest,T10:Manifest,T11:Manifest,T12:Manifest,T13:Manifest,T14:Manifest,T15:Manifest,T16:Manifest](x: Rep[SEntry16[T1,T2,T3,T4,T5,T6,T7,T8,T9,T10,T11,T12,T13,T14,T15,T16]])(implicit pos: SourceContext) =
    (x.get(1).asInstanceOf[Rep[T1]],x.get(2).asInstanceOf[Rep[T2]],x.get(3).asInstanceOf[Rep[T3]],x.get(4).asInstanceOf[Rep[T4]],x.get(5).asInstanceOf[Rep[T5]],x.get(6).asInstanceOf[Rep[T6]],x.get(7).asInstanceOf[Rep[T7]],x.get(8).asInstanceOf[Rep[T8]],x.get(9).asInstanceOf[Rep[T9]],x.get(10).asInstanceOf[Rep[T10]],x.get(11).asInstanceOf[Rep[T11]],x.get(12).asInstanceOf[Rep[T12]],x.get(13).asInstanceOf[Rep[T13]],x.get(14).asInstanceOf[Rep[T14]],x.get(15).asInstanceOf[Rep[T15]],x.get(16).asInstanceOf[Rep[T16]])
  implicit def conv2SEntry17[T1:Manifest,T2:Manifest,T3:Manifest,T4:Manifest,T5:Manifest,T6:Manifest,T7:Manifest,T8:Manifest,T9:Manifest,T10:Manifest,T11:Manifest,T12:Manifest,T13:Manifest,T14:Manifest,T15:Manifest,T16:Manifest,T17:Manifest](x: Rep[SEntry17[T1,T2,T3,T4,T5,T6,T7,T8,T9,T10,T11,T12,T13,T14,T15,T16,T17]])(implicit pos: SourceContext) =
    (x.get(1).asInstanceOf[Rep[T1]],x.get(2).asInstanceOf[Rep[T2]],x.get(3).asInstanceOf[Rep[T3]],x.get(4).asInstanceOf[Rep[T4]],x.get(5).asInstanceOf[Rep[T5]],x.get(6).asInstanceOf[Rep[T6]],x.get(7).asInstanceOf[Rep[T7]],x.get(8).asInstanceOf[Rep[T8]],x.get(9).asInstanceOf[Rep[T9]],x.get(10).asInstanceOf[Rep[T10]],x.get(11).asInstanceOf[Rep[T11]],x.get(12).asInstanceOf[Rep[T12]],x.get(13).asInstanceOf[Rep[T13]],x.get(14).asInstanceOf[Rep[T14]],x.get(15).asInstanceOf[Rep[T15]],x.get(16).asInstanceOf[Rep[T16]],x.get(17).asInstanceOf[Rep[T17]])
  implicit def conv2SEntry18[T1:Manifest,T2:Manifest,T3:Manifest,T4:Manifest,T5:Manifest,T6:Manifest,T7:Manifest,T8:Manifest,T9:Manifest,T10:Manifest,T11:Manifest,T12:Manifest,T13:Manifest,T14:Manifest,T15:Manifest,T16:Manifest,T17:Manifest,T18:Manifest](x: Rep[SEntry18[T1,T2,T3,T4,T5,T6,T7,T8,T9,T10,T11,T12,T13,T14,T15,T16,T17,T18]])(implicit pos: SourceContext) =
    (x.get(1).asInstanceOf[Rep[T1]],x.get(2).asInstanceOf[Rep[T2]],x.get(3).asInstanceOf[Rep[T3]],x.get(4).asInstanceOf[Rep[T4]],x.get(5).asInstanceOf[Rep[T5]],x.get(6).asInstanceOf[Rep[T6]],x.get(7).asInstanceOf[Rep[T7]],x.get(8).asInstanceOf[Rep[T8]],x.get(9).asInstanceOf[Rep[T9]],x.get(10).asInstanceOf[Rep[T10]],x.get(11).asInstanceOf[Rep[T11]],x.get(12).asInstanceOf[Rep[T12]],x.get(13).asInstanceOf[Rep[T13]],x.get(14).asInstanceOf[Rep[T14]],x.get(15).asInstanceOf[Rep[T15]],x.get(16).asInstanceOf[Rep[T16]],x.get(17).asInstanceOf[Rep[T17]],x.get(18).asInstanceOf[Rep[T18]])
  implicit def conv2SEntry19[T1:Manifest,T2:Manifest,T3:Manifest,T4:Manifest,T5:Manifest,T6:Manifest,T7:Manifest,T8:Manifest,T9:Manifest,T10:Manifest,T11:Manifest,T12:Manifest,T13:Manifest,T14:Manifest,T15:Manifest,T16:Manifest,T17:Manifest,T18:Manifest,T19:Manifest](x: Rep[SEntry19[T1,T2,T3,T4,T5,T6,T7,T8,T9,T10,T11,T12,T13,T14,T15,T16,T17,T18,T19]])(implicit pos: SourceContext) =
    (x.get(1).asInstanceOf[Rep[T1]],x.get(2).asInstanceOf[Rep[T2]],x.get(3).asInstanceOf[Rep[T3]],x.get(4).asInstanceOf[Rep[T4]],x.get(5).asInstanceOf[Rep[T5]],x.get(6).asInstanceOf[Rep[T6]],x.get(7).asInstanceOf[Rep[T7]],x.get(8).asInstanceOf[Rep[T8]],x.get(9).asInstanceOf[Rep[T9]],x.get(10).asInstanceOf[Rep[T10]],x.get(11).asInstanceOf[Rep[T11]],x.get(12).asInstanceOf[Rep[T12]],x.get(13).asInstanceOf[Rep[T13]],x.get(14).asInstanceOf[Rep[T14]],x.get(15).asInstanceOf[Rep[T15]],x.get(16).asInstanceOf[Rep[T16]],x.get(17).asInstanceOf[Rep[T17]],x.get(18).asInstanceOf[Rep[T18]],x.get(19).asInstanceOf[Rep[T19]])
  implicit def conv2SEntry20[T1:Manifest,T2:Manifest,T3:Manifest,T4:Manifest,T5:Manifest,T6:Manifest,T7:Manifest,T8:Manifest,T9:Manifest,T10:Manifest,T11:Manifest,T12:Manifest,T13:Manifest,T14:Manifest,T15:Manifest,T16:Manifest,T17:Manifest,T18:Manifest,T19:Manifest,T20:Manifest](x: Rep[SEntry20[T1,T2,T3,T4,T5,T6,T7,T8,T9,T10,T11,T12,T13,T14,T15,T16,T17,T18,T19,T20]])(implicit pos: SourceContext) =
    (x.get(1).asInstanceOf[Rep[T1]],x.get(2).asInstanceOf[Rep[T2]],x.get(3).asInstanceOf[Rep[T3]],x.get(4).asInstanceOf[Rep[T4]],x.get(5).asInstanceOf[Rep[T5]],x.get(6).asInstanceOf[Rep[T6]],x.get(7).asInstanceOf[Rep[T7]],x.get(8).asInstanceOf[Rep[T8]],x.get(9).asInstanceOf[Rep[T9]],x.get(10).asInstanceOf[Rep[T10]],x.get(11).asInstanceOf[Rep[T11]],x.get(12).asInstanceOf[Rep[T12]],x.get(13).asInstanceOf[Rep[T13]],x.get(14).asInstanceOf[Rep[T14]],x.get(15).asInstanceOf[Rep[T15]],x.get(16).asInstanceOf[Rep[T16]],x.get(17).asInstanceOf[Rep[T17]],x.get(18).asInstanceOf[Rep[T18]],x.get(19).asInstanceOf[Rep[T19]],x.get(20).asInstanceOf[Rep[T20]])
  implicit def conv2SEntry21[T1:Manifest,T2:Manifest,T3:Manifest,T4:Manifest,T5:Manifest,T6:Manifest,T7:Manifest,T8:Manifest,T9:Manifest,T10:Manifest,T11:Manifest,T12:Manifest,T13:Manifest,T14:Manifest,T15:Manifest,T16:Manifest,T17:Manifest,T18:Manifest,T19:Manifest,T20:Manifest,T21:Manifest](x: Rep[SEntry21[T1,T2,T3,T4,T5,T6,T7,T8,T9,T10,T11,T12,T13,T14,T15,T16,T17,T18,T19,T20,T21]])(implicit pos: SourceContext) =
    (x.get(1).asInstanceOf[Rep[T1]],x.get(2).asInstanceOf[Rep[T2]],x.get(3).asInstanceOf[Rep[T3]],x.get(4).asInstanceOf[Rep[T4]],x.get(5).asInstanceOf[Rep[T5]],x.get(6).asInstanceOf[Rep[T6]],x.get(7).asInstanceOf[Rep[T7]],x.get(8).asInstanceOf[Rep[T8]],x.get(9).asInstanceOf[Rep[T9]],x.get(10).asInstanceOf[Rep[T10]],x.get(11).asInstanceOf[Rep[T11]],x.get(12).asInstanceOf[Rep[T12]],x.get(13).asInstanceOf[Rep[T13]],x.get(14).asInstanceOf[Rep[T14]],x.get(15).asInstanceOf[Rep[T15]],x.get(16).asInstanceOf[Rep[T16]],x.get(17).asInstanceOf[Rep[T17]],x.get(18).asInstanceOf[Rep[T18]],x.get(19).asInstanceOf[Rep[T19]],x.get(20).asInstanceOf[Rep[T20]],x.get(21).asInstanceOf[Rep[T21]])
  implicit def conv2SEntry22[T1:Manifest,T2:Manifest,T3:Manifest,T4:Manifest,T5:Manifest,T6:Manifest,T7:Manifest,T8:Manifest,T9:Manifest,T10:Manifest,T11:Manifest,T12:Manifest,T13:Manifest,T14:Manifest,T15:Manifest,T16:Manifest,T17:Manifest,T18:Manifest,T19:Manifest,T20:Manifest,T21:Manifest,T22:Manifest](x: Rep[SEntry22[T1,T2,T3,T4,T5,T6,T7,T8,T9,T10,T11,T12,T13,T14,T15,T16,T17,T18,T19,T20,T21,T22]])(implicit pos: SourceContext) =
    (x.get(1).asInstanceOf[Rep[T1]],x.get(2).asInstanceOf[Rep[T2]],x.get(3).asInstanceOf[Rep[T3]],x.get(4).asInstanceOf[Rep[T4]],x.get(5).asInstanceOf[Rep[T5]],x.get(6).asInstanceOf[Rep[T6]],x.get(7).asInstanceOf[Rep[T7]],x.get(8).asInstanceOf[Rep[T8]],x.get(9).asInstanceOf[Rep[T9]],x.get(10).asInstanceOf[Rep[T10]],x.get(11).asInstanceOf[Rep[T11]],x.get(12).asInstanceOf[Rep[T12]],x.get(13).asInstanceOf[Rep[T13]],x.get(14).asInstanceOf[Rep[T14]],x.get(15).asInstanceOf[Rep[T15]],x.get(16).asInstanceOf[Rep[T16]],x.get(17).asInstanceOf[Rep[T17]],x.get(18).asInstanceOf[Rep[T18]],x.get(19).asInstanceOf[Rep[T19]],x.get(20).asInstanceOf[Rep[T20]],x.get(21).asInstanceOf[Rep[T21]],x.get(22).asInstanceOf[Rep[T22]])

  implicit def storeEntry2SEntryOpsCls[E<:Entry:Manifest](x: Rep[E]): SEntryOpsCls[E] = new SEntryOpsCls[E](x)
  def newEntry[E<:Entry:Manifest](args:Rep[Any]*):Rep[E]
  def sampleEntry[E<:Entry:Manifest](args:(Int,Rep[Any])*):Rep[E]
  def sampleFullEntry[E<:Entry:Manifest](args:Rep[Any]*):Rep[E]
  def steMakeMutable[E<:Entry:Manifest](x: Rep[E]):Rep[E]

  def steUpdate[E<:Entry:Manifest](x: Rep[E], i: Int, v: Rep[Any]):Rep[Unit]
  def steIncrease[E<:Entry:Manifest](x: Rep[E], i: Int, v: Rep[Any]):Rep[Unit]
  def steDecrease[E<:Entry:Manifest](x: Rep[E], i: Int, v: Rep[Any]):Rep[Unit]
  def steGet[E<:Entry:Manifest](x: Rep[E], i: Int):Rep[Any]

  def checkOrInsertEntryClass[E](m:Manifest[E]):Unit
}

trait SEntryExp extends StoreOps with BaseExp with EffectExp with VariablesExp {

  case class SteNewSEntry   [E<:Entry:Manifest](mE: Manifest[E], args:Seq[Rep[Any]]) extends Def[E]
  case class SteSampleSEntry[E<:Entry:Manifest](mE: Manifest[E], args:Seq[(Int,Rep[Any])]) extends Def[E]

  case class SteMakeMutable [E<:Entry:Manifest](x: Exp[E]) extends Def[E]
  case class SteUpdate      [E<:Entry:Manifest](x: Exp[E], i: Int, v: Exp[Any]) extends Def[Unit]
  case class SteIncrease    [E<:Entry:Manifest](x: Exp[E], i: Int, v: Exp[Any]) extends Def[Unit]
  case class SteDecrease    [E<:Entry:Manifest](x: Exp[E], i: Int, v: Exp[Any]) extends Def[Unit]
  case class SteGet         [E<:Entry:Manifest](x: Exp[E], i: Int) extends Def[Any]

  def newEntry       [E<:Entry:Manifest](args:Rep[Any]*):Exp[E] = { checkOrInsertEntryClass[E](manifest[E]); reflectMutable(SteNewSEntry[E](manifest[E], args)) }
  def sampleEntry    [E<:Entry:Manifest](args:(Int,Rep[Any])*):Exp[E] = { checkOrInsertEntryClass[E](manifest[E]); SteSampleSEntry[E](manifest[E], args) }
  def sampleFullEntry[E<:Entry:Manifest](args:Rep[Any]*):Exp[E] = { checkOrInsertEntryClass[E](manifest[E]); SteSampleSEntry[E](manifest[E], args.zipWithIndex.map{case (arg, i) => ((i+1, arg))}) }
  def steMakeMutable [E<:Entry:Manifest](x: Exp[E]):Exp[E] = reflectMutable(SteMakeMutable[E](x))
  def steUpdate      [E<:Entry:Manifest](x: Exp[E], i: Int, v: Exp[Any]):Exp[Unit] = reflectWrite(x)(SteUpdate[E](x, i, v))
  def steIncrease    [E<:Entry:Manifest](x: Exp[E], i: Int, v: Exp[Any]):Exp[Unit] = reflectWrite(x)(SteIncrease[E](x, i, v))
  def steDecrease    [E<:Entry:Manifest](x: Exp[E], i: Int, v: Exp[Any]):Exp[Unit] = reflectWrite(x)(SteDecrease[E](x, i, v))
  def steGet         [E<:Entry:Manifest](x: Exp[E], i: Int):Exp[Any] = SteGet[E](x, i)

  //////////////
  // mirroring
  override def mirror[A:Manifest](e: Def[A], f: Transformer)(implicit pos: SourceContext): Exp[A] = (e match {
    case _ => super.mirror(e,f)
  }).asInstanceOf[Exp[A]] // why??

  def checkOrInsertEntryClass[E](m:Manifest[E]):Unit = {
    val ms = m.toString
    storeEntryClasses.get(ms) match {
      case None => storeEntryClasses += (ms -> (m, new collection.mutable.HashSet[Sym[_]]))
      case _ => ()
    }
  }
  //this hashmap will map (entry manifest string) -> ((entry class name),(list of argument type names),(list of Store[E] symbols, containing entry of the type given as key))
  val storeEntryClasses: collection.mutable.Map[String, (Manifest[_],collection.mutable.Set[Sym[_]])] = new collection.mutable.HashMap[String, (Manifest[_],collection.mutable.Set[Sym[_]])]()

  val ENTRY_INDICES_KEY = "StoreOps.Entry.indices"
}

trait SEntryExpOpt extends SEntryExp

trait ScalaGenSEntry extends ScalaGenBase with dbtoptimizer.ToasterBoosterScalaCodegen {
  val IR: SEntryExp with dbtoptimizer.ToasterBoosterExpression with Effects
  import IR._

  override def emitNode(sym: Sym[Any], rhs: Def[Any]) = rhs match {
    case SteNewSEntry(mE, args) => emitValDef(sym, /*"new " + */ remap(mE) + "("+args.map(quote(_)).mkString(", ")+")")
    case SteSampleSEntry(mE, args) => {
      val symName = "se%d".format(sym.id)
      staticFields += ("SEntryOps."+symName -> "val %s = new %s".format(symName, remap(mE)))
      emitValDef(sym, "{ "+args.map{
        case (i, v) => symName+"._"+i+" = "+quote(v)+"; "
      }.mkString +" "+symName+" }")
    }
    case SteMakeMutable(x) => emitValDef(sym, quote(x)+" /*made mutable*/")
    case SteUpdate(x,i,v) => emitValDef(sym, quote(x)+"._"+i+" = "+quote(v))
    case SteIncrease(x,i,v) => emitValDef(sym, quote(x)+"._"+i+" += "+quote(v))
    case SteDecrease(x,i,v) => emitValDef(sym, quote(x)+"._"+i+" -= "+quote(v))
    case SteGet(x,i) => emitValDef(sym, quote(x)+"._"+i)
    case _ => super.emitNode(sym, rhs)
  }

  def extractEntryClassName[E](m:Manifest[E]) = {
    val ms = m.toString
    val targs = m.typeArguments
    val fullClsName = ms.take(ms.indexOf("["))
    val baseClsName = fullClsName.takeRight(fullClsName.size - fullClsName.lastIndexOf('.') - 1)
    val targsStrList = targs.map(tp => remap(tp))
    val clsName = baseClsName+"_"+targsStrList.map(tp => simplifyTypeName(tp)).mkString
    (clsName, targsStrList)
  }

  def simplifyTypeName(tp:String):String = tp match {
    case "Int" => "I"
    case "Long" => "L"
    case "Float" => "F"
    case "Double" => "D"
    case "Boolean" => "B"
    case "java.util.Date" => "A"
    case "java.lang.String" => "S"
    case _ => tp.replace(".","_")
  }

  def zeroValue(tp:String):String = tp match {
    case "Int" => "0"
    case "Long" => "0L"
    case "Float" => "0f"
    case "Double" => "0D"
    case "Boolean" => "false"
    case _ => "null"
  }

  override def remap[A](m: Manifest[A]): String = m match {
    case _ if classOf[SEntry[_]] isAssignableFrom m.runtimeClass =>
      // call remap on all type arguments
      val targs = m.typeArguments
      if (targs.length > 0) {
        extractEntryClassName[A](m)._1
      }
      else super.remap[A](m)
    case _ => super.remap[A](m)
  }

  override def emitDataStructures(out: java.io.PrintWriter): Unit = {
    storeEntryClasses.foreach { case (_, (m, storeSyms)) =>
      val (clsName, argTypes) = extractEntryClassName(m)
      val indices: collection.mutable.ArrayBuffer[(IndexType,Seq[Int],Boolean,Int)] = if(storeSyms.size > 1) {
        throw new GenerationFailedException("Mutiple storeSyms for an entry class is not supported yet => " + storeSyms)
      } else if(storeSyms.size <= 0) {
        val m = new collection.mutable.ArrayBuffer[(IndexType,Seq[Int],Boolean,Int)]
        m += ((IList, (1 to argTypes.size), false, -1))
      } else {
        val sym = storeSyms.iterator.next
        sym.attributes(ENTRY_INDICES_KEY).asInstanceOf[collection.mutable.ArrayBuffer[(IndexType,Seq[Int],Boolean,Int)]]
      }
      out.println("  case class %s(%s) extends storage.Entry(%d) {".format(clsName, argTypes.zipWithIndex.map{ case (argTp, i) =>
        "var _%d:%s = %s".format(i+1, argTp, zeroValue(argTp))
      }.mkString(", "), argTypes.size))
      val tupleHashSeed = "0xcafebabe"
      out.println("    def hash(i: Int):Int = {\n      var hash:Int = %s\n      %s\n      hash\n    }".format(tupleHashSeed,indices.zipWithIndex.map{ case ((idxType,idxLoc,idxUniq,idxSliceIdx), j) =>
        "if(i == %d) {\n%s\n      }".format(j, genHashFunc("        ",idxType,idxLoc,idxUniq,idxSliceIdx,argTypes))
      }.mkString(" else ")))
      out.println("    def cmp(i: Int, e0:Entry):Int = {\n      val e=e0.asInstanceOf[%s]\n      %s else { 0 }\n    }".format(clsName, indices.zipWithIndex.map{ case ((idxType,idxLoc,idxUniq,idxSliceIdx), j) =>
        "if(i == %d) {\n%s\n      }".format(j, genCmpFunc("        ",idxType,idxLoc,idxUniq,idxSliceIdx))
      }.mkString(" else ")))
      out.println("    def copy = %s(%s)".format(clsName, argTypes.zipWithIndex.map{ case (_, i) => "_%d".format(i+1) }.mkString(", ")))
      out.println("  }")
    }
  }

  override def generateClassArgsDefs(out: java.io.PrintWriter, functionNames:Seq[String]) {
    classArgs.foreach { c =>
      val idxArr = c.attributes.get(ENTRY_INDICES_KEY) match {
        case Some(m) => m.asInstanceOf[collection.mutable.ArrayBuffer[(IndexType,Seq[Int],Boolean,Int)]]
        case None => val m = new collection.mutable.ArrayBuffer[(IndexType,Seq[Int],Boolean,Int)]
                     m += ((IList, (1 to c.tp.typeArguments.size),false,-1))
      }
      val cName = quote(c, true)
      out.println("    val %s = new %s(%d)".format(cName, remap(c.tp), idxArr.size))
      idxArr.zipWithIndex.foreach { case ((idxType, idxLoc, idxUniq, idxSliceIdx), i) => idxType match {
        case IList => out.println("    %s.index(%d,IList,%s)".format(cName, i, idxUniq))
        case IHash => out.println("    %s.index(%d,IHash,%s)".format(cName, i, idxUniq))
        case ISliceHeapMax => out.println("    %s.index(%d,ISliceHeapMax,%s,%d)".format(cName, i, idxUniq, idxSliceIdx))
        case ISliceHeapMin => out.println("    %s.index(%d,ISliceHeapMin,%s,%d)".format(cName, i, idxUniq, idxSliceIdx))
        case _ => out.println("    ... not supported ...")
      }}
      out.println()
    }
    functionNames.foreach { fn =>
      out.println("    val %sInst = new %s(%s)".format(fn,fn,classArgs.map{ c =>
        quote(c, true)
      }.mkString(", ")))
    }
  }

  /*
   * Implementation of MurmurHash3
   * based on scala.util.hashing.MurmurHash3
   * for Products
   *
   * https://github.com/scala/scala/blob/v2.10.2/src/library/scala/util/hashing/MurmurHash3.scala
   */
  def genHashFunc(prefix:String, idxType: IndexType, idxLoc: Seq[Int], idxUniq: Boolean, idxSliceIdx:Int, argTypes:Seq[String]):String = {
    def elemHashFunc(tp:String) = tp match {
      case "Int" => ""
      case _ => ".##"
    }
    def genHashFuncInternal(idxLocations: Seq[Int]) = {
      def rotl(i: String, distance: String) = "("+i+" << "+distance+") | ("+i+" >>> -"+distance+")"
      var counter:Int = 0
      idxLocations.map { i =>
        counter+=1
        //TODO: Check whether hashCode works better compared to ##
        //      as we know that everything is type-checked
        prefix + (if(counter == 1) "var mix:Int" else "mix") + " = _"+i+elemHashFunc(argTypes(i-1))+" * 0xcc9e2d51\n" +
        prefix + "mix = " + rotl("mix", "15")+"\n" +
        prefix + "mix *= 0x1b873593\n" +
        prefix + "mix ^= hash\n" +
        prefix + "mix = " + rotl("mix", "13")+"\n" +
        prefix + "hash = (mix << 1) + mix + 0xe6546b64\n"
      }.mkString +
      prefix + "hash ^= " + idxLocations.size + "\n" +
      prefix + "hash ^= hash >>> 16\n" +
      prefix + "hash *= 0x85ebca6b\n" +
      prefix + "hash ^= hash >>> 13\n" +
      prefix + "hash *= 0xc2b2ae35\n" +
      prefix + "hash ^= hash >>> 16\n" //+
      //currently we are doing it in IHash index, so it is
      //better not to do it here again
      //javaHashMapHashFunc("hash", prefix)
    }

    idxType match {
      case IHash | IList => genHashFuncInternal(idxLoc)
      case ISliceHeapMax | ISliceHeapMin => genHashFuncInternal(List(idxLoc(0)))
      case _ => prefix+"... not supported ..."
    }
  }

  /**
   * Applies a supplemental hash function to a given hashCode, which
   * defends against poor quality hash functions.  This is critical
   * because HashMap uses power-of-two length hash tables, that
   * otherwise encounter collisions for hashCodes that do not differ
   * in lower bits. Note: Null keys always map to hash 0, thus index 0.
   */
  def javaHashMapHashFunc(hash: String, prefix: String) = {
    prefix + hash+" ^= ("+hash+" >>> 20) ^ ("+hash+" >>> 12)\n" +
    prefix + hash+" ^= ("+hash+" >>> 7) ^ ("+hash+" >>> 4)"
  }

  def genCmpFunc(prefix:String, idxType: IndexType, idxLoc: Seq[Int], idxUniq: Boolean, idxSliceIdx:Int):String = idxType match {
    case IHash | IList => "%sif(%s) 0 else 1".format(prefix,idxLoc.map(i => "_%d == e._%d".format(i,i)).mkString(" && "))
    case ISliceHeapMin => val i = idxLoc(0); "%sif(_%s < e._%s) { -1 } else if(_%s > e._%s) { 1 } else { 0 }".format(prefix,i,i,i,i)
    case ISliceHeapMax => val i = idxLoc(0); "%sif(_%s < e._%s) { 1 } else if(_%s > e._%s) { -1 } else { 0 }".format(prefix,i,i,i,i)
    case _ => prefix+"... not supported ..."
  }
}

trait StoreOps extends Base with SEntryOps {

  class StoreOpsCls[E<:Entry:Manifest](x: Rep[Store[E]]) {
    def insert(e: Rep[E]):Rep[Unit] = stInsert[E](x, e)
    def update(e: Rep[E]):Rep[Unit] = stUpdate[E](x, e)
    def delete(e: Rep[E]):Rep[Unit] = stDelete[E](x, e)
    def get(key:Rep[E],idx:Int=(-1)):Rep[E] = stGet(x, idx, key)
    //def getOrPrev(key:Rep[E],idx:Int=(-1)):Rep[E] = stGetOrPrev(x, idx, key)
    //def getOrNext(key:Rep[E],idx:Int=(-1)):Rep[E] = stGetOrNext(x, idx, key)
    def getSliceMin(key:Rep[E],targetField:Int,sliceIdx:Int=(-1),minIdx:Int=(-1)):Rep[E] = stGetSliceMin(x, key, targetField, sliceIdx, minIdx)
    def getSliceMax(key:Rep[E],targetField:Int,sliceIdx:Int=(-1),maxIdx:Int=(-1)):Rep[E] = stGetSliceMax(x, key, targetField, sliceIdx, maxIdx)
    //def getMin(idx:Int=(-1)):Rep[E] = stGetMin(x, idx)
    //def getMax(idx:Int=(-1)):Rep[E] = stGetMax(x, idx)
    //def getMedian(idx:Int=(-1)):Rep[E] = stGetMedian(x, idx)
    def foreach(f:Rep[E]=>Rep[Unit]):Rep[Unit] = stForeach(x, f)
    def slice(key:Rep[E],f:Rep[E]=>Rep[Unit],idx:Int=(-1)):Rep[Unit] = stSlice(x,idx,key,f)
    //def range(min:Rep[E],max:Rep[E],f:Rep[E]=>Rep[Unit],withMin:Rep[Boolean]=unit(true),withMax:Rep[Boolean]=unit(true),idx:Int=(-1)):Rep[Unit] = stRange(x,idx,min,max,withMin,withMax,f)
    def delete(key:Rep[E],idx:Int=(-1)):Rep[Unit] = stDelete(x,idx,key)
    def clear:Rep[Unit] = stClear(x)
    def size:Rep[Int] = stSize(x)
    def index(tp:Rep[IndexType],unique:Rep[Boolean],idx:Int=(-1)):Rep[Unit] = stIndex(x, idx, tp, unique)
    def mutable = stMutable(x)
    //def idxs(idx:Int):Rep[Array[Idx[E]]] = stIdxs(x, idx)
  }

  implicit def store2StoreOpsCls[E<:Entry:Manifest](x: Rep[Store[E]]): StoreOpsCls[E] = new StoreOpsCls[E](x)

  def newStore     [E<:Entry:Manifest](sndIdx: Rep[Array[Idx[E]]]):Rep[Store[E]]
  //def newStore     [E<:Entry:Manifest]():Rep[Store[E]] = newStore[E](null.asInstanceOf[Rep[Array[Idx[E]]]])
  def stInsert     [E<:Entry:Manifest](x: Rep[Store[E]], e: Rep[E]):Rep[Unit]
  def stUpdate     [E<:Entry:Manifest](x: Rep[Store[E]], e: Rep[E]):Rep[Unit]
  def stDelete     [E<:Entry:Manifest](x: Rep[Store[E]], e: Rep[E]):Rep[Unit]
  def stGet        [E<:Entry:Manifest](x: Rep[Store[E]], idx:Int,key:Rep[E]):Rep[E]
  //def stGetOrPrev  [E<:Entry:Manifest](x: Rep[Store[E]], idx:Int,key:Rep[E]):Rep[E]
  //def stGetOrNext  [E<:Entry:Manifest](x: Rep[Store[E]], idx:Int,key:Rep[E]):Rep[E]
  def stGetSliceMin[E<:Entry:Manifest](x: Rep[Store[E]], key:Rep[E],targetField:Int,sliceIdx:Int,minIdx:Int):Rep[E]
  def stGetSliceMax[E<:Entry:Manifest](x: Rep[Store[E]], key:Rep[E],targetField:Int,sliceIdx:Int,maxIdx:Int):Rep[E]
  //def stGetMin     [E<:Entry:Manifest](x: Rep[Store[E]], idx:Int):Rep[E]
  //def stGetMax     [E<:Entry:Manifest](x: Rep[Store[E]], idx:Int):Rep[E]
  //def stGetMedian  [E<:Entry:Manifest](x: Rep[Store[E]], idx:Int):Rep[E]
  def stForeach    [E<:Entry:Manifest](x: Rep[Store[E]], f:Rep[E]=>Rep[Unit]):Rep[Unit]
  def stSlice      [E<:Entry:Manifest](x: Rep[Store[E]], idx:Int,key:Rep[E],f:Rep[E]=>Rep[Unit]):Rep[Unit]
  //def stRange      [E<:Entry:Manifest](x: Rep[Store[E]], idx:Int,min:Rep[E],max:Rep[E],withMin:Rep[Boolean],withMax:Rep[Boolean],f:Rep[E]=>Rep[Unit]):Rep[Unit]
  def stDelete     [E<:Entry:Manifest](x: Rep[Store[E]], idx:Int,key:Rep[E]):Rep[Unit]
  def stClear      [E<:Entry:Manifest](x: Rep[Store[E]]):Rep[Unit]
  def stSize       [E<:Entry:Manifest](x: Rep[Store[E]]):Rep[Int]
  def stIndex      [E<:Entry:Manifest](x: Rep[Store[E]], idx:Int,tp:Rep[IndexType],unique:Rep[Boolean]):Rep[Unit]
  //def stIdxs     [E<:Entry:Manifest](x: Rep[Store[E]], idx:Int):Rep[Array[Idx[E]]]
  def stMutable    [E<:Entry:Manifest](x: Rep[Store[E]]):Rep[Store[E]]
}

trait StoreExp extends StoreOps with BaseExp with EffectExp with VariablesExp with SEntryExp {
  case class StNewStore   [E<:Entry:Manifest](mE: Manifest[E], sndIdx: Exp[Array[Idx[E]]]) extends Def[Store[E]]

  case class StInsert     [E<:Entry:Manifest](x: Exp[Store[E]], e: Exp[E]) extends Def[Unit]
  case class StUpdate     [E<:Entry:Manifest](x: Exp[Store[E]], e: Exp[E]) extends Def[Unit]
  case class StDelete     [E<:Entry:Manifest](x: Exp[Store[E]], e: Exp[E]) extends Def[Unit]
  case class StGet        [E<:Entry:Manifest](x: Exp[Store[E]], idx:Int,key:Exp[E]) extends Def[E]
  //case class StGetOrPrev  [E<:Entry:Manifest](x: Exp[Store[E]], idx:Int,key:Exp[E]) extends Def[E]
  //case class StGetOrNext  [E<:Entry:Manifest](x: Exp[Store[E]], idx:Int,key:Exp[E]) extends Def[E]
  case class StGetSliceMin[E<:Entry:Manifest](x: Exp[Store[E]], key:Exp[E],targetField:Int,sliceIdx:Int,minIdx:Int) extends Def[E]
  case class StGetSliceMax[E<:Entry:Manifest](x: Exp[Store[E]], key:Exp[E],targetField:Int,sliceIdx:Int,maxIdx:Int) extends Def[E]
  //case class StGetMin     [E<:Entry:Manifest](x: Exp[Store[E]], idx:Int) extends Def[E]
  //case class StGetMax     [E<:Entry:Manifest](x: Exp[Store[E]], idx:Int) extends Def[E]
  //case class StGetMedian  [E<:Entry:Manifest](x: Exp[Store[E]], idx:Int) extends Def[E]
  case class StForeach    [E<:Entry:Manifest](x: Exp[Store[E]], blockSym: Sym[E], block: Block[Unit]) extends Def[Unit]
  case class StSlice      [E<:Entry:Manifest](x: Exp[Store[E]], idx:Int, key:Exp[E], blockSym: Sym[E], block: Block[Unit]) extends Def[Unit]
  //case class StRange      [E<:Entry:Manifest](x: Exp[Store[E]], idx:Int, min:Exp[E],max:Exp[E], withMin:Exp[Boolean], withMax:Exp[Boolean], blockSym: Sym[E], block: Block[Unit]) extends Def[Unit]
  case class StDeleteOnIdx[E<:Entry:Manifest](x: Exp[Store[E]], idx:Int, key:Exp[E]) extends Def[Unit]
  case class StClear      [E<:Entry:Manifest](x: Exp[Store[E]]) extends Def[Unit]
  case class StSize       [E<:Entry:Manifest](x: Exp[Store[E]]) extends Def[Int]
  case class StIndex      [E<:Entry:Manifest](x: Exp[Store[E]], idx:Int, tp:Exp[IndexType], unique:Exp[Boolean]) extends Def[Unit]
  //case class StIdxs     [E<:Entry:Manifest](x: Exp[Store[E]], idx:Int) extends Def[Array[Idx[E]]]
  case class StMutable    [E<:Entry:Manifest](x: Exp[Store[E]]) extends Def[Store[E]]

  def addIndicesToEntryClass[E<:Entry:Manifest](x:Exp[Store[E]], fn: ((Sym[_],collection.mutable.ArrayBuffer[(IndexType,Seq[Int],Boolean,Int)]) => Unit)) = {
    var xx = x.asInstanceOf[Sym[_]]
    x match {
      case Def(Reflect(StMutable(xy),_,_)) => xx = xy.asInstanceOf[Sym[_]]
      case _ => ()
    }
    xx.attributes.get(ENTRY_INDICES_KEY) match {
      case Some(m) => fn(xx, m.asInstanceOf[collection.mutable.ArrayBuffer[(IndexType,Seq[Int],Boolean,Int)]])
      case None => {
        val m = new collection.mutable.ArrayBuffer[(IndexType,Seq[Int],Boolean,Int)]
        fn(xx, m)
        xx.attributes.put(ENTRY_INDICES_KEY, m)
      }
    }
    storeEntryClasses(manifest[E].toString)._2 += xx
  }

  def newStore     [E<:Entry:Manifest](sndIdx: Exp[Array[Idx[E]]]):Exp[Store[E]] = { checkOrInsertEntryClass[E](manifest[E]); reflectMutable(StNewStore[E](manifest[E], sndIdx)) }
  def stInsert     [E<:Entry:Manifest](x: Exp[Store[E]], e: Exp[E]):Exp[Unit] = reflectWrite(x)(StInsert[E](x, e))
  def stUpdate     [E<:Entry:Manifest](x: Exp[Store[E]], e: Exp[E]):Exp[Unit] = reflectWrite(x)(StUpdate[E](x, e))
  def stDelete     [E<:Entry:Manifest](x: Exp[Store[E]], e: Exp[E]):Exp[Unit] = reflectWrite(x)(StDelete[E](x, e))
  def stGet        [E<:Entry:Manifest](x: Exp[Store[E]], idx_in:Int,key:Exp[E]):Exp[E] = {
    var idx = idx_in
    key match {
      case Def(SteSampleSEntry(mE, args)) => addIndicesToEntryClass[E](x, (xx, m) => {
        val tupVal = ((IHash,args.map(_._1),true,-1))
        idx = m.indexOf(tupVal)
        if(idx < 0) {
          m += tupVal
          idx = m.size - 1
        }
      })
      case _ => throw new GenerationFailedException("You should provide a sample entry to this method: Store.get")
    }
    val elem:Exp[E]=StGet[E](x,idx,key)
    steMakeMutable(elem)
  }
  //def stGetOrPrev  [E<:Entry:Manifest](x: Exp[Store[E]], idx:Int,key:Exp[E]):Exp[E] = { val elem:Exp[E]=StGetOrPrev[E](x,idx,key); steMakeMutable(elem)}
  //def stGetOrNext  [E<:Entry:Manifest](x: Exp[Store[E]], idx:Int,key:Exp[E]):Exp[E] = { val elem:Exp[E]=StGetOrNext[E](x,idx,key); steMakeMutable(elem)}
  def stGetSliceMin[E<:Entry:Manifest](x: Exp[Store[E]], key:Exp[E],targetField:Int,sliceIdx_in:Int,minIdx_in:Int):Exp[E] = {
    var sliceIdx = sliceIdx_in
    var minIdx = minIdx_in
    key match {
      case Def(SteSampleSEntry(mE, args)) => addIndicesToEntryClass[E](x, (xx, m) => {
        val sliceIdxTupVal = ((IHash,args.map(_._1),false,-1))
        sliceIdx = m.indexOf(sliceIdxTupVal)
        if(sliceIdx < 0) {
          m += sliceIdxTupVal
          sliceIdx = m.size - 1
        }
        val minTupVal = ((ISliceHeapMin, List(targetField),false,sliceIdx))
        minIdx = m.indexOf(minTupVal)
        if(minIdx < 0) {
          m += minTupVal
          minIdx = m.size - 1
        }
      })
      case _ => throw new GenerationFailedException("You should provide a sample entry to this method: Store.getSliceMin")
    }
    val elem:Exp[E]=StGetSliceMin[E](x,key,targetField,sliceIdx,minIdx)
    steMakeMutable(elem)
  }
  def stGetSliceMax[E<:Entry:Manifest](x: Exp[Store[E]], key:Exp[E],targetField:Int,sliceIdx_in:Int,maxIdx_in:Int):Exp[E] = {
    var sliceIdx = sliceIdx_in
    var maxIdx = maxIdx_in
    key match {
      case Def(SteSampleSEntry(mE, args)) => addIndicesToEntryClass[E](x, (xx, m) => {
        val sliceIdxTupVal = ((IHash,args.map(_._1),false,-1))
        sliceIdx = m.indexOf(sliceIdxTupVal)
        if(sliceIdx < 0) {
          m += sliceIdxTupVal
          sliceIdx = m.size - 1
        }
        val maxTupVal = ((ISliceHeapMax, List(targetField),false,sliceIdx))
        maxIdx = m.indexOf(maxTupVal)
        if(maxIdx < 0) {
          m += maxTupVal
          maxIdx = m.size - 1
        }
      })
      case _ => throw new GenerationFailedException("You should provide a sample entry to this method: Store.getSliceMax")
    }
    val elem:Exp[E]=StGetSliceMax[E](x,key,targetField,sliceIdx,maxIdx)
    steMakeMutable(elem)
  }
  //def stGetMin   [E<:Entry:Manifest](x: Exp[Store[E]], idx:Int):Exp[E] = { val elem:Exp[E]=StGetMin[E](x,idx); steMakeMutable(elem)}
  //def stGetMax   [E<:Entry:Manifest](x: Exp[Store[E]], idx:Int):Exp[E] = { val elem:Exp[E]=StGetMax[E](x,idx); steMakeMutable(elem)}
  //def stGetMedian[E<:Entry:Manifest](x: Exp[Store[E]], idx:Int):Exp[E] = { val elem:Exp[E]=StGetMedian[E](x,idx); steMakeMutable(elem)}
  def stForeach    [E<:Entry:Manifest](x: Exp[Store[E]], f:Exp[E]=>Exp[Unit]):Exp[Unit] = {
    val blkSym = fresh[E]
    val blk = reifyEffects(f(blkSym))
    reflectEffect(StForeach[E](x, blkSym, blk), summarizeEffects(blk).star)
  }
  def stSlice      [E<:Entry:Manifest](x: Exp[Store[E]], idx_in:Int,key:Exp[E],f:Exp[E]=>Exp[Unit]):Exp[Unit] = {
    var idx = idx_in
    key match {
      case Def(SteSampleSEntry(mE, args)) => addIndicesToEntryClass[E](x, (xx, m) => {
        val tupVal = ((IHash,args.map(_._1),false,-1))
        idx = m.indexOf(tupVal)
        if(idx < 0) {
          m += tupVal
          idx = m.size - 1
        }
      })
      case _ => throw new GenerationFailedException("You should provide a sample entry to this method: Store.slice")
    }
    val blkSym = fresh[E]
    val blk = reifyEffects(f(blkSym))
    reflectEffect(StSlice[E](x, idx, key, blkSym, blk), summarizeEffects(blk).star)
  }
  //def stRange      [E<:Entry:Manifest](x: Exp[Store[E]], idx:Int,min:Exp[E],max:Exp[E],withMin:Exp[Boolean],withMax:Exp[Boolean],f:Exp[E]=>Exp[Unit]):Exp[Unit] = {
  //  val blkSym = fresh[E]
  //  val blk = reifyEffects(f(blkSym))
  //  reflectEffect(StRange[E](x, idx, min, max, withMin, withMax, blkSym, blk), summarizeEffects(blk).star)
  //}
  def stDelete     [E<:Entry:Manifest](x: Exp[Store[E]], idx:Int,key:Exp[E]):Exp[Unit] = reflectWrite(x)(StDeleteOnIdx[E](x, idx, key))
  def stClear      [E<:Entry:Manifest](x: Exp[Store[E]]):Exp[Unit] = reflectWrite(x)(StClear[E](x))
  def stSize       [E<:Entry:Manifest](x: Exp[Store[E]]):Rep[Int] = StSize[E](x)
  def stIndex      [E<:Entry:Manifest](x: Exp[Store[E]], idx:Int,tp:Exp[IndexType],unique:Exp[Boolean]):Exp[Unit] = reflectWrite(x)(StIndex[E](x, idx, tp, unique))
  //def stIdxs     [E<:Entry:Manifest](x: Exp[Store[E]], idx:Int):Exp[Array[Idx[E]]] = ...
  def stMutable    [E<:Entry:Manifest](x: Exp[Store[E]]):Exp[Store[E]] = reflectMutable(StMutable[E](x))

  //////////////
  // mirroring
  override def mirror[A:Manifest](e: Def[A], f: Transformer)(implicit pos: SourceContext): Exp[A] = (e match {
    // case e@StNewStore(mE, sndIdx) => newStore(f(sndIdx))(e.mE)
    // case e@StMutableEntry(elem) => StMutableEntry(f(elem))

    // case K3Contains(x, key) => k3Contains(f(x), f(key))
    // case e@K3Lookup(x, key) => k3Lookup(f(x), f(key))(mtype(e.mV))
    // case e@K3LookupOrDefault(x, key, defaultVal) => k3LookupOrDefault(f(x).asInstanceOf[Exp[Store[Any, A]]], f(key), f(defaultVal))(mtype(e.mV)) //why cast is needed?
    // case K3UpdateValue(x, key, value) => k3UpdateValue(f(x), f(key), f(value))
    // case K3Remove(x, key) => k3Remove(f(x), f(key))
    // case K3Clear(x) => k3Clear(f(x))
    // case K3Mutable(x) => k3Mutable(f(x))
    // case e@K3Slice(x, kp, idx) => k3Slice(f(x), f(kp), f(idx))(mtype(e.mK),mtype(e.mV),mtype(e.mK2))
    // case K3Foreach(a, x, body) => K3Foreach(f(a),x,f(body))
    // case Reflect(e@StNewStore(mK, mV, name, elems, sndIdx), u, es) => reflectMirrored(Reflect(StNewStore(mtype(mK), mtype(mV), f(name), f(elems), f(sndIdx)), mapOver(f,u), f(es)))(mtype(manifest[A]))
    // case Reflect(K3Contains(x, key), u, es) => reflectMirrored(Reflect(K3Contains(f(x), f(key)), mapOver(f,u), f(es)))(mtype(manifest[A]))
    // case Reflect(e@K3Lookup(x, key), u, es) => reflectMirrored(Reflect(K3Lookup(f(x), f(key))(mtype(e.mV)), mapOver(f,u), f(es)))(mtype(manifest[A]))
    // case Reflect(e@K3LookupOrDefault(x, key, defaultVal), u, es) => reflectMirrored(Reflect(K3LookupOrDefault(f(x).asInstanceOf[Exp[Store[Any, A]]], f(key), f(defaultVal))(mtype(e.mV)), mapOver(f,u), f(es)))(mtype(manifest[A]))
    // case Reflect(K3UpdateValue(x, key, value), u, es) => reflectMirrored(Reflect(K3UpdateValue(f(x), f(key), f(value)), mapOver(f,u), f(es)))(mtype(manifest[A]))
    // case Reflect(K3Remove(x, key), u, es) => reflectMirrored(Reflect(K3Remove(f(x), f(key)), mapOver(f,u), f(es)))(mtype(manifest[A]))
    // case Reflect(K3Clear(x), u, es) => reflectMirrored(Reflect(K3Clear(f(x)), mapOver(f,u), f(es)))(mtype(manifest[A]))
    // case Reflect(K3Mutable(x), u, es) => reflectMirrored(Reflect(K3Mutable(f(x)), mapOver(f,u), f(es)))(mtype(manifest[A]))
    // case Reflect(e@K3Slice(x, kp, idx), u, es) => reflectMirrored(Reflect(K3Slice(f(x), f(kp), f(idx))(mtype(e.mK),mtype(e.mV),mtype(e.mK2)), mapOver(f,u), f(es)))(mtype(manifest[A]))
    // case Reflect(K3Foreach(a, x, body), u, es) => reflectMirrored(Reflect(K3Foreach(f(a), x, f(body)), mapOver(f,u), f(es)))(mtype(manifest[A]))
    case _ => super.mirror(e,f)
  }).asInstanceOf[Exp[A]] // why??

  override def syms(e: Any): List[Sym[Any]] = e match {
    case StForeach(x, blockSym, block) => syms(x):::syms(block)
    case StSlice(x, idx, key, blockSym, block) => syms(x):::syms(idx):::syms(key):::syms(block)
    //case StRange(x, idx, min, max, withMin, withMax, blockSym, block) => syms(x):::syms(idx):::syms(min):::syms(max):::syms(withMin):::syms(withMax):::syms(block)
    case _ => super.syms(e)
  }

  override def boundSyms(e: Any): List[Sym[Any]] = e match {
    case StForeach(x, blockSym, block) => blockSym :: effectSyms(block)
    case StSlice(x, idx, key, blockSym, block) => blockSym :: effectSyms(block)
    //case StRange(x, idx, min, max, withMin, withMax, blockSym, block) => blockSym :: effectSyms(block)
    case _ => super.boundSyms(e)
  }

  override def symsFreq(e: Any): List[(Sym[Any], Double)] = e match {
    case StForeach(x, blockSym, block) => freqNormal(x):::freqHot(block)
    case StSlice(x, idx, key, blockSym, block) => freqNormal(x):::freqNormal(idx):::freqNormal(key):::freqHot(block)
    //case StRange(x, idx, min, max, withMin, withMax, blockSym, block) => freqNormal(x):::freqNormal(idx):::freqNormal(min):::freqNormal(max):::freqNormal(withMin):::freqNormal(withMax):::freqHot(block)
    case _ => super.symsFreq(e)
  }
}

trait StoreExpOpt extends StoreExp with SEntryExpOpt

trait ScalaGenStore extends ScalaGenBase with GenericNestedCodegen {
  val IR: StoreExp
  import IR._

  override def emitNode(sym: Sym[Any], rhs: Def[Any]) = rhs match {
    case StNewStore(mE, sndIdx) => emitValDef(sym, "new Store[" + remap(mE) + "]("+quote(sndIdx)+")")
    case StInsert(x,e) => emitValDef(sym, quote(x)+".insert("+quote(e)+")")
    case StUpdate(x,e) => emitValDef(sym, quote(x)+".update("+quote(e)+")")
    case StDelete(x,e) => emitValDef(sym, quote(x)+".delete("+quote(e)+")")
    //case StGet(x,idx,key) => emitValDef(sym, quote(x)+".get("+{if(idx == (-1)) "0" else ""+idx}+","+quote(key)+")")
    case StGet(x,idx,key) => emitValDef(sym, quote(x)+".idxs("+{if(idx == (-1)) "0" else ""+idx}+").get("+quote(key)+")")
    //case StGetOrPrev(x,idx,key) => emitValDef(sym, quote(x)+".getOrPrev("+{if(idx == (-1)) "1" else ""+idx}+", "+quote(key)+")")
    //case StGetOrNext(x,idx,key) => emitValDef(sym, quote(x)+".getOrNext("+{if(idx == (-1)) "1" else ""+idx}+", "+quote(key)+")")
    //case StGetSliceMin(x,key,targetField,sliceIdx,minIdx) => emitValDef(sym, quote(x)+".get("+{if(minIdx == (-1)) "1" else minIdx}+","+quote(key)+") /* min */")
    case StGetSliceMin(x,key,targetField,sliceIdx,minIdx) => emitValDef(sym, quote(x)+".idxs("+{if(minIdx == (-1)) "1" else minIdx}+").get("+quote(key)+") /* min */")
    //case StGetSliceMax(x,key,targetField,sliceIdx,maxIdx) => emitValDef(sym, quote(x)+".get("+{if(maxIdx == (-1)) "1" else maxIdx}+","+quote(key)+") /* max */")
    case StGetSliceMax(x,key,targetField,sliceIdx,maxIdx) => emitValDef(sym, quote(x)+".idxs("+{if(maxIdx == (-1)) "1" else maxIdx}+").get("+quote(key)+") /* max */")
    //case StGetMin(x,idx) => emitValDef(sym, quote(x)+".getMin("+{if(idx == (-1)) "1" else ""+idx}+")")
    //case StGetMax(x,idx) => emitValDef(sym, quote(x)+".getMax("+{if(idx == (-1)) "1" else ""+idx}+")")
    //case StGetMedian(x,idx) => emitValDef(sym, quote(x)+".getMedian("+{if(idx == (-1)) "1" else ""+idx}+")")
    // case StForeach(x, blockSym, block) => emitValDef(sym, quote(x)+".idxs(0).foreach{")
    //   stream.println(quote(blockSym) + " => ")
    //   emitBlock(block)
    //   stream.println("}")
    //case StForeach(x, blockSym, block) => emitValDef(sym, quote(x)+".foreach{")
    case StForeach(x, blockSym, block) => emitValDef(sym, quote(x)+".idxs(0).foreach{")
      stream.println(quote(blockSym) + " => ")
      emitBlock(block)
      stream.println("}")
    //case StSlice(x, idx, key, blockSym, block) => emitValDef(sym, quote(x)+".slice("+{if(idx == (-1)) "1" else ""+idx}+","+quote(key)+",{")
    case StSlice(x, idx, key, blockSym, block) => emitValDef(sym, quote(x)+".idxs("+{if(idx == (-1)) "1" else ""+idx}+").slice("+quote(key)+",{")
      stream.println(quote(blockSym) + " => ")
      emitBlock(block)
      stream.println("})")
    //case StRange(x, idx, min, max, withMin, withMax, blockSym, block) => emitValDef(sym, quote(x)+".range("+{if(idx == (-1)) "1" else ""+idx}+","+quote(min)+","+quote(max)+","+quote(withMin)+","+quote(withMax)+",{")
    //  stream.println(quote(blockSym) + " => ")
    //  emitBlock(block)
    //  stream.println("})")
    case StDeleteOnIdx(x,idx,key) => emitValDef(sym, quote(x)+".delete("+{if(idx == (-1)) "0" else ""+idx}+", "+quote(key)+")")
    case StClear(x) => emitValDef(sym, quote(x)+".clear")
    case StSize(x) => emitValDef(sym, quote(x)+".size")
    case StIndex(x, idx, tp, unique) => emitValDef(sym,  quote(x)+".get("+{if(idx == (-1)) "0" else ""+idx}+", "+quote(tp)+", "+quote(unique)+")")
    case StMutable(x) => emitValDef(sym, quote(x)+" /*store made mutable*/")
    case _ => super.emitNode(sym, rhs)
  }
}
