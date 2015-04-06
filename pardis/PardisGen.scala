package ddbt.codegen.pardis

import ddbt.ast.M3._
import ddbt.ast.M3
import ddbt.ast._
import ddbt.lib._
import ddbt.lib.ManifestHelper.{man,zero,manEntry,manStore,manContainer}
import ddbt.codegen.IScalaGen
import ch.epfl.data.pardis
import pardis.ir._
import pardis.types.PardisTypeImplicits._
import pardis.effects._
import pardis.deep._
import pardis.deep.scalalib._
import pardis.deep.scalalib.collection._
import pardis.deep.scalalib.io._


abstract class PardisGen(override val cls:String="Query", val impl: PardisExpGen) extends IScalaGen {

  import impl._
  import ddbt.lib.store._

  var cx : Ctx[Rep[_]] = null

  def me(ks:List[Type],v:Type=null) = manEntry(if (v==null) ks else ks:::List(v))

  def mapProxy(m:Rep[_]) = impl.store2StoreOpsCls(m.asInstanceOf[Rep[Store[Entry]]])

  // Expression CPS transformation from M3 AST to LMS graph representation
  //   ex : expression to convert
  //   co : continuation in which the result should be plugged
  //   am : shared aggregation map for Add and AggSum, avoiding useless intermediate map where possible
  def expr(ex:Expr, co: Rep[_]=>Rep[Unit], am:Option[List[(String,Type)]]=None):Rep[Unit] = ex match {
    case Ref(n) => co(cx(n))
    case Const(tp,v) => ex.tp match {
      case TypeLong => co(impl.unit(v.toLong))
      case TypeDouble => co(impl.unit(v.toDouble))
      case TypeString => co(impl.unit(v))
      case TypeDate => sys.error("No date constant conversion") //co(impl.unit(new java.util.Date()))
      case _ => sys.error("Unsupported type "+tp)
    }
    case Exists(e) => expr(e,(ve:Rep[_]) => co(impl.__ifThenElse(impl.infix_!=(ve,impl.unit(0L)),impl.unit(1L),impl.unit(0L))))
    case Cmp(l,r,op) => expr(l,(vl:Rep[_]) => expr(r,(vr:Rep[_]) => co(cmp(vl,op,vr,ex.tp)) )) // formally, we should take the derived type from left and right, but this makes no difference to LMS
    case a@Apply(fn,tp,as) =>
      def app(es:List[Expr],vs:List[Rep[_]]):Rep[Unit] = es match {
        case x :: xs => expr(x,(v:Rep[_]) => app(xs,v::vs))
        case Nil => co(impl.m3apply(fn,vs.reverse,tp))
      }
      if (as.forall(_.isInstanceOf[Const])) co(impl.named(constApply(a),tp,false)) // hoist constants resulting from function application
      else app(as,Nil)
    case m@MapRef(n,tp,ks) =>
      val (ko,ki) = ks.zipWithIndex.partition{case(k,i)=>cx.contains(k)}
      val proxy = mapProxy(cx(n))
      if(ks.size == 0) { // variable
        co(cx(n))
      } else if(ki.size == 0) { // all keys are bound
        val z = impl.unit(zero(tp))
        val vs = ks.zipWithIndex.map{ case (n,i) => (i+1,cx(n))}
        val r = proxy.get(vs : _*)
        co(impl.__ifThenElse(impl.infix_==(r,impl.unit(null)),z,impl.steGet(r,ks.size+1)))
      } else { // we need to iterate over all keys not bound (ki)
        if (ko.size > 0) {
          implicit val mE=me(m.tks,tp)
          val mm = cx(n).asInstanceOf[Rep[Store[Entry]]]
          impl.stSlice(mm, {
            (e:Rep[Entry])=> cx.add(ki.map{ case (k,i) => (k,impl.steGet(e, i+1)) }.toMap); co(impl.steGet(e, ks.size+1))
          },ko.map{ case (k,i) => (i+1,cx(k)) } : _*)
        } else {
          proxy.foreach((e:Rep[Entry])=> {
            cx.add(ki.map{ case (k,i) => (k,impl.steGet(e, i+1)) }.toMap); co(impl.steGet(e, ks.size+1))
          })
        }
      }
    case Lift(n,e) =>
      if (cx.contains(n)) expr(e,(ve:Rep[_])=>co(impl.__ifThenElse(impl.equals(ve,cx(n)),impl.unit(1L),impl.unit(0L))  ),am)
      else e match {
        case Ref(n2) => cx.add(n,cx(n2)); co(impl.unit(1L))
        case _ => expr(e,(ve:Rep[_]) => { cx.add(n,ve); co(impl.unit(1L)) } )
      }
    case Mul(l,r) => expr(l,(vl:Rep[_])=> expr(r,(vr:Rep[_]) => co(mul(vl,vr,ex.tp)),am),am)
    case a@Add(l,r) =>
      if (a.agg==Nil) {
        val cur=cx.save
        expr(l,(vl:Rep[_])=>{
          cx.load(cur)
          expr(r,(vr:Rep[_])=>{
            cx.load(cur)
            co(add(vl,vr,ex.tp))
          },am)
        },am)
      } else am match {
        case Some(t) if t.toSet.subsetOf(a.agg.toSet) =>
          val cur=cx.save
          expr(l,co,am)
          cx.load(cur)
          expr(r,co,am)
          cx.load(cur)
          impl.unit(())
        case _ =>
          implicit val mE=me(a.agg.map(_._2),a.tp)
          val acc = impl.m3temp()(mE)
          val inCo = (v:Rep[_]) => impl.m3add(acc,impl.stNewEntry2(acc, (a.agg.map(x=>cx(x._1))++List(v)) : _*))(mE)
          val cur = cx.save
          expr(l,inCo,Some(a.agg)); cx.load(cur)
          expr(r,inCo,Some(a.agg)); cx.load(cur)
          foreach(acc,a.agg,a.tp,co)
      }
    case a@AggSum(ks,e) =>
      val agg_keys = (ks zip a.tks).filter{ case (n,t)=> !cx.contains(n) } // the aggregation is only made on free variables
      if (agg_keys.size==0) { // Accumulate expr(e) in the acc, returns (Rep[Unit],ctx) and we ignore ctx
        val cur=cx.save;
        val acc:impl.Var[_] = ex.tp match {
          case TypeLong =>
            val agg:impl.Var[Long] = impl.var_new[Long](impl.unit(0L))
            expr(e,
              (v:Rep[_]) => impl.var_assign[Long](agg.asInstanceOf[impl.Var[Long]], impl.numeric_plus[Long](impl.readVar[Long](agg.asInstanceOf[impl.Var[Long]]),v.asInstanceOf[Rep[Long]]))
            )
            agg
          case TypeDouble =>
            val agg:impl.Var[Double] = impl.var_new[Double](impl.unit(0.0))
            expr(e,
              (v:Rep[_]) => impl.var_assign[Double](agg.asInstanceOf[impl.Var[Double]], impl.numeric_plus[Double](impl.readVar[Double](agg.asInstanceOf[impl.Var[Double]]),v.asInstanceOf[Rep[Double]]))
            )
            agg
          case TypeString =>
            val agg:impl.Var[String] = impl.var_new[String](impl.unit(""))
            expr(e,
              (v:Rep[_]) => impl.var_assign[String](agg.asInstanceOf[impl.Var[String]], impl.string_plus(impl.readVar[String](agg.asInstanceOf[impl.Var[String]]),v.asInstanceOf[Rep[String]]))
            )
            agg
          //case TypeDate =>
          // val agg:impl.Var[java.util.Date] = impl.var_new[java.util.Date](impl.unit(new java.util.Date()))
          // expr(e,
          //  (v:Rep[_]) => impl.var_assign[java.util.Date](agg.asInstanceOf[impl.Var[java.util.Date]], impl.numeric_plus[java.util.Date](impl.readVar[java.util.Date](agg.asInstanceOf[impl.Var[java.util.Date]]),v.asInstanceOf[Rep[java.util.Date]])),
          //  None
          // )
          // agg
          case _ => sys.error("Unsupported type "+ex.tp)
        }
        cx.load(cur)
        co(impl.readVar(acc))
      } else am match {
        case Some(t) if t.toSet.subsetOf(agg_keys.toSet) => expr(e,co,am)
        case _ =>

        val cur = cx.save

        implicit val mE=me(agg_keys.map(_._2),ex.tp)
        val acc = impl.m3temp()(mE)
        val coAcc = (v:Rep[_]) => {
          val vs:List[Rep[_]] = agg_keys.map(x=>cx(x._1)).toList ::: List(v)
          impl.m3add(acc, impl.stNewEntry2(acc, vs : _*))
        }
        expr(e,coAcc,Some(agg_keys)); cx.load(cur) // returns (Rep[Unit],ctx) and we ignore ctx
        foreach(acc,agg_keys,a.tp,co)
      }
    case _ => sys.error("Unimplemented: "+ex)
  }

  def foreach(map:Rep[_],keys:List[(String,Type)],value_tp:Type,co:Rep[_]=>Rep[Unit]):Rep[Unit] = {
    implicit val mE = manEntry(keys.map(_._2) ::: List(value_tp))
    val proxy = mapProxy(map)
    proxy.foreach{ e:Rep[Entry] =>
      cx.add(keys.zipWithIndex.filter(x=> !cx.contains(x._1._1)).map { case ((n,t),i) => (n,impl.steGet(e, i+1)) }.toMap)
      co(impl.steGet(e, keys.size+1))
    }
  }

  def accessTuple(tuple: Rep[_],elemTp:Type,sz: Int, idx: Int)(implicit pos: scala.reflect.SourceContext): Rep[_] = sz match {
      case 1 => tuple
      case _ => impl.getClass.getDeclaredMethod("tuple%d_get%d".format(sz,idx+1),Class.forName("java.lang.Object"), Class.forName("scala.reflect.Manifest"), Class.forName("scala.reflect.SourceContext"))
                 .invoke(impl, tuple, man(elemTp), pos).asInstanceOf[Rep[_]]
  }

  def mul(l:Rep[_], r:Rep[_], tp:Type) = {
    tp match {
      case TypeLong | TypeDouble => impl.m3apply("mul",List(l,r),tp)
      case _ => sys.error("Mul(l,r) only allowed on numeric types")
    }
  }

  def add(l:Rep[_], r:Rep[_], tp:Type) = {
    @inline def plus[T:Numeric:Manifest]() = impl.numeric_plus[T](l.asInstanceOf[Rep[T]],r.asInstanceOf[Rep[T]])
    tp match {
      case TypeLong => plus[Long]()
      case TypeDouble => plus[Double]()
      case _ => sys.error("Add(l,r) only allowed on numeric types")
    }
  }

  def cmp(l:Rep[_], op:OpCmp, r:Rep[_], tp:Type): Rep[Long] = {
    @inline def cmp2[T:Ordering:Manifest](vl:Rep[_],vr:Rep[_]): Rep[Boolean] = {
      val (ll,rr)=(vl.asInstanceOf[Rep[T]],vr.asInstanceOf[Rep[T]])
      op match {
        case OpEq => impl.infix_==[T,T](ll,rr)
        case OpNe => impl.infix_!=[T,T](ll,rr)
        case OpGt => impl.ordering_gt[T](ll,rr)
        case OpGe => impl.ordering_gteq[T](ll,rr)
      }
    }
    impl.__ifThenElse(tp match {
      case TypeLong => cmp2[Long](l,r)
      case TypeDouble => cmp2[Double](l,r)
      case TypeString => cmp2[String](l,r)
      case TypeDate => cmp2[Long](impl.dtGetTime(l.asInstanceOf[Rep[java.util.Date]]),impl.dtGetTime(r.asInstanceOf[Rep[java.util.Date]]))
      case _ => sys.error("Unsupported type")
    },impl.unit(1L),impl.unit(0L))
  }

  def filterStatement(s:Stmt) = true

  // Trigger code generation
  def genTriggerLMS(t:Trigger, s0:System) = {
    val (name,args) = t.evt match {
      case EvtReady => ("SystemReady",Nil)
      case EvtBatchUpdate(Schema(n,cs)) => ("BatchUpdate"+n,cs)
      case EvtAdd(Schema(n,cs)) => ("Add"+n,cs)
      case EvtDel(Schema(n,cs)) => ("Del"+n,cs)
    }

    var params = ""
    val block = impl.reifyBlock {
      params = t.evt match {
        case EvtBatchUpdate(Schema(n,_)) =>
          val rel = s0.sources.filter(_.schema.name == n)(0).schema
          val name = rel.deltaSchema

          name+":Store["+impl.codegen.storeEntryType(ctx0(name)._1)+"]"
        case _ =>
          args.map(a=>a._1+":"+a._2.toScala).mkString(", ")
      }
      // Trigger context: global maps + trigger arguments
      cx = Ctx((
        ctx0.map{ case (name,(sym,keys,tp)) => (name, sym) }.toList union
        {
          t.evt match {
            case EvtBatchUpdate(Schema(n,_)) =>
              Nil
            case _ =>
              args.map{ case (name,tp) => (name,impl.named(name,tp)) }
          }
        }
      ).toMap)
      // Execute each statement
      t.stmts.filter(filterStatement).map {
        case StmtMap(m,e,op,oi) => cx.load()
          if (m.keys.size==0) {
            val mm = m.tp match {
              case TypeLong => impl.Var(cx(m.name).asInstanceOf[Rep[impl.Var[Long]]])
              case TypeDouble => impl.Var(cx(m.name).asInstanceOf[Rep[impl.Var[Double]]])
              case TypeString => impl.Var(cx(m.name).asInstanceOf[Rep[impl.Var[String]]])
              //case TypeDate => impl.Variable(cx(m.name).asInstanceOf[Rep[impl.Variable[java.util.Date]]])
              case _ => sys.error("Unsupported type "+m.tp)
            }
            expr(e,(r:Rep[_]) => op match { case OpAdd => impl.var_plusequals(mm,r) case OpSet => impl.__assign(mm,r) })
          } else {
            val mm = cx(m.name).asInstanceOf[Rep[Store[Entry]]]
            implicit val mE = manEntry(m.tks ++ List(m.tp)).asInstanceOf[Manifest[Entry]]
            if (op==OpSet) impl.stClear(mm)
            oi match { case None => case Some(ie) =>
              expr(ie,(r:Rep[_]) => {
                val ent = impl.stNewEntry2(mm, (m.keys.map(cx) ++ List(r)) : _*)
                impl.__ifThenElse(impl.equals(mapProxy(mm).get(m.keys.zipWithIndex.map{ case (n,i) => (i+1,cx(n))} : _*),impl.unit(null)),impl.m3set(mm,ent),impl.unit(()))
              })
            }
            cx.load()
            expr(e,(r:Rep[_]) => {
              val ent = impl.stNewEntry2(mm, (m.keys.map(cx) ++ List(r)) : _*)
              op match { case OpAdd | OpSet => impl.m3add(mm, ent)(mE) }
            }, /*if (op==OpAdd)*/ Some(m.keys zip m.tks) /*else None*/) // XXXX commented out the if expression
          }
        case m@MapDef(name,tp,keys,_) =>
          // val m = me(keys.map(_._2),tp)
          // val s=impl.named(name,true)(manStore(m))
          // impl.collectStore(s)(m)
          // cx = Ctx(cx.ctx0 + (name -> s))
        case _ => sys.error("Unimplemented") // we leave room for other type of events
      }
      impl.unit(())
    }
    cx = null; (name,params,block)
  }

  override def genMap(m:MapDef):String = {
    if (m.keys.size==0) createVarDefinition(m.name, m.tp)+";"
    else {
      impl.codegen.generateNewStore(ctx0(m.name)._1.asInstanceOf[impl.codegen.IR.Sym[_]], true)
    }
  }

  def genAllMaps(maps:Seq[MapDef]) = maps.map(genMap).mkString("\n")
  def createVarDefinition(name: String, tp:Type) = "var "+name+":"+tp.toScala+" = "+tp.zero

	var ctx0 = Map[String,(Rep[_], List[(String,Type)], Type)]()
	override def genPardis(s0:M3.System):(String, String, String, String) = {
    val classLevelMaps = s0.triggers.filter(_.evt match {
      case EvtBatchUpdate(s) => true
      case _ => false
    }).map(_.evt match { //delta relations
      case EvtBatchUpdate(sc) =>
        val name = sc.name
        val schema = s0.sources.filter(x => x.schema.name == name)(0).schema
        val deltaRel = sc.deltaSchema
        val tp = TypeLong
        val keys = schema.fields
        MapDef(deltaRel,tp,keys,null)
      case _ => null
    }) ++
    s0.triggers.flatMap{ t=> //local maps
      t.stmts.filter{
        case MapDef(_,_,_,_) => true
        case _ => false
      }.map{
        case m@MapDef(_,_,_,_) => m
        case _ => null
      }
    } ++
    maps.map{
      case (_,m@MapDef(_,_,_,_)) => m
    } // XXX missing indexes
    ctx0 = classLevelMaps.map{
      case MapDef(name,tp,keys,_) => if (keys.size==0) {
        val m = man(tp)
        val s = impl.named(name,false)(m)
        s.emitted = true
        (name,(s,keys,tp))
      } else {
        val m = me(keys.map(_._2),tp)
        val s=impl.named(name,true)(manStore(m))
        impl.ind(s)(m)
        (name,(/*impl.newSStore()(m)*/s,keys,tp))
      }
    }.toMap // XXX missing indexes
    val (str,ld0,_) = genInternals(s0)
    //TODO: this should be replaced by a specific traversal for completing the slice information
    // s0.triggers.map(super.genTrigger)
    val tsResBlks = s0.triggers.map(genTriggerLMS(_,s0)) // triggers (need to be generated before maps)
    val ts = tsResBlks.map{ case (name,params,b) =>
      impl.emitTrigger(b,name,params)
    }.mkString("\n\n")
    val ms = genAllMaps(classLevelMaps) // maps
    var outStream = new java.io.StringWriter
    var outWriter = new java.io.PrintWriter(outStream)
    // impl.codegen.generateClassArgsDefs(outWriter,Nil)

    impl.codegen.emitDataStructures(outWriter)
    val ds = outStream.toString
    val printInfoDef = "def printMapsInfo() = {}"
    val r=ds+"\n"+ms+"\n"+ts+"\n"+printInfoDef
    (r,str,ld0,consts)
	}
}

class PardisScalaGen(cls:String="Query") extends PardisGen(cls, ScalaExpGen)